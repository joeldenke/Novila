package se.novila.feature.training

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import se.novila.coach.CoachPlanner
import se.novila.coach.CoachPreferences
import se.novila.data.CoachStore
import se.novila.data.HealthStore
import se.novila.data.SavedCoach
import se.novila.exercise.Exercise
import se.novila.exercise.ExerciseType
import se.novila.exercise.TrainingProgramGenerator
import java.time.Duration
import java.time.LocalDate

/**
 * The week's program for the Training tab's ViewModels, from the saved days and the coach's answers. Each ViewModel
 * gets its own; the stores behind it are shared, so a finished session or a changed answer shows up everywhere
 * through [states].
 */
@Inject
class TrainingRepository(private val health: HealthStore, private val coach: CoachStore) {
    /**
     * The program for the week of each date in [dates], read again whenever a day or the coach's answers are saved.
     * [drafts] are answers the user is trying out before setting up the coach.
     */
    fun states(dates: Flow<LocalDate>, drafts: Flow<CoachPreferences?> = flowOf(null)): Flow<TrainingUiState> =
        combine(dates, drafts, health.days.filterNotNull(), coach.answers.filterNotNull()) { date, draft, days, saved ->
            read(days, saved, date, draft)
        }

    /** The program for the week of [today] as saved right now, or null before the stores' first read. */
    fun current(today: LocalDate, draft: CoachPreferences? = null): TrainingUiState? {
        val days = health.days.value ?: return null
        val saved = coach.answers.value ?: return null
        return read(days, saved, today, draft)
    }

    /** The program for the week of [today], once the stores are read. */
    suspend fun read(today: LocalDate): TrainingUiState = states(flowOf(today)).first()

    private fun read(
        saved: HealthStore.SavedDays,
        answers: SavedCoach,
        today: LocalDate,
        draft: CoachPreferences?,
    ): TrainingUiState {
        // Strength sessions done in session mode are the history so far. No ExerciseSource is connected
        // yet (Google Health API needs sign-in first); once one is, its workouts join these.
        val days = saved.lastDays(count = HISTORY_DAYS, today = today)
        val strength = days
            .filter { it.strengthMinutes > 0 }
            .map { Exercise(ExerciseType.Strength, it.date, Duration.ofMinutes(it.strengthMinutes.toLong())) }
        val active = answers.preferences
        val preferences = active ?: draft ?: CoachPreferences()
        val plan = CoachPlanner.plan(preferences, history = strength, today = today)
        val program = if (active != null) plan.program else TrainingProgramGenerator.generate(strength, today)
        return TrainingUiState(
            program = program,
            coach = CoachUiState(preferences, plan, active = active != null),
            today = today.dayOfWeek,
            strengthMinutesToday = days.first().strengthMinutes,
        )
    }

    private companion object {
        /** The program looks at four whole weeks before this one; five weeks covers them on any weekday. */
        const val HISTORY_DAYS = 35
    }
}
