package se.novila.feature.today

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import se.novila.DayEntry
import se.novila.DayTip
import se.novila.Greeting
import se.novila.R
import se.novila.WATER_GOAL
import se.novila.balance
import se.novila.designsystem.component.NovilaHeroCard
import se.novila.designsystem.component.NovilaHeroSegment
import se.novila.designsystem.component.NovilaScaffold
import se.novila.designsystem.component.NovilaStatTile
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.greetingFor
import se.novila.sleepProgress
import se.novila.tip
import se.novila.ui.common.HistoryRow
import se.novila.ui.common.MoodCard
import se.novila.ui.common.ScreenList
import se.novila.ui.common.glassesText
import se.novila.ui.common.hoursText
import se.novila.ui.common.longDateText
import se.novila.ui.common.moodText
import se.novila.ui.preview.PreviewData

/** The dashboard: greeting, today's balance, one tile per part, mood and recent days. */
@Composable
fun TodayScreen(
    today: DayEntry,
    history: List<DayEntry>,
    hour: Int,
    onMood: (Int) -> Unit,
    onOpenTraining: () -> Unit,
    onOpenSleep: () -> Unit,
    onOpenNutrition: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "header") { Header(today, greetingFor(hour)) }
        item(key = "balance") { BalanceCard(today) }
        item(key = "pillars") {
            PillarTiles(today, onOpenTraining, onOpenSleep, onOpenNutrition)
        }
        item(key = "mood") { MoodCard(today.mood, onMood) }
        item(key = "history-title") {
            NovilaText(stringResource(R.string.history_title), style = NovilaTheme.typography.titleMedium)
        }
        items(history, key = { it.date.toString() }) { day ->
            HistoryRow(day, glassesText(day.waterGlasses), hoursText(day.sleepHours), moodText(day.mood))
        }
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
private fun BalanceCard(today: DayEntry) {
    val hero = NovilaTheme.hero
    NovilaHeroCard(
        title = stringResource(R.string.balance_title),
        value = stringResource(R.string.balance_value, today.balance),
        unit = stringResource(R.string.balance_unit),
        body = when (val tip = today.tip()) {
            DayTip.LogSleep -> stringResource(R.string.tip_log_sleep)
            is DayTip.DrinkWater -> pluralStringResource(R.plurals.tip_drink_water, tip.glassesLeft, tip.glassesLeft)
            DayTip.GoalsMet -> stringResource(R.string.tip_goals_met)
        },
        segments = listOf(
            NovilaHeroSegment(today.sleepProgress, hero.sleep),
            NovilaHeroSegment(today.waterProgress, hero.nutrition),
        ),
    )
}

@Composable
private fun PillarTiles(
    today: DayEntry,
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
            icon = painterResource(R.drawable.ic_training),
            value = stringResource(R.string.value_missing),
            label = stringResource(R.string.nav_training),
            role = pillars.training,
            onClick = onOpenTraining,
            modifier = tile,
        )
        NovilaStatTile(
            icon = painterResource(R.drawable.ic_sleep),
            value = hoursText(today.sleepHours),
            label = stringResource(R.string.nav_sleep),
            role = pillars.sleep,
            onClick = onOpenSleep,
            modifier = tile,
        )
        NovilaStatTile(
            icon = painterResource(R.drawable.ic_nutrition),
            value = stringResource(R.string.water_fraction, today.waterGlasses, WATER_GOAL),
            label = stringResource(R.string.water_title),
            role = pillars.nutrition,
            onClick = onOpenNutrition,
            modifier = tile,
        )
    }
}

@Composable
private fun TodayScreenContent() {
    NovilaTheme {
        NovilaScaffold { padding ->
            TodayScreen(
                today = PreviewData.today,
                history = PreviewData.history,
                hour = 8,
                onMood = {},
                onOpenTraining = {},
                onOpenSleep = {},
                onOpenNutrition = {},
                contentPadding = padding,
            )
        }
    }
}

@Preview
@Composable
private fun TodayScreenPreview() = TodayScreenContent()

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun TodayScreenDarkPreview() = TodayScreenContent()
