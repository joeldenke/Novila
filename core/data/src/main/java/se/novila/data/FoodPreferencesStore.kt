package se.novila.data

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import se.novila.recipes.FoodPreferences
import se.novila.recipes.FoodPreferencesCodec

/**
 * The user's diet, allergens and time for recipe suggestions in Preferences DataStore (file `food`), on the device
 * only. One instance for the whole app, so the Nutrition tab suggests other recipes as soon as Settings saves.
 */
@SingleIn(AppScope::class)
class FoodPreferencesStore internal constructor(context: Context, scope: CoroutineScope) {
    @Inject constructor(context: Context) : this(context, storeScope())

    private val store = PreferencesStore(context, NAME, scope)

    /** The saved preferences, the defaults where nothing is saved. Null until the first read is done. */
    val preferences: StateFlow<FoodPreferences?> =
        store.snapshots { FoodPreferencesCodec.decode(it[KEY]) ?: FoodPreferences() }

    fun save(preferences: FoodPreferences) = store.edit { it[KEY] = FoodPreferencesCodec.encode(preferences) }

    /** Back to the defaults. */
    fun clear() = store.edit { it.remove(KEY) }

    private companion object {
        const val NAME = "food"
        val KEY = stringPreferencesKey("preferences")
    }
}
