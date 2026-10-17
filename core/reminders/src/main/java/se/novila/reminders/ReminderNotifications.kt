package se.novila.reminders

import android.Manifest
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
import se.novila.model.Reminder
import se.novila.ui.R as UiR

/**
 * The reminders' notifications, in their own channel so the user can turn them off in the phone's settings. Whether
 * Novila may notify is checked each time, never remembered: the user can take it back at any moment.
 */
@Inject
class ReminderNotifications(private val context: Context) {
    private val manager get() = NotificationManagerCompat.from(context)

    /** Whether a notification would show: notifications are on for Novila and, on Android 13+, allowed. */
    fun canNotify(): Boolean = manager.areNotificationsEnabled() && hasPermission()

    /** Shows [reminder], opening the app when tapped. Does nothing when Novila may not notify. */
    fun show(reminder: Reminder) {
        if (!manager.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        createChannel()
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(reminder.icon)
            .setContentTitle(context.getString(reminder.title))
            .setContentText(context.getString(reminder.text))
            .setContentIntent(openApp(reminder))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        try {
            manager.notify(reminder.ordinal + 1, notification)
        } catch (e: SecurityException) {
            // Taken back between the check and the call.
            Log.w("ReminderNotifications", "Not allowed to notify", e)
        }
    }

    private fun hasPermission(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

    private fun createChannel() {
        val channel = NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName(context.getString(R.string.reminders_channel))
            .setDescription(context.getString(R.string.reminders_channel_description))
            .build()
        manager.createNotificationChannel(channel)
    }

    /** The app's start screen; immutable, so no one else can change where it leads. */
    private fun openApp(reminder: Reminder): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        return PendingIntent.getActivity(
            context,
            reminder.ordinal,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private companion object {
        const val CHANNEL = "reminders"
    }
}

private val Reminder.icon: Int
    get() = when (this) {
        Reminder.Sleep -> UiR.drawable.ic_sleep
        Reminder.Training -> UiR.drawable.ic_training
        Reminder.Nutrition -> UiR.drawable.ic_nutrition
    }

private val Reminder.title: Int
    get() = when (this) {
        Reminder.Sleep -> R.string.reminder_sleep_title
        Reminder.Training -> R.string.reminder_training_title
        Reminder.Nutrition -> R.string.reminder_nutrition_title
    }

private val Reminder.text: Int
    get() = when (this) {
        Reminder.Sleep -> R.string.reminder_sleep_text
        Reminder.Training -> R.string.reminder_training_text
        Reminder.Nutrition -> R.string.reminder_nutrition_text
    }
