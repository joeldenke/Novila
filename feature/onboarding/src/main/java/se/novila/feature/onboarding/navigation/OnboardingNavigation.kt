package se.novila.feature.onboarding.navigation

import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.serialization.Serializable
import se.novila.feature.onboarding.OnboardingActions
import se.novila.feature.onboarding.OnboardingScreen
import se.novila.feature.onboarding.OnboardingViewModel
import se.novila.healthconnect.HealthConnectSource
import se.novila.healthconnect.rememberHealthPermissionRequest

/** The intro, shown on its own before the tabs until it is done, and again when opened from Settings. */
@Serializable data object OnboardingRoute : NavKey

/** The intro's entry. Finishing it saves the answers, and the app then opens on Today by itself. */
fun EntryProviderScope<NavKey>.onboardingEntry() {
    entry<OnboardingRoute> {
        val viewModel = metroViewModel<OnboardingViewModel>()
        // Access may have changed in Health Connect's own settings while the app was in the background.
        LifecycleResumeEffect(viewModel) {
            viewModel.refresh()
            onPauseOrDispose { }
        }
        // Nothing to show until the profile and the coach's answers are read, a moment at app start.
        val state = viewModel.state.collectAsStateWithLifecycle().value ?: return@entry
        val context = LocalContext.current
        val connectHealth = rememberHealthPermissionRequest(viewModel::onMovementPermissionsResult)
        OnboardingScreen(
            state = state,
            // The questions when the setup was skipped earlier, so the user lands where there is something to do.
            initialPage = viewModel.startPage.value,
            actions = OnboardingActions(
                onName = viewModel::onName,
                onPreferences = viewModel::onPreferences,
                onConnectHealth = connectHealth,
                onOpenHealthSettings = { HealthConnectSource.openSettings(context) },
                onInstallHealth = { HealthConnectSource.openInstall(context) },
                onFinish = viewModel::finish,
                onSkipAll = viewModel::skip,
            ),
        )
    }
}
