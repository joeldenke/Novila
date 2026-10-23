package se.novila.feature.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import se.novila.anatomy.Equipment
import se.novila.coach.BodyArea
import se.novila.coach.CoachGoal
import se.novila.coach.CoachPreferences
import se.novila.coach.Consideration
import se.novila.coach.RehabArea
import se.novila.designsystem.component.NovilaAccentButton
import se.novila.designsystem.component.NovilaButton
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaChoiceChip
import se.novila.designsystem.component.NovilaInsight
import se.novila.designsystem.component.NovilaStepper
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.component.NovilaTextButton
import se.novila.designsystem.theme.NovilaTheme
import se.novila.feature.training.R
import se.novila.ui.common.ScreenList
import java.time.DayOfWeek
import java.time.format.TextStyle
import se.novila.ui.R as UiR

/**
 * The training coach's settings, opened from the Training tab and from *My goals*: the user's goal, time and days,
 * focus, equipment and considerations, with the coach's reasons for what they give. Before the coach is set up the
 * answers are only tried out, and [onCreate] makes the plan the week's program; after that every change applies
 * right away. The week itself, and another suggestion for it, are on the Training tab. With the rehab goal, a rehab
 * area and phase replace the focus areas.
 */
@Composable
fun CoachScreen(
    coach: CoachUiState,
    onChange: (CoachPreferences) -> Unit,
    onCreate: () -> Unit,
    onReset: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val preferences = coach.preferences
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "intro") {
            NovilaCard(
                title = stringResource(R.string.coach_title),
                icon = painterResource(UiR.drawable.ic_training),
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
                    value = pluralStringResource(
                        R.plurals.coach_days_value,
                        preferences.daysPerWeek,
                        preferences.daysPerWeek,
                    ),
                    onDecrement = { onChange(preferences.withDays(preferences.daysPerWeek - 1)) },
                    onIncrement = { onChange(preferences.withDays(preferences.daysPerWeek + 1)) },
                    decrementDescription = stringResource(R.string.coach_fewer_days),
                    incrementDescription = stringResource(R.string.coach_more_days),
                    role = training,
                    decrementEnabled = preferences.daysPerWeek > CoachPreferences.MIN_DAYS,
                )
                Weekdays(preferences, onChange)
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
        if (preferences.goal == CoachGoal.Rehab) {
            item(key = "rehab") {
                RehabCard(preferences, onChange)
            }
        } else {
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
            item(key = "equipment") {
                NovilaCard(title = stringResource(R.string.coach_equipment_title)) {
                    NovilaText(
                        stringResource(R.string.coach_equipment_body),
                        style = NovilaTheme.typography.bodyMedium,
                        color = NovilaTheme.colors.onSurfaceVariant,
                    )
                    ChipRows(Equipment.entries, columns = 3) { equipment, chipModifier ->
                        NovilaChoiceChip(
                            label = stringResource(equipment.label),
                            selected = equipment in preferences.equipment,
                            onClick = { onChange(preferences.toggle(equipment)) },
                            modifier = chipModifier,
                        )
                    }
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
                    NovilaInsight(painterResource(UiR.drawable.ic_training), coachNoteText(note), training)
                }
                // The coach suggests a new rehab phase from the pain after the latest sessions; the user decides.
                val suggested = coach.plan.suggestedPhase
                if (coach.active && suggested != null) {
                    NovilaButton(
                        stringResource(R.string.coach_rehab_change_phase, suggested),
                        onClick = { onChange(preferences.withRehabPhase(suggested)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (coach.active) {
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
    }
}

/**
 * The weekdays to train on: the ones the coach spread out, or the user's own once they tap a day. A new number of
 * days, or the button, lets the coach spread them out again.
 */
@Composable
private fun Weekdays(preferences: CoachPreferences, onChange: (CoachPreferences) -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val chosen = preferences.trainingDays
    NovilaText(stringResource(R.string.coach_weekdays_title), style = NovilaTheme.typography.labelLarge)
    ChipRows(DayOfWeek.entries, columns = 4) { day, chipModifier ->
        NovilaChoiceChip(
            label = dayName(day, locale),
            selected = day in chosen,
            onClick = { onChange(preferences.toggle(day)) },
            contentDescription = day.getDisplayName(TextStyle.FULL, locale),
            modifier = chipModifier,
        )
    }
    NovilaText(
        stringResource(
            if (preferences.days.isEmpty()) R.string.coach_weekdays_spread else R.string.coach_weekdays_own,
        ),
        style = NovilaTheme.typography.bodySmall,
        color = NovilaTheme.colors.onSurfaceVariant,
    )
    if (preferences.days.isNotEmpty()) {
        NovilaTextButton(
            text = stringResource(R.string.coach_weekdays_spread_again),
            onClick = { onChange(preferences.withSpreadDays()) },
        )
    }
}

/** The rehab area and phase, with when to seek care instead. */
@Composable
private fun RehabCard(preferences: CoachPreferences, onChange: (CoachPreferences) -> Unit) {
    val training = NovilaTheme.pillars.training
    val phase = preferences.rehabPhase
    NovilaCard(title = stringResource(R.string.coach_rehab_title)) {
        ChipRows(RehabArea.entries, columns = 2) { area, chipModifier ->
            NovilaChoiceChip(
                label = stringResource(area.label),
                selected = preferences.rehabArea == area,
                onClick = { onChange(preferences.copy(rehabArea = area)) },
                modifier = chipModifier,
            )
        }
        NovilaStepper(
            title = stringResource(R.string.coach_rehab_phase),
            value = stringResource(
                R.string.coach_rehab_phase_value,
                phase,
                CoachPreferences.MAX_REHAB_PHASE,
                stringResource(rehabPhaseLabel(phase)),
            ),
            onDecrement = { onChange(preferences.withRehabPhase(phase - 1)) },
            onIncrement = { onChange(preferences.withRehabPhase(phase + 1)) },
            decrementDescription = stringResource(R.string.coach_rehab_easier),
            incrementDescription = stringResource(R.string.coach_rehab_harder),
            role = training,
            decrementEnabled = phase > 1,
        )
        NovilaText(
            stringResource(R.string.coach_rehab_safety),
            style = NovilaTheme.typography.bodySmall,
            color = NovilaTheme.colors.onSurfaceVariant,
        )
        NovilaText(
            stringResource(
                when (preferences.rehabArea) {
                    RehabArea.Knee -> R.string.coach_rehab_safety_knee
                    RehabArea.LowerBack -> R.string.coach_rehab_safety_lower_back
                    RehabArea.Shoulder -> R.string.coach_rehab_safety_shoulder
                    RehabArea.Neck -> R.string.coach_rehab_safety_neck
                    RehabArea.Hip -> R.string.coach_rehab_safety_hip
                },
            ),
            style = NovilaTheme.typography.bodySmall,
            color = NovilaTheme.colors.onSurfaceVariant,
        )
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
            onChange = {},
            onCreate = {},
            onReset = {},
            contentPadding = PaddingValues(),
        )
    }
}

@Preview(showBackground = true, heightDp = 2400)
@Composable
private fun CoachScreenPreview() = CoachScreenContent(TrainingPreviewData.coach)

/** The user picked their own days: the coach can spread them out again. */
@Preview(showBackground = true, heightDp = 2400)
@Composable
private fun CoachScreenOwnDaysPreview() = CoachScreenContent(
    TrainingPreviewData.coach.let {
        it.copy(preferences = it.preferences.toggle(DayOfWeek.SUNDAY).toggle(DayOfWeek.MONDAY))
    },
)

@Preview(showBackground = true)
@Composable
private fun CoachScreenNewPreview() = CoachScreenContent(TrainingPreviewData.newCoach)

@Preview(showBackground = true, heightDp = 2000)
@Composable
private fun CoachScreenRehabPreview() = CoachScreenContent(TrainingPreviewData.rehabCoach)
