package se.novila.exercise

import java.time.LocalDate
import kotlin.time.Duration

/**
 * Puts the workouts Novila saved itself (strength sessions, cardio logged by hand) together with the ones a source
 * recorded, such as a watch through Health Connect. The same walk can be in both, so per day and [ExerciseKind] only
 * the side with the most time counts: a logged walk and the watch's recording of it are counted once, and a walk
 * the watch missed still counts.
 */
object TrainingHistory {
    fun merge(own: List<Exercise>, recorded: List<Exercise>): List<Exercise> {
        val ownByDay = own.groupBy { it.date to it.type.kind }
        val recordedByDay = recorded.groupBy { it.date to it.type.kind }
        return (ownByDay.keys + recordedByDay.keys)
            .flatMap { key ->
                val mine = ownByDay[key].orEmpty()
                val theirs = recordedByDay[key].orEmpty()
                if (mine.total() >= theirs.total()) mine else theirs
            }
            .sortedBy { it.date }
    }

    /** Minutes of [kind] on [date], counted as [merge] does. */
    fun minutes(own: List<Exercise>, recorded: List<Exercise>, date: LocalDate, kind: ExerciseKind): Int =
        merge(own.filter { it.date == date }, recorded.filter { it.date == date })
            .filter { it.type.kind == kind }
            .total()
            .inWholeMinutes
            .toInt()

    private fun List<Exercise>.total(): Duration = fold(Duration.ZERO) { sum, it -> sum + it.duration }
}
