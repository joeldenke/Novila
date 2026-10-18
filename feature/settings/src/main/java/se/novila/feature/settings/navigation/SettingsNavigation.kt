package se.novila.feature.settings.navigation

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.serialization.Serializable
import se.novila.feature.settings.DisclaimerScreen
import se.novila.feature.settings.FoodPreferencesScreen
import se.novila.feature.settings.FoodPreferencesViewModel
import se.novila.feature.settings.GoalsActions
import se.novila.feature.settings.GoalsScreen
import se.novila.feature.settings.GoalsViewModel
import se.novila.feature.settings.ProfileScreen
import se.novila.feature.settings.ProfileViewModel
import se.novila.feature.settings.R
import se.novila.feature.settings.RemindersActions
import se.novila.feature.settings.RemindersScreen
import se.novila.feature.settings.RemindersViewModel
import se.novila.feature.settings.SettingsActions
import se.novila.feature.settings.SettingsScreen
import se.novila.feature.settings.SettingsViewModel
import se.novila.healthconnect.HealthConnectSource
import se.novila.healthconnect.HealthDataInfoScreen
import se.novila.healthconnect.rememberHealthPermissionRequest
import se.novila.model.Reminder
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

/** The diet, allergens and time that recipe suggestions on the Nutrition tab take into account. */
@Serializable data object FoodPreferencesRoute : NavKey

/** Reminders to log sleep, train and log food, each at its own time. */
@Serializable data object RemindersRoute : NavKey

/**
 * Settings and the screens opened from it. Settings, the profile, the goals, the food preferences and the reminders
 * have their own ViewModels, scoped to their entries; the other two are static text. Navigation inside goes through
 * [navigator], on whichever tab Settings was opened from.
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
        val food = viewModel.foodPreferences.collectAsStateWithLifecycle().value ?: return@entry
        val movement by viewModel.movement.collectAsStateWithLifecycle()
        val context = LocalContext.current
        val connectHealth = rememberHealthPermissionRequest(viewModel::onMovementPermissionsResult)
        SettingsScreen(
            settings = settings,
            profile = profile,
            goals = goals,
            foodPreferences = food,
            movement = movement,
            version = viewModel.version,
            canUseDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
            canChooseLanguage = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
            actions = SettingsActions(
                onOpenProfile = { navigator.navigate(ProfileRoute) },
                onOpenGoals = { navigator.navigate(GoalsRoute) },
                onOpenFoodPreferences = { navigator.navigate(FoodPreferencesRoute) },
                onOpenReminders = { navigator.navigate(RemindersRoute) },
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
    entry<FoodPreferencesRoute> {
        val viewModel = metroViewModel<FoodPreferencesViewModel>()
        // Nothing to show until the preferences are read, a moment at app start.
        val preferences = viewModel.preferences.collectAsStateWithLifecycle().value ?: return@entry
        FoodPreferencesScreen(preferences = preferences, onChange = viewModel::change, contentPadding = contentPadding)
    }
    entry<RemindersRoute> { RemindersEntry(contentPadding) }
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
    FoodPreferencesRoute -> stringResource(R.string.food_preferences_title)
    RemindersRoute -> stringResource(R.string.settings_reminders)
    else -> null
}

/**
 * The Reminders screen. Turning a reminder on asks for permission to notify first (Android 13+), and only turns it on
 * if the user says yes. Said no for good, the screen leads to the phone's notification settings instead of asking
 * again. Whether Novila may notify is read again on every resume, never remembered.
 */
@Composable
private fun RemindersEntry(contentPadding: PaddingValues) {
    val viewModel = metroViewModel<RemindersViewModel>()
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    // Nothing to show until the reminders are read, a moment at app start.
    val state = viewModel.state.collectAsStateWithLifecycle().value ?: return
    val context = LocalContext.current
    val activity = LocalActivity.current
    var pending by rememberSaveable { mutableStateOf<Reminder?>(null) }
    var refused by rememberSaveable { mutableStateOf(false) }
    val askToNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pending?.let { if (granted) viewModel.setOn(it, on = true) }
        pending = null
        // No rationale to show after a no means the dialog won't come again: only the settings can change it.
        refused = !granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)
        viewModel.refresh()
    }
    RemindersScreen(
        reminders = state.reminders,
        notificationsOff = refused || (state.reminders.anyOn && !state.notificationsAllowed),
        actions = RemindersActions(
            onToggle = { reminder, on ->
                if (on && needsNotificationPermission(context)) {
                    pending = reminder
                    askToNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    viewModel.setOn(reminder, on)
                }
            },
            onTime = viewModel::setTime,
            onOpenNotificationSettings = { openNotificationSettings(context) },
        ),
        contentPadding = contentPadding,
    )
}

/** Android 13+ asks before an app may notify; earlier, notifications are on unless the user turned them off. */
private fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED

/** The phone's notification settings for Novila, where the user can allow notifications again. */
private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Log.w("Settings", "No notification settings", e)
    }
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
