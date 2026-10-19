package se.novila.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Guards the platform Auto Backup configuration (there is no AndroidX backup implementation
 * artifact; see docs/backup.md):
 * - Android 12+ reads dataExtractionRules, Android 11 and older read fullBackupContent.
 * - Both must include :core:data's Preferences DataStore directory and the legacy
 *   SharedPreferences files, and nothing else.
 * - The manifest must point at both files and keep backup enabled.
 *
 * Gradle runs unit tests with the module directory as working directory.
 */
class BackupRulesTest {
    private val manifest = File("src/main/AndroidManifest.xml")

    @Test fun `backup is enabled and points at both rule files`() {
        val application = manifest.document().getElementsByTagName("application").item(0) as Element

        assertEquals("true", application.getAttribute("android:allowBackup"))
        assertEquals("@xml/data_extraction_rules", application.getAttribute("android:dataExtractionRules"))
        assertEquals("@xml/backup_rules", application.getAttribute("android:fullBackupContent"))
    }

    @Test fun `Android 12 rules back up DataStore and legacy preferences only`() {
        val rules = File("src/main/res/xml/data_extraction_rules.xml").document()

        for (section in listOf("cloud-backup", "device-transfer")) {
            val rulesForSection = includes(rules, section)
            assertEquals(
                "$section must carry DataStore and legacy SharedPreferences, and nothing else",
                setOf("file:datastore/" to true, "sharedpref:." to true),
                rulesForSection,
            )
        }
    }

    @Test fun `Android 11 and older rules back up DataStore and legacy preferences only`() {
        val rules = File("src/main/res/xml/backup_rules.xml").document()

        assertEquals(
            "fullBackupContent must carry DataStore and legacy SharedPreferences, and nothing else",
            setOf("file:datastore/" to true, "sharedpref:." to true),
            includes(rules, "full-backup-content"),
        )
    }

    /** Every include and exclude as `domain:path` to `isInclude`, so excludes cannot hide in the list. */
    private fun includes(document: org.w3c.dom.Document, tag: String): Set<Pair<String, Boolean>> {
        val section = document.getElementsByTagName(tag)
        assertTrue("Missing <$tag> in the backup rules", section.length > 0)
        val includeNodes = (section.item(0) as Element).getElementsByTagName("include")
        val excludeNodes = (section.item(0) as Element).getElementsByTagName("exclude")
        return buildSet {
            for (index in 0 until includeNodes.length) {
                val element = includeNodes.item(index) as Element
                add("${element.getAttribute("domain")}:${element.getAttribute("path")}" to true)
            }
            for (index in 0 until excludeNodes.length) {
                val element = excludeNodes.item(index) as Element
                add("${element.getAttribute("domain")}:${element.getAttribute("path")}" to false)
            }
        }
    }

    private fun File.document() = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(this)
}
