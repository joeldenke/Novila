package se.novila.feature.training

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import se.novila.coach.CoachPlanner
import se.novila.coach.CoachPreferences
import se.novila.coach.SessionFeedback
import se.novila.data.CoachStore
import se.novila.data.HealthStore
import se.novila.data.SavedCoach
import se.novila.exercise.Exercise
import se.novila.exercise.ExerciseKind
import se.novila.exercise.ExerciseRepository
import se.novila.exercise.ExerciseState
import se.novila.exercise.ExerciseType
import se.novila.exercise.TrainingHistory
import se.novila.exercise.TrainingProgramGenerator
import se.novila.model.CardioType
import se.novila.model.DayEntry
import java.time.LocalDate
import kotlin.time.Duration.Companion.minutes

/**
 * The week's program for the Training tab's ViewModels, from the saved days, the coach's answers and the workouts
 * Health Connect recorded. Each ViewModel gets its own; the stores behind it are shared, so a finished session, a
 * logged walk or a changed answer shows up everywhere through [states].
 */
@Inject
class TrainingRepository(
    private val health: HealthStore,
    private val coach: CoachStore,
    private val exercises: ExerciseRepository,
) {
    /**
     * The program for the week of each date in [dates], read again whenever a day or the coach's answers are saved,
     * or new workouts are read. [drafts] are answers the user is trying out before setting up the coach.
     */
    fun states(dates: Flow<LocalDate>, drafts: Flow<CoachPreferences?> = flowOf(null)): Flow<TrainingUiState> =
        combine(
            dates,
            drafts,
            health.days.filterNotNull(),
            coach.answers.filterNotNull(),
            exercises.state,
        ) { date, draft, days, saved, recorded -> read(days, saved, recorded, date, draft) }

    /** The program for the week of [today] as saved right now, or null before the stores' first read. */
    fun current(today: LocalDate, draft: CoachPreferences? = null): TrainingUiState? {
        val days = health.days.value ?: return null
        val saved = coach.answers.value ?: return null
        return read(days, saved, exercises.state.value, today, draft)
    }

    /** The program for the week of [today], once the stores are read. */
    suspend fun read(today: LocalDate): TrainingUiState = states(flowOf(today)).first()

    private fun read(
        saved: HealthStore.SavedDays,
        answers: SavedCoach,
        recorded: ExerciseState,
        today: LocalDate,
        draft: CoachPreferences?,
    ): TrainingUiState {
        // Strength sessions done in session mode and cardio logged by hand, together with what Health Connect
        // recorded. The same walk can be in both, so TrainingHistory counts it once.
        val days = saved.lastDays(count = ExerciseRepository.HISTORY_DAYS.toInt(), today = today)
        val own = days.flatMap(::ownExercises)
        val fromSource = (recorded as? ExerciseState.Connected)?.exercises.orEmpty()
        val history = TrainingHistory.merge(own, fromSource)
        val feedback = days.mapNotNull(::feedback)
        val active = answers.preferences
        val preferences = active ?: draft ?: CoachPreferences()
        val plan = CoachPlanner.plan(preferences, history = history, today = today, feedback = feedback)
        val program = if (active != null) plan.program else TrainingProgramGenerator.generate(history, today)
        val day = days.first()
        return TrainingUiState(
            program = program,
            coach = CoachUiState(preferences, plan, active = active != null),
            today = today.dayOfWeek,
            strengthMinutesToday = day.strengthMinutes,
            cardioToday = CardioToday(
                logged = day.cardio,
                recordedMinutes = TrainingHistory.minutes(emptyList(), fromSource, today, ExerciseKind.Cardio),
            ),
        )
    }

    private fun ownExercises(day: DayEntry): List<Exercise> = buildList {
        if (day.strengthMinutes > 0) add(Exercise(ExerciseType.Strength, day.date, day.strengthMinutes.minutes))
        day.cardio.forEach { add(Exercise(it.type.exerciseType, day.date, it.minutes.minutes)) }
    }

    private fun feedback(day: DayEntry): SessionFeedback? {
        if (day.effort == 0 && day.pain == null) return null
        return SessionFeedback(day.date, day.effort.takeIf { it > 0 }, day.pain, day.rehabPhase)
    }
}

/** The training program's type for cardio logged by hand. */
val CardioType.exerciseType: ExerciseType
    get() = when (this) {
        CardioType.Walking -> ExerciseType.Walking
        CardioType.Running -> ExerciseType.Running
        CardioType.Cycling -> ExerciseType.Cycling
        CardioType.Swimming -> ExerciseType.Swimming
    }

/** The cardio type to log for a planned [type], or null when it isn't one the user logs by hand. */
val ExerciseType.cardioType: CardioType?
    get() = CardioType.entries.firstOrNull { it.exerciseType == this }
