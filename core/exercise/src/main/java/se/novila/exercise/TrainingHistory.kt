package se.novila.exercise

import java.time.LocalDate
import kotlin.math.roundToInt
import kotlin.time.Duration

/**
 * Puts the workouts Novila saved itself (strength sessions, cardio logged by hand) together with the ones a source
 * recorded, such as a watch through Health Connect. The same walk can be in both, so per day and [ExerciseKind] only
 * the side with the most time counts: a logged walk and the watch's recording of it are counted once, and a walk
 * the watch missed still counts. When the recording wins, it keeps the effort the user gave for the same day and kind
 * (weighted by minutes), since it is the same session.
 */
object TrainingHistory {
    fun merge(own: List<Exercise>, recorded: List<Exercise>): List<Exercise> {
        val ownByDay = own.groupBy { it.date to it.type.kind }
        val recordedByDay = recorded.groupBy { it.date to it.type.kind }
        return (ownByDay.keys + recordedByDay.keys)
            .flatMap { key ->
                val mine = ownByDay[key].orEmpty()
                val theirs = recordedByDay[key].orEmpty()
                if (mine.total() >= theirs.total()) mine else theirs.withEffort(mine.effort())
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

    /** The minute-weighted effort of the sessions the user rated, or null when none was. */
    private fun List<Exercise>.effort(): Int? {
        val rated = filter { it.effort != null && it.duration > Duration.ZERO }
        if (rated.isEmpty()) return null
        val minutes = rated.sumOf { it.duration.inWholeSeconds }.toDouble()
        return rated.sumOf { (it.effort ?: 0) * it.duration.inWholeSeconds }.div(minutes).roundToInt()
    }

    private fun List<Exercise>.withEffort(effort: Int?): List<Exercise> =
        if (effort == null) this else map { it.copy(effort = it.effort ?: effort) }
}
