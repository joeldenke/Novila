package se.novila.feature.today

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
import se.novila.data.HealthStore.SavedDays
import se.novila.model.Achievements
import se.novila.model.Goals
import se.novila.model.achievements
import se.novila.sleep.SleepRepository
import se.novila.sleep.SleepState
import se.novila.sleep.nightsByDate
import se.novila.sleep.withNight
import java.time.LocalDate

/**
 * The weeks in a row and the milestones, opened from Today's insights, scoped to its entry. Every saved day counts,
 * and nights a watch recorded fill in the ones the user didn't log, as on Today.
 */
@ViewModelKey
@ContributesIntoMap(AppScope::class)
@Inject
class AchievementsViewModel(
    private val store: HealthStore,
    private val goalsStore: GoalsStore,
    private val sleepRepository: SleepRepository,
) : ViewModel() {
    private val today = MutableStateFlow(LocalDate.now())

    /** The achievements, or null before the stores' first reads. */
    val state: StateFlow<Achievements?> =
        combine(
            store.days.filterNotNull(),
            goalsStore.goals.filterNotNull(),
            sleepRepository.state,
            today,
            ::read,
        ).stateIn(viewModelScope, SharingStarted.Eagerly, initial())

    private fun initial(): Achievements? {
        val saved = store.days.value ?: return null
        val goals = goalsStore.goals.value ?: return null
        return read(saved, goals, sleepRepository.state.value, today.value)
    }

    private fun read(saved: SavedDays, goals: Goals, watch: SleepState, today: LocalDate): Achievements {
        val nights = watch.nightsByDate
        val days = saved.showing { it.withNight(nights[it.date]) }
        val dates = saved.dates() + nights.keys
        return achievements(dates.map(days::day), today, goals)
    }

    /** Called on resume so "today" stays correct after midnight. */
    fun refresh() {
        today.value = LocalDate.now()
    }
}
