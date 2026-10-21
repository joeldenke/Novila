package se.novila.feature.today

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaInsight
import se.novila.designsystem.component.NovilaReveal
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.model.Connection
import se.novila.model.Goals
import se.novila.model.History
import se.novila.model.HistoryRange
import se.novila.model.TrainingAfterSleep
import se.novila.model.TrendFeedback
import se.novila.model.trendFeedback
import se.novila.ui.common.HistoryRangePicker
import se.novila.ui.common.NutritionHistoryCard
import se.novila.ui.common.ScreenList
import se.novila.ui.common.SleepHistoryCard
import se.novila.ui.common.TrainingHistoryCard
import se.novila.ui.common.connectionText
import se.novila.ui.common.durationText
import se.novila.ui.common.historyBeforeText
import se.novila.ui.common.hoursText
import se.novila.ui.preview.PreviewData
import kotlin.math.abs
import se.novila.ui.R as UiR

/**
 * Sleep, food and training side by side over the same range ([onRange]: a week, four weeks or a year), opened from
 * Today's insights. It starts with what the [history] says against the range before ([feedback]: the parts that move
 * together first) and the strongest [connections] between the parts, then each part's chart against the [goals].
 */
@Composable
fun TrendsScreen(
    history: History,
    goals: Goals,
    feedback: List<TrendFeedback>,
    connections: List<Connection>,
    onRange: (HistoryRange) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "feedback") {
            NovilaReveal(order = 0) { FeedbackCard(history, goals, feedback, connections, onRange) }
        }
        item(key = "sleep") { NovilaReveal(order = 1) { SleepHistoryCard(history, goals) } }
        item(key = "nutrition") { NovilaReveal(order = 2) { NutritionHistoryCard(history, goals) } }
        item(key = "training") { NovilaReveal(order = 3) { TrainingHistoryCard(history) } }
    }
}

/** The range for every chart below, and what the range says in a few sentences. */
@Composable
private fun FeedbackCard(
    history: History,
    goals: Goals,
    feedback: List<TrendFeedback>,
    connections: List<Connection>,
    onRange: (HistoryRange) -> Unit,
) {
    NovilaCard(title = stringResource(R.string.trends_feedback_title)) {
        HistoryRangePicker(history.range, onRange, NovilaTheme.pillars.training)
        if (feedback.isEmpty() && connections.isEmpty()) {
            NovilaText(
                stringResource(R.string.trends_feedback_empty),
                style = NovilaTheme.typography.bodyMedium,
                color = NovilaTheme.colors.onSurfaceVariant,
            )
        }
        feedback.forEach { FeedbackRow(it, history.range, goals) }
        connections.forEach { connection ->
            NovilaInsight(
                painterResource(UiR.drawable.ic_insight),
                connectionText(connection),
                if (connection is TrainingAfterSleep) NovilaTheme.pillars.training else NovilaTheme.pillars.sleep,
            )
        }
        NovilaText(
            stringResource(R.string.trends_footer),
            style = NovilaTheme.typography.bodySmall,
            color = NovilaTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun FeedbackRow(feedback: TrendFeedback, range: HistoryRange, goals: Goals) {
    val pillars = NovilaTheme.pillars
    val sleepIcon = painterResource(UiR.drawable.ic_sleep)
    val nutritionIcon = painterResource(UiR.drawable.ic_nutrition)
    val trainingIcon = painterResource(UiR.drawable.ic_training)
    val before = historyBeforeText(range)
    when (feedback) {
        is TrendFeedback.BothImproving -> NovilaInsight(
            painterResource(UiR.drawable.ic_trend),
            stringResource(
                R.string.trends_both_improving,
                hoursText(feedback.sleepHours),
                feedback.nutritionPoints,
                before,
            ),
            pillars.sleep,
        )

        is TrendFeedback.MoreTrainingLessSleep -> NovilaInsight(
            trainingIcon,
            stringResource(
                R.string.trends_training_less_sleep,
                durationText(feedback.trainingMinutes),
                before,
                hoursText(abs(feedback.sleepHours)),
            ),
            pillars.training,
        )

        is TrendFeedback.Sleep -> NovilaInsight(
            sleepIcon,
            stringResource(
                if (feedback.hours > 0f) R.string.trends_sleep_more else R.string.trends_sleep_less,
                hoursText(abs(feedback.hours)),
                before,
            ),
            pillars.sleep,
        )

        is TrendFeedback.Nutrition -> NovilaInsight(
            nutritionIcon,
            stringResource(
                if (feedback.points > 0) R.string.trends_nutrition_up else R.string.trends_nutrition_down,
                abs(feedback.points),
                before,
            ),
            pillars.nutrition,
        )

        is TrendFeedback.Training -> NovilaInsight(
            trainingIcon,
            stringResource(
                if (feedback.minutes > 0) R.string.trends_training_more else R.string.trends_training_less,
                durationText(abs(feedback.minutes)),
                before,
            ),
            pillars.training,
        )

        is TrendFeedback.SleepGoal -> NovilaInsight(
            sleepIcon,
            stringResource(
                R.string.trends_sleep_goal,
                feedback.reached,
                feedback.logged,
                hoursText(goals.sleepHours),
            ),
            pillars.sleep,
        )

        is TrendFeedback.WaterGoal -> NovilaInsight(
            nutritionIcon,
            stringResource(R.string.trends_water_goal, feedback.reached, feedback.logged, goals.waterGlasses),
            pillars.nutrition,
        )
    }
}

@Composable
private fun TrendsContent(range: HistoryRange) {
    val history = PreviewData.history(range)
    NovilaTheme {
        TrendsScreen(
            history = history,
            goals = Goals(),
            feedback = trendFeedback(history),
            connections = emptyList(),
            onRange = {},
            contentPadding = PaddingValues(),
        )
    }
}

@Preview(showBackground = true, heightDp = 1800)
@Composable
private fun TrendsScreenPreview() = TrendsContent(HistoryRange.Week)

/** Four weeks, a bar per week for each part. */
@Preview(showBackground = true, heightDp = 1800)
@Composable
private fun TrendsScreenMonthPreview() = TrendsContent(HistoryRange.Month)

/** A tablet: the cards stay readable in the middle. */
@Preview(showBackground = true, widthDp = 900, heightDp = 1600)
@Composable
private fun TrendsScreenTabletPreview() = TrendsContent(HistoryRange.Week)
