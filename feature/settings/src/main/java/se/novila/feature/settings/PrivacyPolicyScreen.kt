package se.novila.feature.settings

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaScaffold
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.feature.settings.R
import se.novila.ui.common.ScreenList

/** The parts of the privacy policy, in reading order: a title and a body each. */
private val PrivacySections = listOf(
    R.string.privacy_what_title to R.string.privacy_what_body,
    R.string.privacy_where_title to R.string.privacy_where_body,
    R.string.privacy_health_connect_title to R.string.privacy_health_connect_body,
    R.string.privacy_shared_title to R.string.privacy_shared_body,
    R.string.privacy_screen_title to R.string.privacy_screen_body,
    R.string.privacy_control_title to R.string.privacy_control_body,
    R.string.privacy_copy_title to R.string.privacy_copy_body,
    R.string.privacy_google_title to R.string.privacy_google_body,
    R.string.privacy_adults_title to R.string.privacy_adults_body,
    R.string.privacy_contact_title to R.string.privacy_contact_body,
)

/**
 * Novila's privacy policy. Google Play requires it inside the app as well as in Play Console, and the app has no
 * network access, so the whole policy is shown here. `docs/privacy-policy.md` is the same text for the public URL;
 * change both together, and the date.
 */
@Composable
fun PrivacyPolicyScreen(contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "intro") {
            NovilaText(
                stringResource(R.string.privacy_intro),
                style = NovilaTheme.typography.bodyLarge,
                color = NovilaTheme.colors.onSurface,
            )
        }
        PrivacySections.forEach { (title, body) ->
            item(key = title) { Section(title, body) }
        }
        item(key = "updated") {
            NovilaText(
                stringResource(R.string.privacy_updated),
                style = NovilaTheme.typography.bodySmall,
                color = NovilaTheme.colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Section(@StringRes title: Int, @StringRes body: Int) {
    NovilaCard(title = stringResource(title)) {
        NovilaText(
            stringResource(body),
            style = NovilaTheme.typography.bodyMedium,
            color = NovilaTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun PrivacyPolicyContent() {
    NovilaTheme {
        NovilaScaffold { padding -> PrivacyPolicyScreen(contentPadding = padding) }
    }
}

@Preview(heightDp = 2400)
@Composable
private fun PrivacyPolicyPreview() = PrivacyPolicyContent()

@Preview(heightDp = 2400, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun PrivacyPolicyDarkPreview() = PrivacyPolicyContent()
