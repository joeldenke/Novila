package se.novila.data

import android.content.Context

/**
 * The SharedPreferences files each store's data lived in before the move to DataStore. Kept in one place so
 * "delete all data" can remove them even for a store the user never opened.
 */
private val LEGACY_PREFERENCES =
    listOf("health", "coach", "sleep_coach", "settings", "profile", "goals", "reminders", "measurements", "food")

/**
 * Removes the legacy SharedPreferences files, if any are left.
 * [androidx.datastore.preferences.SharedPreferencesMigration] deletes each one the first time its store reads, but a
 * store the user never opened can leave its file on disk; deleting all data takes them all.
 *
 * The DataStore files themselves are emptied by each store's `clear()`, and the Tink keyset is deliberately kept:
 * it holds no data, it never leaves the device (it is sealed with the Android Keystore and excluded from backups),
 * and it is what the settings that survive a delete are still sealed with.
 */
fun deleteLegacyPreferences(context: Context) {
    for (name in LEGACY_PREFERENCES) {
        context.deleteSharedPreferences(name)
    }
}
