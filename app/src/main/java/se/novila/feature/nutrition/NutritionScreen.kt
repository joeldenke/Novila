package se.novila.feature.nutrition

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
import se.novila.ui.common.WaterCard
import se.novila.ui.common.glassesText
import se.novila.ui.preview.PreviewData

@Composable
fun NutritionScreen(
    today: DayEntry,
    history: List<DayEntry>,
    onAddWater: () -> Unit,
    onRemoveWater: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "water") { WaterCard(today, onAddWater, onRemoveWater) }
        item(key = "history-title") {
            NovilaText(stringResource(R.string.history_title), style = NovilaTheme.typography.titleMedium)
        }
        items(history, key = { it.date.toString() }) { day -> HistoryRow(day, glassesText(day.waterGlasses)) }
    }
}

@Preview(showBackground = true)
@Composable
private fun NutritionScreenPreview() {
    NovilaTheme {
        NutritionScreen(
            PreviewData.today,
            PreviewData.history,
            onAddWater = {},
            onRemoveWater = {},
            contentPadding = PaddingValues(),
        )
    }
}
