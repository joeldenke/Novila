package se.novila.feature.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import se.novila.data.GoalsStore
import se.novila.data.HealthStore
import se.novila.data.SleepCoachStore
import se.novila.model.Goals
import se.novila.sleep.COACH_NIGHTS
import se.novila.sleep.SleepCoachPlan
import se.novila.sleep.SleepCoachPreferences
import se.novila.sleep.sleepCoachPlan
import java.time.LocalDate
import java.time.LocalTime

/**
 * The sleep coach screen: the answers, the plan they give from the user's own nights. Scoped to the coach entry,
 * so answers tried out before the coach is set up are forgotten when the user leaves it.
 */
data class SleepCoachUiState(
    val preferences: SleepCoachPreferences,
    val active: Boolean,
    val plan: SleepCoachPlan,
)

/** The Sleep coach, scoped to its entry. */
@ViewModelKey
@ContributesIntoMap(AppScope::class)
@Inject
class SleepCoachViewModel(
    private val store: HealthStore,
    private val goalsStore: GoalsStore,
    private val coachStore: SleepCoachStore,
) : ViewModel() {
    private val today = MutableStateFlow(LocalDate.now())

    /** Answers the user is trying out before the coach is set up. Not saved until they start the coach. */
    private val draft = MutableStateFlow<SleepCoachPreferences?>(null)

    /** The answers and their plan, or null before the stores' first read. */
    val state: StateFlow<SleepCoachUiState?> =
        combine(
            combine(store.days.filterNotNull(), goalsStore.goals.filterNotNull(), ::Pair),
            coachStore.answers.filterNotNull(),
            draft,
            today,
        ) { saved, savedCoach, draft, today ->
            val preferences = draft ?: savedCoach.preferences ?: SleepCoachPreferences()
            read(saved.first, saved.second, preferences, savedCoach.preferences != null, today)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, initial())

    private fun initial(): SleepCoachUiState? {
        val days = store.days.value ?: return null
        val goals = goalsStore.goals.value ?: return null
        val saved = coachStore.answers.value ?: return null
        val preferences = draft.value ?: saved.preferences ?: SleepCoachPreferences()
        return read(days, goals, preferences, saved.preferences != null, today.value)
    }

    /**
     * The plan from the last [COACH_NIGHTS] of days, ending on today whatever day is shown: the coach looks at the
     * nights as they are, not at an earlier day the user went back to on the Sleep tab.
     */
    private fun read(
        days: HealthStore.SavedDays,
        goals: Goals,
        preferences: SleepCoachPreferences,
        active: Boolean,
        today: LocalDate,
    ) = SleepCoachUiState(
        preferences = preferences,
        active = active,
        plan = sleepCoachPlan(days.lastDays(COACH_NIGHTS, today), preferences, goals, LocalTime.now()),
    )

    /** Called on resume so "today" and the evening's next step stay correct after midnight. */
    fun refresh() {
        today.value = LocalDate.now()
    }

    /**
     * A changed answer, shown right away. Once the coach is set up it is also saved; before that it is only tried
     * out here and forgotten if the user leaves without starting the coach.
     */
    fun change(preferences: SleepCoachPreferences) {
        draft.value = preferences
        if (state.value?.active == true) coachStore.save(preferences)
    }

    /** Saves the answers, which sets the coach up. */
    fun start() {
        state.value?.let { coachStore.save(it.preferences) }
    }

    /** Back to the standard advice, without the coach. */
    fun reset() {
        coachStore.clear()
        draft.value = null
    }
}
