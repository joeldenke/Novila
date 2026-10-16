package se.novila.feature.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import se.novila.CoachUiState
import se.novila.R
import se.novila.coach.BodyArea
import se.novila.coach.CoachGoal
import se.novila.coach.CoachPreferences
import se.novila.coach.Consideration
import se.novila.designsystem.component.NovilaAccentButton
import se.novila.designsystem.component.NovilaButton
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaChoiceChip
import se.novila.designsystem.component.NovilaInsight
import se.novila.designsystem.component.NovilaStepper
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.ui.common.ScreenList
import se.novila.ui.common.StrengthSessionCard
import se.novila.ui.common.TrainingProgramCard
import se.novila.ui.common.coachNoteText
import se.novila.ui.common.label
import se.novila.ui.preview.PreviewData
import java.time.DayOfWeek

/**
 * The training coach: the user's goal, time, focus and considerations in, the week the coach plans out,
 * with its reasons. Before the coach is set up the answers are only tried out, and [onCreate] makes the
 * plan the week's program; after that every change applies right away and [onSuggest] asks for another.
 */
@Composable
fun CoachScreen(
    coach: CoachUiState,
    today: DayOfWeek,
    onChange: (CoachPreferences) -> Unit,
    onCreate: () -> Unit,
    onSuggest: () -> Unit,
    onReset: () -> Unit,
    onOpenExercise: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val preferences = coach.preferences
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "intro") {
            NovilaCard(
                title = stringResource(R.string.coach_title),
                icon = painterResource(R.drawable.ic_training),
                role = NovilaTheme.pillars.training,
            ) {
                NovilaText(
                    stringResource(R.string.coach_intro),
                    style = NovilaTheme.typography.bodyMedium,
                    color = NovilaTheme.colors.onSurfaceVariant,
                )
            }
        }
        item(key = "goal") {
            NovilaCard(title = stringResource(R.string.coach_goal_title)) {
                ChipRows(CoachGoal.entries, columns = 2) { goal, chipModifier ->
                    NovilaChoiceChip(
                        label = stringResource(goal.label),
                        selected = preferences.goal == goal,
                        onClick = { onChange(preferences.copy(goal = goal)) },
                        modifier = chipModifier,
                    )
                }
            }
        }
        item(key = "time") {
            val training = NovilaTheme.pillars.training
            val minutes = preferences.sessionMinutes
            val step = CoachPreferences.MINUTES_STEP
            NovilaCard(title = stringResource(R.string.coach_time_title)) {
                NovilaStepper(
                    title = stringResource(R.string.coach_days),
                    value = stringResource(R.string.coach_days_value, preferences.daysPerWeek),
                    onDecrement = { onChange(preferences.withDays(preferences.daysPerWeek - 1)) },
                    onIncrement = { onChange(preferences.withDays(preferences.daysPerWeek + 1)) },
                    decrementDescription = stringResource(R.string.coach_fewer_days),
                    incrementDescription = stringResource(R.string.coach_more_days),
                    role = training,
                    decrementEnabled = preferences.daysPerWeek > CoachPreferences.MIN_DAYS,
                )
                NovilaStepper(
                    title = stringResource(R.string.coach_minutes),
                    value = stringResource(R.string.coach_minutes_value, preferences.sessionMinutes),
                    onDecrement = { onChange(preferences.withMinutes(minutes - step)) },
                    onIncrement = { onChange(preferences.withMinutes(minutes + step)) },
                    decrementDescription = stringResource(R.string.coach_shorter),
                    incrementDescription = stringResource(R.string.coach_longer),
                    role = training,
                    decrementEnabled = preferences.sessionMinutes > CoachPreferences.MIN_MINUTES,
                )
            }
        }
        item(key = "focus") {
            NovilaCard(title = stringResource(R.string.coach_focus_title)) {
                ChipRows(BodyArea.entries, columns = 3) { area, chipModifier ->
                    NovilaChoiceChip(
                        label = stringResource(area.label),
                        selected = area in preferences.focus,
                        onClick = { onChange(preferences.toggle(area)) },
                        modifier = chipModifier,
                    )
                }
            }
        }
        item(key = "considerations") {
            NovilaCard(title = stringResource(R.string.coach_consider_title)) {
                ChipRows(Consideration.entries, columns = 2) { consideration, chipModifier ->
                    NovilaChoiceChip(
                        label = stringResource(consideration.label),
                        selected = consideration in preferences.considerations,
                        onClick = { onChange(preferences.toggle(consideration)) },
                        modifier = chipModifier,
                    )
                }
            }
        }
        item(key = "why") {
            val training = NovilaTheme.pillars.training
            NovilaCard(title = stringResource(R.string.coach_why_title)) {
                coach.plan.notes.forEach { note ->
                    NovilaInsight(painterResource(R.drawable.ic_training), coachNoteText(note), training)
                }
                if (coach.active) {
                    NovilaAccentButton(
                        stringResource(R.string.coach_suggest),
                        onClick = onSuggest,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    NovilaButton(
                        stringResource(R.string.coach_reset),
                        onClick = onReset,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    NovilaAccentButton(
                        stringResource(R.string.coach_create),
                        onClick = onCreate,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        item(key = "week") {
            TrainingProgramCard(coach.plan.program, today)
        }
        coach.plan.program.sessions.filter { it.exercises.isNotEmpty() }.forEach { session ->
            item(key = "strength-${session.day}") {
                StrengthSessionCard(session = session, onOpenExercise = onOpenExercise, onStart = null, next = false)
            }
        }
    }
}

/** [items] as chips, [columns] to a row, each as wide as the others. */
@Composable
private fun <T> ChipRows(items: List<T>, columns: Int, chip: @Composable (T, Modifier) -> Unit) {
    items.chunked(columns).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s)) {
            row.forEach { chip(it, Modifier.weight(1f)) }
            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun CoachScreenContent(coach: CoachUiState) {
    NovilaTheme {
        CoachScreen(
            coach = coach,
            today = PreviewData.today.date.dayOfWeek,
            onChange = {},
            onCreate = {},
            onSuggest = {},
            onReset = {},
            onOpenExercise = {},
            contentPadding = PaddingValues(),
        )
    }
}

@Preview(showBackground = true, heightDp = 3200)
@Composable
private fun CoachScreenPreview() = CoachScreenContent(PreviewData.coach)

@Preview(showBackground = true)
@Composable
private fun CoachScreenNewPreview() = CoachScreenContent(PreviewData.newCoach)
