package se.novila.ui.common

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import se.novila.designsystem.component.NovilaButton
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.ui.R

/**
 * The card at the top of a tab's root when the introduction was skipped ([setupIncomplete]): a quiet note that the
 * profile, the goals and the coaches are not set up yet, and the one action ([onCompleteSetup]) that opens the intro
 * again. It is not dismissible: finishing the setup is what removes it, and until then it stays the same single card.
 */
fun LazyListScope.setupHint(setupIncomplete: Boolean, onCompleteSetup: () -> Unit) {
    if (!setupIncomplete) return
    item(key = "setup-hint") { SetupHintCard(onCompleteSetup) }
}

/** The setup hint itself: what is missing, and the way to finish it. */
@Composable
private fun SetupHintCard(onCompleteSetup: () -> Unit) {
    NovilaCard(title = stringResource(R.string.setup_hint_title)) {
        NovilaText(
            stringResource(R.string.setup_hint_body),
            style = NovilaTheme.typography.bodyMedium,
            color = NovilaTheme.colors.onSurfaceVariant,
        )
        // The sun is already the screen's main action, so this one stays tonal.
        NovilaButton(text = stringResource(R.string.setup_hint_action), onClick = onCompleteSetup)
    }
}
