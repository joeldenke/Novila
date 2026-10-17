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
import se.novila.feature.settings.GoalsActions
import se.novila.feature.settings.GoalsScreen
import se.novila.feature.settings.GoalsViewModel
import se.novila.feature.settings.ProfileScreen
import se.novila.feature.settings.ProfileViewModel
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

/** The user's profile, on the phone only. */
@Serializable data object ProfileRoute : NavKey

/** The user's daily goals for steps, sleep and water. */
@Serializable data object GoalsRoute : NavKey

/**
 * Settings and the screens opened from it. Settings, the profile and the goals have their own ViewModels, scoped to
 * their entries; the other two are static text. Navigation inside goes through [navigator], on whichever tab Settings
 * was opened from.
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
        val profile = viewModel.profile.collectAsStateWithLifecycle().value ?: return@entry
        val goals = viewModel.goals.collectAsStateWithLifecycle().value ?: return@entry
        val movement by viewModel.movement.collectAsStateWithLifecycle()
        val context = LocalContext.current
        val connectHealth = rememberMovementPermissionRequest(viewModel::onMovementPermissionsResult)
        SettingsScreen(
            settings = settings,
            profile = profile,
            goals = goals,
            movement = movement,
            version = viewModel.version,
            canUseDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
            canChooseLanguage = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
            actions = SettingsActions(
                onOpenProfile = { navigator.navigate(ProfileRoute) },
                onOpenGoals = { navigator.navigate(GoalsRoute) },
                onTheme = viewModel::setTheme,
                onDynamicColor = viewModel::setDynamicColor,
                onChooseLanguage = { openLanguageSettings(context) },
                onConnectHealth = connectHealth,
                onOpenHealthSettings = { HealthConnectSource.openSettings(context) },
                onInstallHealth = { HealthConnectSource.openInstall(context) },
                onOpenHealthInfo = { navigator.navigate(HealthDataRoute) },
                onOpenDisclaimer = { navigator.navigate(DisclaimerRoute) },
                onDeleteData = viewModel::deleteAllData,
                onShowIntro = viewModel::showIntro,
            ),
            contentPadding = contentPadding,
        )
    }
    entry<ProfileRoute> {
        val viewModel = metroViewModel<ProfileViewModel>()
        // Nothing to show until the profile is read, a moment at app start.
        val state = viewModel.state.collectAsStateWithLifecycle().value ?: return@entry
        ProfileScreen(
            form = state.form,
            currentYear = state.currentYear,
            onChange = viewModel::onChange,
            contentPadding = contentPadding,
        )
    }
    entry<GoalsRoute> {
        val viewModel = metroViewModel<GoalsViewModel>()
        // Nothing to show until the goals and the profile are read, a moment at app start.
        val state = viewModel.state.collectAsStateWithLifecycle().value ?: return@entry
        GoalsScreen(
            goals = state.goals,
            suggestion = state.suggestion,
            actions = GoalsActions(onChange = viewModel::change, onUseSuggestion = viewModel::save),
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
    ProfileRoute -> stringResource(R.string.profile_title)
    GoalsRoute -> stringResource(R.string.goals_title)
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
