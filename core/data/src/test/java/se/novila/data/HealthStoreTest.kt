package se.novila.data

import android.content.Context
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import se.novila.model.DayEntry
import se.novila.model.DayEntryCodec
import se.novila.model.FoodGroup
import se.novila.model.HistoryRange
import se.novila.model.averageSleep
import se.novila.model.glassesOfWater
import se.novila.model.portions
import se.novila.model.withPortions
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class HealthStoreTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val stores = StoreScope()
    private val today = LocalDate.of(2026, 10, 16)

    @After fun close() = stores.close()

    @Test fun `days saved in SharedPreferences are moved over`() {
        val legacy = context.getSharedPreferences("health", Context.MODE_PRIVATE)
        val saved = DayEntry(today.minusDays(1), sleepHours = 7.5f, mood = 4, strengthMinutes = 25)
        legacy.edit().putString(saved.date.toString(), DayEntryCodec.encode(saved)).commit()

        val days = HealthStore(context, stores.scope, stores.cipher).days.await()

        assertEquals(saved, days.day(saved.date))
        assertEquals(saved, days.week(today).history.first())
        assertTrue(context.getSharedPreferences("health", Context.MODE_PRIVATE).all.isEmpty())
    }

    @Test fun `an empty day reads as nothing logged`() {
        val days = HealthStore(context, stores.scope, stores.cipher).days.await()

        assertEquals(DayEntry(today), days.day(today))
        assertEquals(6, days.week(today).history.size)
    }

    @Test fun `days older than a week show up in the history`() {
        val store = HealthStore(context, stores.scope, stores.cipher)
        val spring = LocalDate.of(2026, 4, 2)
        store.update(spring) { it.withSleep(8f) }

        val history = store.days.await { it.day(spring).sleepHours > 0f }.history(HistoryRange.Year, today)

        assertEquals(8f, history.periods.single { it.start == LocalDate.of(2026, 4, 1) }.days.averageSleep())
        assertEquals(8f, store.history(HistoryRange.Year, today)?.days?.single { it.date == spring }?.sleepHours)
    }

    @Test fun `saves apply in order, each to the day as the last one left it`() {
        val store = HealthStore(context, stores.scope, stores.cipher)
        repeat(3) { store.update(today) { it.withPortions(FoodGroup.Vegetables, 1) } }
        store.update(today) { it.withSleep(6f) }
        store.update(today) { it.withSleep(8f) }
        store.update(today) { it.withMood(5) }

        val day = store.days.await { it.day(today).mood == 5 }.day(today)

        assertEquals(3, day.portions(FoodGroup.Vegetables))
        assertEquals(8f, day.sleepHours)
    }

    @Test fun `saved days are still there after a restart`() {
        val store = HealthStore(context, stores.scope, stores.cipher)
        store.update(today) { it.withStrengthSession(30) }
        store.days.await { it.day(today).strengthMinutes == 30 }

        stores.restart()

        assertEquals(30, HealthStore(context, stores.scope, stores.cipher).days.await().day(today).strengthMinutes)
    }

    @Test fun `days can be shown changed without saving the change`() {
        val store = HealthStore(context, stores.scope, stores.cipher)
        store.update(today) { it.withMood(3) }
        val saved = store.days.await { it.day(today).mood == 3 }

        val shown = saved.showing { it.withSleep(7f) }.showing { it.copy(drinks = glassesOfWater(2)) }

        assertEquals(7f, shown.day(today).sleepHours)
        assertEquals(2, shown.week(today).history.first().waterGlasses)
        assertEquals(7f, shown.history(HistoryRange.Week, today).days.last().sleepHours)
        assertEquals(DayEntry(today, mood = 3), saved.day(today))
    }

    @Test fun `a snapshot decodes each day once, also for its views`() {
        val store = HealthStore(context, stores.scope, stores.cipher)
        store.update(today) { it.withMood(3) }
        val saved = store.days.await { it.day(today).mood == 3 }
        val shown = saved.showing { it.withSleep(7f) }

        assertSame(saved.day(today), saved.day(today))
        assertSame(saved.day(today), saved.showing { it }.day(today))
        assertSame(shown.day(today), shown.week(today).today)
        assertEquals(DayEntry(today, mood = 3), saved.day(today))
    }

    @Test fun `clearing forgets every day`() {
        val store = HealthStore(context, stores.scope, stores.cipher)
        store.update(today) { it.withSleep(7f) }
        store.update(today.minusDays(3)) { it.withMood(2) }
        store.days.await { it.day(today.minusDays(3)).mood == 2 }

        store.clear()
        val days = store.days.await { it.day(today) == DayEntry(today) }

        assertEquals(DayEntry(today.minusDays(3)), days.day(today.minusDays(3)))
    }
}
