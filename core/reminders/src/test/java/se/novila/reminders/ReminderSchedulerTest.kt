package se.novila.reminders

import android.content.Context
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import se.novila.model.Reminder
import se.novila.model.ReminderSetting
import java.time.LocalDateTime
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
class ReminderSchedulerTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val now = LocalDateTime.of(2026, 10, 17, 7, 0)

    @Before fun setUp() {
        val configuration = Configuration.Builder().setExecutor(SynchronousExecutor()).build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, configuration)
    }

    private fun work(reminder: Reminder): List<WorkInfo> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWork(ReminderScheduler.workName(reminder)).get()

    @Test fun `a reminder that is on waits for its time, one piece of work per reminder`() {
        val scheduler = ReminderScheduler(context)
        scheduler.apply(Reminder.Sleep, ReminderSetting(on = true, time = LocalTime.of(8, 30)), now)
        scheduler.apply(Reminder.Sleep, ReminderSetting(on = true, time = LocalTime.of(9, 0)), now)

        val waiting = work(Reminder.Sleep).filter { it.state == WorkInfo.State.ENQUEUED }
        assertEquals(1, waiting.size)
        assertTrue(ReminderScheduler.TAG in waiting.single().tags)
        assertTrue(work(Reminder.Nutrition).isEmpty())
    }

    @Test fun `turning a reminder off cancels it`() {
        val scheduler = ReminderScheduler(context)
        val time = LocalTime.of(19, 30)
        scheduler.apply(Reminder.Nutrition, ReminderSetting(on = true, time = time), now)
        scheduler.apply(Reminder.Nutrition, ReminderSetting(on = false, time = time), now)

        assertTrue(work(Reminder.Nutrition).all { it.state == WorkInfo.State.CANCELLED })
    }

    @Test fun `cancelling all stops every planned reminder`() {
        val scheduler = ReminderScheduler(context)
        scheduler.apply(Reminder.Sleep, ReminderSetting(on = true, time = LocalTime.of(7, 30)), now)
        scheduler.apply(Reminder.Training, ReminderSetting(on = true, time = LocalTime.of(17, 0)), now)
        scheduler.apply(Reminder.Nutrition, ReminderSetting(on = true, time = LocalTime.of(19, 30)), now)

        scheduler.cancelAll()

        val planned = WorkManager.getInstance(context).getWorkInfosByTag(ReminderScheduler.TAG).get()
        assertEquals(3, planned.size)
        assertTrue(planned.all { it.state == WorkInfo.State.CANCELLED })
    }
}
