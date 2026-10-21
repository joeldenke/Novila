package se.novila.feature.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.novila.data.FoodPreferencesStore
import se.novila.recipes.OwnRecipeDraft
import se.novila.recipes.RecipeBook

/** The recipe being written, and whether it is a [new] one (it has not been saved yet). */
data class OwnRecipeUiState(val draft: OwnRecipeDraft, val new: Boolean)

/**
 * Writes a recipe of the user's own, or changes the one with [recipeId] (0 for a new one), scoped to its entry. The
 * draft lives here until it is saved, so it survives the keyboard and rotation.
 */
@AssistedInject
class OwnRecipeViewModel(
    @Assisted private val recipeId: Int,
    private val store: FoodPreferencesStore,
) : ViewModel() {
    private val draft = MutableStateFlow(store.recipes.value?.let(::start))

    /** The draft, or null before the recipes are read. */
    val state: StateFlow<OwnRecipeUiState?> = draft.asStateFlow()

    init {
        if (draft.value == null) {
            viewModelScope.launch {
                val book = store.recipes.filterNotNull().first()
                draft.update { it ?: start(book) }
            }
        }
    }

    private fun start(book: RecipeBook): OwnRecipeUiState {
        val saved = book.recipe(recipeId)
        return OwnRecipeUiState(saved?.let(OwnRecipeDraft::of) ?: OwnRecipeDraft(), new = saved == null)
    }

    /** Makes [change] to the draft. */
    fun change(change: (OwnRecipeDraft) -> OwnRecipeDraft) = draft.update { it?.copy(draft = change(it.draft)) }

    /** Saves the draft in the recipe book, when it can be saved. */
    fun save() {
        val recipe = draft.value?.draft?.takeIf { it.canSave }?.toRecipe() ?: return
        store.updateRecipes { it.saved(recipe) }
    }

    /** Deletes the recipe from the book, together with what it remembers of when it was cooked. */
    fun delete() = store.updateRecipes { it.removed(recipeId) }

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(@Assisted recipeId: Int): OwnRecipeViewModel
    }
}
