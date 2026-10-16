package se.novila.feature.training

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import se.novila.coach.CoachPlanner
import se.novila.coach.CoachPreferences
import se.novila.data.CoachStore
import se.novila.data.HealthStore
import se.novila.exercise.Exercise
import se.novila.exercise.ExerciseType
import se.novila.exercise.TrainingProgramGenerator
import java.time.Duration
import java.time.LocalDate

/**
 * The week's program for the Training tab's ViewModels, from the saved days and the coach's answers. Each ViewModel
 * gets its own; the stores behind it are shared, so a finished session or a changed answer shows up everywhere
 * through [changes].
 */
@Inject
class TrainingRepository(private val health: HealthStore, private val coach: CoachStore) {
    /** Emits whenever a day or the coach's answers are saved. */
    val changes: Flow<Unit> = combine(health.changes, coach.changes) { _, _ -> }

    /** The program for the week of [today]. [draft] is answers the user is trying out before setting up the coach. */
    fun read(today: LocalDate, draft: CoachPreferences? = null): TrainingUiState {
        // Strength sessions done in session mode are the history so far. No ExerciseSource is connected
        // yet (Google Health API needs sign-in first); once one is, its workouts join these.
        val days = health.lastDays(count = HISTORY_DAYS, today = today)
        val strength = days
            .filter { it.strengthMinutes > 0 }
            .map { Exercise(ExerciseType.Strength, it.date, Duration.ofMinutes(it.strengthMinutes.toLong())) }
        val saved = coach.load()
        val preferences = saved ?: draft ?: CoachPreferences()
        val plan = CoachPlanner.plan(preferences, history = strength, today = today)
        val program = if (saved != null) plan.program else TrainingProgramGenerator.generate(strength, today)
        return TrainingUiState(
            program = program,
            coach = CoachUiState(preferences, plan, active = saved != null),
            today = today.dayOfWeek,
            strengthMinutesToday = days.first().strengthMinutes,
        )
    }

    private companion object {
        /** The program looks at four whole weeks before this one; five weeks covers them on any weekday. */
        const val HISTORY_DAYS = 35
    }
}
