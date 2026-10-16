package se.novila.feature.training

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import se.novila.anatomy.ExerciseLibrary
import se.novila.anatomy.ExerciseMotion
import se.novila.designsystem.component.NovilaAccentButton
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaScaffold
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.exercise.PlannedExercise
import se.novila.feature.training.R
import se.novila.ui.common.ScreenList
import se.novila.ui.preview.PreviewData

private const val DETAIL_ASPECT_RATIO = 1.2f
private const val REPETITIONS = 3

/**
 * How to do one exercise: the anatomy figure plays a few calm repetitions and stops, with the
 * muscles it trains, the steps and one thing to keep in mind. With [planned], it also shows the dose
 * from this week's program.
 */
@Composable
fun ExerciseDetailScreen(
    motion: ExerciseMotion,
    planned: PlannedExercise?,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var replays by rememberSaveable(motion.id) { mutableIntStateOf(0) }
    val text = moveText(motion.id)
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "figure") {
            NovilaCard {
                AnatomyFigure(
                    motion = motion,
                    contentDescription = stringResource(
                        R.string.move_figure_description,
                        moveName(motion.id),
                        muscleList(motion.primary),
                    ),
                    repetitions = if (motion.isHold) 0 else REPETITIONS,
                    replay = replays,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(DETAIL_ASPECT_RATIO)
                        .background(NovilaTheme.colors.surfaceContainerLow, NovilaTheme.shapes.small),
                )
                if (planned != null) {
                    NovilaText(
                        stringResource(
                            R.string.exercise_in_program,
                            dosageText(planned),
                            stringResource(R.string.dose_rest, planned.rest.inWholeSeconds.toInt()),
                        ),
                        style = NovilaTheme.typography.titleSmall,
                        color = NovilaTheme.pillars.training.color,
                    )
                }
                if (!motion.isHold) {
                    NovilaAccentButton(
                        text = stringResource(R.string.exercise_replay),
                        onClick = { replays++ },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        item(key = "muscles") {
            NovilaCard(title = stringResource(R.string.exercise_muscles_title)) {
                val anatomy = NovilaTheme.anatomy
                MuscleLegend(
                    stringResource(R.string.exercise_primary_legend, muscleList(motion.primary)),
                    fill = anatomy.musclePrimary,
                    edge = anatomy.musclePrimary,
                )
                if (motion.secondary.isNotEmpty()) {
                    MuscleLegend(
                        stringResource(R.string.exercise_secondary_legend, muscleList(motion.secondary)),
                        fill = anatomy.muscleSecondary,
                        edge = anatomy.musclePrimary,
                    )
                }
            }
        }
        if (text != null) {
            item(key = "how") {
                NovilaCard(title = stringResource(R.string.exercise_how_title)) {
                    stringArrayResource(text.steps).forEachIndexed { i, step -> Step(i + 1, step) }
                }
            }
            item(key = "tip") {
                NovilaCard(title = stringResource(R.string.exercise_tip_title)) {
                    NovilaText(stringResource(text.tip), style = NovilaTheme.typography.bodyMedium)
                    NovilaText(
                        stringResource(R.string.exercise_safety),
                        style = NovilaTheme.typography.bodySmall,
                        color = NovilaTheme.colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** A swatch drawn like the figure's muscles (filled, or a light fill with an edge) and what it means. */
@Composable
private fun MuscleLegend(label: String, fill: Color, edge: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(NovilaTheme.spacing.l)) {
            val radius = size.minDimension / 2f
            drawCircle(fill, radius)
            drawCircle(edge, radius * 0.88f, style = Stroke(width = radius * 0.24f))
        }
        NovilaText(label, style = NovilaTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Step(number: Int, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.m)) {
        NovilaText(
            stringResource(R.string.step_number, number),
            style = NovilaTheme.typography.titleSmall,
            color = NovilaTheme.pillars.training.color,
        )
        NovilaText(text, style = NovilaTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ExerciseDetailContent(motion: ExerciseMotion) {
    NovilaTheme {
        NovilaScaffold { padding ->
            ExerciseDetailScreen(
                motion = motion,
                planned = TrainingPreviewData.program.sessions.flatMap {
                    it.exercises
                }.firstOrNull { it.exerciseId == motion.id },
                contentPadding = padding,
            )
        }
    }
}

@Preview(heightDp = 1400)
@Composable
private fun ExerciseDetailPreview() = ExerciseDetailContent(ExerciseLibrary.squat)

@Preview(heightDp = 1400, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun ExerciseDetailDarkPreview() = ExerciseDetailContent(ExerciseLibrary.plank)
