package se.novila.feature.training

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import se.novila.exercise.Dosage
import se.novila.exercise.PlannedExercise
import se.novila.exercise.Workout
import se.novila.exercise.WorkoutStep
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@RunWith(RobolectricTestRunner::class)
class WorkoutNotificationsTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val manager: NotificationManager = context.getSystemService(NotificationManager::class.java)
    private val notifications = WorkoutNotifications(context)
    private val pushUp = PlannedExercise(
        "push_up",
        sets = 3,
        amount = 8,
        dosage = Dosage.Repetitions,
        perSide = false,
        rest = 60.seconds,
    )
    private val next = WorkoutStep.Work(pushUp, exerciseNumber = 1, set = 2)
    private val endsAt = Instant.fromEpochSeconds(2_000)

    @Before
    fun allowNotifications() {
        shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    @Test fun `a rest counts down in a notification until it is taken down`() {
        notifications.countdown(WorkoutStep.Rest(60.seconds, next), endsAt)
        assertEquals(1, posted())
        assertEquals("Rest", title(TIMER_ID))
        assertEquals("Next: Push-up, set 2 of 3", text(TIMER_ID))
        notifications.cancel()
        assertEquals(0, posted())
    }

    @Test fun `the warm-up and the cool-down get their own words`() {
        notifications.countdown(WorkoutStep.WarmUp(Workout.WARM_UP, next), endsAt)
        assertEquals("Warm up", title(TIMER_ID))
        notifications.countdown(WorkoutStep.CoolDown(Workout.COOL_DOWN), endsAt)
        assertEquals(1, posted())
        assertEquals("Cool down", title(TIMER_ID))
        assertEquals("Almost done with the session", text(TIMER_ID))
    }

    @Test fun `when a rest runs out the timer is replaced by the next set`() {
        notifications.countdown(WorkoutStep.Rest(60.seconds, next), endsAt)
        notifications.ready(next)
        assertEquals(1, posted())
        assertNull("the countdown is gone", shadowOf(manager).getNotification(TIMER_ID))
        assertEquals("Ready for the next set", title(READY_ID))
        assertEquals("Next: Push-up, set 2 of 3", text(READY_ID))
        notifications.cancel()
        assertEquals(0, posted())
    }

    @Test fun `nothing is posted when Novila may not notify`() {
        shadowOf(context as Application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        notifications.countdown(WorkoutStep.Rest(60.seconds, next), endsAt)
        notifications.ready(next)
        assertEquals(0, posted())
    }

    private fun posted(): Int = shadowOf(manager).allNotifications.size

    private fun title(id: Int): CharSequence? = extras(id)?.getCharSequence(Notification.EXTRA_TITLE)

    private fun text(id: Int): CharSequence? = extras(id)?.getCharSequence(Notification.EXTRA_TEXT)

    private fun extras(id: Int): Bundle? = shadowOf(manager).getNotification(id)?.extras

    private companion object {
        const val TIMER_ID = 10
        const val READY_ID = 11
    }
}
