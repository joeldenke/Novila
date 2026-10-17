package se.novila.exercise

import java.time.LocalDate
import kotlin.time.Duration

/** What a session trains. The program balances these, following WHO's activity guidelines. */
enum class ExerciseKind { Cardio, Strength, Mobility }

/** Exercise types Novila tells apart. Anything else from a source becomes [Other]. */
enum class ExerciseType(val kind: ExerciseKind) {
    Walking(ExerciseKind.Cardio),
    Running(ExerciseKind.Cardio),
    Cycling(ExerciseKind.Cardio),
    Swimming(ExerciseKind.Cardio),
    Strength(ExerciseKind.Strength),

    /** A calm, phased rehab session from the coach, for a knee or the lower back. */
    Rehab(ExerciseKind.Strength),
    Mobility(ExerciseKind.Mobility),
    Other(ExerciseKind.Cardio),
}

/** A finished workout, whatever the data source is. [date] is the local day it started. */
data class Exercise(
    val type: ExerciseType,
    val date: LocalDate,
    val duration: Duration,
)

/** What the app can show about recorded workouts, whatever the source is. */
sealed interface ExerciseState {
    /** The source isn't on this device, or not set up. The program builds on Novila's own sessions alone. */
    data object Unavailable : ExerciseState

    /**
     * Not connected yet. [denied] after the user said no: the source then stops asking, so access
     * can only be given in its own settings.
     */
    data class NeedsPermission(val denied: Boolean = false) : ExerciseState

    /** Workouts that started on the asked day or later, oldest first. */
    data class Connected(val exercises: List<Exercise>) : ExerciseState
}

/**
 * Where workouts come from (Health Connect in the app, the Google Health API later). Like MovementSource,
 * implementations never throw: a missing sign-in or a network problem is a failed [Result], so the repository keeps
 * what it showed. Everything else is an [ExerciseState].
 */
fun interface ExerciseSource {
    /** Workouts that started on [from] or later, oldest first. */
    suspend fun read(from: LocalDate): Result<ExerciseState>
}
