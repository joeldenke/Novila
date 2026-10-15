package se.novila.ui.common

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import se.novila.R
import se.novila.anatomy.AnatomyFigure
import se.novila.anatomy.ExerciseLibrary
import se.novila.anatomy.ExerciseMotion
import se.novila.anatomy.MuscleGroup
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaScaffold
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme

private const val FIGURE_ASPECT_RATIO = 1.6f

/**
 * An exercise's preview: the anatomy figure at the hardest moment of the repetition, its name and the
 * muscle groups it trains, main ones first.
 */
@Composable
fun ExerciseCard(motion: ExerciseMotion, modifier: Modifier = Modifier) {
    val name = stringResource(moveName(motion.id) ?: R.string.exercise_strength)
    val primary = muscleList(motion.primary)
    NovilaCard(modifier = modifier) {
        AnatomyFigure(
            motion = motion,
            contentDescription = stringResource(R.string.move_figure_description, name, primary),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(FIGURE_ASPECT_RATIO)
                .background(NovilaTheme.colors.surfaceContainerLow, NovilaTheme.shapes.small),
        )
        NovilaText(name, style = NovilaTheme.typography.titleMedium)
        NovilaText(
            stringResource(R.string.move_works, primary),
            style = NovilaTheme.typography.bodyMedium,
            color = NovilaTheme.pillars.training.color,
        )
        if (motion.secondary.isNotEmpty()) {
            NovilaText(
                stringResource(R.string.move_also, muscleList(motion.secondary)),
                style = NovilaTheme.typography.bodySmall,
                color = NovilaTheme.colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun muscleList(groups: Set<MuscleGroup>): String {
    val separator = stringResource(R.string.list_separator)
    return groups.map { stringResource(it.label) }.joinToString(separator)
}

/** The name of an exercise in [ExerciseLibrary], or null for an id the app doesn't know. */
@StringRes
internal fun moveName(id: String): Int? = when (id) {
    ExerciseLibrary.squat.id -> R.string.move_squat
    ExerciseLibrary.pushUp.id -> R.string.move_push_up
    ExerciseLibrary.gluteBridge.id -> R.string.move_glute_bridge
    ExerciseLibrary.splitSquat.id -> R.string.move_split_squat
    ExerciseLibrary.jumpingJack.id -> R.string.move_jumping_jack
    else -> null
}

@get:StringRes
internal val MuscleGroup.label: Int
    get() = when (this) {
        MuscleGroup.Chest -> R.string.muscle_chest
        MuscleGroup.Shoulders -> R.string.muscle_shoulders
        MuscleGroup.Biceps -> R.string.muscle_biceps
        MuscleGroup.Triceps -> R.string.muscle_triceps
        MuscleGroup.Abs -> R.string.muscle_abs
        MuscleGroup.UpperBack -> R.string.muscle_upper_back
        MuscleGroup.Lats -> R.string.muscle_lats
        MuscleGroup.LowerBack -> R.string.muscle_lower_back
        MuscleGroup.Glutes -> R.string.muscle_glutes
        MuscleGroup.Quads -> R.string.muscle_quads
        MuscleGroup.Hamstrings -> R.string.muscle_hamstrings
        MuscleGroup.Calves -> R.string.muscle_calves
    }

@Composable
private fun ExerciseLibraryContent() {
    NovilaTheme {
        NovilaScaffold { padding ->
            ScreenList(contentPadding = padding) {
                items(ExerciseLibrary.all, key = { it.id }) { ExerciseCard(it) }
            }
        }
    }
}

@Preview(heightDp = 1900)
@Composable
private fun ExerciseLibraryPreview() = ExerciseLibraryContent()

@Preview(heightDp = 1900, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun ExerciseLibraryDarkPreview() = ExerciseLibraryContent()
