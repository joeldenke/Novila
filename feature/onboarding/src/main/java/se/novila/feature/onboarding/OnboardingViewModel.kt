package se.novila.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import se.novila.coach.CoachPreferences
import se.novila.data.CoachStore
import se.novila.data.ProfileStore
import se.novila.data.SavedCoach
import se.novila.data.SettingsStore
import se.novila.model.Profile
import se.novila.movement.MovementRepository
import se.novila.movement.MovementState
import java.time.LocalDate

/** The intro's answers as they stand: the name, the coach's goal and time, and Health Connect access. */
data class OnboardingUiState(
    val name: String,
    val preferences: CoachPreferences,
    val movement: MovementState,
)

/**
 * The intro at first start, or opened again from Settings. Starts from what is saved, so a second run shows the
 * user's own answers. Nothing is saved until [finish]: then the name goes to the profile, the goal and time become the
 * coach's answers (so Training shows the coach's program from day one) and the app opens on Today.
 */
@ViewModelKey
@ContributesIntoMap(AppScope::class)
@Inject
class OnboardingViewModel(
    private val settingsStore: SettingsStore,
    private val profileStore: ProfileStore,
    private val coachStore: CoachStore,
    private val movementRepository: MovementRepository,
) : ViewModel() {
    /** What the user has typed or chosen here, or null for what is saved. */
    private val typedName = MutableStateFlow<String?>(null)
    private val chosen = MutableStateFlow<CoachPreferences?>(null)

    /** The answers, or null before the stores' first read. */
    val state: StateFlow<OnboardingUiState?> = combine(
        typedName,
        chosen,
        profileStore.profile,
        coachStore.answers,
        movementRepository.state,
    ) { name, preferences, profile, coach, movement -> read(name, preferences, profile, coach, movement) }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            read(null, null, profileStore.profile.value, coachStore.answers.value, movementRepository.state.value),
        )

    private fun read(
        name: String?,
        preferences: CoachPreferences?,
        profile: Profile?,
        coach: SavedCoach?,
        movement: MovementState,
    ): OnboardingUiState? {
        if (profile == null || coach == null) return null
        return OnboardingUiState(
            name = name ?: profile.name,
            preferences = preferences ?: coach.preferences ?: CoachPreferences(),
            movement = movement,
        )
    }

    fun onName(name: String) {
        typedName.value = name.take(Profile.MAX_NAME_LENGTH)
    }

    fun onPreferences(preferences: CoachPreferences) {
        chosen.value = preferences
    }

    /** Called on resume: the user may have changed Health Connect access in its own settings meanwhile. */
    fun refresh() {
        viewModelScope.launch { movementRepository.refresh(LocalDate.now()) }
    }

    /** Result of Health Connect's permission dialog: whether the user gave access to anything. */
    fun onMovementPermissionsResult(granted: Boolean) {
        viewModelScope.launch { movementRepository.onPermissionResult(granted, LocalDate.now()) }
    }

    /**
     * Saves the answers and opens the app. The saves are queued in each store's own scope, in order, so they finish
     * even though the intro leaves the screen at once. What was typed is forgotten, so the intro opened again from
     * Settings starts from the saved answers.
     */
    fun finish() {
        val answers = state.value
        if (answers != null) {
            typedName.value?.let { name ->
                val profile = profileStore.profile.value ?: Profile()
                profileStore.save(profile.copy(name = Profile.cleanName(name)))
            }
            coachStore.save(answers.preferences)
        }
        typedName.value = null
        chosen.value = null
        settingsStore.setOnboarded(true)
    }
}
