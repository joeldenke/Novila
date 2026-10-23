package se.novila.exercise

import java.time.LocalDate
import kotlin.time.Duration

/** What a session trains. The program balances these, following WHO's activity guidelines. */
enum class ExerciseKind { Cardio, Strength, Mobility }

/**
 * How hard a kind of cardio usually is, as WHO counts it: 150–300 minutes of moderate activity a week, or 75–150 of
 * vigorous, so a vigorous minute counts as two ([moderateMinutes]). Running is vigorous (at least 6 MET in the
 * Compendium of Physical Activities). Walking, cycling and swimming span both, from a stroll to a race, so they count
 * as moderate: Novila would rather give too little credit than say the guideline is met when it isn't.
 */
enum class Intensity(val moderateMinutes: Int) { Moderate(1), Vigorous(2) }

/** Exercise types Novila tells apart. Anything else from a source becomes [Other]. */
enum class ExerciseType(val kind: ExerciseKind, val intensity: Intensity = Intensity.Moderate) {
    Walking(ExerciseKind.Cardio),
    Running(ExerciseKind.Cardio, Intensity.Vigorous),
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
) {
    /** The workout's minutes as WHO counts them towards the cardio guideline: a vigorous minute counts as two. */
    val moderateMinutes: Long get() = duration.inWholeMinutes * type.intensity.moderateMinutes
}

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
