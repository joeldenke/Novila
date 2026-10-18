package se.novila.feature.today

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import se.novila.designsystem.component.NovilaHeroCard
import se.novila.designsystem.component.NovilaHeroSegment
import se.novila.designsystem.component.NovilaIconButton
import se.novila.designsystem.component.NovilaReveal
import se.novila.designsystem.component.NovilaScaffold
import se.novila.designsystem.component.NovilaStatTile
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.feature.today.R
import se.novila.model.DayEntry
import se.novila.model.DayTip
import se.novila.model.Goals
import se.novila.model.Greeting
import se.novila.model.SleepAnalysis
import se.novila.model.TrainingToday
import se.novila.model.analyzeSleep
import se.novila.model.balance
import se.novila.model.dayChecklist
import se.novila.model.greetingFor
import se.novila.model.nutritionScore
import se.novila.model.sleepProgress
import se.novila.model.stepsProgress
import se.novila.model.tip
import se.novila.model.weekInsights
import se.novila.model.weekOf
import se.novila.movement.MovementState
import se.novila.ui.common.ScreenList
import se.novila.ui.common.hoursText
import se.novila.ui.common.longDateText
import se.novila.ui.common.nextPortionText
import se.novila.ui.common.stepsText
import se.novila.ui.preview.PreviewData
import se.novila.ui.R as UiR

/**
 * What to do today: greeting (by [name] when the profile has one), today's balance as rings, the day's [checklist]
 * (sleep, [the program's training][checklist], food, water and mood, each with the one button that does it), one
 * tile per part with its trend, what the logs say (the strongest finding about the user's [sleep] first, then the
 * week), and today's mood, energy, stress and diary line, which the checklist's mood task scrolls to. Everything is
 * measured against the user's [goals]; the weeks in charts are on each part's own tab. Blocks arrive one by one.
 */
@Composable
fun TodayScreen(
    today: DayEntry,
    history: List<DayEntry>,
    hour: Int,
    name: String,
    goals: Goals,
    sleep: SleepAnalysis,
    movement: MovementState,
    onMood: (Int) -> Unit,
    onEnergy: (Int) -> Unit,
    onStress: (Int) -> Unit,
    onDiary: (String) -> Unit,
    onAddWater: () -> Unit,
    onOpenTraining: () -> Unit,
    onOpenSleep: () -> Unit,
    onOpenNutrition: () -> Unit,
    onOpenSettings: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    training: TrainingToday = TrainingToday(),
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
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val actions = ChecklistActions(
        onOpenTraining = onOpenTraining,
        onOpenSleep = onOpenSleep,
        onOpenNutrition = onOpenNutrition,
        onAddWater = onAddWater,
        onMood = { scope.launch { list.animateScrollToItem(MOOD_ITEM) } },
    )
    ScreenList(contentPadding = contentPadding, modifier = modifier, state = list) {
        item(key = "header") { NovilaReveal(order = 0) { Header(today, greetingFor(hour), name, onOpenSettings) } }
        item(key = "balance") { NovilaReveal(order = 1) { BalanceCard(today, steps, goals) } }
        item(key = "checklist") { NovilaReveal(order = 2) { DayChecklistCard(checklist, actions) } }
        item(key = "pillars") {
            NovilaReveal(order = 3) {
                PillarTiles(today, week, steps, goals, onOpenTraining, onOpenSleep, onOpenNutrition)
            }
        }
        item(key = "insights") { NovilaReveal(order = 4) { InsightsCard(insights, sleep) } }
        // MOOD_ITEM: the checklist's mood task scrolls here.
        item(key = "mood") {
            NovilaReveal(order = 5) {
                MoodCard(today.mood, today.energy, today.stress, today.diary, onMood, onEnergy, onStress, onDiary)
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
private fun BalanceCard(today: DayEntry, steps: Long?, goals: Goals) {
    val hero = NovilaTheme.hero
    NovilaHeroCard(
        title = stringResource(R.string.balance_title),
        value = today.balance(steps, goals),
        unit = stringResource(UiR.string.balance_unit),
        body = when (val tip = today.tip(goals)) {
            DayTip.LogSleep -> stringResource(R.string.tip_log_sleep)
            is DayTip.Eat -> nextPortionText(tip.next)
            DayTip.GoalsMet -> stringResource(R.string.tip_goals_met)
        },
        segments = listOfNotNull(
            steps?.let {
                NovilaHeroSegment(stringResource(R.string.pillar_movement), stepsProgress(it, goals), hero.training)
            },
            NovilaHeroSegment(stringResource(UiR.string.nav_sleep), today.sleepProgress(goals), hero.sleep),
            NovilaHeroSegment(
                stringResource(UiR.string.nav_nutrition),
                today.nutritionScore(goals)?.progress ?: 0f,
                hero.nutrition,
            ),
        ),
    )
}

@Composable
private fun PillarTiles(
    today: DayEntry,
    week: List<DayEntry>,
    steps: Long?,
    goals: Goals,
    onOpenTraining: () -> Unit,
    onOpenSleep: () -> Unit,
    onOpenNutrition: () -> Unit,
) {
    val pillars = NovilaTheme.pillars
    Row(
        modifier = Modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s),
    ) {
        val tile = Modifier.weight(1f).fillMaxHeight()
        NovilaStatTile(
            icon = painterResource(UiR.drawable.ic_training),
            value = stepsText(steps),
            label = stringResource(if (steps != null) UiR.string.movement_steps else UiR.string.nav_training),
            role = pillars.training,
            onClick = onOpenTraining,
            modifier = tile,
            progress = steps?.let { stepsProgress(it, goals) },
        )
        NovilaStatTile(
            icon = painterResource(UiR.drawable.ic_sleep),
            value = hoursText(today.sleepHours),
            label = stringResource(UiR.string.nav_sleep),
            role = pillars.sleep,
            onClick = onOpenSleep,
            modifier = tile,
            trend = week.map { day -> day.sleepHours.takeIf { it > 0f } },
            trendRange = TileSleepRange,
        )
        val score = today.nutritionScore(goals)
        NovilaStatTile(
            icon = painterResource(UiR.drawable.ic_nutrition),
            value = score?.let { stringResource(UiR.string.balance_value, it.total) }
                ?: stringResource(UiR.string.value_missing),
            label = stringResource(R.string.nutrition_score_short),
            role = pillars.nutrition,
            onClick = onOpenNutrition,
            modifier = tile,
            trend = week.map { it.nutritionScore(goals)?.total?.toFloat() },
            trendRange = TileScoreRange,
        )
    }
}

/** The mood card's place in the list: header, balance, checklist, tiles, insights, mood. */
private const val MOOD_ITEM = 5

private val TileSleepRange = 4f..10f
private val TileScoreRange = 0f..100f

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
                movement = PreviewData.movement,
                onMood = {},
                onEnergy = {},
                onStress = {},
                onDiary = {},
                onAddWater = {},
                onOpenTraining = {},
                onOpenSleep = {},
                onOpenNutrition = {},
                onOpenSettings = {},
                contentPadding = padding,
                training = TrainingToday(plannedMinutes = 30),
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
