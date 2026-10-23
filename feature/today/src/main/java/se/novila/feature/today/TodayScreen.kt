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
import se.novila.coach.readiness
import se.novila.designsystem.component.NovilaHeroCard
import se.novila.designsystem.component.NovilaHeroSegment
import se.novila.designsystem.component.NovilaIconButton
import se.novila.designsystem.component.NovilaReveal
import se.novila.designsystem.component.NovilaScaffold
import se.novila.designsystem.component.NovilaSky
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.exercise.TrainingLoad
import se.novila.feature.today.R
import se.novila.model.DayEntry
import se.novila.model.DiscoveryProgress
import se.novila.model.Goals
import se.novila.model.Greeting
import se.novila.model.SleepAnalysis
import se.novila.model.TrainingToday
import se.novila.model.analyzeSleep
import se.novila.model.balance
import se.novila.model.dayChecklist
import se.novila.model.discoveryProgress
import se.novila.model.greetingFor
import se.novila.model.nutritionScore
import se.novila.model.sleepProgress
import se.novila.model.stepsProgress
import se.novila.model.weekInsights
import se.novila.model.weekOf
import se.novila.movement.MovementState
import se.novila.ui.common.ScreenList
import se.novila.ui.common.longDateText
import se.novila.ui.common.setupHint
import se.novila.ui.preview.PreviewData
import se.novila.ui.R as UiR

/**
 * What to do today: greeting (by [name] when the profile has one), today's balance as rings, the day's checklist
 * led by the one [coach]'s connected suggestion (sleep, the program's [training], food and water, each with the one
 * button that does it) and the week's insights with what the logs will unlock ([upcoming]), the way to the trends of
 * sleep, food and training together ([onOpenTrends]), to all discoveries ([onOpenDiscoveries]), where the
 * connections between the parts are, and to the weeks in a row and milestones ([onOpenAchievements]). The rings are the summary of the day; tapping a part on
 * them opens that part's tab, so the balance is not repeated as a row of tiles. Everything is measured against the
 * user's [goals]; the weeks in charts are on each part's own tab. Blocks arrive one by one. When the introduction
 * was skipped ([setupIncomplete]), a quiet card under the greeting leads to finishing the setup ([onCompleteSetup]).
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
    onOpenDiscoveries: () -> Unit = {},
    onOpenTrends: () -> Unit = {},
    onOpenAchievements: () -> Unit = {},
    /** What the logs will unlock, shown in the insights until it is there. */
    upcoming: List<DiscoveryProgress> = emptyList(),
    /** The introduction was skipped, so the profile and the coaches are not set up: one card offers to finish it. */
    setupIncomplete: Boolean = false,
    onCompleteSetup: () -> Unit = {},
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
        setupHint(setupIncomplete, onCompleteSetup)
        item(key = "balance") {
            NovilaReveal(order = 1) {
                BalanceCard(today, steps, goals, NovilaSky.at(hour), onOpenTraining, onOpenSleep, onOpenNutrition)
            }
        }
        item(key = "checklist") { NovilaReveal(order = 2) { DayChecklistCard(checklist, actions, coach) } }
        item(key = "insights") {
            NovilaReveal(order = 3) {
                InsightsCard(insights, sleep, upcoming, onOpenTrends, onOpenDiscoveries, onOpenAchievements)
            }
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
    sky: NovilaSky,
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
        sky = sky,
    )
}

@Composable
private fun TodayScreenContent(
    hour: Int = 8,
    coach: DailyCoach = dailyCoach(PreviewData.today, Goals(), TrainingToday(plannedMinutes = 30)),
) {
    NovilaTheme {
        NovilaScaffold { padding ->
            TodayScreen(
                today = PreviewData.today,
                history = PreviewData.history,
                hour = hour,
                name = PreviewData.profile.name,
                goals = Goals(),
                sleep = analyzeSleep(PreviewData.days()),
                coach = coach,
                movement = PreviewData.movement,
                onAddWater = {},
                onOpenTraining = {},
                onOpenSleep = {},
                onOpenNutrition = {},
                onOpenSettings = {},
                contentPadding = padding,
                training = TrainingToday(plannedMinutes = 30),
                // Two weeks in: the weekday pattern is still on its way.
                upcoming = discoveryProgress(PreviewData.days(count = 14)),
            )
        }
    }
}

@Preview(heightDp = 1400)
@Composable
private fun TodayScreenPreview() = TodayScreenContent()

/** A short night and a heavy week: the coach suggests a calm day and says why. */
@Preview(heightDp = 1400)
@Composable
private fun TodayScreenCalmPreview() {
    val night = PreviewData.today.copy(sleepHours = 5.5f)
    val heavy = TrainingLoad(week = 1400, usualWeek = 1000, monotony = 1.0, sessions = 4, rated = 2)
    TodayScreenContent(
        coach = dailyCoach(
            night,
            Goals(),
            TrainingToday(plannedMinutes = 30),
            readiness = readiness(night, Goals(), load = heavy),
        ),
    )
}

/** Dark theme late in the evening, under the moon. */
@Preview(heightDp = 1400, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun TodayScreenDarkPreview() = TodayScreenContent(hour = 22)
