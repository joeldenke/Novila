package se.novila

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Cheap guard rails enforced in CI, over every module's sources:
 * - Only :core:designsystem may use Material 3 directly; everything else uses Novila components.
 *   (Modules get Material 3 transitively because NovilaTheme exposes M3 types, so the compiler
 *   alone can't enforce this.)
 * - UI text comes from string resources, never string literals.
 * - Features never depend on each other: the app connects them through navigation.
 *
 * The sources are declared as inputs of the test task (novila.android.application), so it reruns when they change.
 */
class ArchitectureTest {
    // Gradle runs unit tests with the module directory as working directory.
    private val root = File("..").canonicalFile

    private fun modules(): List<File> = listOf("core", "feature").flatMap { group ->
        File(root, group).listFiles().orEmpty().filter { File(it, "build.gradle.kts").isFile }
    } + File(root, "app")

    private fun sources(): List<File> = modules()
        .filterNot { it.name == "designsystem" }
        .flatMap {
            File(it, "src/main").walkTopDown().filter { file -> file.isFile && file.extension == "kt" }.toList()
        }

    @Test fun `sources are found`() {
        assertTrue("No sources under $root", sources().any { it.path.contains("feature") })
    }

    @Test fun `only the design system imports Material 3`() {
        val offenders = sources().filter { file ->
            file.readLines().any { it.trim().startsWith("import androidx.compose.material3") }
        }
        assertTrue("Use se.novila.designsystem instead of Material 3 in: ${offenders.names()}", offenders.isEmpty())
    }

    @Test fun `no hardcoded text in UI code`() {
        val literalText = Regex("""\b(Novila)?Text\(\s*(text\s*=\s*)?"""")
        val offenders = sources().filter { file -> file.readLines().any { literalText.containsMatchIn(it) } }
        assertTrue("Use stringResource(...) instead of string literals in: ${offenders.names()}", offenders.isEmpty())
    }

    @Test fun `features do not depend on each other`() {
        val offenders = File(root, "feature").listFiles().orEmpty().filter { feature ->
            File(feature, "build.gradle.kts").takeIf { it.isFile }?.readText()?.contains("projects.feature.") == true
        }
        assertTrue("Connect features through navigation in :app instead: ${offenders.names()}", offenders.isEmpty())
    }

    private fun List<File>.names() = joinToString { it.relativeTo(root).path }
}
