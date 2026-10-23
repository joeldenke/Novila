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

/** The parts of the terms of use, in reading order: a title and a body each. */
private val TermsSections = listOf(
    R.string.terms_about_title to R.string.terms_about_body,
    R.string.terms_health_title to R.string.terms_health_body,
    R.string.terms_use_title to R.string.terms_use_body,
    R.string.terms_data_title to R.string.terms_data_body,
    R.string.terms_warranty_title to R.string.terms_warranty_body,
    R.string.terms_rights_title to R.string.terms_rights_body,
    R.string.terms_changes_title to R.string.terms_changes_body,
    R.string.terms_law_title to R.string.terms_law_body,
)

/**
 * Novila's terms of use, shown in full on the phone since the app has no network access. Reading them is never a
 * step before using the app, and nothing is to be accepted: they say what the app is and isn't, and Swedish consumer
 * law comes first. `docs/terms-of-use.md` is the same text for a public page; change both together, and the date.
 */
@Composable
fun TermsScreen(contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "intro") {
            NovilaText(
                stringResource(R.string.terms_intro),
                style = NovilaTheme.typography.bodyLarge,
                color = NovilaTheme.colors.onSurface,
            )
        }
        TermsSections.forEach { (title, body) ->
            item(key = title) { Section(title, body) }
        }
        item(key = "updated") {
            NovilaText(
                stringResource(R.string.terms_updated),
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
private fun TermsContent() {
    NovilaTheme {
        NovilaScaffold { padding -> TermsScreen(contentPadding = padding) }
    }
}

@Preview(heightDp = 2600)
@Composable
private fun TermsPreview() = TermsContent()

@Preview(heightDp = 2600, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun TermsDarkPreview() = TermsContent()
