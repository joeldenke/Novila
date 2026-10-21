package se.novila.feature.training

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import se.novila.designsystem.component.NovilaReveal
import se.novila.designsystem.theme.NovilaTheme
import se.novila.ui.common.ScreenList
import java.time.DayOfWeek

/**
 * The week's program, opened from the Training tab: every planned day (tap one to open its session,
 * [onOpenSession]), and once a session is done, the weekly review of what's done and what next week holds.
 */
@Composable
fun TrainingProgramScreen(
    state: TrainingUiState,
    onOpenSession: (DayOfWeek) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val review = remember(state) { state.weeklyReview() }
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "program") {
            NovilaReveal(order = 0) {
                TrainingProgramCard(state.program, state.today, state.weekDone, onOpenSession = onOpenSession)
            }
        }
        if (review != null) {
            item(key = "review") { NovilaReveal(order = 1) { WeeklyReviewCard(review) } }
        }
    }
}

/** Thursday, with Monday's strength and Tuesday's run done: the weekly review counts them. */
@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun TrainingProgramScreenPreview() {
    NovilaTheme {
        TrainingProgramScreen(
            state = TrainingUiState(
                program = TrainingPreviewData.program,
                coach = TrainingPreviewData.newCoach,
                today = DayOfWeek.THURSDAY,
                strengthMinutesToday = 0,
                weekDone = mapOf(DayOfWeek.MONDAY to 28, DayOfWeek.TUESDAY to 32),
                nextProgram = TrainingPreviewData.nextProgram,
            ),
            onOpenSession = {},
            contentPadding = PaddingValues(),
        )
    }
}
