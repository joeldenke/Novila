package se.novila.healthconnect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The manifest declares exactly the Health Connect permissions the code asks for, plus what Health
 * Connect needs to show its dialog. More permissions mean a bigger Play declaration, so adding one
 * should fail here first.
 */
class HealthConnectManifestTest {
    // Gradle runs unit tests with the module directory as working directory.
    private val manifest = DocumentBuilderFactory.newInstance()
        .apply { isNamespaceAware = true }
        .newDocumentBuilder()
        .parse(File("src/main/AndroidManifest.xml"))

    private fun elements(tag: String): List<Element> = manifest.getElementsByTagName(tag).let { nodes ->
        (0 until nodes.length).map { nodes.item(it) as Element }
    }

    private fun Element.android(name: String) = getAttributeNS(ANDROID, name)

    @Test fun `health permissions match the code`() {
        val declared = elements("uses-permission").map { it.android("name") }
            .filter { it.startsWith("android.permission.health.") }
            .toSet()
        assertEquals(HEALTH_PERMISSIONS, declared)
    }

    @Test fun `no writing, background or history access`() {
        val declared = elements("uses-permission").map { it.android("name") }
        val tooMuch = declared.filter {
            it.startsWith("android.permission.health.WRITE_") ||
                it == "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND" ||
                it == "android.permission.health.READ_HEALTH_DATA_HISTORY"
        }
        assertTrue("Not allowed without the owner's approval: $tooMuch", tooMuch.isEmpty())
    }

    @Test fun `Health Connect can show the privacy screen`() {
        val actions = elements("action").map { it.android("name") }
        assertTrue(actions.contains("androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE"))
        assertTrue(actions.contains("android.intent.action.VIEW_PERMISSION_USAGE"))
        val alias = elements("activity-alias").single()
        assertEquals("android.permission.START_VIEW_PERMISSION_USAGE", alias.android("permission"))
    }

    private companion object {
        const val ANDROID = "http://schemas.android.com/apk/res/android"
    }
}
