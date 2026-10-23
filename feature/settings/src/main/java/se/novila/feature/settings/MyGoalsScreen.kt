package se.novila.feature.settings

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import se.novila.coach.CoachPreferences
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaIcon
import se.novila.designsystem.component.NovilaListRow
import se.novila.designsystem.component.NovilaScaffold
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaColorRole
import se.novila.designsystem.theme.NovilaTheme
import se.novila.feature.settings.R
import se.novila.model.Goals
import se.novila.recipes.Allergen
import se.novila.recipes.Diet
import se.novila.recipes.FoodPreferences
import se.novila.sleep.SleepCoachPreferences
import se.novila.ui.common.ScreenList
import se.novila.ui.common.clockText
import se.novila.ui.common.foodPreferencesSummary
import se.novila.ui.common.hoursText
import se.novila.ui.common.stepsText
import java.time.LocalTime
import se.novila.ui.R as UiR

/** Where each part of My goals is changed. The entry connects these to the screens that change them. */
data class MyGoalsActions(
    val onOpenGoals: () -> Unit = {},
    val onOpenTrainingCoach: () -> Unit = {},
    val onOpenSleepCoach: () -> Unit = {},
    val onOpenFoodPreferences: () -> Unit = {},
    val onOpenCycle: () -> Unit = {},
)

/**
 * Everything Novila plans by, in one place: the daily goals, the training coach, the sleep coach, the food preferences
 * and *Follow your cycle*, each with what it is set to now and a row to where it is changed. The same screens open from
 * Your plan on each tab; this is where the user finds them all.
 */
@Composable
fun MyGoalsScreen(
    state: MyGoalsUiState,
    actions: MyGoalsActions,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val pillars = NovilaTheme.pillars
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "intro") {
            NovilaText(
                stringResource(R.string.my_goals_intro),
                style = NovilaTheme.typography.bodyMedium,
                color = NovilaTheme.colors.onSurfaceVariant,
            )
        }
        item(key = "daily") {
            val goals = state.goals
            Part(
                title = stringResource(R.string.my_goals_daily),
                icon = painterResource(UiR.drawable.ic_training),
                role = pillars.training,
                row = stringResource(R.string.my_goals_daily_row),
                summary = stringResource(
                    R.string.settings_goals_summary,
                    stepsText(goals.steps.toLong()),
                    hoursText(goals.sleepHours),
                    goals.waterGlasses,
                ),
                onClick = actions.onOpenGoals,
            )
        }
        item(key = "training") {
            val coach = state.coach
            Part(
                title = stringResource(UiR.string.nav_training),
                icon = painterResource(UiR.drawable.ic_training),
                role = pillars.training,
                row = stringResource(R.string.my_goals_training_row),
                summary = if (coach != null) {
                    pluralStringResource(
                        R.plurals.my_goals_training_on,
                        coach.daysPerWeek,
                        coach.daysPerWeek,
                        coach.sessionMinutes,
                    )
                } else {
                    stringResource(R.string.my_goals_training_off)
                },
                onClick = actions.onOpenTrainingCoach,
            )
        }
        item(key = "sleep") {
            val sleepCoach = state.sleepCoach
            Part(
                title = stringResource(UiR.string.nav_sleep),
                icon = painterResource(UiR.drawable.ic_sleep),
                role = pillars.sleep,
                row = stringResource(R.string.my_goals_sleep_row),
                summary = if (sleepCoach != null) {
                    stringResource(
                        R.string.my_goals_sleep_on,
                        clockText(sleepCoach.bedtime),
                        clockText(sleepCoach.wakeTime),
                    )
                } else {
                    stringResource(R.string.my_goals_sleep_off)
                },
                onClick = actions.onOpenSleepCoach,
            )
        }
        item(key = "nutrition") {
            Part(
                title = stringResource(UiR.string.nav_nutrition),
                icon = painterResource(UiR.drawable.ic_nutrition),
                role = pillars.nutrition,
                row = stringResource(R.string.food_preferences_title),
                summary = foodPreferencesSummary(state.food),
                onClick = actions.onOpenFoodPreferences,
            )
        }
        item(key = "cycle") {
            Part(
                title = stringResource(R.string.cycle_part),
                icon = painterResource(UiR.drawable.ic_training),
                role = pillars.training,
                row = stringResource(R.string.cycle_title),
                summary = stringResource(if (state.cycle) R.string.cycle_on else R.string.cycle_off),
                onClick = actions.onOpenCycle,
            )
        }
    }
}

/** One part of the user's goals: a card in the part's color with the row that leads to where it is changed. */
@Composable
private fun Part(
    title: String,
    icon: Painter,
    role: NovilaColorRole,
    row: String,
    summary: String,
    onClick: () -> Unit,
) {
    NovilaCard(title = title, icon = icon, role = role) {
        NovilaListRow(
            title = row,
            summary = summary,
            onClick = onClick,
            trailing = {
                // The row's title already says where it leads.
                NovilaIcon(
                    painter = painterResource(R.drawable.ic_chevron),
                    contentDescription = null,
                    tint = NovilaTheme.colors.onSurfaceVariant,
                )
            },
        )
    }
}

@Composable
private fun MyGoalsContent(state: MyGoalsUiState) {
    NovilaTheme {
        NovilaScaffold { padding ->
            MyGoalsScreen(state = state, actions = MyGoalsActions(), contentPadding = padding)
        }
    }
}

/** Nothing set up yet: the general goals, no coach and every recipe. */
@Preview(heightDp = 1000)
@Composable
private fun MyGoalsPreview() = MyGoalsContent(MyGoalsUiState(Goals(), null, null, FoodPreferences()))

/** Both coaches set up, and a vegetarian without peanuts. */
@Preview(heightDp = 1000, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun MyGoalsSetUpPreview() = MyGoalsContent(
    MyGoalsUiState(
        goals = Goals(steps = 10000, sleepHours = 7.5f),
        coach = CoachPreferences(daysPerWeek = 3, sessionMinutes = 45),
        sleepCoach = SleepCoachPreferences(bedtime = LocalTime.of(22, 30), wakeTime = LocalTime.of(6, 30)),
        food = FoodPreferences(Diet.Vegetarian, setOf(Allergen.Peanuts)),
        cycle = true,
    ),
)
