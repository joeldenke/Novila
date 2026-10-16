package se.novila.feature.today

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import se.novila.designsystem.component.NovilaHeroCard
import se.novila.designsystem.component.NovilaHeroSegment
import se.novila.designsystem.component.NovilaReveal
import se.novila.designsystem.component.NovilaScaffold
import se.novila.designsystem.component.NovilaStatTile
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.feature.today.R
import se.novila.model.DayEntry
import se.novila.model.DayTip
import se.novila.model.Greeting
import se.novila.model.balance
import se.novila.model.greetingFor
import se.novila.model.nutritionScore
import se.novila.model.sleepProgress
import se.novila.model.stepsProgress
import se.novila.model.tip
import se.novila.model.weekInsights
import se.novila.model.weekOf
import se.novila.movement.MovementState
import se.novila.ui.common.NutritionWeekCard
import se.novila.ui.common.ScreenList
import se.novila.ui.common.SleepWeekCard
import se.novila.ui.common.hoursText
import se.novila.ui.common.longDateText
import se.novila.ui.common.nextPortionText
import se.novila.ui.common.stepsText
import se.novila.ui.preview.PreviewData
import se.novila.ui.R as UiR

/**
 * The dashboard: greeting, today's balance as rings, one tile per part with its trend, what the
 * week's logs say, the week's sleep and nutrition, and today's mood. Blocks arrive one by one.
 */
@Composable
fun TodayScreen(
    today: DayEntry,
    history: List<DayEntry>,
    hour: Int,
    movement: MovementState,
    onMood: (Int) -> Unit,
    onOpenTraining: () -> Unit,
    onOpenSleep: () -> Unit,
    onOpenNutrition: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    // Today's steps once Health Connect is connected, otherwise null.
    val steps = (movement as? MovementState.Connected)?.today?.steps
    val week = remember(today, history) { weekOf(today, history) }
    val insights = remember(week, steps) { weekInsights(week, steps) }
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "header") { NovilaReveal(order = 0) { Header(today, greetingFor(hour)) } }
        item(key = "balance") { NovilaReveal(order = 1) { BalanceCard(today, steps) } }
        item(key = "pillars") {
            NovilaReveal(order = 2) {
                PillarTiles(today, week, steps, onOpenTraining, onOpenSleep, onOpenNutrition)
            }
        }
        item(key = "insights") { NovilaReveal(order = 3) { InsightsCard(insights) } }
        item(key = "sleep-week") { NovilaReveal(order = 4) { SleepWeekCard(week) } }
        item(key = "nutrition-week") { NovilaReveal(order = 5) { NutritionWeekCard(week) } }
        item(key = "mood") { NovilaReveal(order = 6) { MoodCard(today.mood, onMood) } }
    }
}

@Composable
private fun Header(today: DayEntry, greeting: Greeting) {
    Column(verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.xxs)) {
        NovilaText(
            longDateText(today.date),
            style = NovilaTheme.typography.bodyMedium,
            color = NovilaTheme.colors.onSurfaceVariant,
        )
        NovilaText(
            stringResource(
                when (greeting) {
                    Greeting.Morning -> R.string.greeting_morning
                    Greeting.Afternoon -> R.string.greeting_afternoon
                    Greeting.Evening -> R.string.greeting_evening
                },
            ),
            style = NovilaTheme.typography.headlineMedium,
        )
    }
}

@Composable
private fun BalanceCard(today: DayEntry, steps: Long?) {
    val hero = NovilaTheme.hero
    NovilaHeroCard(
        title = stringResource(R.string.balance_title),
        value = today.balance(steps),
        unit = stringResource(UiR.string.balance_unit),
        body = when (val tip = today.tip()) {
            DayTip.LogSleep -> stringResource(R.string.tip_log_sleep)
            is DayTip.Eat -> nextPortionText(tip.next)
            DayTip.GoalsMet -> stringResource(R.string.tip_goals_met)
        },
        segments = listOfNotNull(
            steps?.let {
                NovilaHeroSegment(stringResource(R.string.pillar_movement), stepsProgress(it), hero.training)
            },
            NovilaHeroSegment(stringResource(UiR.string.nav_sleep), today.sleepProgress, hero.sleep),
            NovilaHeroSegment(
                stringResource(UiR.string.nav_nutrition),
                today.nutritionScore()?.progress ?: 0f,
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
            progress = steps?.let(::stepsProgress),
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
        val score = today.nutritionScore()
        NovilaStatTile(
            icon = painterResource(UiR.drawable.ic_nutrition),
            value = score?.let { stringResource(UiR.string.balance_value, it.total) }
                ?: stringResource(UiR.string.value_missing),
            label = stringResource(R.string.nutrition_score_short),
            role = pillars.nutrition,
            onClick = onOpenNutrition,
            modifier = tile,
            trend = week.map { it.nutritionScore()?.total?.toFloat() },
            trendRange = TileScoreRange,
        )
    }
}

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
                movement = PreviewData.movement,
                onMood = {},
                onOpenTraining = {},
                onOpenSleep = {},
                onOpenNutrition = {},
                contentPadding = padding,
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
