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

/** The parts of the health disclaimer, in reading order: a title and a body each. */
private val DisclaimerSections = listOf(
    R.string.disclaimer_not_medical_title to R.string.disclaimer_not_medical_body,
    R.string.disclaimer_ask_title to R.string.disclaimer_ask_body,
    R.string.disclaimer_stop_title to R.string.disclaimer_stop_body,
    R.string.disclaimer_rehab_title to R.string.disclaimer_rehab_body,
    R.string.disclaimer_sources_title to R.string.disclaimer_sources_body,
)

/**
 * What Novila is and is not: general advice from public guidelines, never medical advice. The coach, the rehab
 * programs and the AI comment all rest on this, so it stays one tap from Settings.
 */
@Composable
fun DisclaimerScreen(contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        DisclaimerSections.forEach { (title, body) ->
            item(key = title) { Section(title, body) }
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
private fun DisclaimerContent() {
    NovilaTheme {
        NovilaScaffold { padding -> DisclaimerScreen(contentPadding = padding) }
    }
}

@Preview(heightDp = 1200)
@Composable
private fun DisclaimerPreview() = DisclaimerContent()

@Preview(heightDp = 1200, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun DisclaimerDarkPreview() = DisclaimerContent()
