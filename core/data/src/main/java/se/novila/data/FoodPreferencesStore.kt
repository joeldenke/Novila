package se.novila.data

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import se.novila.model.MealMemory
import se.novila.model.MealMemoryCodec
import se.novila.recipes.FoodPreferences
import se.novila.recipes.FoodPreferencesCodec
import se.novila.recipes.RecipeBook
import se.novila.recipes.RecipeBookCodec

/**
 * The user's diet, allergens and time for recipe suggestions, the meals they wrote in their own words and starred
 * ([MealMemory], for the quick picks), and their own recipes and the ones they cooked lately ([RecipeBook]), in
 * Preferences DataStore (file `food`), on the device only. One instance for the
 * whole app, so the Nutrition tab suggests other recipes as soon as Settings saves.
 */
@SingleIn(AppScope::class)
class FoodPreferencesStore internal constructor(context: Context, scope: CoroutineScope, cipher: DatastoreCipher) {
    @Inject constructor(context: Context, cipher: DatastoreCipher) : this(context, storeScope(), cipher)

    internal val store = PreferencesStore(context, NAME, scope, cipher)

    /** The saved preferences, the defaults where nothing is saved. Null until the first read is done. */
    val preferences: StateFlow<FoodPreferences?> =
        store.snapshots { FoodPreferencesCodec.decode(it[KEY]) ?: FoodPreferences() }

    fun save(preferences: FoodPreferences) = store.edit { it[KEY] = FoodPreferencesCodec.encode(preferences) }

    /** The meals logged lately and the starred ones. Null until the first read is done. */
    val meals: StateFlow<MealMemory?> = store.snapshots { MealMemoryCodec.decode(it[MEALS]) }

    /** Changes the remembered meals and saves them, in order with other saves. */
    fun updateMeals(change: (MealMemory) -> MealMemory) = store.edit { prefs ->
        prefs[MEALS] = MealMemoryCodec.encode(change(MealMemoryCodec.decode(prefs[MEALS])))
    }

    /** The user's own recipes and what they cooked lately. Null until the first read is done. */
    val recipes: StateFlow<RecipeBook?> = store.snapshots { RecipeBookCodec.decode(it[RECIPES]) }

    /** Changes the recipe book and saves it, in order with other saves. */
    fun updateRecipes(change: (RecipeBook) -> RecipeBook) = store.edit { prefs ->
        prefs[RECIPES] = RecipeBookCodec.encode(change(RecipeBookCodec.decode(prefs[RECIPES])))
    }

    /** Back to the defaults, no meals remembered and no own recipes. */
    fun clear() = store.edit {
        it.remove(KEY)
        it.remove(MEALS)
        it.remove(RECIPES)
    }

    private companion object {
        const val NAME = "food"
        val KEY = stringPreferencesKey("preferences")
        val MEALS = stringPreferencesKey("meals")
        val RECIPES = stringPreferencesKey("recipes")
    }
}
