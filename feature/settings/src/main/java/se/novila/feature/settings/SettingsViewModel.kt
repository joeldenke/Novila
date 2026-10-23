package se.novila.feature.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import se.novila.data.CoachStore
import se.novila.data.CycleStore
import se.novila.data.DataExport
import se.novila.data.FoodPreferencesStore
import se.novila.data.GoalsStore
import se.novila.data.HealthStore
import se.novila.data.ImportResult
import se.novila.data.MeasurementsStore
import se.novila.data.ProfileStore
import se.novila.data.RemindersStore
import se.novila.data.SettingsStore
import se.novila.data.SleepCoachStore
import se.novila.data.deleteLegacyPreferences
import se.novila.model.AppSettings
import se.novila.model.EntryLayout
import se.novila.model.Profile
import se.novila.model.ThemeMode
import se.novila.movement.MovementRepository
import se.novila.movement.MovementState
import se.novila.reminders.ReminderScheduler
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.time.LocalDate

/**
 * Settings: the profile (with the measurements and My goals), how the app looks, Health Connect access, the user's
 * data and the app version. Scoped to its entry.
 */
@ViewModelKey
@ContributesIntoMap(AppScope::class)
@Inject
class SettingsViewModel(
    private val settingsStore: SettingsStore,
    private val healthStore: HealthStore,
    private val coachStore: CoachStore,
    private val sleepCoachStore: SleepCoachStore,
    private val profileStore: ProfileStore,
    private val goalsStore: GoalsStore,
    private val foodPreferencesStore: FoodPreferencesStore,
    private val measurementsStore: MeasurementsStore,
    private val remindersStore: RemindersStore,
    private val cycleStore: CycleStore,
    private val reminderScheduler: ReminderScheduler,
    private val movementRepository: MovementRepository,
    private val dataExport: DataExport,
    private val context: Context,
) : ViewModel() {
    private val transferred = MutableStateFlow<DataTransfer?>(null)

    /** How the latest export or import went, or null before the user tried one. */
    val transfer: StateFlow<DataTransfer?> = transferred

    /** The saved settings, or null before the store's first read. */
    val settings: StateFlow<AppSettings?> = settingsStore.settings

    /** The user's profile, or null before the store's first read. */
    val profile: StateFlow<Profile?> = profileStore.profile

    /** Whether Health Connect is connected, and what the user can do if not. */
    val movement: StateFlow<MovementState> = movementRepository.state

    val version: AppVersion = context.appVersion()

    /** Called on resume: the user may have changed Health Connect access in its own settings meanwhile. */
    fun refresh() {
        viewModelScope.launch { movementRepository.refresh(LocalDate.now()) }
    }

    /** Result of Health Connect's permission dialog: whether the user gave access to anything. */
    fun onMovementPermissionsResult(granted: Boolean) {
        viewModelScope.launch { movementRepository.onPermissionResult(granted, LocalDate.now()) }
    }

    fun setTheme(theme: ThemeMode) = settingsStore.setTheme(theme)

    fun setDynamicColor(enabled: Boolean) = settingsStore.setDynamicColor(enabled)

    fun setEntryLayout(layout: EntryLayout) = settingsStore.setEntryLayout(layout)

    /** Shows the intro from the start again. The app switches to it as soon as the flag is saved. */
    fun showIntro() = settingsStore.setOnboarded(false)

    /**
     * Forgets the profile, its goals and food preferences (allergies are health data), everything the user has logged,
     * including the measurements and the cycle, both coaches' answers and the reminders, and cancels their planned
     * work. Settings stay, since they are not health data. Health Connect's own data is not Novila's to delete.
     *
     * The files themselves are sealed with a Tink key from the Android Keystore, and the backup rules exclude all
     * app data (app/src/main/res/xml), so no copy exists off the device to restore. The legacy SharedPreferences
     * are removed too, for stores that had not read them yet.
     */
    fun deleteAllData() {
        healthStore.clear()
        coachStore.clear()
        sleepCoachStore.clear()
        profileStore.clear()
        goalsStore.clear()
        foodPreferencesStore.clear()
        measurementsStore.clear()
        cycleStore.clear()
        // Clear first, so a worker that is already running reads "off"; then cancel what is still planned.
        remindersStore.clear()
        reminderScheduler.cancelAll()
        deleteLegacyPreferences(context)
    }

    /** Writes the export to [uri], the file the user created in the system's file picker. */
    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            val written = try {
                val text = dataExport.export(LocalDate.now(), version.code)
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.encodeToByteArray()) }
                } != null
            } catch (e: IOException) {
                false
            } catch (e: SecurityException) {
                false
            }
            transferred.value = if (written) DataTransfer.Exported else DataTransfer.ExportFailed
        }
    }

    /**
     * Replaces the data with the export in [uri], the file the user chose. A file that isn't an export, or is
     * larger than one can be, changes nothing.
     */
    fun importFrom(uri: Uri) {
        viewModelScope.launch {
            val text = try {
                withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)?.use(::readExport) }
            } catch (e: IOException) {
                null
            } catch (e: SecurityException) {
                null
            }
            transferred.value = when (text?.let { dataExport.restore(it, version.code) }) {
                ImportResult.Imported -> DataTransfer.Imported
                ImportResult.TooNew -> DataTransfer.ImportTooNew
                ImportResult.Unreadable, null -> DataTransfer.ImportUnreadable
            }
        }
    }
}

/** How the latest export or import went, for the line under the buttons. */
enum class DataTransfer { Exported, ExportFailed, Imported, ImportUnreadable, ImportTooNew }

/** The text of an export, or null when it is larger than [DataExport.MAX_BYTES], which no export is. */
private fun readExport(input: InputStream): String? {
    val bytes = ByteArrayOutputStream()
    val buffer = ByteArray(BUFFER_BYTES)
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        bytes.write(buffer, 0, read)
        if (bytes.size() > DataExport.MAX_BYTES) return null
    }
    return bytes.toByteArray().decodeToString()
}

private const val BUFFER_BYTES = 64 * 1024
