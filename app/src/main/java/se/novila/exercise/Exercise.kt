package se.novila.exercise

import java.time.Duration
import java.time.LocalDate

/** What a session trains. The program balances these, following WHO's activity guidelines. */
enum class ExerciseKind { Cardio, Strength, Mobility }

/** Exercise types Novila tells apart. Anything else from a source becomes [Other]. */
enum class ExerciseType(val kind: ExerciseKind) {
    Walking(ExerciseKind.Cardio),
    Running(ExerciseKind.Cardio),
    Cycling(ExerciseKind.Cardio),
    Swimming(ExerciseKind.Cardio),
    Strength(ExerciseKind.Strength),
    Mobility(ExerciseKind.Mobility),
    Other(ExerciseKind.Cardio),
}

/** A finished workout, whatever the data source is. [date] is the local day it started. */
data class Exercise(
    val type: ExerciseType,
    val date: LocalDate,
    val duration: Duration,
)

/**
 * Where workouts come from (the Google Health API to begin with). Like MovementSource, implementations
 * never throw: a missing sign-in or a network problem is a failed [Result].
 */
fun interface ExerciseSource {
    /** Workouts that started on [from] or later, oldest first. */
    suspend fun read(from: LocalDate): Result<List<Exercise>>
}
