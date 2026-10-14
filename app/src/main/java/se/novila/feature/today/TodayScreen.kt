package se.novila.feature.today

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import se.novila.DayEntry
import se.novila.R
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.ui.common.HistoryRow
import se.novila.ui.common.MoodCard
import se.novila.ui.common.ScreenList
import se.novila.ui.common.glassesText
import se.novila.ui.common.hoursText
import se.novila.ui.common.moodText
import se.novila.ui.preview.PreviewData

@Composable
fun TodayScreen(
    today: DayEntry,
    history: List<DayEntry>,
    onMood: (Int) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "mood") { MoodCard(today.mood, onMood) }
        item(key = "summary") {
            NovilaCard(title = stringResource(R.string.summary_title)) {
                SummaryLine(stringResource(R.string.water_title), glassesText(today.waterGlasses))
                SummaryLine(stringResource(R.string.nav_sleep), hoursText(today.sleepHours))
            }
        }
        item(key = "history-title") {
            NovilaText(stringResource(R.string.history_title), style = NovilaTheme.typography.titleMedium)
        }
        items(history, key = { it.date.toString() }) { day ->
            HistoryRow(day, glassesText(day.waterGlasses), hoursText(day.sleepHours), moodText(day.mood))
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    NovilaText(
        stringResource(R.string.summary_line, label, value),
        style = NovilaTheme.typography.bodyLarge,
    )
}

@Preview(showBackground = true)
@Composable
private fun TodayScreenPreview() {
    NovilaTheme {
        TodayScreen(
            today = PreviewData.today,
            history = PreviewData.history,
            onMood = {},
            contentPadding = PaddingValues(),
        )
    }
}
