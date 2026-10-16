package se.novila.feature.settings

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
import se.novila.data.ProfileStore
import se.novila.model.Profile
import se.novila.model.ProfileForm
import java.time.LocalDate

/** The profile form as typed, and the year its birth years are checked against. */
data class ProfileUiState(val form: ProfileForm, val currentYear: Int)

/**
 * The profile screen. Scoped to its entry. Every change is saved as it is typed, like the other settings; the text
 * stays here, so a number that is still wrong stays on screen while the saved profile keeps its last valid value.
 */
@ViewModelKey
@ContributesIntoMap(AppScope::class)
@Inject
class ProfileViewModel(private val store: ProfileStore) : ViewModel() {
    private val currentYear = LocalDate.now().year

    /** What the user has typed since the screen opened, or null before the first change. */
    private val typed = MutableStateFlow<ProfileForm?>(null)

    /** The form, or null before the store's first read. */
    val state: StateFlow<ProfileUiState?> = combine(typed, store.profile) { typed, saved -> read(typed, saved) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, read(null, store.profile.value))

    private fun read(typed: ProfileForm?, saved: Profile?): ProfileUiState? {
        val form = typed ?: saved?.let(ProfileForm::of) ?: return null
        return ProfileUiState(form, currentYear)
    }

    /** The last profile saved from here. Saves are written in the background, so the store may not have it yet. */
    private var lastSaved: Profile? = null

    fun onChange(form: ProfileForm) {
        val capped = form.copy(name = form.name.take(Profile.MAX_NAME_LENGTH))
        typed.value = capped
        val profile = capped.toProfile(saved = lastSaved ?: store.profile.value ?: Profile(), currentYear = currentYear)
        lastSaved = profile
        store.save(profile)
    }
}
