package se.novila.data

import android.content.Context
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import se.novila.model.DayEntry
import se.novila.model.Goals
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class DataExportTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val stores = StoreScope()
    private val today = LocalDate.of(2026, 10, 23)

    @After fun close() = stores.close()

    private fun export(health: HealthStore, goals: GoalsStore, cycle: CycleStore) =
        DataExport(listOf(health.store, goals.store, cycle.store).associateBy { it.name })

    @Test fun `an export imported again gives back the same data, and replaces what was there`() {
        val health = HealthStore(context, stores.scope, stores.cipher)
        val goals = GoalsStore(context, stores.scope, stores.cipher)
        val cycle = CycleStore(context, stores.scope, stores.cipher)
        val data = export(health, goals, cycle)
        health.update(today) { it.withSleep(7.5f) }
        health.update(today.minusDays(400)) { it.withStrengthSession(30) }
        goals.save(Goals(steps = 9000, sleepHours = 7.5f, waterGlasses = 10))
        cycle.setEnabled(true)
        health.days.await { it.day(today).sleepHours == 7.5f && it.day(today.minusDays(400)).strengthMinutes == 30 }
        goals.goals.await { it.steps == 9000 }
        cycle.cycle.await { it.enabled }
        val text = runBlocking { data.export(today, version = 42) }

        // Something changed after the export, and something new: the import puts it back as it was.
        health.update(today) { it.withSleep(5f) }
        health.update(today.minusDays(1)) { it.withSleep(6f) }
        cycle.setEnabled(false)
        health.days.await { it.day(today.minusDays(1)).sleepHours == 6f }

        assertEquals(ImportResult.Imported, data.restore(text, version = 42))

        val days = health.days.await { it.day(today).sleepHours == 7.5f }
        assertEquals(30, days.day(today.minusDays(400)).strengthMinutes)
        assertEquals(DayEntry(today.minusDays(1)), days.day(today.minusDays(1)))
        assertEquals(Goals(steps = 9000, sleepHours = 7.5f, waterGlasses = 10), goals.goals.await())
        assertTrue(cycle.cycle.await { it.enabled }.enabled)
    }

    @Test fun `a file that is not an export changes nothing`() {
        val health = HealthStore(context, stores.scope, stores.cipher)
        val goals = GoalsStore(context, stores.scope, stores.cipher)
        val data = export(health, goals, CycleStore(context, stores.scope, stores.cipher))
        health.update(today) { it.withSleep(7f) }
        health.days.await { it.day(today).sleepHours == 7f }

        fun restore(text: String) = data.restore(text, version = 42)
        assertEquals(ImportResult.Unreadable, restore("not json"))
        assertEquals(ImportResult.Unreadable, restore("""{"app": "Other", "format": 1, "version": 1, "files": {}}"""))
        assertEquals(
            ImportResult.Unreadable,
            restore("""{"app": "Novila", "format": 1, "version": 1, "files": {"goals": {"steps": {"int": "x"}}}}"""),
        )
        // A later file format, or a later build of the app, may hold data this one can't read.
        assertEquals(ImportResult.TooNew, restore("""{"app": "Novila", "format": 99, "version": 1, "files": {}}"""))
        assertEquals(ImportResult.TooNew, restore("""{"app": "Novila", "format": 1, "version": 43, "files": {}}"""))

        // A save after the refused imports still reads the day as it was.
        health.update(today) { it.withMood(3) }
        assertEquals(7f, health.days.await { it.day(today).mood == 3 }.day(today).sleepHours)
    }

    @Test fun `an export from an older build is read, with the values as that build saved them`() {
        val health = HealthStore(context, stores.scope, stores.cipher)
        val goals = GoalsStore(context, stores.scope, stores.cipher)
        val data = export(health, goals, CycleStore(context, stores.scope, stores.cipher))
        // A day as a build from before drinks saved it (v6, two glasses of water in its own field).
        val old = """{"app": "Novila", "format": 1, "version": 3, "files": {"health": {
            "2026-10-20": {"string": "v6;2;7.5;4;1;2;1;0;0;0;;0;;0;;;0;;0;0;"}}}}"""

        assertEquals(ImportResult.Imported, data.restore(old, version = 42))

        val date = LocalDate.of(2026, 10, 20)
        val day = health.days.await { it.day(date).sleepHours > 0f }.day(date)
        assertEquals(7.5f, day.sleepHours)
        assertEquals(2, day.waterGlasses)
    }

    @Test fun `the export keeps each value's type`() {
        val export = ExportCodec.decode(
            """
            {"app": "Novila", "format": 1, "version": 42, "exported": "2026-10-23", "files": {
              "goals": {"steps": {"int": 9000}, "sleep_hours": {"float": 7.5}},
              "cycle": {"enabled": {"boolean": true}, "log": {"string": "x"}},
              "other": {"tags": {"string_set": ["a", "b"]}, "big": {"long": 5000000000}, "share": {"double": 0.25}}
            }}
            """.trimIndent(),
        )
        val again = ExportCodec.decode(ExportCodec.encode(export, today))
        assertEquals(42L, again.version)
        assertEquals(export.files.mapValues { it.value.asMap() }, again.files.mapValues { it.value.asMap() })
    }
}
