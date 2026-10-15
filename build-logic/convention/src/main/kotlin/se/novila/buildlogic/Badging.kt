package se.novila.buildlogic

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.BuiltArtifactsLoader
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import org.w3c.dom.Element
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Badging: what the built APK tells Android and Google Play about itself, kept in a reviewed file.
 *
 * For each variant:
 * - `generate<Variant>Badging` runs `aapt2 dump badging` on the APK (package, SDK levels, permissions,
 *   implied features, launcher, locales) and adds what badging leaves out but Play and security
 *   reviews care about: exported components and package queries from the merged manifest.
 * - `check<Variant>Badging` compares it with `badging/<variant>-badging.txt` and fails on any change.
 * - `update<Variant>Badging` copies the new output there.
 *
 * Libraries can add permissions or components to the merged manifest without touching our own
 * AndroidManifest.xml, so this catches them in the PR diff instead of after release.
 * Same idea as Now in Android's badging tasks.
 */
internal fun Project.configureBadging(components: ApplicationAndroidComponentsExtension) {
    val sdkDirectory = components.sdkComponents.sdkDirectory
    components.onVariants { variant ->
        val name = variant.name.replaceFirstChar { it.uppercase() }
        val generate = tasks.register<GenerateBadgingTask>("generate${name}Badging") {
            apk.set(variant.artifacts.get(SingleArtifact.APK))
            apkLoader.set(variant.artifacts.getBuiltArtifactsLoader())
            mergedManifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
            sdk.set(sdkDirectory)
            badging.set(layout.buildDirectory.file("outputs/badging/${variant.name}-badging.txt"))
        }
        val goldenDir = layout.projectDirectory.dir("badging")
        val goldenName = "${variant.name}-badging.txt"
        val update = tasks.register<Copy>("update${name}Badging") {
            description = "Saves the ${variant.name} APK's badging as the reviewed reference."
            from(generate.flatMap { it.badging })
            into(goldenDir)
            rename { goldenName }
        }
        tasks.register<CheckBadgingTask>("check${name}Badging") {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description = "Fails when the ${variant.name} APK's badging differs from badging/$goldenName."
            golden.from(goldenDir.file(goldenName))
            generated.set(generate.flatMap { it.badging })
            updateTask.set(update.map { it.path })
        }
    }
}

@DisableCachingByDefault(because = "Runs aapt2 from the local SDK; fast enough to rerun.")
abstract class GenerateBadgingTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val apk: DirectoryProperty

    @get:Internal
    abstract val apkLoader: Property<BuiltArtifactsLoader>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val mergedManifest: RegularFileProperty

    @get:Internal
    abstract val sdk: DirectoryProperty

    @get:OutputFile
    abstract val badging: RegularFileProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun generate() {
        val artifacts = apkLoader.get().load(apk.get()) ?: throw GradleException("No APK in ${apk.get()}")
        val apkFile = artifacts.elements.single().outputFile
        val aapt2 = findAapt2(sdk.get().asFile)
        val out = ByteArrayOutputStream()
        execOperations.exec {
            commandLine(aapt2.absolutePath, "dump", "badging", apkFile)
            standardOutput = out
        }
        val manifest = manifestSummary(mergedManifest.get().asFile)
        // Trailing whitespace is trimmed so the file survives editors and copy-paste from CI annotations.
        val dump = out.toString(Charsets.UTF_8.name()).lines().map { it.trimEnd() }.filter { it.isNotEmpty() }
        badging.get().asFile.writeText(dump.joinToString("") { "$it\n" } + manifest)
    }
}

@DisableCachingByDefault(because = "A verification with no outputs.")
abstract class CheckBadgingTask : DefaultTask() {
    /** The reviewed file. A collection, so a missing file is reported by this task, not by Gradle. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val golden: ConfigurableFileCollection

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val generated: RegularFileProperty

    @get:Input
    abstract val updateTask: Property<String>

    @TaskAction
    fun check() {
        val goldenFile = golden.singleFile
        val actual = generated.get().asFile.readLines()
        val fix = "If the change is intended, run ./gradlew ${updateTask.get()} " +
            "(or comment /record-badging on the PR) and commit ${goldenFile.name}."
        if (!goldenFile.exists()) {
            throw GradleException(
                "${goldenFile.name} is missing. $fix\nGenerated:\n" + actual.joinToString("\n"),
            )
        }
        val expected = goldenFile.readLines()
        if (expected == actual) return
        val removed = expected.filterNot { it in actual }.map { "- $it" }
        val added = actual.filterNot { it in expected }.map { "+ $it" }
        val diff = (removed + added).ifEmpty { listOf("(same lines, different order)") }
        throw GradleException(
            "The APK's badging changed (permissions, SDK levels, features or exported components). $fix\n" +
                diff.joinToString("\n"),
        )
    }
}

/** aapt2 from the newest installed build-tools. Badging output is stable between versions. */
private fun findAapt2(sdk: File): File {
    fun version(dir: File) = dir.name.split('.', '-').map { it.toIntOrNull() ?: -1 }
    val versions = Comparator<List<Int>> { a, b ->
        a.zip(b).map { (x, y) -> x.compareTo(y) }.firstOrNull { it != 0 } ?: a.size.compareTo(b.size)
    }
    return File(sdk, "build-tools").listFiles().orEmpty()
        .filter { File(it, "aapt2").canExecute() }
        .maxWithOrNull(compareBy(versions) { version(it) })
        ?.let { File(it, "aapt2") }
        ?: throw GradleException("No aapt2 in $sdk/build-tools. Install build-tools with sdkmanager.")
}

/**
 * Exported components (with the permission that guards them) and package queries from the merged
 * manifest, in manifest order. Badging only shows the launcher activity.
 */
private fun manifestSummary(mergedManifest: File): String {
    val android = "http://schemas.android.com/apk/res/android"
    val document = DocumentBuilderFactory.newInstance()
        .apply { isNamespaceAware = true }
        .newDocumentBuilder()
        .parse(mergedManifest)
    val lines = mutableListOf<String>()
    fun elements(tag: String): List<Element> = document.getElementsByTagName(tag).let { nodes ->
        (0 until nodes.length).map { nodes.item(it) as Element }
    }
    for (tag in listOf("activity", "activity-alias", "service", "receiver", "provider")) {
        elements(tag).filter { it.getAttributeNS(android, "exported") == "true" }.forEach {
            val permission = it.getAttributeNS(android, "permission").ifEmpty { "none" }
            lines += "exported-$tag: name='${it.getAttributeNS(android, "name")}' permission='$permission'"
        }
    }
    elements("queries").forEach { queries ->
        queries.getElementsByTagName("package").let { nodes ->
            (0 until nodes.length).forEach {
                lines += "queries-package: '${(nodes.item(it) as Element).getAttributeNS(android, "name")}'"
            }
        }
        queries.getElementsByTagName("action").let { nodes ->
            (0 until nodes.length).forEach {
                lines += "queries-intent-action: '${(nodes.item(it) as Element).getAttributeNS(android, "name")}'"
            }
        }
        queries.getElementsByTagName("provider").let { nodes ->
            (0 until nodes.length).forEach {
                lines += "queries-provider: '${(nodes.item(it) as Element).getAttributeNS(android, "authorities")}'"
            }
        }
    }
    return lines.joinToString("") { "$it\n" }
}
