package se.novila.feature.sleep

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import se.novila.designsystem.component.NovilaReveal
import se.novila.designsystem.theme.NovilaTheme
import se.novila.model.Goals
import se.novila.model.History
import se.novila.model.HistoryRange
import se.novila.model.SleepAnalysis
import se.novila.model.analyzeSleep
import se.novila.ui.common.ScreenList
import se.novila.ui.common.SleepHistoryCard
import se.novila.ui.preview.PreviewData

/**
 * The sleep over time, opened from the Sleep tab: the [history] up to today over a week, four weeks or a year
 * ([onRange]) compared with the period before and the [goals], and what the user's logs say about their sleep
 * ([analysis]).
 */
@Composable
fun SleepTrendsScreen(
    history: History,
    goals: Goals,
    analysis: SleepAnalysis,
    onRange: (HistoryRange) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "history") { NovilaReveal(order = 0) { SleepHistoryCard(history, goals, onRange) } }
        item(key = "findings") { NovilaReveal(order = 1) { SleepFindingsCard(analysis) } }
    }
}

@Composable
private fun SleepTrendsScreenContent(range: HistoryRange, analysis: SleepAnalysis = analyzeSleep(PreviewData.days())) {
    NovilaTheme {
        SleepTrendsScreen(
            history = PreviewData.history(range),
            goals = Goals(),
            analysis = analysis,
            onRange = {},
            contentPadding = PaddingValues(),
        )
    }
}

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun SleepTrendsScreenPreview() = SleepTrendsScreenContent(HistoryRange.Week)

/** The last twelve months, a point per month. */
@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun SleepTrendsScreenYearPreview() = SleepTrendsScreenContent(HistoryRange.Year)

/** Too few nights logged for any finding yet. */
@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun SleepTrendsScreenFewNightsPreview() = SleepTrendsScreenContent(
    HistoryRange.Week,
    analysis = analyzeSleep(PreviewData.days(count = 5)),
)
