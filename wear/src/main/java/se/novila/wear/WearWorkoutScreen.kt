package se.novila.wear

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import se.novila.designsystem.theme.NovilaTheme
import se.novila.exercise.Dosage
import se.novila.exercise.PlannedExercise
import se.novila.exercise.StrengthLevel
import se.novila.exercise.Workout
import se.novila.exercise.WorkoutStep

/**
 * The watch's one screen. It shows the start screen, the running step or the summary. It is state-less: the entry
 * passes [WearWorkoutUiState] and callbacks, so every part can be previewed.
 */
@Composable
fun WearWorkoutScreen(
    state: WearWorkoutUiState,
    onLevel: (StrengthLevel) -> Unit,
    onSession: (Boolean) -> Unit,
    onStart: () -> Unit,
    onNext: () -> Unit,
    onNewSession: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val workout = state.workout
    KeepScreenOn(enabled = workout != null && !state.isFinished)
    // A new step starts at the top, instead of keeping the plan's scroll position.
    val scrollState = key(state.workout?.index, state.isFinished) { rememberScalingLazyListState() }
    ScreenScaffold(scrollState = scrollState, modifier = modifier) { innerPadding ->
        ScalingLazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when {
                workout == null -> planItems(state, onLevel, onSession, onStart)
                state.isFinished -> finishedItems(workout, onNewSession)
                else -> stepItems(state, onNext)
            }
        }
    }
}

/** Keeps the screen on while a session runs, so the set and the rest are readable at a glance. */
@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    val keepOn by rememberUpdatedState(enabled)
    DisposableEffect(view) {
        onDispose { view.keepScreenOn = false }
    }
    view.keepScreenOn = keepOn
}

private fun ScalingLazyListScope.planItems(
    state: WearWorkoutUiState,
    onLevel: (StrengthLevel) -> Unit,
    onSession: (Boolean) -> Unit,
    onStart: () -> Unit,
) {
    val plan = state.planned
    item {
        Text(
            text = stringResource(R.string.wear_plan_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
    }
    item {
        Text(
            text = stringResource(
                R.string.wear_plan_summary,
                stringResource(if (state.second) R.string.wear_session_b else R.string.wear_session_a),
                plan.exercises.size,
                plan.minutes,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
    item {
        Text(
            text = stringResource(R.string.wear_level_title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    items(StrengthLevel.entries.size) { index ->
        val level = StrengthLevel.entries[index]
        val selected = level == state.level
        val onClick = { onLevel(level) }
        if (selected) {
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(level.label))
            }
        } else {
            OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(level.label))
            }
        }
    }
    item {
        Text(
            text = stringResource(R.string.wear_session_switch),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    item {
        RowOfSessionChoices(second = state.second, onSession = onSession)
    }
    plan.exercises.forEach { exercise ->
        item { PlanLine(exercise) }
    }
    item {
        Text(
            text = stringResource(R.string.wear_plan_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
    item {
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.wear_start))
        }
    }
}

/** One row of the plan: name, sets and repetitions or seconds, so the watch shows the same dose as the phone. */
@Composable
private fun PlanLine(exercise: PlannedExercise) {
    Text(
        text = stringResource(
            if (exercise.dosage == Dosage.Seconds) R.string.wear_exercise_hold else R.string.wear_exercise_reps,
            stringResource(exercise.name),
            exercise.sets,
            exercise.amount,
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun RowOfSessionChoices(second: Boolean, onSession: (Boolean) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.xs),
    ) {
        val a = { onSession(false) }
        val b = { onSession(true) }
        if (second) {
            OutlinedButton(onClick = a) { Text(stringResource(R.string.wear_session_a)) }
        } else {
            Button(onClick = a) { Text(stringResource(R.string.wear_session_a)) }
        }
        if (second) {
            Button(onClick = b) { Text(stringResource(R.string.wear_session_b)) }
        } else {
            OutlinedButton(onClick = b) { Text(stringResource(R.string.wear_session_b)) }
        }
    }
}

private fun ScalingLazyListScope.stepItems(state: WearWorkoutUiState, onNext: () -> Unit) {
    val workout = state.workout ?: return
    when (val step = state.step) {
        is WorkoutStep.WarmUp -> timedItems(
            title = R.string.wear_warmup_title,
            body = R.string.wear_warmup_body,
            secondsLeft = state.secondsLeft,
            action = R.string.wear_warmup_done,
            onNext = onNext,
        )

        is WorkoutStep.Work -> workItems(workout, step, onNext)

        is WorkoutStep.Rest -> {
            val next = step.next
            item {
                Text(
                    text = stringResource(R.string.wear_rest_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            item { Countdown(secondsLeft = state.secondsLeft) }
            item {
                Text(
                    text = stringResource(
                        R.string.wear_rest_next,
                        stringResource(next.exercise.name),
                        next.set,
                        next.exercise.sets,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
            item {
                Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.wear_skip_rest))
                }
            }
        }

        is WorkoutStep.CoolDown -> timedItems(
            title = R.string.wear_cooldown_title,
            body = R.string.wear_cooldown_body,
            secondsLeft = state.secondsLeft,
            action = R.string.wear_cooldown_done,
            onNext = onNext,
        )

        null -> Unit
    }
}

private fun ScalingLazyListScope.workItems(workout: Workout, step: WorkoutStep.Work, onNext: () -> Unit) {
    val exercise = step.exercise
    item {
        Text(
            text = stringResource(
                R.string.wear_progress,
                step.exerciseNumber,
                workout.exerciseCount,
                step.set,
                exercise.sets,
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    item {
        Text(
            text = stringResource(exercise.name),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
    }
    item {
        Text(
            text = dosageText(exercise),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
    item {
        Text(
            text = stringResource(R.string.wear_set_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
    item {
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.wear_set_done))
        }
    }
}

private fun ScalingLazyListScope.timedItems(
    @StringRes title: Int,
    @StringRes body: Int,
    secondsLeft: Int?,
    @StringRes action: Int,
    onNext: () -> Unit,
) {
    item {
        Text(text = stringResource(title), style = MaterialTheme.typography.titleMedium)
    }
    item { Countdown(secondsLeft = secondsLeft) }
    item {
        Text(
            text = stringResource(body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
    item {
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(action))
        }
    }
}

@Composable
private fun Countdown(secondsLeft: Int?) {
    Text(
        text = stringResource(R.string.wear_countdown, (secondsLeft ?: 0) / 60, (secondsLeft ?: 0) % 60),
        style = MaterialTheme.typography.numeralLarge,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
    )
}

private fun ScalingLazyListScope.finishedItems(workout: Workout, onNewSession: () -> Unit) {
    item {
        Text(
            text = stringResource(R.string.wear_finished_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
    }
    item {
        Text(
            text = stringResource(
                R.string.wear_finished_summary,
                workout.exerciseCount,
                workout.setsDone,
                workout.finishedAt?.let(workout::minutes) ?: workout.minutes(workout.startedAt),
            ),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
    item {
        Text(
            text = stringResource(R.string.wear_finished_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
    item {
        Button(onClick = onNewSession, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.wear_new_session))
        }
    }
}

@Composable
private fun dosageText(exercise: PlannedExercise): String = when (exercise.dosage) {
    Dosage.Repetitions -> stringResource(
        if (exercise.perSide) R.string.wear_reps_side else R.string.wear_reps,
        exercise.amount,
    )

    Dosage.Seconds -> stringResource(R.string.wear_hold, exercise.amount)
}

/** The exercise's name from the phone's strings, or a neutral label if the watch doesn't know it yet. */
@get:StringRes
private val PlannedExercise.name: Int
    get() = when (exerciseId) {
        "squat" -> R.string.move_squat
        "push_up" -> R.string.move_push_up
        "glute_bridge" -> R.string.move_glute_bridge
        "plank" -> R.string.move_plank
        "superman" -> R.string.move_superman
        "mountain_climber" -> R.string.move_mountain_climber
        "reverse_lunge" -> R.string.move_reverse_lunge
        "good_morning" -> R.string.move_good_morning
        "crunch" -> R.string.move_crunch
        "bird_dog" -> R.string.move_bird_dog
        "jumping_jack" -> R.string.move_jumping_jack
        "split_squat" -> R.string.move_split_squat
        else -> R.string.wear_exercise_other
    }

@get:StringRes
private val StrengthLevel.label: Int
    get() = when (this) {
        StrengthLevel.Beginner -> R.string.wear_level_beginner
        StrengthLevel.Intermediate -> R.string.wear_level_intermediate
        StrengthLevel.Advanced -> R.string.wear_level_advanced
    }
