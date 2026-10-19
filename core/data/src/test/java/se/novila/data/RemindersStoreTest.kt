package se.novila.data

import android.content.Context
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import se.novila.model.Reminder
import se.novila.model.ReminderSetting
import se.novila.model.Reminders
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
class RemindersStoreTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val stores = StoreScope()

    @After fun close() = stores.close()

    @Test fun `every reminder is off until the user turns it on`() {
        assertEquals(Reminders(), RemindersStore(context, stores.scope).reminders.await())
    }

    @Test fun `a reminder and its time survive a restart`() {
        val store = RemindersStore(context, stores.scope)
        val sleep = ReminderSetting(on = true, time = LocalTime.of(7, 15))
        store.save(Reminder.Sleep, sleep)
        store.reminders.await { it[Reminder.Sleep] == sleep }

        stores.restart()
        val reopened = RemindersStore(context, stores.scope).reminders.await()
        assertEquals(sleep, reopened[Reminder.Sleep])
        assertEquals(Reminders()[Reminder.Training], reopened[Reminder.Training])
    }

    @Test fun `clearing turns every reminder off again`() {
        val store = RemindersStore(context, stores.scope)
        store.save(Reminder.Sleep, ReminderSetting(on = true, time = LocalTime.of(7, 15)))
        store.save(Reminder.Nutrition, ReminderSetting(on = true, time = LocalTime.of(18, 30)))
        store.reminders.await { it[Reminder.Sleep].on && it[Reminder.Nutrition].on }

        store.clear()

        assertEquals(Reminders(), store.reminders.await { it == Reminders() })
    }
}
