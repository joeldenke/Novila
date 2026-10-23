package se.novila

import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

/**
 * The shortcuts the launcher shows on a long press of the app icon. Each opens [MainActivity] with its [action]:
 * starting today's session opens session mode, and logging water logs a glass and shows Nutrition.
 */
enum class AppShortcut(
    val id: String,
    val action: String,
    @StringRes val shortLabel: Int,
    @StringRes val longLabel: Int,
    @DrawableRes val icon: Int,
) {
    StartSession(
        id = "start_session",
        action = "se.novila.action.START_SESSION",
        shortLabel = R.string.shortcut_start_session,
        longLabel = R.string.shortcut_start_session_long,
        icon = R.drawable.ic_shortcut_training,
    ),
    LogWater(
        id = "log_water",
        action = "se.novila.action.LOG_WATER",
        shortLabel = R.string.shortcut_log_water,
        longLabel = R.string.shortcut_log_water_long,
        icon = R.drawable.ic_shortcut_water,
    ),
    ;

    companion object {
        /**
         * The shortcut [intent] comes from, or null when the app was opened some other way. The recent apps screen
         * reopens the app with the intent it was first started with, so a shortcut used then is not used again: one
         * tap on "Log a glass of water" logs one glass.
         */
        fun of(intent: Intent?): AppShortcut? {
            if (intent == null || (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) return null
            return entries.firstOrNull { it.action == intent.action }
        }

        /**
         * Publishes the shortcuts as dynamic ones, so they always open this build's [MainActivity] (a static shortcut
         * names the package, which differs between the debug and release builds). Called when the app opens, which
         * also gives them the current language.
         */
        fun publish(context: Context) {
            val shortcuts = entries.map { shortcut ->
                ShortcutInfoCompat.Builder(context, shortcut.id)
                    .setShortLabel(context.getString(shortcut.shortLabel))
                    .setLongLabel(context.getString(shortcut.longLabel))
                    .setIcon(IconCompat.createWithResource(context, shortcut.icon))
                    .setIntent(Intent(context, MainActivity::class.java).setAction(shortcut.action))
                    .build()
            }
            ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
        }
    }
}
