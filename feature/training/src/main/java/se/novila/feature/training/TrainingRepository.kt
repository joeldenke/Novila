package se.novila.feature.training

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import se.novila.coach.CoachPreferences
import se.novila.data.CoachStore
import se.novila.data.HealthStore
import se.novila.data.SavedCoach
import se.novila.data.trainingWeek
import se.novila.exercise.ExerciseKind
import se.novila.exercise.ExerciseLog
import se.novila.exercise.ExerciseRepository
import se.novila.exercise.ExerciseState
import se.novila.exercise.TrainingHistory
import se.novila.recovery.RecoveryRepository
import se.novila.recovery.RecoveryState
import se.novila.recovery.RecoveryStatus
import se.novila.recovery.analyzeRecovery
import java.time.LocalDate

/**
 * The week's program for the Training tab's ViewModels, from the saved days, the coach's answers, the workouts
 * Health Connect recorded, the watch's recovery and what the user did of each exercise lately. Each ViewModel gets
 * its own; the stores behind it are shared, so a finished session, a logged walk or a changed answer shows up
 * everywhere through [states].
 */
@Inject
class TrainingRepository(
    private val health: HealthStore,
    private val coach: CoachStore,
    private val exercises: ExerciseRepository,
    private val recovery: RecoveryRepository,
) {
    /**
     * The program for the week of each date in [dates], read again whenever a day or the coach's answers are saved,
     * new workouts are read, or recovery changes. [drafts] are answers the user is trying out before setting up the
     * coach.
     */
    fun states(dates: Flow<LocalDate>, drafts: Flow<CoachPreferences?> = flowOf(null)): Flow<TrainingUiState> {
        val inputs = combine(
            dates,
            drafts,
            health.days.filterNotNull(),
            coach.answers.filterNotNull(),
            exercises.state,
        ) { date, draft, days, saved, recorded -> TrainingInputs(date, draft, days, saved, recorded) }
        return combine(inputs, recovery.state, coach.progress.filterNotNull()) { input, recovery, progress ->
            read(input.days, input.saved, input.recorded, input.date, input.draft, recovery, progress)
        }
    }

    /** The program for the week of [today] as saved right now, or null before the stores' first read. */
    fun current(today: LocalDate, draft: CoachPreferences? = null): TrainingUiState? {
        val days = health.days.value ?: return null
        val saved = coach.answers.value ?: return null
        val progress = coach.progress.value ?: return null
        return read(days, saved, exercises.state.value, today, draft, recovery.state.value, progress)
    }

    /** The program for the week of [today], once the stores are read. */
    suspend fun read(today: LocalDate): TrainingUiState = states(flowOf(today)).first()

    private fun read(
        saved: HealthStore.SavedDays,
        answers: SavedCoach,
        recorded: ExerciseState,
        today: LocalDate,
        draft: CoachPreferences?,
        recovery: RecoveryState,
        progress: ExerciseLog,
    ): TrainingUiState {
        val fromSource = (recorded as? ExerciseState.Connected)?.exercises.orEmpty()
        val status = (recovery as? RecoveryState.Connected)?.let { analyzeRecovery(it.days, today) }
            ?: RecoveryStatus.Unknown
        val week = trainingWeek(saved, answers, fromSource, today, draft, status, progress)
        val program = week.program
        val day = week.days.first()
        return TrainingUiState(
            program = program,
            coach = CoachUiState(week.preferences, week.plan, active = week.active),
            recovery = status,
            today = today.dayOfWeek,
            strengthMinutesToday = day.strengthMinutes,
            cardioToday = CardioToday(
                logged = day.cardio,
                recordedMinutes = TrainingHistory.minutes(emptyList(), fromSource, today, ExerciseKind.Cardio),
            ),
            weekDone = week.history
                .filter { it.date >= program.weekStart && it.date <= today }
                .groupBy { it.date.dayOfWeek }
                .mapValues { (_, done) -> done.sumOf { it.duration.inWholeMinutes }.toInt() },
            nextProgram = week.nextProgram,
        )
    }
}

/** The stores' latest values, together, so recovery can be combined in as well. */
private data class TrainingInputs(
    val date: LocalDate,
    val draft: CoachPreferences?,
    val days: HealthStore.SavedDays,
    val saved: SavedCoach,
    val recorded: ExerciseState,
)
