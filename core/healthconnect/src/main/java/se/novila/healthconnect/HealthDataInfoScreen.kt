package se.novila.healthconnect

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import se.novila.designsystem.component.NovilaButton
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaScaffold
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.healthconnect.R
import se.novila.ui.common.ScreenList
import se.novila.ui.R as UiR

/**
 * What Novila does with Health Connect data, in plain words. Health Connect links here from its
 * permission dialog, and Google Play expects the same answers in the Health apps declaration.
 */
@Composable
fun HealthDataInfoScreen(
    onOpenHealthSettings: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "read") { InfoCard(R.string.health_info_read_title, R.string.health_info_read_body) }
        item(key = "why") { InfoCard(R.string.health_info_why_title, R.string.health_info_why_body) }
        item(key = "where") { InfoCard(R.string.health_info_where_title, R.string.health_info_where_body) }
        item(key = "revoke") {
            InfoCard(R.string.health_info_revoke_title, R.string.health_info_revoke_body) {
                NovilaButton(
                    text = stringResource(UiR.string.movement_open_settings),
                    onClick = onOpenHealthSettings,
                    role = NovilaTheme.pillars.training,
                )
            }
        }
    }
}

@Composable
private fun InfoCard(@StringRes title: Int, @StringRes body: Int, action: @Composable () -> Unit = {}) {
    NovilaCard(title = stringResource(title)) {
        NovilaText(
            stringResource(body),
            style = NovilaTheme.typography.bodyMedium,
            color = NovilaTheme.colors.onSurfaceVariant,
        )
        action()
    }
}

@Composable
private fun HealthDataInfoContent() {
    NovilaTheme {
        NovilaScaffold { padding -> HealthDataInfoScreen(onOpenHealthSettings = {}, contentPadding = padding) }
    }
}

@Preview
@Composable
private fun HealthDataInfoPreview() = HealthDataInfoContent()

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun HealthDataInfoDarkPreview() = HealthDataInfoContent()
