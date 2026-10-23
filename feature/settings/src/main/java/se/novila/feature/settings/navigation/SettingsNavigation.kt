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
import androidx.annotation.RawRes
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
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.serialization.Serializable
import se.novila.feature.settings.CycleScreen
import se.novila.feature.settings.CycleViewModel
import se.novila.feature.settings.DisclaimerScreen
import se.novila.feature.settings.FoodPreferencesScreen
import se.novila.feature.settings.FoodPreferencesViewModel
import se.novila.feature.settings.GoalsActions
import se.novila.feature.settings.GoalsScreen
import se.novila.feature.settings.GoalsViewModel
import se.novila.feature.settings.LicensesScreen
import se.novila.feature.settings.MyGoalsActions
import se.novila.feature.settings.MyGoalsScreen
import se.novila.feature.settings.MyGoalsViewModel
import se.novila.feature.settings.PrivacyPolicyScreen
import se.novila.feature.settings.ProfileActions
import se.novila.feature.settings.ProfileScreen
import se.novila.feature.settings.ProfileViewModel
import se.novila.feature.settings.R
import se.novila.feature.settings.RemindersActions
import se.novila.feature.settings.RemindersScreen
import se.novila.feature.settings.RemindersViewModel
import se.novila.feature.settings.SettingsActions
import se.novila.feature.settings.SettingsScreen
import se.novila.feature.settings.SettingsViewModel
import se.novila.feature.settings.TermsScreen
import se.novila.healthconnect.HealthConnectSource
import se.novila.healthconnect.HealthDataInfoScreen
import se.novila.healthconnect.rememberHealthPermissionRequest
import se.novila.model.Reminder
import se.novila.navigation.Navigator
import se.novila.navigation.SharedScreen
import se.novila.navigation.sharedScreen
import java.time.LocalDate
import se.novila.healthconnect.R as HealthConnectR
import se.novila.ui.R as UiR

/** Settings, opened from the top bar of any tab and pushed onto that tab's back stack. */
@Serializable data object SettingsRoute : NavKey

/** What Novila reads from Health Connect: the same screen Health Connect links to from its permission dialog. */
@Serializable data object HealthDataRoute : NavKey

/** The health disclaimer. */
@Serializable data object DisclaimerRoute : NavKey

/** Novila's privacy policy, which Google Play requires inside the app. */
@Serializable data object PrivacyPolicyRoute : NavKey

/** Novila's terms of use. */
@Serializable data object TermsRoute : NavKey

/** The open-source licenses of everything the app ships. */
@Serializable data object LicensesRoute : NavKey

/** The user's profile with the body measurements and the way to My goals, on the phone only. */
@Serializable data object ProfileRoute : NavKey

/** Everything Novila plans by, in one place: daily goals, both coaches and food preferences. */
@Serializable data object MyGoalsRoute : NavKey

/** The user's daily goals for steps, sleep and water. */
@Serializable data object GoalsRoute : NavKey

/** The diet, allergens and time that recipe suggestions on the Nutrition tab take into account. */
@Serializable data object FoodPreferencesRoute : NavKey

/** *Follow your cycle*: turned on or off here, logged on Training. */
@Serializable data object CycleRoute : NavKey

/** Reminders to log sleep, train and log food, each at its own time. */
@Serializable data object RemindersRoute : NavKey

/**
 * Settings and the screens opened from it. Settings, the profile (with the measurements), My goals, the goals, the
 * food preferences, the cycle and the reminders have their own ViewModels, scoped to their entries; the others are
 * static text. Navigation inside goes through [navigator], on whichever tab Settings was opened from. My goals leads
 * to the training and the sleep coach, which other features own, through [onOpenTrainingCoach] and [onOpenSleepCoach]
 * from `:app`.
 */
fun EntryProviderScope<NavKey>.settingsEntries(
    navigator: Navigator,
    contentPadding: PaddingValues,
    onOpenTrainingCoach: () -> Unit,
    onOpenSleepCoach: () -> Unit,
    // R.raw.aboutlibraries, generated by the AboutLibraries plugin in :app; the feature cannot name it itself.
    @RawRes licensesResId: Int,
) {
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
        val movement by viewModel.movement.collectAsStateWithLifecycle()
        val context = LocalContext.current
        val connectHealth = rememberHealthPermissionRequest(viewModel::onMovementPermissionsResult)
        val transfer by viewModel.transfer.collectAsStateWithLifecycle()
        // The system's file picker: the user chooses where the copy goes and which file comes back, so no permission
        // is needed and nothing is sent anywhere.
        val exportFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(EXPORT_TYPE)) { uri ->
            uri?.let(viewModel::exportTo)
        }
        val importFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let(viewModel::importFrom)
        }
        SettingsScreen(
            settings = settings,
            profile = profile,
            movement = movement,
            version = viewModel.version,
            canUseDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
            canChooseLanguage = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
            actions = SettingsActions(
                onOpenProfile = { navigator.navigate(ProfileRoute) },
                onOpenReminders = { navigator.navigate(RemindersRoute) },
                onTheme = viewModel::setTheme,
                onDynamicColor = viewModel::setDynamicColor,
                onEntryLayout = viewModel::setEntryLayout,
                onChooseLanguage = { openLanguageSettings(context) },
                onConnectHealth = connectHealth,
                onOpenHealthSettings = { HealthConnectSource.openSettings(context) },
                onInstallHealth = { HealthConnectSource.openInstall(context) },
                onOpenHealthInfo = { navigator.navigate(HealthDataRoute) },
                onOpenDisclaimer = { navigator.navigate(DisclaimerRoute) },
                onOpenPrivacy = { navigator.navigate(PrivacyPolicyRoute) },
                onOpenTerms = { navigator.navigate(TermsRoute) },
                onOpenLicenses = { navigator.navigate(LicensesRoute) },
                onDeleteData = viewModel::deleteAllData,
                onExportData = { exportFile.launch(exportFileName(LocalDate.now())) },
                onImportData = { importFile.launch(IMPORT_TYPES) },
                onShowIntro = viewModel::showIntro,
            ),
            contentPadding = contentPadding,
            transfer = transfer,
        )
    }
    entry<MyGoalsRoute> {
        val viewModel = metroViewModel<MyGoalsViewModel>()
        // Nothing to show until the answers are read, a moment at app start.
        val state = viewModel.state.collectAsStateWithLifecycle().value ?: return@entry
        MyGoalsScreen(
            state = state,
            actions = MyGoalsActions(
                onOpenGoals = { navigator.navigate(GoalsRoute) },
                onOpenTrainingCoach = onOpenTrainingCoach,
                onOpenSleepCoach = onOpenSleepCoach,
                onOpenFoodPreferences = { navigator.navigate(FoodPreferencesRoute) },
                onOpenCycle = { navigator.navigate(CycleRoute) },
            ),
            contentPadding = contentPadding,
        )
    }
    entry<ProfileRoute> { ProfileEntry(navigator, contentPadding) }
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
    entry<FoodPreferencesRoute>(metadata = sharedScreen(SharedScreen.FoodPreferences)) {
        val viewModel = metroViewModel<FoodPreferencesViewModel>()
        // Nothing to show until the preferences are read, a moment at app start.
        val preferences = viewModel.preferences.collectAsStateWithLifecycle().value ?: return@entry
        FoodPreferencesScreen(preferences = preferences, onChange = viewModel::change, contentPadding = contentPadding)
    }
    entry<CycleRoute> {
        val viewModel = metroViewModel<CycleViewModel>()
        // Nothing to show until the cycle is read, a moment at app start.
        val cycle = viewModel.cycle.collectAsStateWithLifecycle().value ?: return@entry
        CycleScreen(cycle = cycle, onEnabled = viewModel::setEnabled, contentPadding = contentPadding)
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
    entry<PrivacyPolicyRoute> { PrivacyPolicyScreen(contentPadding = contentPadding) }
    entry<TermsRoute> { TermsScreen(contentPadding = contentPadding) }
    entry<LicensesRoute> {
        val libraries by produceLibraries(licensesResId)
        LicensesScreen(libraries = libraries, contentPadding = contentPadding)
    }
}

/** The top bar title of Settings or a screen opened from it, or null when [route] is not one of them. */
@Composable
fun settingsTitle(route: NavKey?): String? = when (route) {
    SettingsRoute -> stringResource(UiR.string.settings_title)
    HealthDataRoute -> stringResource(HealthConnectR.string.health_info_title)
    DisclaimerRoute -> stringResource(R.string.disclaimer_title)
    PrivacyPolicyRoute -> stringResource(R.string.privacy_title)
    TermsRoute -> stringResource(R.string.terms_title)
    LicensesRoute -> stringResource(R.string.licenses_title)
    ProfileRoute -> stringResource(R.string.profile_title)
    MyGoalsRoute -> stringResource(R.string.my_goals_title)
    GoalsRoute -> stringResource(R.string.goals_title)
    FoodPreferencesRoute -> stringResource(R.string.food_preferences_title)
    CycleRoute -> stringResource(R.string.cycle_title)
    RemindersRoute -> stringResource(R.string.settings_reminders)
    else -> null
}

/**
 * The profile with the measurements: they are logged for today unless the user went back to fill in a forgotten day,
 * and the fields show what that day already has. "Today" is read again on every resume, so a night past midnight
 * never keeps logging the day before.
 */
@Composable
private fun ProfileEntry(navigator: Navigator, contentPadding: PaddingValues) {
    val viewModel = metroViewModel<ProfileViewModel>()
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    // Nothing to show until the profile and the measurements are read, a moment at app start.
    val state = viewModel.state.collectAsStateWithLifecycle().value ?: return
    ProfileScreen(
        state = state,
        actions = ProfileActions(
            onChange = viewModel::onChange,
            onMeasurementsChange = viewModel::onMeasurementsChange,
            onSelectDay = viewModel::onSelectDay,
            onSaveMeasurements = viewModel::saveMeasurements,
            onOpenMyGoals = { navigator.navigate(MyGoalsRoute) },
        ),
        contentPadding = contentPadding,
    )
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

/** The export's file type. */
private const val EXPORT_TYPE = "application/json"

/** What the import offers: JSON, and the types some file apps give a `.json` file they don't know. */
private val IMPORT_TYPES = arrayOf(EXPORT_TYPE, "application/octet-stream", "text/plain")

/** "novila-2026-10-23.json": the date tells copies apart. */
private fun exportFileName(date: LocalDate) = "novila-$date.json"
