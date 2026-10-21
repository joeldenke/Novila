package se.novila.feature.training

import android.content.res.Configuration
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import se.novila.anatomy.ExerciseLibrary
import se.novila.anatomy.ExerciseMotion
import se.novila.anatomy.MuscleGroup
import se.novila.designsystem.component.NovilaAccentButton
import se.novila.designsystem.component.NovilaButton
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaScaffold
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.exercise.Dosage
import se.novila.exercise.ExerciseType
import se.novila.exercise.PlannedExercise
import se.novila.exercise.PlannedSession
import se.novila.feature.training.R
import se.novila.ui.common.ScreenList
import se.novila.ui.preview.PreviewData
import java.time.format.TextStyle
import se.novila.ui.R as UiR

private const val THUMBNAIL_ASPECT_RATIO = 1.3f

/** An exercise's texts: its name, the steps to do it and one thing to keep in mind. */
data class MoveText(
    @param:StringRes val name: Int,
    @param:ArrayRes val steps: Int,
    @param:StringRes val tip: Int,
)

/** The texts of an exercise in [ExerciseLibrary], or null for an id the app doesn't know. */
internal fun moveText(id: String): MoveText? = when (id) {
    "squat" -> MoveText(R.string.move_squat, R.array.move_squat_steps, R.string.move_squat_tip)

    "push_up" -> MoveText(R.string.move_push_up, R.array.move_push_up_steps, R.string.move_push_up_tip)

    "glute_bridge" -> MoveText(
        R.string.move_glute_bridge,
        R.array.move_glute_bridge_steps,
        R.string.move_glute_bridge_tip,
    )

    "split_squat" -> MoveText(R.string.move_split_squat, R.array.move_split_squat_steps, R.string.move_split_squat_tip)

    "jumping_jack" -> MoveText(
        R.string.move_jumping_jack,
        R.array.move_jumping_jack_steps,
        R.string.move_jumping_jack_tip,
    )

    "calf_raise" -> MoveText(R.string.move_calf_raise, R.array.move_calf_raise_steps, R.string.move_calf_raise_tip)

    "plank" -> MoveText(R.string.move_plank, R.array.move_plank_steps, R.string.move_plank_tip)

    "mountain_climber" -> MoveText(
        R.string.move_mountain_climber,
        R.array.move_mountain_climber_steps,
        R.string.move_mountain_climber_tip,
    )

    "superman" -> MoveText(R.string.move_superman, R.array.move_superman_steps, R.string.move_superman_tip)

    "reverse_lunge" -> MoveText(
        R.string.move_reverse_lunge,
        R.array.move_reverse_lunge_steps,
        R.string.move_reverse_lunge_tip,
    )

    "good_morning" -> MoveText(
        R.string.move_good_morning,
        R.array.move_good_morning_steps,
        R.string.move_good_morning_tip,
    )

    "bird_dog" -> MoveText(R.string.move_bird_dog, R.array.move_bird_dog_steps, R.string.move_bird_dog_tip)

    "crunch" -> MoveText(R.string.move_crunch, R.array.move_crunch_steps, R.string.move_crunch_tip)

    "arm_raise" -> MoveText(R.string.move_arm_raise, R.array.move_arm_raise_steps, R.string.move_arm_raise_tip)

    "neck_nod" -> MoveText(R.string.move_neck_nod, R.array.move_neck_nod_steps, R.string.move_neck_nod_tip)

    "knee_push_up" -> MoveText(
        R.string.move_knee_push_up,
        R.array.move_knee_push_up_steps,
        R.string.move_knee_push_up_tip,
    )

    "single_leg_bridge" -> MoveText(
        R.string.move_single_leg_bridge,
        R.array.move_single_leg_bridge_steps,
        R.string.move_single_leg_bridge_tip,
    )

    "dead_bug" -> MoveText(R.string.move_dead_bug, R.array.move_dead_bug_steps, R.string.move_dead_bug_tip)

    "high_knees" -> MoveText(R.string.move_high_knees, R.array.move_high_knees_steps, R.string.move_high_knees_tip)

    "goblet_squat" -> MoveText(
        R.string.move_goblet_squat,
        R.array.move_goblet_squat_steps,
        R.string.move_goblet_squat_tip,
    )

    "dumbbell_row" -> MoveText(
        R.string.move_dumbbell_row,
        R.array.move_dumbbell_row_steps,
        R.string.move_dumbbell_row_tip,
    )

    "dumbbell_press" -> MoveText(
        R.string.move_dumbbell_press,
        R.array.move_dumbbell_press_steps,
        R.string.move_dumbbell_press_tip,
    )

    "biceps_curl" -> MoveText(R.string.move_biceps_curl, R.array.move_biceps_curl_steps, R.string.move_biceps_curl_tip)

    "dumbbell_deadlift" -> MoveText(
        R.string.move_dumbbell_deadlift,
        R.array.move_dumbbell_deadlift_steps,
        R.string.move_dumbbell_deadlift_tip,
    )

    "band_row" -> MoveText(R.string.move_band_row, R.array.move_band_row_steps, R.string.move_band_row_tip)

    "band_pulldown" -> MoveText(
        R.string.move_band_pulldown,
        R.array.move_band_pulldown_steps,
        R.string.move_band_pulldown_tip,
    )

    "band_chest_press" -> MoveText(
        R.string.move_band_chest_press,
        R.array.move_band_chest_press_steps,
        R.string.move_band_chest_press_tip,
    )

    "front_squat" -> MoveText(R.string.move_front_squat, R.array.move_front_squat_steps, R.string.move_front_squat_tip)

    "deadlift" -> MoveText(R.string.move_deadlift, R.array.move_deadlift_steps, R.string.move_deadlift_tip)

    "barbell_row" -> MoveText(R.string.move_barbell_row, R.array.move_barbell_row_steps, R.string.move_barbell_row_tip)

    else -> null
}

/** The exercise's name, or a generic one for an id the app doesn't know. */
@Composable
fun moveName(id: String): String = stringResource(moveText(id)?.name ?: R.string.exercise_strength)

/** The muscle groups as a readable list, in the order the exercise names them. */
@Composable
fun muscleList(groups: Set<MuscleGroup>): String {
    val separator = stringResource(R.string.list_separator)
    return groups.map { stringResource(it.label) }.joinToString(separator)
}

/**
 * "3 × 10", "3 × 10 per side" or "3 × 30 s", and "up from 9" or "down from 11" when the progression moved the
 * amount on from the last time ([PlannedExercise.previous]).
 */
@Composable
fun dosageText(planned: PlannedExercise): String {
    val dose = when {
        planned.dosage == Dosage.Seconds -> stringResource(R.string.dose_seconds, planned.sets, planned.amount)
        planned.perSide -> stringResource(R.string.dose_reps_per_side, planned.sets, planned.amount)
        else -> stringResource(R.string.dose_reps, planned.sets, planned.amount)
    }
    val previous = planned.previous ?: return dose
    return when {
        planned.amount > previous -> stringResource(R.string.dose_up_from, dose, previous)
        planned.amount < previous -> stringResource(R.string.dose_down_from, dose, previous)
        else -> dose
    }
}

/** The anatomy figure still at the exercise's peak, on a soft backdrop. */
@Composable
fun ExerciseThumbnail(motion: ExerciseMotion, modifier: Modifier = Modifier) {
    AnatomyFigure(
        motion = motion,
        contentDescription = stringResource(
            R.string.move_figure_description,
            moveName(motion.id),
            muscleList(motion.primary),
        ),
        modifier = modifier
            .aspectRatio(THUMBNAIL_ASPECT_RATIO)
            .background(NovilaTheme.colors.surfaceContainerLow, NovilaTheme.shapes.small),
    )
}

/**
 * A strength or rehab day in the program: one row per exercise with its sets, repetitions and rest, and a button
 * that starts the guided session (none without [onStart]). The [next] strength day gets the sun, so a screen
 * has only one. [minutesDone] shows that the session is already done today.
 */
@Composable
fun StrengthSessionCard(
    session: PlannedSession,
    onOpenExercise: (String) -> Unit,
    onStart: (() -> Unit)?,
    next: Boolean,
    minutesDone: Int? = null,
) {
    val locale = LocalConfiguration.current.locales[0]
    val rehab = session.type == ExerciseType.Rehab
    NovilaCard(
        title = stringResource(
            if (rehab) R.string.rehab_session_title else R.string.strength_session_title,
            session.day.getDisplayName(TextStyle.FULL, locale),
        ),
        icon = painterResource(UiR.drawable.ic_training),
        role = NovilaTheme.pillars.training,
    ) {
        NovilaText(
            stringResource(
                if (rehab) R.string.rehab_session_summary else R.string.strength_session_summary,
                session.minutes,
            ),
            style = NovilaTheme.typography.bodyMedium,
            color = NovilaTheme.colors.onSurfaceVariant,
        )
        session.exercises.forEach { planned ->
            ExerciseLibrary.byId(planned.exerciseId)?.let { motion ->
                PlannedExerciseRow(motion, planned, onOpenExercise)
            }
        }
        val start = stringResource(R.string.workout_start)
        when {
            minutesDone != null -> NovilaText(
                stringResource(R.string.workout_done_today, minutesDone),
                style = NovilaTheme.typography.titleSmall,
                color = NovilaTheme.pillars.training.color,
            )

            onStart == null -> Unit

            next -> NovilaAccentButton(start, onClick = onStart, modifier = Modifier.fillMaxWidth())

            else -> NovilaButton(start, onClick = onStart, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PlannedExerciseRow(motion: ExerciseMotion, planned: PlannedExercise, onOpen: (String) -> Unit) {
    val name = moveName(motion.id)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NovilaTheme.shapes.small)
            .clickable(onClickLabel = stringResource(R.string.exercise_open, name), role = Role.Button) {
                onOpen(motion.id)
            },
        horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Small in a session's list; the library shows them large.
        ExerciseThumbnail(motion, Modifier.weight(1f))
        Column(Modifier.weight(3f), verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.xxs)) {
            NovilaText(name, style = NovilaTheme.typography.titleSmall)
            NovilaText(
                dosageText(planned),
                style = NovilaTheme.typography.bodyMedium,
                color = NovilaTheme.pillars.training.color,
            )
            NovilaText(
                stringResource(R.string.dose_rest, planned.rest.inWholeSeconds.toInt()),
                style = NovilaTheme.typography.bodySmall,
                color = NovilaTheme.colors.onSurfaceVariant,
            )
        }
    }
}

/**
 * The exercise library on the Training tab: a few exercises as large tiles, each opening its instructions, and the
 * way to all of them with search ([onOpenLibrary]).
 */
@Composable
fun ExerciseLibraryCard(onOpenExercise: (String) -> Unit, onOpenLibrary: () -> Unit) {
    NovilaCard(title = stringResource(R.string.exercises_title)) {
        NovilaText(
            stringResource(R.string.exercises_body),
            style = NovilaTheme.typography.bodyMedium,
            color = NovilaTheme.colors.onSurfaceVariant,
        )
        ExerciseGrid(ExerciseLibrary.all.take(LIBRARY_PREVIEW), onOpenExercise)
        NovilaButton(
            stringResource(R.string.library_show_all, ExerciseLibrary.all.size),
            onClick = onOpenLibrary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private const val LIBRARY_PREVIEW = 4

/** Exercises as large tiles, two to a row, each opening its instructions. */
@Composable
fun ExerciseGrid(motions: List<ExerciseMotion>, onOpenExercise: (String) -> Unit) {
    motions.chunked(2).forEach { pair ->
        Row(horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.m)) {
            pair.forEach { motion -> ExerciseTile(motion, onOpenExercise, Modifier.weight(1f)) }
            if (pair.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun ExerciseTile(motion: ExerciseMotion, onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    val name = moveName(motion.id)
    Column(
        modifier = modifier
            .clip(NovilaTheme.shapes.small)
            .clickable(onClickLabel = stringResource(R.string.exercise_open, name), role = Role.Button) {
                onOpen(motion.id)
            },
        verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.xs),
    ) {
        ExerciseThumbnail(motion, Modifier.fillMaxWidth())
        NovilaText(name, style = NovilaTheme.typography.titleSmall)
    }
}

@get:StringRes
internal val MuscleGroup.label: Int
    get() = when (this) {
        MuscleGroup.Chest -> R.string.muscle_chest
        MuscleGroup.Shoulders -> R.string.muscle_shoulders
        MuscleGroup.Biceps -> R.string.muscle_biceps
        MuscleGroup.Triceps -> R.string.muscle_triceps
        MuscleGroup.Abs -> R.string.muscle_abs
        MuscleGroup.Neck -> R.string.muscle_neck
        MuscleGroup.UpperBack -> R.string.muscle_upper_back
        MuscleGroup.Lats -> R.string.muscle_lats
        MuscleGroup.LowerBack -> R.string.muscle_lower_back
        MuscleGroup.Glutes -> R.string.muscle_glutes
        MuscleGroup.Quads -> R.string.muscle_quads
        MuscleGroup.Hamstrings -> R.string.muscle_hamstrings
        MuscleGroup.Calves -> R.string.muscle_calves
    }

@Composable
private fun ExerciseCardsContent() {
    NovilaTheme {
        NovilaScaffold { padding ->
            ScreenList(contentPadding = padding) {
                item(key = "session") {
                    StrengthSessionCard(
                        session = TrainingPreviewData.program.sessions.first { it.exercises.isNotEmpty() },
                        onOpenExercise = {},
                        onStart = {},
                        next = true,
                    )
                }
                item(key = "library") { ExerciseLibraryCard(onOpenExercise = {}, onOpenLibrary = {}) }
            }
        }
    }
}

@Preview(heightDp = 2000)
@Composable
private fun ExerciseCardsPreview() = ExerciseCardsContent()

@Preview(heightDp = 2000, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun ExerciseCardsDarkPreview() = ExerciseCardsContent()
