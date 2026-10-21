package se.novila.feature.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import se.novila.data.GoalsStore
import se.novila.data.HealthStore
import se.novila.data.HealthStore.SavedDays
import se.novila.data.SavedSleepCoach
import se.novila.data.SelectedDay
import se.novila.data.SleepCoachStore
import se.novila.model.DayEntry
import se.novila.model.Goals
import se.novila.model.History
import se.novila.model.HistoryRange
import se.novila.model.SleepFactor
import se.novila.sleep.COACH_NIGHTS
import se.novila.sleep.SleepCoachPreferences
import se.novila.sleep.SleepRepository
import se.novila.sleep.SleepState
import se.novila.sleep.hasOwnSleep
import se.novila.sleep.nightsByDate
import se.novila.sleep.sleepCoachPlan
import se.novila.sleep.withNight
import java.time.LocalDate
import java.time.LocalTime

/**
 * The [history] over a week up to the shown day (today, or an earlier one the user went back to), [today] and the
 * user's [goals]. Nights a watch recorded
 * ([watch]) fill in the ones the user didn't log; [fromWatch] when the shown day's night is one of them. [coach] is
 * the sleep coach in short: whether it is set up, its goals, this week and tonight's next step.
 */
data class SleepUiState(
    val history: History,
    val goals: Goals,
    val today: LocalDate,
    val coach: SleepCoachCardState,
    val watch: SleepState = SleepState.Unavailable,
    val fromWatch: Boolean = false,
) {
    /** The shown day, the last in the history. */
    val day: DayEntry get() = history.days.last()
}

/** The Sleep tab, scoped to its entry. */
@ViewModelKey
@ContributesIntoMap(AppScope::class)
@Inject
class SleepViewModel(
    private val store: HealthStore,
    private val goalsStore: GoalsStore,
    private val sleepRepository: SleepRepository,
    private val coachStore: SleepCoachStore,
) : ViewModel() {
    private val day = SelectedDay()

    /** The history, the goals and the coach, or null before the stores' first reads. */
    val state: StateFlow<SleepUiState?> =
        combine(
            combine(
                combine(store.days.filterNotNull(), sleepRepository.state, ::Saved),
                goalsStore.goals.filterNotNull(),
                day.today,
                day.dates,
            ) { saved, goals, today, date -> Inputs(saved, goals, today, date) },
            coachStore.answers.filterNotNull(),
        ) { inputs, coach -> read(inputs, coach) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, initial())

    /** The saved days and the nights a watch recorded, read together. */
    private data class Saved(val days: SavedDays, val watch: SleepState)

    /** Everything read except the sleep coach's answers, which arrive with them separately. */
    private data class Inputs(
        val saved: Saved,
        val goals: Goals,
        val today: LocalDate,
        val date: LocalDate,
    )

    private fun initial(): SleepUiState? {
        val inputs = run {
            val saved = store.days.value ?: return null
            val goals = goalsStore.goals.value ?: return null
            Inputs(Saved(saved, sleepRepository.state.value), goals, day.today.value, day.value)
        }
        val coach = coachStore.answers.value ?: return null
        return read(inputs, coach)
    }

    private fun read(inputs: Inputs, coach: SavedSleepCoach): SleepUiState {
        val (saved, goals, today, date) = inputs
        val nights = saved.watch.nightsByDate
        val shown = saved.days.showing { it.withNight(nights[it.date]) }
        return SleepUiState(
            history = shown.history(HistoryRange.Week, date),
            goals = goals,
            today = today,
            coach = coachState(shown, goals, today, coach),
            watch = saved.watch,
            fromWatch = date in nights && !saved.days.day(date).hasOwnSleep,
        )
    }

    /** The sleep coach in short from the last nights, ending today whatever day is shown. */
    private fun coachState(
        days: SavedDays,
        goals: Goals,
        today: LocalDate,
        coach: SavedSleepCoach,
    ): SleepCoachCardState {
        val preferences = coach.preferences ?: SleepCoachPreferences()
        val plan = sleepCoachPlan(days.lastDays(COACH_NIGHTS, today), preferences, goals, LocalTime.now())
        return SleepCoachCardState(
            active = coach.preferences != null,
            bedtime = preferences.bedtime,
            wakeTime = preferences.wakeTime,
            week = plan.week,
            tonight = plan.tonight,
            hasSteps = plan.hasSteps,
        )
    }

    /**
     * Called on resume so "today" stays correct after midnight. Health Connect only allows reads while the app is in
     * the foreground, so the watch's nights are read here too.
     */
    fun refresh() {
        day.refresh()
        viewModelScope.launch { sleepRepository.refresh(day.today.value) }
    }

    /** Result of Health Connect's permission dialog: whether the user gave access to anything. */
    fun onHealthPermissionsResult(granted: Boolean) {
        viewModelScope.launch { sleepRepository.onPermissionResult(granted, day.today.value) }
    }

    /** Shows and changes [date] instead, to fill in a night that was forgotten. */
    fun selectDay(date: LocalDate) = day.select(date)

    /** Sets the shown day's sleep in hours, without times. */
    fun setSleep(hours: Float) = store.update(day.value) { it.withSleep(hours) }

    /** Sets when the user went to bed; the wake time is the watch's, or the usual one, until they give it. */
    fun setBedtime(time: LocalTime) {
        val watch = watchNight()
        store.update(day.value) { it.withSleepTimes(time, it.wakeTime ?: watch?.wakeTime ?: DefaultWakeTime) }
    }

    /** Sets when the user woke up; the bedtime is the watch's, or the usual one, until they give it. */
    fun setWakeTime(time: LocalTime) {
        val watch = watchNight()
        store.update(day.value) { it.withSleepTimes(it.bedtime ?: watch?.bedtime ?: DefaultBedtime, time) }
    }

    /** The night a watch recorded for the shown day, which the user is now making their own. */
    private fun watchNight() = sleepRepository.state.value.nightsByDate[day.value]

    fun setQuality(quality: Int) = store.update(day.value) { it.withSleepQuality(quality) }

    fun setFactor(factor: SleepFactor, on: Boolean) = store.update(day.value) { it.withSleepFactor(factor, on) }
}
