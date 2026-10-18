package se.novila.feature.settings

import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.StateFlow
import se.novila.data.FoodPreferencesStore
import se.novila.recipes.FoodPreferences

/** The food preferences screen. Scoped to its entry. Every change is saved at once, like the other settings. */
@ViewModelKey
@ContributesIntoMap(AppScope::class)
@Inject
class FoodPreferencesViewModel(private val store: FoodPreferencesStore) : ViewModel() {
    /** The preferences, or null before the store's first read. */
    val preferences: StateFlow<FoodPreferences?> = store.preferences

    /** The last preferences saved from here. Saves are written in the background, so the store may not have them. */
    private var lastSaved: FoodPreferences? = null

    /** Changes the preferences saved last, so quick taps never undo each other. */
    fun change(update: (FoodPreferences) -> FoodPreferences) {
        val preferences = update(lastSaved ?: store.preferences.value ?: FoodPreferences())
        lastSaved = preferences
        store.save(preferences)
    }
}
