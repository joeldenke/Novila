package se.novila.feature.today

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import se.novila.designsystem.component.NovilaReveal
import se.novila.designsystem.theme.NovilaTheme
import se.novila.model.Connection
import se.novila.model.Goals
import se.novila.model.Insight
import se.novila.model.SleepAnalysis
import se.novila.model.TrainingAfterSleep
import se.novila.model.WeekdayMetric
import se.novila.model.WeekdayPattern
import se.novila.model.analyzeSleep
import se.novila.model.weekInsights
import se.novila.ui.common.ScreenList
import se.novila.ui.preview.PreviewData
import java.time.DayOfWeek

/**
 * Everything the logs say, opened from Today: the week's [insights] with what the nights say ([sleep]), and, when
 * the logs join the parts, the [connections] between them.
 */
@Composable
fun DiscoveriesScreen(
    insights: List<Insight>,
    sleep: SleepAnalysis,
    connections: List<Connection>,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "insights") { NovilaReveal(order = 0) { InsightsCard(insights, sleep) } }
        if (connections.isNotEmpty()) {
            item(key = "connections") { NovilaReveal(order = 1) { ConnectionsCard(connections) } }
        }
    }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun DiscoveriesScreenPreview() {
    NovilaTheme {
        DiscoveriesScreen(
            insights = weekInsights(PreviewData.history.reversed() + PreviewData.today, 9000, limit = 5),
            sleep = analyzeSleep(PreviewData.days()),
            connections = listOf(
                TrainingAfterSleep(35.0, 10.0, 14, 9, 2.8),
                WeekdayPattern(WeekdayMetric.TrainingMinutes, DayOfWeek.TUESDAY, 35.0, DayOfWeek.SUNDAY, 5.0, 84),
            ),
            contentPadding = PaddingValues(),
        )
    }
}

/** Too few days logged to say anything yet. */
@Preview(showBackground = true, heightDp = 600)
@Composable
private fun DiscoveriesScreenEmptyPreview() {
    NovilaTheme {
        DiscoveriesScreen(
            insights = emptyList(),
            sleep = analyzeSleep(emptyList(), Goals()),
            connections = emptyList(),
            contentPadding = PaddingValues(),
        )
    }
}
