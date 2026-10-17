package se.novila.feature.nutrition

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import se.novila.designsystem.component.NovilaReveal
import se.novila.designsystem.theme.NovilaTheme
import se.novila.model.DayEntry
import se.novila.model.FoodGroup
import se.novila.model.Goals
import se.novila.model.History
import se.novila.model.HistoryRange
import se.novila.ui.common.DayPicker
import se.novila.ui.common.NutritionHistoryCard
import se.novila.ui.common.ScreenList
import se.novila.ui.preview.PreviewData
import java.time.LocalDate

/**
 * The shown [day]'s nutrition score with its breakdown, logging per food group and the scores in the [history] up to
 * it, over a week, four weeks or a year ([onRange]), water against [goals]. [today] is the day it really is: the user
 * can go back to an earlier day ([onSelectDay]) and fill in what was forgotten.
 */
@Composable
fun NutritionScreen(
    day: DayEntry,
    history: History,
    goals: Goals,
    onPortions: (FoodGroup, Int) -> Unit,
    onTreats: (Int) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    today: LocalDate = day.date,
    onSelectDay: (LocalDate) -> Unit = {},
    onRange: (HistoryRange) -> Unit = {},
) {
    val isToday = day.date == today
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "day") { DayPicker(day.date, today, onSelectDay, NovilaTheme.pillars.nutrition) }
        item(key = "score") { NovilaReveal(order = 0) { NutritionScoreCard(day, goals, isToday) } }
        item(key = "log") { NovilaReveal(order = 1) { FoodLogCard(day, goals, onPortions, onTreats, isToday) } }
        item(key = "history") { NovilaReveal(order = 2) { NutritionHistoryCard(history, goals, onRange) } }
    }
}

@Preview(showBackground = true, heightDp = 1800)
@Composable
private fun NutritionScreenPreview() {
    NovilaTheme {
        NutritionScreen(
            PreviewData.today,
            PreviewData.history(HistoryRange.Week),
            Goals(),
            onPortions = { _, _ -> },
            onTreats = {},
            contentPadding = PaddingValues(),
        )
    }
}

/** Gone back to yesterday. */
@Preview(showBackground = true, heightDp = 1800)
@Composable
private fun NutritionScreenYesterdayPreview() {
    NovilaTheme {
        NutritionScreen(
            day = PreviewData.history.first(),
            history = PreviewData.history(HistoryRange.Week, until = PreviewData.history.first().date),
            goals = Goals(),
            onPortions = { _, _ -> },
            onTreats = {},
            contentPadding = PaddingValues(),
            today = PreviewData.date,
        )
    }
}

/** The last four weeks, a bar per week. */
@Preview(showBackground = true, heightDp = 1800)
@Composable
private fun NutritionScreenMonthPreview() {
    NovilaTheme {
        NutritionScreen(
            PreviewData.today,
            PreviewData.history(HistoryRange.Month),
            Goals(),
            onPortions = { _, _ -> },
            onTreats = {},
            contentPadding = PaddingValues(),
        )
    }
}
