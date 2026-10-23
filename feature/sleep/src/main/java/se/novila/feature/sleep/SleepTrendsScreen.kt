package se.novila.feature.sleep

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import se.novila.designsystem.component.NovilaReveal
import se.novila.designsystem.theme.NovilaTheme
import se.novila.model.BodyClock
import se.novila.model.Goals
import se.novila.model.History
import se.novila.model.HistoryRange
import se.novila.model.SleepAnalysis
import se.novila.model.SleepDebt
import se.novila.model.SleepRegularity
import se.novila.model.analyzeSleep
import se.novila.model.bodyClock
import se.novila.model.sleepDebt
import se.novila.model.sleepRegularity
import se.novila.ui.common.ScreenList
import se.novila.ui.common.SleepHistoryCard
import se.novila.ui.preview.PreviewData

/**
 * The sleep over time, opened from the Sleep tab: the [history] up to today over a week, four weeks or a year
 * ([onRange]) compared with the period before and the [goals], and what the user's logs say about their sleep
 * ([analysis]), how regular the nights are ([regularity]), and the sleep need: the two weeks' [debt] and the user's
 * [bodyClock].
 */
@Composable
fun SleepTrendsScreen(
    history: History,
    goals: Goals,
    analysis: SleepAnalysis,
    regularity: SleepRegularity,
    debt: SleepDebt,
    bodyClock: BodyClock?,
    onRange: (HistoryRange) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "history") { NovilaReveal(order = 0) { SleepHistoryCard(history, goals, onRange) } }
        item(key = "findings") { NovilaReveal(order = 1) { SleepFindingsCard(analysis) } }
        item(key = "need") { NovilaReveal(order = 2) { SleepNeedCard(debt, bodyClock, goals.sleepHours) } }
        item(key = "regularity") { NovilaReveal(order = 3) { SleepRegularityCard(regularity) } }
    }
}

@Composable
private fun SleepTrendsScreenContent(range: HistoryRange, analysis: SleepAnalysis = analyzeSleep(PreviewData.days())) {
    NovilaTheme {
        SleepTrendsScreen(
            history = PreviewData.history(range),
            goals = Goals(),
            analysis = analysis,
            regularity = sleepRegularity(PreviewData.days(), PreviewData.today.date),
            debt = sleepDebt(PreviewData.days(), Goals(), PreviewData.today.date),
            bodyClock = bodyClock(PreviewData.days(), PreviewData.today.date),
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
