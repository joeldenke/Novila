package se.novila.feature.breathing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import se.novila.breathing.BreathingCatalog
import se.novila.breathing.BreathingMoment
import se.novila.designsystem.component.NovilaAccentButton
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.component.NovilaTextButton
import se.novila.designsystem.theme.NovilaTheme
import se.novila.ui.common.ScreenList

/**
 * One guided session: the pacer in the middle, the phase and its count written out (so large text and screen readers
 * get them), the round, and the time left. Pause/continue is the screen's one main action; start again sits below it.
 *
 * On the last breath the screen says so and offers to start again. Nothing is saved.
 */
@Composable
fun BreathingSessionScreen(state: BreathingUiState, actions: BreathingActions, contentPadding: PaddingValues) {
    val sleep = NovilaTheme.pillars.sleep
    ScreenList(contentPadding = contentPadding) {
        item(key = "pacer") {
            NovilaCard(role = sleep) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.m),
                ) {
                    NovilaText(stringResource(sessionName(state.session)), style = NovilaTheme.typography.titleLarge)
                    val moment = state.moment
                    if (moment == null) {
                        SessionFinished()
                    } else {
                        BreathingPacer(
                            phase = moment.phase,
                            progress = moment.progress,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                        )
                        Phase(moment)
                        NovilaText(
                            stringResource(R.string.breathing_round, moment.round, state.session.rounds),
                            style = NovilaTheme.typography.labelLarge,
                            color = NovilaTheme.colors.onSurfaceVariant,
                        )
                        NovilaText(
                            stringResource(
                                R.string.breathing_remaining,
                                clockCountText(state.remainingSeconds),
                            ),
                            style = NovilaTheme.typography.labelMedium,
                            color = NovilaTheme.colors.onSurfaceVariant,
                        )
                    }
                    Actions(state, actions)
                    NovilaText(
                        stringResource(R.string.breathing_note),
                        style = NovilaTheme.typography.bodySmall,
                        color = NovilaTheme.colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** The phase in words and its second, the count large so it reads at a glance. */
@Composable
private fun Phase(moment: BreathingMoment) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        NovilaText(
            stringResource(moment.phase.label),
            style = NovilaTheme.typography.headlineSmall,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        NovilaText(
            moment.secondsLeft.toString(),
            style = NovilaTheme.typography.displayMedium,
            color = NovilaTheme.pillars.sleep.color,
        )
    }
}

/** Pause or continue, and start over. The accent is the one main action, pause/continue. */
@Composable
private fun Actions(state: BreathingUiState, actions: BreathingActions) {
    val sleep = NovilaTheme.pillars.sleep
    NovilaAccentButton(
        text = stringResource(if (state.running) R.string.breathing_pause else R.string.breathing_resume),
        onClick = actions.onToggle,
        modifier = Modifier.fillMaxWidth(),
    )
    NovilaTextButton(
        text = stringResource(R.string.breathing_restart),
        onClick = actions.onRestart,
        role = sleep,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** The last breath is over. */
@Composable
private fun SessionFinished() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s),
    ) {
        NovilaText(stringResource(R.string.breathing_done_title), style = NovilaTheme.typography.titleLarge)
        NovilaText(
            stringResource(R.string.breathing_done_body),
            style = NovilaTheme.typography.bodyMedium,
            color = NovilaTheme.colors.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun BreathingSessionScreenPreview() {
    NovilaTheme {
        BreathingSessionScreen(breathingPreviewState(), BreathingActions(), PaddingValues())
    }
}

/** A session a few seconds in, for previews and screenshot tests (which draw the settled frame). */
internal fun breathingPreviewState(): BreathingUiState = BreathingUiState(
    session = BreathingCatalog.box,
    moment = BreathingCatalog.box.momentAt(6_000L),
    elapsedMillis = 6_000L,
    running = true,
)

@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun BreathingSessionScreenFinishedPreview() {
    NovilaTheme {
        val finished = breathingPreviewState()
            .copy(moment = null, elapsedMillis = BreathingCatalog.box.totalMillis, running = false)
        BreathingSessionScreen(finished, BreathingActions(), PaddingValues())
    }
}
