package se.novila.feature.training

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.zacsweers.metro.Inject
import se.novila.exercise.WorkoutStep
import kotlin.time.Instant
import se.novila.ui.R as UiR

/**
 * The rest timer as a notification, so the user can put the phone down during a rest, a warm-up or a cool-down
 * and still start the next set on time: a silent countdown while the step runs, with the time left drawn by the
 * system, and an alert with the next set when it runs out. The notifications belong to the session: when the
 * ViewModel takes them down ([cancel]), none of them is left behind. Whether Novila may notify is checked each
 * time, never remembered: the user can take it back at any moment.
 */
@Inject
class WorkoutNotifications(private val context: Context) {
    private val manager get() = NotificationManagerCompat.from(context)

    /** A silent countdown for a timed step, counting down on its own until [endsAt]. */
    fun countdown(step: WorkoutStep.Timed, endsAt: Instant) {
        if (!canNotify()) return
        createChannel(
            TIMER_CHANNEL,
            NotificationManagerCompat.IMPORTANCE_LOW,
            R.string.workout_channel_timer,
            R.string.workout_channel_timer_description,
        )
        val notification = NotificationCompat.Builder(context, TIMER_CHANNEL)
            .setSmallIcon(UiR.drawable.ic_training)
            .setContentTitle(context.getString(step.title()))
            .setContentText(step.text())
            .setContentIntent(openApp())
            .setWhen(endsAt.toEpochMilliseconds())
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        notify(TIMER_ID, notification)
    }

    /** The timer ran out on a rest or a warm-up: [next] is the set to do now, with a sound. */
    fun ready(next: WorkoutStep.Work) {
        if (!canNotify()) return
        manager.cancel(TIMER_ID)
        createChannel(
            READY_CHANNEL,
            NotificationManagerCompat.IMPORTANCE_DEFAULT,
            R.string.workout_channel_ready,
            R.string.workout_channel_ready_description,
        )
        val notification = NotificationCompat.Builder(context, READY_CHANNEL)
            .setSmallIcon(UiR.drawable.ic_training)
            .setContentTitle(context.getString(R.string.workout_ready_title))
            .setContentText(next.text())
            .setContentIntent(openApp())
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        notify(READY_ID, notification)
    }

    /** Takes the countdown down: the step it counted is not running anymore. */
    fun cancelCountdown() {
        manager.cancel(TIMER_ID)
    }

    /** Takes both notifications down: the user skipped ahead, finished the session or left it. */
    fun cancel() {
        cancelCountdown()
        manager.cancel(READY_ID)
    }

    private fun WorkoutStep.Timed.title(): Int = when (this) {
        is WorkoutStep.Rest -> R.string.workout_rest_title
        is WorkoutStep.WarmUp -> R.string.workout_warmup_title
        is WorkoutStep.CoolDown -> R.string.workout_cooldown_title
    }

    private fun WorkoutStep.Timed.text(): String = when (this) {
        is WorkoutStep.Rest -> next.text()
        is WorkoutStep.WarmUp -> next.text()
        is WorkoutStep.CoolDown -> context.getString(R.string.workout_cooldown_note)
    }

    /** "Push-ups, set 2 of 3", the same words the screen shows under the countdown. */
    private fun WorkoutStep.Work.text(): String = context.getString(
        R.string.workout_next,
        context.getString(moveText(exercise.exerciseId)?.name ?: R.string.exercise_strength),
        set,
        exercise.sets,
    )

    /** Whether a notification would show: notifications are on for Novila and, on Android 13+, allowed. */
    private fun canNotify(): Boolean = manager.areNotificationsEnabled() && hasPermission()

    private fun hasPermission(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

    private fun notify(id: Int, notification: Notification) {
        try {
            manager.notify(id, notification)
        } catch (e: SecurityException) {
            // Taken back between the check and the call.
            Log.w("WorkoutNotifications", "Not allowed to notify", e)
        }
    }

    private fun createChannel(id: String, importance: Int, name: Int, description: Int) {
        val channel = NotificationChannelCompat.Builder(id, importance)
            .setName(context.getString(name))
            .setDescription(context.getString(description))
            .build()
        manager.createNotificationChannel(channel)
    }

    /** The app's start screen; immutable, so no one else can change where it leads. */
    private fun openApp(): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        return PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private companion object {
        const val TIMER_CHANNEL = "workout_timer"
        const val READY_CHANNEL = "workout_ready"

        /** The reminders already use the first three ids. */
        const val TIMER_ID = 10
        const val READY_ID = 11
        const val NOTIFICATION_ID = 12
    }
}
