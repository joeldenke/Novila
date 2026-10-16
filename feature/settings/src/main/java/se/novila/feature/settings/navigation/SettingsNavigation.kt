package se.novila.feature.settings.navigation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.serialization.Serializable
import se.novila.feature.settings.DisclaimerScreen
import se.novila.feature.settings.R
import se.novila.feature.settings.SettingsActions
import se.novila.feature.settings.SettingsScreen
import se.novila.feature.settings.SettingsViewModel
import se.novila.healthconnect.HealthConnectSource
import se.novila.healthconnect.HealthDataInfoScreen
import se.novila.healthconnect.rememberMovementPermissionRequest
import se.novila.navigation.Navigator
import se.novila.healthconnect.R as HealthConnectR
import se.novila.ui.R as UiR

/** Settings, opened from the top bar of any tab and pushed onto that tab's back stack. */
@Serializable data object SettingsRoute : NavKey

/** What Novila reads from Health Connect: the same screen Health Connect links to from its permission dialog. */
@Serializable data object HealthDataRoute : NavKey

/** The health disclaimer. */
@Serializable data object DisclaimerRoute : NavKey

/**
 * Settings and the screens opened from it. Settings has its own ViewModel, scoped to the entry; the other two are
 * static text. Navigation inside goes through [navigator], on whichever tab Settings was opened from.
 */
fun EntryProviderScope<NavKey>.settingsEntries(navigator: Navigator, contentPadding: PaddingValues) {
    entry<SettingsRoute> {
        val viewModel = metroViewModel<SettingsViewModel>()
        // Access may have changed in Health Connect's own settings while the app was in the background.
        LifecycleResumeEffect(viewModel) {
            viewModel.refresh()
            onPauseOrDispose { }
        }
        // Nothing to show until the settings are read, a moment at app start.
        val settings = viewModel.settings.collectAsStateWithLifecycle().value ?: return@entry
        val movement by viewModel.movement.collectAsStateWithLifecycle()
        val context = LocalContext.current
        val connectHealth = rememberMovementPermissionRequest(viewModel::onMovementPermissionsResult)
        SettingsScreen(
            settings = settings,
            movement = movement,
            version = viewModel.version,
            canUseDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
            canChooseLanguage = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
            actions = SettingsActions(
                onTheme = viewModel::setTheme,
                onDynamicColor = viewModel::setDynamicColor,
                onChooseLanguage = { openLanguageSettings(context) },
                onConnectHealth = connectHealth,
                onOpenHealthSettings = { HealthConnectSource.openSettings(context) },
                onInstallHealth = { HealthConnectSource.openInstall(context) },
                onOpenHealthInfo = { navigator.navigate(HealthDataRoute) },
                onOpenDisclaimer = { navigator.navigate(DisclaimerRoute) },
                onDeleteData = viewModel::deleteAllData,
            ),
            contentPadding = contentPadding,
        )
    }
    entry<HealthDataRoute> {
        val context = LocalContext.current
        HealthDataInfoScreen(
            onOpenHealthSettings = { HealthConnectSource.openSettings(context) },
            contentPadding = contentPadding,
        )
    }
    entry<DisclaimerRoute> { DisclaimerScreen(contentPadding = contentPadding) }
}

/** The top bar title of Settings or a screen opened from it, or null when [route] is not one of them. */
@Composable
fun settingsTitle(route: NavKey?): String? = when (route) {
    SettingsRoute -> stringResource(UiR.string.settings_title)
    HealthDataRoute -> stringResource(HealthConnectR.string.health_info_title)
    DisclaimerRoute -> stringResource(R.string.disclaimer_title)
    else -> null
}

/** The phone's language setting for Novila alone (Android 13+), where English or Swedish can be chosen. */
private fun openLanguageSettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS, Uri.fromParts("package", context.packageName, null))
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Log.w("Settings", "No per-app language settings", e)
    }
}
