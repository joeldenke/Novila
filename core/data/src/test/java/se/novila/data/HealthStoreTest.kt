package se.novila.data

import android.content.Context
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import se.novila.model.DayEntry
import se.novila.model.DayEntryCodec
import se.novila.model.FoodGroup
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

        val days = HealthStore(context, stores.scope).days.await()

        assertEquals(saved, days.day(saved.date))
        assertEquals(saved, days.week(today).history.first())
        assertTrue(context.getSharedPreferences("health", Context.MODE_PRIVATE).all.isEmpty())
    }

    @Test fun `an empty day reads as nothing logged`() {
        val days = HealthStore(context, stores.scope).days.await()

        assertEquals(DayEntry(today), days.day(today))
        assertEquals(6, days.week(today).history.size)
    }

    @Test fun `saves apply in order, each to the day as the last one left it`() {
        val store = HealthStore(context, stores.scope)
        repeat(3) { store.update(today) { it.withPortions(FoodGroup.Vegetables, 1) } }
        store.update(today) { it.withSleep(6f) }
        store.update(today) { it.withSleep(8f) }
        store.update(today) { it.withMood(5) }

        val day = store.days.await { it.day(today).mood == 5 }.day(today)

        assertEquals(3, day.portions(FoodGroup.Vegetables))
        assertEquals(8f, day.sleepHours)
    }

    @Test fun `saved days are still there after a restart`() {
        val store = HealthStore(context, stores.scope)
        store.update(today) { it.withStrengthSession(30) }
        store.days.await { it.day(today).strengthMinutes == 30 }

        stores.restart()

        assertEquals(30, HealthStore(context, stores.scope).days.await().day(today).strengthMinutes)
    }
}
