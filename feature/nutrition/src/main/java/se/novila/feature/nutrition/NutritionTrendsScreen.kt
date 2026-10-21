package se.novila.feature.nutrition

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import se.novila.designsystem.component.NovilaReveal
import se.novila.designsystem.theme.NovilaTheme
import se.novila.model.Goals
import se.novila.model.History
import se.novila.model.HistoryRange
import se.novila.ui.common.NutritionHistoryCard
import se.novila.ui.common.ScreenList
import se.novila.ui.preview.PreviewData

/**
 * The nutrition scores in the [history] up to today, over a week, four weeks or a year ([onRange]) and compared with
 * the period before, water against [goals]. Opened from the Nutrition tab.
 */
@Composable
fun NutritionTrendsScreen(
    history: History,
    goals: Goals,
    onRange: (HistoryRange) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "history") { NovilaReveal(order = 0) { NutritionHistoryCard(history, goals, onRange) } }
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun NutritionTrendsScreenPreview() {
    NovilaTheme {
        NutritionTrendsScreen(
            history = PreviewData.history(HistoryRange.Week),
            goals = Goals(),
            onRange = {},
            contentPadding = PaddingValues(),
        )
    }
}

/** The last four weeks, a bar per week. */
@Preview(showBackground = true, heightDp = 800)
@Composable
private fun NutritionTrendsScreenMonthPreview() {
    NovilaTheme {
        NutritionTrendsScreen(
            history = PreviewData.history(HistoryRange.Month),
            goals = Goals(),
            onRange = {},
            contentPadding = PaddingValues(),
        )
    }
}
