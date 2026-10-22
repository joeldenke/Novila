package se.novila.feature.training

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import se.novila.designsystem.component.NovilaReveal
import se.novila.designsystem.theme.NovilaTheme
import se.novila.model.DayEntry
import se.novila.model.HistoryRange
import se.novila.model.trainingMinutes
import se.novila.ui.common.ScreenList
import se.novila.ui.common.TrainingHistoryCard
import se.novila.ui.preview.PreviewData

/**
 * The training over time, opened from the Training tab: the minutes over a week, four weeks or a year ([onRange]),
 * compared with the range before, counting the watch's workouts with Novila's own ([state]).
 */
@Composable
fun TrainingTrendsScreen(
    state: TrainingTrendsUiState,
    onRange: (HistoryRange) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val minutes = remember(state.minutes) { { day: DayEntry -> state.minutes[day.date] ?: 0 } }
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "history") {
            NovilaReveal(order = 0) {
                TrainingHistoryCard(
                    history = state.history,
                    onRange = onRange,
                    minutes = minutes,
                    includesWatch = state.includesWatch,
                )
            }
        }
    }
}

@Composable
private fun TrainingTrendsContent(range: HistoryRange, includesWatch: Boolean = true) {
    val history = PreviewData.history(range)
    NovilaTheme {
        TrainingTrendsScreen(
            state = TrainingTrendsUiState(
                history = history,
                minutes = (history.previous + history.days).associate { it.date to it.trainingMinutes },
                includesWatch = includesWatch,
            ),
            onRange = {},
            contentPadding = PaddingValues(),
        )
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun TrainingTrendsScreenPreview() = TrainingTrendsContent(HistoryRange.Week)

/** Four weeks, a bar per week, without a watch connected. */
@Preview(showBackground = true, heightDp = 800)
@Composable
private fun TrainingTrendsScreenMonthPreview() = TrainingTrendsContent(HistoryRange.Month, includesWatch = false)

/** The last twelve months, a bar per month. */
@Preview(showBackground = true, heightDp = 800)
@Composable
private fun TrainingTrendsScreenYearPreview() = TrainingTrendsContent(HistoryRange.Year)
