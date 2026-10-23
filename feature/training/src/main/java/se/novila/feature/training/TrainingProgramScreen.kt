package se.novila.feature.training

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import se.novila.coach.CoachGoal
import se.novila.designsystem.component.NovilaButton
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaReveal
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.component.NovilaTextButton
import se.novila.designsystem.theme.NovilaTheme
import se.novila.exercise.TrainingLoad
import se.novila.feature.training.R
import se.novila.ui.common.ScreenList
import java.time.DayOfWeek

/**
 * The week's program, opened from the Training tab: every planned day (tap one to open its session,
 * [onOpenSession]), the coach (another program from the same answers, [onSuggest], and its settings,
 * [onOpenCoach]), once a session is done the weekly review of what's done and what next week holds, and the last
 * seven days' training load against the usual.
 */
@Composable
fun TrainingProgramScreen(
    state: TrainingUiState,
    onOpenSession: (DayOfWeek) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onSuggest: () -> Unit = {},
    onOpenCoach: () -> Unit = {},
) {
    val review = remember(state) { state.weeklyReview() }
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "program") {
            NovilaReveal(order = 0) {
                TrainingProgramCard(state.program, state.today, state.weekDone, onOpenSession = onOpenSession)
            }
        }
        item(key = "coach") { NovilaReveal(order = 1) { CoachSuggestCard(state.coach, onSuggest, onOpenCoach) } }
        if (review != null) {
            item(key = "review") { NovilaReveal(order = 2) { WeeklyReviewCard(review) } }
        }
        state.load?.let { load ->
            item(key = "load") { NovilaReveal(order = 3) { TrainingLoadCard(load) } }
        }
    }
}

/**
 * Where the program comes from. With the coach set up, another suggestion gives the strength sessions other
 * exercises ([onSuggest]; rehab is chosen by hand, so it has none) and says so once it has; the coach's settings
 * open from here ([onOpenCoach]), also before it is set up.
 */
@Composable
private fun CoachSuggestCard(coach: CoachUiState, onSuggest: () -> Unit, onOpenCoach: () -> Unit) {
    var suggested by rememberSaveable { mutableStateOf(false) }
    val canSuggest = coach.active && coach.preferences.goal != CoachGoal.Rehab
    NovilaCard(title = stringResource(R.string.coach_title)) {
        NovilaText(
            stringResource(
                when {
                    !coach.active -> R.string.program_coach_off
                    suggested -> R.string.program_coach_suggested
                    canSuggest -> R.string.program_coach_on
                    else -> R.string.program_coach_rehab
                },
            ),
            style = NovilaTheme.typography.bodyMedium,
            color = NovilaTheme.colors.onSurfaceVariant,
        )
        if (canSuggest) {
            NovilaButton(
                text = stringResource(R.string.coach_suggest),
                onClick = {
                    onSuggest()
                    suggested = true
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        NovilaTextButton(text = stringResource(R.string.program_coach_settings), onClick = onOpenCoach)
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
                load = TrainingLoad(week = 1240, usualWeek = 880, monotony = 1.1, sessions = 3, rated = 1),
            ),
            onOpenSession = {},
            contentPadding = PaddingValues(),
        )
    }
}

/** The coach is set up: it can suggest another program. */
@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun TrainingProgramScreenCoachPreview() {
    NovilaTheme {
        TrainingProgramScreen(
            state = TrainingUiState(
                program = TrainingPreviewData.program,
                coach = TrainingPreviewData.coach,
                today = DayOfWeek.MONDAY,
                strengthMinutesToday = 0,
            ),
            onOpenSession = {},
            contentPadding = PaddingValues(),
        )
    }
}
