package se.novila.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import se.novila.data.CoachStore
import se.novila.data.HealthStore
import se.novila.data.SettingsStore
import se.novila.model.AppSettings
import se.novila.model.ThemeMode
import se.novila.movement.MovementRepository
import se.novila.movement.MovementState
import java.time.LocalDate

/** Settings: how the app looks, Health Connect access, the user's data and the app version. Scoped to its entry. */
@ViewModelKey
@ContributesIntoMap(AppScope::class)
@Inject
class SettingsViewModel(
    private val settingsStore: SettingsStore,
    private val healthStore: HealthStore,
    private val coachStore: CoachStore,
    private val movementRepository: MovementRepository,
    context: Context,
) : ViewModel() {
    /** The saved settings, or null before the store's first read. */
    val settings: StateFlow<AppSettings?> = settingsStore.settings

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

    /**
     * Forgets everything the user has logged and the coach's answers. Settings stay, since they are not health
     * data. Health Connect's own data is not Novila's to delete.
     */
    fun deleteAllData() {
        healthStore.clear()
        coachStore.clear()
    }
}
