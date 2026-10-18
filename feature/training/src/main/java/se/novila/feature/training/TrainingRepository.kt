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
import se.novila.exercise.ExerciseRepository
import se.novila.exercise.ExerciseState
import se.novila.exercise.TrainingHistory
import java.time.LocalDate

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
        val fromSource = (recorded as? ExerciseState.Connected)?.exercises.orEmpty()
        val week = trainingWeek(saved, answers, fromSource, today, draft)
        val program = week.program
        val day = week.days.first()
        return TrainingUiState(
            program = program,
            coach = CoachUiState(week.preferences, week.plan, active = week.active),
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
