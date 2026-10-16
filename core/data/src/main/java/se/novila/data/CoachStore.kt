package se.novila.data

import android.content.Context
import se.novila.coach.CoachPreferences
import se.novila.coach.CoachPreferencesCodec

/** The coach's answers in SharedPreferences, on the device only. Null until the user sets up the coach. */
class CoachStore(context: Context) {
    private val prefs = context.getSharedPreferences("coach", Context.MODE_PRIVATE)

    fun load(): CoachPreferences? = CoachPreferencesCodec.decode(prefs.getString(KEY, null))

    fun save(preferences: CoachPreferences) {
        prefs.edit().putString(KEY, CoachPreferencesCodec.encode(preferences)).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    private companion object {
        const val KEY = "preferences"
    }
}
