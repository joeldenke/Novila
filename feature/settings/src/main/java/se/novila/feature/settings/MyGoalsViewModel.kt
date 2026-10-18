package se.novila.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import se.novila.coach.CoachPreferences
import se.novila.data.CoachStore
import se.novila.data.FoodPreferencesStore
import se.novila.data.GoalsStore
import se.novila.data.SavedCoach
import se.novila.data.SavedSleepCoach
import se.novila.data.SleepCoachStore
import se.novila.model.Goals
import se.novila.recipes.FoodPreferences
import se.novila.sleep.SleepCoachPreferences

/**
 * Everything Novila plans by: the daily [goals], the training [coach]'s and the [sleepCoach]'s answers (null until
 * the user sets that coach up) and the [food] preferences for recipes.
 */
data class MyGoalsUiState(
    val goals: Goals,
    val coach: CoachPreferences?,
    val sleepCoach: SleepCoachPreferences?,
    val food: FoodPreferences,
)

/** My goals, which only shows the answers and leads to where each is changed. Scoped to its entry. */
@ViewModelKey
@ContributesIntoMap(AppScope::class)
@Inject
class MyGoalsViewModel(
    goalsStore: GoalsStore,
    coachStore: CoachStore,
    sleepCoachStore: SleepCoachStore,
    foodPreferencesStore: FoodPreferencesStore,
) : ViewModel() {
    /** The answers, or null before the stores' first reads. */
    val state: StateFlow<MyGoalsUiState?> =
        combine(goalsStore.goals, coachStore.answers, sleepCoachStore.answers, foodPreferencesStore.preferences, ::read)
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                read(
                    goalsStore.goals.value,
                    coachStore.answers.value,
                    sleepCoachStore.answers.value,
                    foodPreferencesStore.preferences.value,
                ),
            )

    private fun read(
        goals: Goals?,
        coach: SavedCoach?,
        sleepCoach: SavedSleepCoach?,
        food: FoodPreferences?,
    ): MyGoalsUiState? {
        if (goals == null || coach == null || sleepCoach == null || food == null) return null
        return MyGoalsUiState(goals, coach.preferences, sleepCoach.preferences, food)
    }
}
