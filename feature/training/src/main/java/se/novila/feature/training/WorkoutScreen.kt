package se.novila.feature.training

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import se.novila.anatomy.ExerciseLibrary
import se.novila.designsystem.component.NovilaAccentButton
import se.novila.designsystem.component.NovilaButton
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaProgressBar
import se.novila.designsystem.component.NovilaScaffold
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.exercise.Dosage
import se.novila.exercise.Workout
import se.novila.exercise.WorkoutStep
import se.novila.feature.training.R
import se.novila.ui.common.ScreenList
import se.novila.ui.preview.PreviewData
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private const val FIGURE_ASPECT_RATIO = 1.2f

/**
 * A strength session, one step at a time: the set to do now, with the figure setting a calm pace, or the
 * rest with a countdown and what comes next. Keeps the screen on while it is shown.
 */
@Composable
fun WorkoutScreen(
    state: WorkoutUiState?,
    onNext: () -> Unit,
    onFinish: () -> Unit,
    onShowHow: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        val workout = state?.workout
        val step = workout?.step
        val secondsLeft = state?.secondsLeft
        when {
            workout == null -> item(key = "none") { NoWorkout() }

            step == null -> item(key = "finished") { Finished(workout, onFinish) }

            step is WorkoutStep.Work -> {
                item(key = "progress") { Progress(workout, step) }
                item(key = "work") { Work(workout, step, secondsLeft, onNext, onShowHow) }
            }

            step is WorkoutStep.Rest -> {
                item(key = "progress") { Progress(workout, step.next) }
                item(key = "rest") { Rest(step, secondsLeft, onNext) }
            }
        }
    }
}

@Composable
private fun Progress(workout: Workout, work: WorkoutStep.Work) {
    NovilaCard {
        NovilaText(
            stringResource(
                R.string.workout_progress,
                work.exerciseNumber,
                workout.exerciseCount,
                work.set,
                work.exercise.sets,
            ),
            style = NovilaTheme.typography.titleSmall,
            color = NovilaTheme.colors.onSurfaceVariant,
        )
        NovilaProgressBar(workout.progress)
    }
}

@Composable
private fun Work(
    workout: Workout,
    step: WorkoutStep.Work,
    secondsLeft: Int?,
    onNext: () -> Unit,
    onShowHow: (String) -> Unit,
) {
    val motion = ExerciseLibrary.byId(step.exercise.exerciseId) ?: return
    val planned = step.exercise
    NovilaCard {
        AnatomyFigure(
            motion = motion,
            contentDescription = stringResource(
                R.string.move_figure_description,
                moveName(motion.id),
                muscleList(motion.primary),
            ),
            // The figure plays the set at the exercise's tempo, then stops: a pace to follow, never a loop.
            repetitions = when {
                motion.isHold -> 0
                planned.dosage == Dosage.Seconds -> (planned.amount * 1000f / motion.repetitionMillis).roundToInt()
                else -> planned.amount
            },
            replay = workout.index,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(FIGURE_ASPECT_RATIO)
                .background(NovilaTheme.colors.surfaceContainerLow, NovilaTheme.shapes.small),
        )
        NovilaText(moveName(motion.id), style = NovilaTheme.typography.headlineSmall)
        if (planned.dosage == Dosage.Seconds && secondsLeft != null) {
            Countdown(secondsLeft)
            NovilaText(
                stringResource(R.string.workout_hold),
                style = NovilaTheme.typography.bodyMedium,
                color = NovilaTheme.colors.onSurfaceVariant,
            )
        } else {
            NovilaText(
                stringResource(
                    if (planned.perSide) R.string.workout_reps_per_side else R.string.workout_reps,
                    planned.amount,
                ),
                style = NovilaTheme.typography.headlineMedium,
                color = NovilaTheme.pillars.training.color,
            )
            NovilaText(
                stringResource(R.string.workout_tempo),
                style = NovilaTheme.typography.bodyMedium,
                color = NovilaTheme.colors.onSurfaceVariant,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.m)) {
            NovilaButton(
                text = stringResource(R.string.exercise_how_title),
                onClick = { onShowHow(motion.id) },
                modifier = Modifier.weight(1f),
            )
            NovilaAccentButton(
                text = stringResource(
                    if (planned.dosage ==
                        Dosage.Seconds
                    ) {
                        R.string.workout_skip_timer
                    } else {
                        R.string.workout_set_done
                    },
                ),
                onClick = onNext,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun Rest(step: WorkoutStep.Rest, secondsLeft: Int?, onNext: () -> Unit) {
    val next = step.next
    val motion = ExerciseLibrary.byId(next.exercise.exerciseId)
    NovilaCard(title = stringResource(R.string.workout_rest_title)) {
        Countdown(secondsLeft ?: step.duration.inWholeSeconds.toInt())
        Row(
            horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (motion != null) ExerciseThumbnail(motion, Modifier.weight(1f))
            NovilaText(
                stringResource(R.string.workout_next, moveName(next.exercise.exerciseId), next.set, next.exercise.sets),
                style = NovilaTheme.typography.titleSmall,
                modifier = Modifier.weight(2f),
            )
        }
        NovilaAccentButton(
            text = stringResource(R.string.workout_skip_rest),
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Minutes and seconds in large digits, read by screen readers as the seconds left (not announced every second). */
@Composable
private fun Countdown(seconds: Int) {
    val description = stringResource(R.string.workout_timer_description, seconds)
    NovilaText(
        stringResource(R.string.workout_timer, seconds / 60, seconds % 60),
        style = NovilaTheme.typography.displayMedium,
        color = NovilaTheme.pillars.training.color,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description },
    )
}

@Composable
private fun Finished(workout: Workout, onFinish: () -> Unit) {
    NovilaCard(title = stringResource(R.string.workout_finished_title)) {
        NovilaText(
            stringResource(
                R.string.workout_finished_body,
                workout.exerciseCount,
                workout.setsDone,
                workout.minutes(workout.finishedAt ?: workout.startedAt),
            ),
            style = NovilaTheme.typography.bodyLarge,
        )
        NovilaAccentButton(
            text = stringResource(R.string.workout_save),
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun NoWorkout() {
    NovilaCard {
        NovilaText(
            stringResource(R.string.workout_none),
            style = NovilaTheme.typography.bodyMedium,
            color = NovilaTheme.colors.onSurfaceVariant,
        )
    }
}

private val PREVIEW_START = Instant.fromEpochSeconds(1_000)

private fun previewWorkout(steps: Int): WorkoutUiState {
    val session = TrainingPreviewData.program.sessions.first { it.exercises.isNotEmpty() }
    var workout = Workout.start(session, PREVIEW_START)
    var now = PREVIEW_START
    while (workout.index < steps && !workout.isFinished) {
        now += 1.minutes
        workout = workout.next(now)
    }
    return WorkoutUiState(workout, workout.secondsLeft(now + 20.seconds))
}

@Composable
private fun WorkoutContent(state: WorkoutUiState) {
    NovilaTheme {
        NovilaScaffold { padding ->
            WorkoutScreen(state, onNext = {}, onFinish = {}, onShowHow = {}, contentPadding = padding)
        }
    }
}

@Preview(heightDp = 1100)
@Composable
private fun WorkoutSetPreview() = WorkoutContent(previewWorkout(steps = 0))

@Preview(heightDp = 1100)
@Composable
private fun WorkoutRestPreview() = WorkoutContent(previewWorkout(steps = 1))

@Preview(heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun WorkoutFinishedDarkPreview() = WorkoutContent(previewWorkout(steps = Int.MAX_VALUE))
