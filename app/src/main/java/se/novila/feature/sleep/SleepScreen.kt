package se.novila.feature.sleep

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import se.novila.DayEntry
import se.novila.designsystem.component.NovilaReveal
import se.novila.designsystem.theme.NovilaTheme
import se.novila.ui.common.ScreenList
import se.novila.ui.common.SleepCard
import se.novila.ui.common.SleepWeekCard
import se.novila.ui.preview.PreviewData
import se.novila.weekOf

@Composable
fun SleepScreen(
    today: DayEntry,
    history: List<DayEntry>,
    onSleep: (Float) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val week = remember(today, history) { weekOf(today, history) }
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "sleep") { NovilaReveal(order = 0) { SleepCard(today.sleepHours, onSleep) } }
        item(key = "week") { NovilaReveal(order = 1) { SleepWeekCard(week) } }
    }
}

@Preview(showBackground = true)
@Composable
private fun SleepScreenPreview() {
    NovilaTheme {
        SleepScreen(PreviewData.today, PreviewData.history, onSleep = {}, contentPadding = PaddingValues())
    }
}
