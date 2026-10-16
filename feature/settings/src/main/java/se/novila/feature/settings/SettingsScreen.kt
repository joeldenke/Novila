package se.novila.feature.settings

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import se.novila.designsystem.component.NovilaButton
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaChoiceChip
import se.novila.designsystem.component.NovilaConfirmDialog
import se.novila.designsystem.component.NovilaIcon
import se.novila.designsystem.component.NovilaListRow
import se.novila.designsystem.component.NovilaScaffold
import se.novila.designsystem.component.NovilaSwitchRow
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.feature.settings.R
import se.novila.model.AppSettings
import se.novila.model.ThemeMode
import se.novila.movement.MovementState
import se.novila.movement.TodayMovement
import se.novila.ui.common.ScreenList
import se.novila.ui.R as UiR

/** What the Settings screen can do. The entry connects these to the ViewModel and to the system. */
data class SettingsActions(
    val onTheme: (ThemeMode) -> Unit = {},
    val onDynamicColor: (Boolean) -> Unit = {},
    val onChooseLanguage: () -> Unit = {},
    val onConnectHealth: () -> Unit = {},
    val onOpenHealthSettings: () -> Unit = {},
    val onInstallHealth: () -> Unit = {},
    val onOpenHealthInfo: () -> Unit = {},
    val onOpenDisclaimer: () -> Unit = {},
    val onDeleteData: () -> Unit = {},
)

/**
 * Settings: appearance, Health Connect access, health disclaimer, the user's data and the app version.
 *
 * @param canUseDynamicColor wallpaper colors exist (Android 12+)
 * @param canChooseLanguage the phone has per-app language settings (Android 13+)
 */
@Composable
fun SettingsScreen(
    settings: AppSettings,
    movement: MovementState,
    version: AppVersion,
    canUseDynamicColor: Boolean,
    canChooseLanguage: Boolean,
    actions: SettingsActions,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "appearance") {
            AppearanceCard(settings, canUseDynamicColor, canChooseLanguage, actions)
        }
        item(key = "health-connect") { HealthConnectCard(movement, actions) }
        item(key = "disclaimer") { DisclaimerCard(actions.onOpenDisclaimer) }
        item(key = "data") { DataCard(actions.onDeleteData) }
        item(key = "about") { AboutCard(version) }
    }
}

@Composable
private fun AppearanceCard(
    settings: AppSettings,
    canUseDynamicColor: Boolean,
    canChooseLanguage: Boolean,
    actions: SettingsActions,
) {
    NovilaCard(title = stringResource(R.string.settings_appearance)) {
        NovilaText(
            stringResource(R.string.settings_theme),
            style = NovilaTheme.typography.labelLarge,
            color = NovilaTheme.colors.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s)) {
            ThemeMode.entries.forEach { theme ->
                NovilaChoiceChip(
                    label = stringResource(theme.label()),
                    selected = settings.theme == theme,
                    onClick = { actions.onTheme(theme) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (canUseDynamicColor) {
            NovilaSwitchRow(
                title = stringResource(R.string.settings_dynamic_color),
                summary = stringResource(R.string.settings_dynamic_color_summary),
                checked = settings.dynamicColor,
                onCheckedChange = actions.onDynamicColor,
            )
        }
        if (canChooseLanguage) {
            NovilaListRow(
                title = stringResource(R.string.settings_language),
                summary = stringResource(R.string.settings_language_summary),
                onClick = actions.onChooseLanguage,
                trailing = { Chevron() },
            )
        }
    }
}

private fun ThemeMode.label(): Int = when (this) {
    ThemeMode.System -> R.string.settings_theme_system
    ThemeMode.Light -> R.string.settings_theme_light
    ThemeMode.Dark -> R.string.settings_theme_dark
}

/** Whether Novila may read movement from Health Connect, and the one thing the user can do about it. */
@Composable
private fun HealthConnectCard(movement: MovementState, actions: SettingsActions) {
    NovilaCard(
        title = stringResource(R.string.settings_health_connect),
        icon = painterResource(UiR.drawable.ic_training),
        role = NovilaTheme.pillars.training,
    ) {
        when (movement) {
            is MovementState.Connected -> {
                Body(stringResource(R.string.settings_health_connected))
                NovilaButton(
                    text = stringResource(UiR.string.movement_open_settings),
                    onClick = actions.onOpenHealthSettings,
                )
            }

            is MovementState.NeedsPermission -> if (movement.denied) {
                Body(stringResource(R.string.settings_health_denied))
                NovilaButton(
                    text = stringResource(UiR.string.movement_open_settings),
                    onClick = actions.onOpenHealthSettings,
                )
            } else {
                Body(stringResource(R.string.settings_health_not_connected))
                NovilaButton(
                    text = stringResource(R.string.settings_health_connect_action),
                    onClick = actions.onConnectHealth,
                )
            }

            MovementState.NeedsInstall -> {
                Body(stringResource(R.string.settings_health_needs_install))
                NovilaButton(
                    text = stringResource(R.string.settings_health_install_action),
                    onClick = actions.onInstallHealth,
                )
            }

            MovementState.Unavailable -> Body(stringResource(R.string.settings_health_unavailable))
        }
        NovilaListRow(
            title = stringResource(R.string.settings_health_info),
            summary = stringResource(R.string.settings_health_info_summary),
            onClick = actions.onOpenHealthInfo,
            trailing = { Chevron() },
        )
    }
}

@Composable
private fun DisclaimerCard(onOpenDisclaimer: () -> Unit) {
    NovilaCard(title = stringResource(R.string.settings_health_and_safety)) {
        Body(stringResource(R.string.settings_disclaimer_short))
        NovilaListRow(
            title = stringResource(R.string.disclaimer_title),
            onClick = onOpenDisclaimer,
            trailing = { Chevron() },
        )
    }
}

/** Deleting asks first: it cannot be undone, and there is no backup outside the phone. */
@Composable
private fun DataCard(onDeleteData: () -> Unit) {
    var confirming by rememberSaveable { mutableStateOf(false) }
    var deleted by rememberSaveable { mutableStateOf(false) }
    NovilaCard(title = stringResource(R.string.settings_data)) {
        Body(stringResource(R.string.settings_data_body))
        if (deleted) {
            Body(stringResource(R.string.settings_data_deleted))
        } else {
            NovilaButton(
                text = stringResource(R.string.settings_delete_data),
                onClick = { confirming = true },
                role = NovilaTheme.pillars.nutrition,
            )
        }
    }
    if (confirming) {
        NovilaConfirmDialog(
            title = stringResource(R.string.settings_delete_data_title),
            text = stringResource(R.string.settings_delete_data_body),
            confirmLabel = stringResource(R.string.settings_delete_data_confirm),
            dismissLabel = stringResource(R.string.settings_cancel),
            onConfirm = {
                confirming = false
                deleted = true
                onDeleteData()
            },
            onDismiss = { confirming = false },
        )
    }
}

@Composable
private fun AboutCard(version: AppVersion) {
    NovilaCard(title = stringResource(R.string.settings_about)) {
        NovilaListRow(
            title = stringResource(R.string.settings_version),
            summary = stringResource(R.string.settings_version_value, version.name, version.code),
        )
        NovilaListRow(
            title = stringResource(R.string.settings_privacy),
            summary = stringResource(R.string.settings_privacy_summary),
        )
        NovilaListRow(
            title = stringResource(R.string.settings_licenses),
            summary = stringResource(R.string.settings_licenses_summary),
        )
    }
}

@Composable
private fun Body(text: String) {
    NovilaText(text, style = NovilaTheme.typography.bodyMedium, color = NovilaTheme.colors.onSurfaceVariant)
}

@Composable
private fun Chevron() {
    // The row's title already says where it leads.
    NovilaIcon(
        painter = painterResource(R.drawable.ic_chevron),
        contentDescription = null,
        tint = NovilaTheme.colors.onSurfaceVariant,
    )
}

@Composable
private fun SettingsContent(movement: MovementState, settings: AppSettings = AppSettings()) {
    NovilaTheme {
        NovilaScaffold { padding ->
            SettingsScreen(
                settings = settings,
                movement = movement,
                version = AppVersion(name = "0.1.42-debug", code = 42),
                canUseDynamicColor = true,
                canChooseLanguage = true,
                actions = SettingsActions(),
                contentPadding = padding,
            )
        }
    }
}

@Preview(heightDp = 1400)
@Composable
private fun SettingsPreview() = SettingsContent(MovementState.NeedsPermission())

@Preview(heightDp = 1400, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun SettingsDarkPreview() = SettingsContent(
    movement = MovementState.Connected(TodayMovement(steps = 6400, activeKilocalories = 210)),
    settings = AppSettings(theme = ThemeMode.Dark, dynamicColor = false),
)
