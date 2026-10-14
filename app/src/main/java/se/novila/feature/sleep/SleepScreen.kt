package se.novila.feature.sleep

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import se.novila.DayEntry
import se.novila.R
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.ui.common.HistoryRow
import se.novila.ui.common.ScreenList
import se.novila.ui.common.SleepCard
import se.novila.ui.common.hoursText
import se.novila.ui.preview.PreviewData

@Composable
fun SleepScreen(
    today: DayEntry,
    history: List<DayEntry>,
    onSleep: (Float) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "sleep") { SleepCard(today.sleepHours, onSleep) }
        item(key = "history-title") {
            NovilaText(stringResource(R.string.history_title), style = NovilaTheme.typography.titleMedium)
        }
        items(history, key = { it.date.toString() }) { day -> HistoryRow(day, hoursText(day.sleepHours)) }
    }
}

@Preview(showBackground = true)
@Composable
private fun SleepScreenPreview() {
    NovilaTheme {
        SleepScreen(PreviewData.today, PreviewData.history, onSleep = {}, contentPadding = PaddingValues())
    }
}
