package se.novila.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import se.novila.model.Profile

/**
 * The user's profile in Preferences DataStore (file `profile`), on the device only. There is no account: the profile
 * is whoever holds the phone. One instance for the whole app, so the greeting on Today changes as soon as the
 * profile screen saves.
 */
@SingleIn(AppScope::class)
class ProfileStore internal constructor(context: Context, scope: CoroutineScope) {
    @Inject constructor(context: Context) : this(context, storeScope())

    private val store = PreferencesStore(context, NAME, scope)

    /** The saved profile, kept in memory. Null until the first read is done. */
    val profile: StateFlow<Profile?> = store.snapshots {
        Profile(
            name = it[NAME_KEY].orEmpty(),
            birthYear = it[BIRTH_YEAR],
            heightCm = it[HEIGHT_CM],
            weightKg = it[WEIGHT_KG],
        )
    }

    /** Saves [profile] in the background, in order with other saves. Empty fields are removed from the file. */
    fun save(profile: Profile) = store.edit {
        it.put(NAME_KEY, profile.name.ifEmpty { null })
        it.put(BIRTH_YEAR, profile.birthYear)
        it.put(HEIGHT_CM, profile.heightCm)
        it.put(WEIGHT_KG, profile.weightKg)
    }

    fun clear() = store.edit { it.clear() }

    private fun <T> MutablePreferences.put(key: Preferences.Key<T>, value: T?) {
        if (value == null) remove(key) else set(key, value)
    }

    private companion object {
        const val NAME = "profile"
        val NAME_KEY = stringPreferencesKey("name")
        val BIRTH_YEAR = intPreferencesKey("birth_year")
        val HEIGHT_CM = intPreferencesKey("height_cm")
        val WEIGHT_KG = intPreferencesKey("weight_kg")
    }
}
