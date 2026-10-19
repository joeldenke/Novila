package se.novila.feature.today

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import se.novila.coach.DailyCoach
import se.novila.coach.dailyCoach
import se.novila.designsystem.component.NovilaHeroCard
import se.novila.designsystem.component.NovilaHeroSegment
import se.novila.designsystem.component.NovilaIconButton
import se.novila.designsystem.component.NovilaReveal
import se.novila.designsystem.component.NovilaScaffold
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.feature.today.R
import se.novila.model.Connection
import se.novila.model.DayEntry
import se.novila.model.Goals
import se.novila.model.Greeting
import se.novila.model.SleepAnalysis
import se.novila.model.TrainingAfterSleep
import se.novila.model.TrainingToday
import se.novila.model.WeekdayMetric
import se.novila.model.WeekdayPattern
import se.novila.model.analyzeSleep
import se.novila.model.balance
import se.novila.model.dayChecklist
import se.novila.model.greetingFor
import se.novila.model.nutritionScore
import se.novila.model.sleepProgress
import se.novila.model.stepsProgress
import se.novila.model.weekInsights
import se.novila.model.weekOf
import se.novila.movement.MovementState
import se.novila.ui.common.ScreenList
import se.novila.ui.common.longDateText
import se.novila.ui.preview.PreviewData
import java.time.DayOfWeek
import se.novila.ui.R as UiR

/**
 * What to do today: greeting (by [name] when the profile has one), today's balance as rings, the one [coach]'s
 * connected suggestion for the whole day, the day's checklist (sleep, the program's [training], food and water, each
 * with the one button that does it) and an insights card. When the logs join the parts ([connections]), a
 * connections card follows it. The rings are the summary of the day; tapping a part on
 * them opens that part's tab, so the balance is not repeated as a row of tiles. Everything is measured against the
 * user's [goals]; the weeks in charts are on each part's own tab. Blocks arrive one by one.
 */
@Composable
fun TodayScreen(
    today: DayEntry,
    history: List<DayEntry>,
    hour: Int,
    name: String,
    goals: Goals,
    sleep: SleepAnalysis,
    coach: DailyCoach,
    movement: MovementState,
    onAddWater: () -> Unit,
    onOpenTraining: () -> Unit,
    onOpenSleep: () -> Unit,
    onOpenNutrition: () -> Unit,
    onOpenSettings: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    training: TrainingToday = TrainingToday(),
    connections: List<Connection> = emptyList(),
) {
    // Today's steps once Health Connect is connected, otherwise null.
    val steps = (movement as? MovementState.Connected)?.today?.steps
    val week = remember(today, history) { weekOf(today, history) }
    // The week's insights, one fewer when the sleep findings have something to say first.
    val sleepFinding = sleep.change != null || sleep.links.isNotEmpty()
    val insights = remember(week, steps, goals, sleepFinding) {
        weekInsights(week, steps, limit = if (sleepFinding) 2 else 3, goals = goals)
    }
    val checklist = remember(today, goals, training) { dayChecklist(today, goals, training) }
    val actions = ChecklistActions(
        onOpenTraining = onOpenTraining,
        onOpenSleep = onOpenSleep,
        onOpenNutrition = onOpenNutrition,
        onAddWater = onAddWater,
    )
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "header") { NovilaReveal(order = 0) { Header(today, greetingFor(hour), name, onOpenSettings) } }
        item(key = "balance") {
            NovilaReveal(order = 1) {
                BalanceCard(today, steps, goals, onOpenTraining, onOpenSleep, onOpenNutrition)
            }
        }
        item(key = "coach") { NovilaReveal(order = 2) { DailyCoachCard(coach) } }
        item(key = "checklist") { NovilaReveal(order = 3) { DayChecklistCard(checklist, actions) } }
        item(key = "insights") { NovilaReveal(order = 4) { InsightsCard(insights, sleep) } }
        if (connections.isNotEmpty()) {
            item(key = "connections") { NovilaReveal(order = 5) { ConnectionsCard(connections) } }
        }
    }
}

@Composable
private fun Header(today: DayEntry, greeting: Greeting, name: String, onOpenSettings: () -> Unit) {
    // Today has no top bar, so Settings sits next to the greeting.
    Row(verticalAlignment = Alignment.CenterVertically) {
        GreetingText(today, greeting, name, Modifier.weight(1f))
        NovilaIconButton(
            painter = painterResource(UiR.drawable.ic_settings),
            contentDescription = stringResource(UiR.string.settings_title),
            onClick = onOpenSettings,
        )
    }
}

@Composable
private fun GreetingText(today: DayEntry, greeting: Greeting, name: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.xxs)) {
        NovilaText(
            longDateText(today.date),
            style = NovilaTheme.typography.bodyMedium,
            color = NovilaTheme.colors.onSurfaceVariant,
        )
        NovilaText(
            if (name.isEmpty()) {
                stringResource(
                    when (greeting) {
                        Greeting.Morning -> R.string.greeting_morning
                        Greeting.Afternoon -> R.string.greeting_afternoon
                        Greeting.Evening -> R.string.greeting_evening
                    },
                )
            } else {
                stringResource(
                    when (greeting) {
                        Greeting.Morning -> R.string.greeting_morning_named
                        Greeting.Afternoon -> R.string.greeting_afternoon_named
                        Greeting.Evening -> R.string.greeting_evening_named
                    },
                    name,
                )
            },
            style = NovilaTheme.typography.headlineMedium,
        )
    }
}

@Composable
private fun BalanceCard(
    today: DayEntry,
    steps: Long?,
    goals: Goals,
    onOpenTraining: () -> Unit,
    onOpenSleep: () -> Unit,
    onOpenNutrition: () -> Unit,
) {
    val hero = NovilaTheme.hero
    NovilaHeroCard(
        title = stringResource(R.string.balance_title),
        value = today.balance(steps, goals),
        unit = stringResource(UiR.string.balance_unit),
        segments = listOfNotNull(
            steps?.let { stepsToday ->
                NovilaHeroSegment(
                    label = stringResource(R.string.pillar_movement),
                    progress = stepsProgress(stepsToday, goals),
                    color = hero.training,
                    onClick = onOpenTraining,
                    clickLabel = stringResource(UiR.string.nav_training),
                )
            },
            NovilaHeroSegment(
                label = stringResource(UiR.string.nav_sleep),
                progress = today.sleepProgress(goals),
                color = hero.sleep,
                onClick = onOpenSleep,
                clickLabel = stringResource(UiR.string.nav_sleep),
            ),
            NovilaHeroSegment(
                label = stringResource(UiR.string.nav_nutrition),
                progress = today.nutritionScore(goals)?.progress ?: 0f,
                color = hero.nutrition,
                onClick = onOpenNutrition,
                clickLabel = stringResource(UiR.string.nav_nutrition),
            ),
        ),
    )
}

@Composable
private fun TodayScreenContent() {
    NovilaTheme {
        NovilaScaffold { padding ->
            TodayScreen(
                today = PreviewData.today,
                history = PreviewData.history,
                hour = 8,
                name = PreviewData.profile.name,
                goals = Goals(),
                sleep = analyzeSleep(PreviewData.days()),
                coach = dailyCoach(PreviewData.today, Goals(), TrainingToday(plannedMinutes = 30)),
                movement = PreviewData.movement,
                onAddWater = {},
                onOpenTraining = {},
                onOpenSleep = {},
                onOpenNutrition = {},
                onOpenSettings = {},
                contentPadding = padding,
                training = TrainingToday(plannedMinutes = 30),
                connections = listOf(
                    TrainingAfterSleep(35.0, 10.0, 14, 9, 2.8),
                    WeekdayPattern(WeekdayMetric.TrainingMinutes, DayOfWeek.TUESDAY, 35.0, DayOfWeek.SUNDAY, 5.0, 84),
                ),
            )
        }
    }
}

@Preview(heightDp = 2000)
@Composable
private fun TodayScreenPreview() = TodayScreenContent()

@Preview(heightDp = 2000, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun TodayScreenDarkPreview() = TodayScreenContent()
