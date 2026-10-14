package se.novila

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Cheap guard rails enforced in CI:
 * - Only the design system may use Material 3 directly; features use Novila components.
 * - UI text comes from string resources, never string literals.
 */
class ArchitectureTest {
    // Gradle runs unit tests with the module directory as working directory.
    private val sources = File("src/main/java/se/novila")
    private val designSystem = File(sources, "designsystem")

    private fun featureFiles(): List<File> = sources.walkTopDown()
        .filter { it.isFile && it.extension == "kt" }
        .filterNot { it.startsWith(designSystem) }
        .toList()

    @Test fun `sources are found`() {
        assertTrue("No sources under ${sources.absolutePath}", featureFiles().isNotEmpty())
    }

    @Test fun `only the design system imports Material 3`() {
        val offenders = featureFiles().filter { file ->
            file.readLines().any { it.trim().startsWith("import androidx.compose.material3") }
        }
        assertTrue("Use se.novila.designsystem instead of Material 3 in: ${offenders.names()}", offenders.isEmpty())
    }

    @Test fun `no hardcoded text in UI code`() {
        val literalText = Regex("""\b(Novila)?Text\(\s*(text\s*=\s*)?"""")
        val offenders = featureFiles().filter { file -> file.readLines().any { literalText.containsMatchIn(it) } }
        assertTrue("Use stringResource(...) instead of string literals in: ${offenders.names()}", offenders.isEmpty())
    }

    private fun List<File>.names() = joinToString { it.relativeTo(sources).path }
}
