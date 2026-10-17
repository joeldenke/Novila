package se.novila.feature.settings

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import se.novila.designsystem.component.NovilaButton
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaScaffold
import se.novila.designsystem.component.NovilaStepper
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaColorRole
import se.novila.designsystem.theme.NovilaTheme
import se.novila.feature.settings.R
import se.novila.model.Goals
import se.novila.ui.common.ScreenList
import se.novila.ui.common.hoursText
import se.novila.ui.common.stepsText

/** What the goals screen can do: change one goal, or take the profile's suggestion. */
data class GoalsActions(
    val onChange: ((Goals) -> Goals) -> Unit = {},
    val onUseSuggestion: (Goals) -> Unit = {},
)

/**
 * The user's daily goals for steps, sleep and water, which Today, Sleep and Nutrition measure against. With age or
 * weight in the profile, a card suggests goals that fit; otherwise it says what the suggestion needs.
 */
@Composable
fun GoalsScreen(
    goals: Goals,
    suggestion: Goals?,
    actions: GoalsActions,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "goals") { GoalsCard(goals, actions.onChange) }
        item(key = "suggestion") { SuggestionCard(goals, suggestion, actions.onUseSuggestion) }
        item(key = "about") {
            NovilaText(
                stringResource(R.string.goals_about),
                style = NovilaTheme.typography.bodyMedium,
                color = NovilaTheme.colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GoalsCard(goals: Goals, onChange: ((Goals) -> Goals) -> Unit) {
    val pillars = NovilaTheme.pillars
    NovilaCard(title = stringResource(R.string.goals_daily)) {
        Column(verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.m)) {
            GoalStepper(
                title = stringResource(R.string.goals_steps),
                value = stringResource(R.string.goals_steps_value, stepsText(goals.steps.toLong())),
                onChange = { delta -> onChange { it.withSteps(delta) } },
                canLower = goals.steps > Goals.STEPS.first,
                canRaise = goals.steps < Goals.STEPS.last,
                role = pillars.training,
            )
            GoalStepper(
                title = stringResource(R.string.goals_sleep),
                value = stringResource(R.string.goals_sleep_value, hoursText(goals.sleepHours)),
                onChange = { delta -> onChange { it.withSleep(delta) } },
                canLower = goals.sleepHours > Goals.SLEEP_HOURS.start,
                canRaise = goals.sleepHours < Goals.SLEEP_HOURS.endInclusive,
                role = pillars.sleep,
            )
            GoalStepper(
                title = stringResource(R.string.goals_water),
                value = stringResource(R.string.goals_water_value, goals.waterGlasses),
                onChange = { delta -> onChange { it.withWater(delta) } },
                canLower = goals.waterGlasses > Goals.WATER_GLASSES.first,
                canRaise = goals.waterGlasses < Goals.WATER_GLASSES.last,
                role = pillars.nutrition,
            )
        }
    }
}

@Composable
private fun GoalStepper(
    title: String,
    value: String,
    onChange: (Int) -> Unit,
    canLower: Boolean,
    canRaise: Boolean,
    role: NovilaColorRole,
) {
    NovilaStepper(
        title = title,
        value = value,
        onDecrement = { onChange(-1) },
        onIncrement = { onChange(+1) },
        decrementDescription = stringResource(R.string.goals_lower, title),
        incrementDescription = stringResource(R.string.goals_raise, title),
        role = role,
        decrementEnabled = canLower,
        incrementEnabled = canRaise,
    )
}

/** Goals from the profile: what they would be and a button to use them, or what the profile is missing. */
@Composable
private fun SuggestionCard(goals: Goals, suggestion: Goals?, onUseSuggestion: (Goals) -> Unit) {
    NovilaCard(title = stringResource(R.string.goals_suggestion)) {
        if (suggestion == null) {
            Body(stringResource(R.string.goals_suggestion_needs_profile))
            return@NovilaCard
        }
        Body(
            stringResource(
                R.string.goals_suggestion_body,
                stepsText(suggestion.steps.toLong()),
                hoursText(suggestion.sleepHours),
                suggestion.waterGlasses,
            ),
        )
        if (suggestion == goals) {
            Body(stringResource(R.string.goals_suggestion_in_use))
        } else {
            NovilaButton(
                text = stringResource(R.string.goals_suggestion_use),
                onClick = { onUseSuggestion(suggestion) },
            )
        }
    }
}

@Composable
private fun Body(text: String) {
    NovilaText(text, style = NovilaTheme.typography.bodyMedium, color = NovilaTheme.colors.onSurfaceVariant)
}

@Composable
private fun GoalsContent(goals: Goals, suggestion: Goals?) {
    NovilaTheme {
        NovilaScaffold { padding ->
            GoalsScreen(goals = goals, suggestion = suggestion, actions = GoalsActions(), contentPadding = padding)
        }
    }
}

@Preview(heightDp = 900)
@Composable
private fun GoalsPreview() = GoalsContent(Goals(), suggestion = Goals(waterGlasses = 7))

// At the top of every range, and a profile without age or weight.
@Preview(heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun GoalsDarkPreview() = GoalsContent(Goals(steps = 20000, sleepHours = 10f, waterGlasses = 16), null)
