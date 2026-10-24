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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import se.novila.anatomy.ExerciseLibrary
import se.novila.coach.CoachFeedback
import se.novila.designsystem.component.NovilaAccentButton
import se.novila.designsystem.component.NovilaButton
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaProgressBar
import se.novila.designsystem.component.NovilaScaffold
import se.novila.designsystem.component.NovilaSlider
import se.novila.designsystem.component.NovilaStepper
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.exercise.Dosage
import se.novila.exercise.PlannedExercise
import se.novila.exercise.Workout
import se.novila.exercise.WorkoutStep
import se.novila.feature.training.R
import se.novila.model.MAX_EFFORT
import se.novila.model.MAX_PAIN
import se.novila.ui.common.ScreenList
import se.novila.ui.preview.PreviewData
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private const val FIGURE_ASPECT_RATIO = 1.2f

/**
 * A strength session, one step at a time: a warm-up, then the set to do now, with the figure setting a calm pace,
 * or the rest with a countdown and what comes next, and a cool-down after the last set. When it is done, the user
 * says how hard it was ([onEffort]) and how much it hurt ([onPain]) before saving it. Keeps the screen on while it
 * is shown.
 */
@Composable
fun WorkoutScreen(
    state: WorkoutUiState?,
    onNext: () -> Unit,
    onEffort: (Int) -> Unit,
    onPain: (Int) -> Unit,
    onFinish: () -> Unit,
    onShowHow: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onDone: (Int) -> Unit = {},
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

            step == null -> {
                item(key = "finished") { Finished(workout, progression = !state.rehab) }
                item(key = "feedback") {
                    Feedback(state.feedback, onEffort, onPain, onFinish)
                }
            }

            step is WorkoutStep.Work -> {
                item(key = "progress") { Progress(workout, step) }
                item(key = "work") {
                    Work(workout, step, secondsLeft, onNext, onShowHow, onDone.takeUnless { state.rehab })
                }
            }

            step is WorkoutStep.Rest -> {
                item(key = "progress") { Progress(workout, step.next) }
                item(key = "rest") {
                    Phase(
                        title = stringResource(R.string.workout_rest_title),
                        secondsLeft = secondsLeft,
                        duration = step.duration,
                        next = step.next,
                        action = stringResource(R.string.workout_skip_rest),
                        onNext = onNext,
                    )
                }
            }

            step is WorkoutStep.WarmUp -> {
                item(key = "progress") { Progress(workout, step.next) }
                item(key = "warmup") {
                    Phase(
                        title = stringResource(R.string.workout_warmup_title),
                        secondsLeft = secondsLeft,
                        duration = step.duration,
                        next = step.next,
                        body = stringResource(R.string.workout_warmup_body),
                        action = stringResource(R.string.workout_warmup_done),
                        onNext = onNext,
                    )
                }
            }

            step is WorkoutStep.CoolDown -> {
                item(key = "cooldown") {
                    Phase(
                        title = stringResource(R.string.workout_cooldown_title),
                        secondsLeft = secondsLeft,
                        duration = step.duration,
                        body = stringResource(R.string.workout_cooldown_body),
                        action = stringResource(R.string.workout_cooldown_done),
                        onNext = onNext,
                    )
                }
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
    onDone: ((Int) -> Unit)?,
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
            val done = workout.doneInSet
            if (onDone != null && done != null) {
                NovilaStepper(
                    title = stringResource(R.string.workout_done_label),
                    value = stringResource(
                        if (planned.perSide) R.string.workout_reps_per_side else R.string.workout_reps,
                        done,
                    ),
                    onDecrement = { onDone(done - 1) },
                    onIncrement = { onDone(done + 1) },
                    decrementDescription = stringResource(R.string.workout_done_less),
                    incrementDescription = stringResource(R.string.workout_done_more),
                    role = NovilaTheme.pillars.training,
                    hint = stringResource(R.string.workout_done_hint),
                    decrementEnabled = done > 0,
                    incrementEnabled = done < Workout.MAX_DONE,
                )
            }
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

/**
 * A timed step: the countdown, what to do while it runs ([body]), the set that comes after it ([next]) and the
 * one button that moves the session on.
 */
@Composable
private fun Phase(
    title: String,
    secondsLeft: Int?,
    duration: Duration,
    action: String,
    onNext: () -> Unit,
    body: String? = null,
    next: WorkoutStep.Work? = null,
) {
    NovilaCard(title = title) {
        Countdown(secondsLeft ?: duration.inWholeSeconds.toInt())
        if (next != null) {
            val motion = ExerciseLibrary.byId(next.exercise.exerciseId)
            Row(
                horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (motion != null) ExerciseThumbnail(motion, Modifier.weight(1f))
                NovilaText(
                    stringResource(
                        R.string.workout_next,
                        moveName(next.exercise.exerciseId),
                        next.set,
                        next.exercise.sets,
                    ),
                    style = NovilaTheme.typography.titleSmall,
                    modifier = Modifier.weight(2f),
                )
            }
        }
        if (body != null) {
            NovilaText(
                body,
                style = NovilaTheme.typography.bodyMedium,
                color = NovilaTheme.colors.onSurfaceVariant,
            )
        }
        NovilaAccentButton(
            text = action,
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

/**
 * What the session was, and with [progression] how each exercise moves on next time: a step up when every set was
 * reached, a step back when one fell clearly short.
 */
@Composable
private fun Finished(workout: Workout, progression: Boolean) {
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
        val changes = if (progression) workout.nextChanges() else emptyList()
        if (changes.isNotEmpty()) {
            NovilaText(stringResource(R.string.workout_next_title), style = NovilaTheme.typography.titleSmall)
            changes.forEach { (exercise, change) ->
                NovilaText(nextTimeText(exercise, change), style = NovilaTheme.typography.bodyMedium)
            }
        }
    }
}

/** "Squat: 1 repetition more", "Plank: 5 seconds more", "the same again" or "a step back". */
@Composable
private fun nextTimeText(exercise: PlannedExercise, change: Int): String {
    val name = moveName(exercise.exerciseId)
    return when {
        change < 0 -> stringResource(R.string.workout_next_less, name)
        change == 0 -> stringResource(R.string.workout_next_same, name)
        exercise.dosage == Dosage.Seconds -> stringResource(R.string.workout_next_more_seconds, name, change)
        else -> pluralStringResource(R.plurals.workout_next_more_reps, change, name, change)
    }
}

/**
 * How hard the session was (RPE, Borg CR10) and how much it hurt, each on a slider that stays unanswered until it is
 * touched. Both are optional; the coach adjusts the dose and suggests the rehab phase from them.
 */
@Composable
private fun Feedback(
    feedback: SessionFeedbackDraft,
    onEffort: (Int) -> Unit,
    onPain: (Int) -> Unit,
    onFinish: () -> Unit,
) {
    val training = NovilaTheme.pillars.training
    val effortTitle = stringResource(R.string.workout_effort_title)
    val painTitle = stringResource(R.string.workout_pain_title)
    NovilaCard(title = stringResource(R.string.workout_feedback_title)) {
        Answer(
            title = effortTitle,
            value = feedback.effort?.let { stringResource(R.string.workout_scale_value, it, effortLabel(it)) },
        )
        NovilaSlider(
            value = (feedback.effort ?: 1).toFloat(),
            onValueChange = { onEffort(it.roundToInt()) },
            valueRange = 1f..MAX_EFFORT.toFloat(),
            steps = MAX_EFFORT - 2,
            role = training,
            modifier = Modifier.semantics { contentDescription = effortTitle },
        )
        Answer(
            title = painTitle,
            value = feedback.pain?.let { stringResource(R.string.workout_scale_value, it, painLabel(it)) },
        )
        NovilaSlider(
            value = (feedback.pain ?: 0).toFloat(),
            onValueChange = { onPain(it.roundToInt()) },
            valueRange = 0f..MAX_PAIN.toFloat(),
            steps = MAX_PAIN - 1,
            role = training,
            modifier = Modifier.semantics { contentDescription = painTitle },
        )
        val pain = feedback.pain
        if (pain != null && pain >= CoachFeedback.TOO_MUCH_PAIN) {
            NovilaText(
                stringResource(R.string.workout_pain_care),
                style = NovilaTheme.typography.titleSmall,
            )
        }
        NovilaText(
            stringResource(R.string.workout_feedback_hint),
            style = NovilaTheme.typography.bodySmall,
            color = NovilaTheme.colors.onSurfaceVariant,
        )
        NovilaAccentButton(
            text = stringResource(R.string.workout_save),
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** A question and its answer so far, or that it isn't answered. */
@Composable
private fun Answer(title: String, value: String?) {
    NovilaText(title, style = NovilaTheme.typography.titleSmall)
    NovilaText(
        value ?: stringResource(R.string.workout_not_answered),
        style = NovilaTheme.typography.bodyMedium,
        color = if (value != null) NovilaTheme.pillars.training.color else NovilaTheme.colors.onSurfaceVariant,
    )
}

/** Words for the Borg CR10 scale. */
@Composable
internal fun effortLabel(effort: Int): String = stringResource(
    when {
        effort <= 2 -> R.string.workout_effort_easy
        effort <= 4 -> R.string.workout_effort_moderate
        effort <= 6 -> R.string.workout_effort_hard
        effort <= 8 -> R.string.workout_effort_very_hard
        else -> R.string.workout_effort_max
    },
)

/** Words for the pain scale, after the pain monitoring model: up to 3 is fine, more than 5 is too much. */
@Composable
private fun painLabel(pain: Int): String = stringResource(
    when {
        pain == 0 -> R.string.workout_pain_none
        pain <= CoachFeedback.CALM_PAIN -> R.string.workout_pain_fine
        pain < CoachFeedback.TOO_MUCH_PAIN -> R.string.workout_pain_some
        else -> R.string.workout_pain_too_much
    },
)

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

/** The session, advanced [steps] times: the warm-up is step 0, the first set one [Workout.next] later. */
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

/** The cool-down is the session's last step: one [Workout.next] less than the finish. */
private fun previewCoolDown(): WorkoutUiState {
    val session = TrainingPreviewData.program.sessions.first { it.exercises.isNotEmpty() }
    return previewWorkout(steps = Workout.start(session, PREVIEW_START).steps.size - 1)
}

@Composable
private fun WorkoutContent(state: WorkoutUiState) {
    NovilaTheme {
        NovilaScaffold { padding ->
            WorkoutScreen(
                state,
                onNext = {},
                onEffort = {},
                onPain = {},
                onFinish = {},
                onShowHow = {},
                contentPadding = padding,
            )
        }
    }
}

@Preview(heightDp = 1100)
@Composable
private fun WorkoutWarmUpPreview() = WorkoutContent(previewWorkout(steps = 0))

@Preview(heightDp = 1100)
@Composable
private fun WorkoutSetPreview() = WorkoutContent(previewWorkout(steps = 1))

@Preview(heightDp = 1100)
@Composable
private fun WorkoutRestPreview() = WorkoutContent(previewWorkout(steps = 2))

@Preview(heightDp = 1100)
@Composable
private fun WorkoutCoolDownPreview() = WorkoutContent(previewCoolDown())

@Preview(heightDp = 1100, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun WorkoutFinishedDarkPreview() = WorkoutContent(previewWorkout(steps = Int.MAX_VALUE))

@Preview(heightDp = 1100)
@Composable
private fun WorkoutFeedbackPreview() = WorkoutContent(
    previewWorkout(steps = Int.MAX_VALUE).copy(feedback = SessionFeedbackDraft(effort = 7, pain = 6)),
)
