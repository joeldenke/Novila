package se.novila.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withGroovyBuilder

/**
 * `novila.android.screenshot`: every `@Preview` in the module becomes a Roborazzi screenshot test on Robolectric
 * (no emulator), on top of `novila.android.unittest`. References live in the module's `src/test/screenshots`;
 * CI records changed ones on a PR whose description contains `/record-screenshots` and pushes them to its branch.
 *
 * Tasks: `recordRoborazziDebug` (write references), `verifyRoborazziDebug` (fail on diff),
 * `verifyAndRecordRoborazziDebug` (fail on diff and write the new reference, what CI runs),
 * `compareRoborazziDebug` (write diff images without failing).
 *
 * Roborazzi is configured through its extension by name, so build-logic needs no compile dependency on the
 * Roborazzi plugin, which may be built with a newer Kotlin than Gradle embeds.
 */
class AndroidScreenshotConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(AndroidUnitTestConventionPlugin::class.java)
            pluginManager.apply("io.github.takahirom.roborazzi")

            val roborazzi = extensions.getByName("roborazzi")
            roborazzi.property<DirectoryProperty>("outputDir").set(layout.projectDirectory.dir("src/test/screenshots"))
            roborazzi.property<Any>("generateComposePreviewRobolectricTests").apply {
                property<Property<Boolean>>("enable").set(true)
                // Only this module's previews: the scanner also sees the previews of modules on the classpath.
                property<ListProperty<String>>("packages").set(listOf(novilaNamespace))
                property<Property<Boolean>>("includePrivatePreviews").set(true)
            }

            dependencies {
                add("testImplementation", libs.library("roborazzi"))
                add("testImplementation", libs.library("roborazzi-compose"))
                add("testImplementation", libs.library("roborazzi-junit-rule"))
                add("testImplementation", libs.library("roborazzi-compose-preview-scanner-support"))
                add("testImplementation", libs.library("composable-preview-scanner"))
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> Any.property(name: String): T = withGroovyBuilder { getProperty(name) } as T
}
