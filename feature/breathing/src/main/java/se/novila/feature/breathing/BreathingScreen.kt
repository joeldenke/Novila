package se.novila.feature.breathing

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import se.novila.breathing.BreathingCatalog
import se.novila.designsystem.component.NovilaEntries
import se.novila.designsystem.component.NovilaEntry
import se.novila.designsystem.component.NovilaEntryLayout
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
 * Breathing and relaxation: every session in [BreathingCatalog] as a way in ([NovilaEntries]), with its own icon,
 * its length and what it does; a tap starts it. Tiles or rows follow the user's choice in Settings. The sessions are
 * data, so this list is the same however many there are.
 */
@Composable
fun BreathingScreen(actions: BreathingActions, contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    val sleep = NovilaTheme.pillars.sleep
    val entries = BreathingCatalog.sessions.map { session ->
        NovilaEntry(
            title = stringResource(sessionName(session)),
            icon = painterResource(sessionIcon(session)),
            role = sleep,
            onClick = { actions.onOpenSession(session.id) },
            summary = stringResource(
                R.string.breathing_session_summary,
                sessionDuration(session),
                stringResource(sessionBody(session)),
            ),
        )
    }
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "intro") {
            NovilaText(
                stringResource(R.string.breathing_intro),
                style = NovilaTheme.typography.bodyMedium,
                color = NovilaTheme.colors.onSurfaceVariant,
            )
        }
        item(key = "sessions") {
            NovilaReveal(order = 1) { NovilaEntries(entries) }
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

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun BreathingScreenPreview() {
    NovilaTheme {
        BreathingScreen(BreathingActions(), PaddingValues())
    }
}

/** The sessions as rows, as chosen in Settings. */
@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun BreathingScreenRowsPreview() {
    NovilaTheme(entryLayout = NovilaEntryLayout.Rows) {
        BreathingScreen(BreathingActions(), PaddingValues())
    }
}
