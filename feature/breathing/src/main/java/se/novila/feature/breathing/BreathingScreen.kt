package se.novila.feature.breathing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import se.novila.breathing.BreathingCatalog
import se.novila.breathing.BreathingSession
import se.novila.designsystem.component.NovilaButton
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaReveal
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.ui.common.ScreenList

/** What the breathing screens can do. The entries connect these to navigation and the session's ViewModel. */
data class BreathingActions(
    val onOpenSession: (String) -> Unit = {},
    val onToggle: () -> Unit = {},
    val onRestart: () -> Unit = {},
)

/**
 * Breathing and relaxation: every session in [BreathingCatalog], each with its length and a start button. The
 * sessions are data, so this list is the same however many there are.
 */
@Composable
fun BreathingScreen(actions: BreathingActions, contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    val sleep = NovilaTheme.pillars.sleep
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "intro") {
            NovilaCard(
                title = stringResource(R.string.breathing_title),
                icon = painterResource(R.drawable.ic_breathing),
                role = sleep,
            ) {
                NovilaText(
                    stringResource(R.string.breathing_intro),
                    style = NovilaTheme.typography.bodyMedium,
                    color = NovilaTheme.colors.onSurfaceVariant,
                )
            }
        }
        BreathingCatalog.sessions.forEachIndexed { index, session ->
            item(key = session.id) {
                NovilaReveal(order = index + 1) { BreathingSessionCard(session, actions.onOpenSession) }
            }
        }
        item(key = "footer") {
            NovilaText(
                stringResource(R.string.breathing_footer),
                style = NovilaTheme.typography.bodySmall,
                color = NovilaTheme.colors.onSurfaceVariant,
            )
        }
    }
}

/** One session: its name, what it does, how long it takes and a button to start. */
@Composable
private fun BreathingSessionCard(session: BreathingSession, onOpen: (String) -> Unit) {
    val name = stringResource(sessionName(session))
    NovilaCard(
        title = name,
        icon = painterResource(R.drawable.ic_breathing),
        role = NovilaTheme.pillars.sleep,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s)) {
            NovilaText(
                stringResource(sessionBody(session)),
                style = NovilaTheme.typography.bodyMedium,
                color = NovilaTheme.colors.onSurfaceVariant,
            )
            NovilaText(
                sessionDuration(session),
                style = NovilaTheme.typography.labelMedium,
                color = NovilaTheme.colors.onSurfaceVariant,
            )
        }
        NovilaButton(
            text = stringResource(R.string.breathing_start),
            onClick = { onOpen(session.id) },
            contentDescription = stringResource(R.string.breathing_start_session, name),
            role = NovilaTheme.pillars.sleep,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun BreathingScreenPreview() {
    NovilaTheme {
        BreathingScreen(BreathingActions(), PaddingValues())
    }
}
