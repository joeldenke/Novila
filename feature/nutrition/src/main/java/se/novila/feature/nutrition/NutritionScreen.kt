package se.novila.feature.nutrition

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import se.novila.designsystem.component.NovilaReveal
import se.novila.designsystem.theme.NovilaTheme
import se.novila.model.DayEntry
import se.novila.model.FoodGroup
import se.novila.model.weekOf
import se.novila.ui.common.NutritionWeekCard
import se.novila.ui.common.ScreenList
import se.novila.ui.preview.PreviewData

/** Today's nutrition score with its breakdown, logging per food group and the week's scores. */
@Composable
fun NutritionScreen(
    today: DayEntry,
    history: List<DayEntry>,
    onPortions: (FoodGroup, Int) -> Unit,
    onTreats: (Int) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val week = remember(today, history) { weekOf(today, history) }
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "score") { NovilaReveal(order = 0) { NutritionScoreCard(today) } }
        item(key = "log") { NovilaReveal(order = 1) { FoodLogCard(today, onPortions, onTreats) } }
        item(key = "week") { NovilaReveal(order = 2) { NutritionWeekCard(week) } }
    }
}

@Preview(showBackground = true, heightDp = 1800)
@Composable
private fun NutritionScreenPreview() {
    NovilaTheme {
        NutritionScreen(
            PreviewData.today,
            PreviewData.history,
            onPortions = { _, _ -> },
            onTreats = {},
            contentPadding = PaddingValues(),
        )
    }
}
