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
import se.novila.data.MeasurementsStore
import se.novila.data.ProfileStore
import se.novila.data.SelectedDay
import se.novila.model.MeasurementEntry
import se.novila.model.MeasurementForm
import se.novila.model.Profile
import se.novila.model.ProfileForm
import se.novila.model.latestWeightKg
import se.novila.model.on
import java.time.LocalDate

/**
 * The measurements as typed, the chosen day ([SelectedDay]: today and the 30 days before it) and every logged day
 * newest first.
 */
data class MeasurementsUiState(
    val form: MeasurementForm,
    val day: LocalDate,
    val today: LocalDate,
    val entries: List<MeasurementEntry>,
)

/** The profile form as typed, the year birth years are checked against, the saved [profile] and the measurements. */
data class ProfileUiState(
    val form: ProfileForm,
    val currentYear: Int,
    val profile: Profile,
    val measurements: MeasurementsUiState,
)

/**
 * The profile screen, with the measurements on it. Scoped to its entry. Every change to the profile is saved as it is
 * typed, like the other settings; the text stays here, so a number that is still wrong stays on screen while the
 * saved profile keeps its last valid value. The weight is the measurements': saving a day moves the profile's weight
 * to the newest logged weight, so the goal suggestions follow the scale and the profile keeps no second number.
 */
@ViewModelKey
@ContributesIntoMap(AppScope::class)
@Inject
class ProfileViewModel(
    private val store: ProfileStore,
    private val measurementsStore: MeasurementsStore,
) : ViewModel() {
    private val currentYear = LocalDate.now().year
    private val selectedDay = SelectedDay()

    /** What the user has typed since the screen opened, or null before the first change. */
    private val typed = MutableStateFlow<ProfileForm?>(null)

    /** What the user has typed for the chosen day's measurements, or null to show that day's saved ones. */
    private val typedMeasurements = MutableStateFlow<MeasurementForm?>(null)

    private val measurements = combine(
        typedMeasurements,
        measurementsStore.entries,
        selectedDay.dates,
        selectedDay.today,
    ) { typed, entries, day, today ->
        entries?.let { MeasurementsUiState(typed ?: MeasurementForm.of(it.on(day)), day, today, it) }
    }

    /** The profile and the measurements, or null before the stores' first reads. */
    val state: StateFlow<ProfileUiState?> =
        combine(typed, store.profile, measurements) { typed, saved, measurements ->
            if (saved == null || measurements == null) return@combine null
            ProfileUiState(typed ?: ProfileForm.of(saved), currentYear, saved, measurements)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** The last profile saved from here. Saves are written in the background, so the store may not have it yet. */
    private var lastSaved: Profile? = null

    private fun savedProfile(): Profile = lastSaved ?: store.profile.value ?: Profile()

    fun onChange(form: ProfileForm) {
        val capped = form.copy(name = form.name.take(Profile.MAX_NAME_LENGTH))
        typed.value = capped
        val saved = savedProfile()
        // The weight is not typed here; it follows the measurements, so keep what they last set.
        val profile = capped.toProfile(saved = saved, currentYear = currentYear).copy(weightKg = saved.weightKg)
        lastSaved = profile
        store.save(profile)
    }

    /** Called on resume, so "today" stays correct after midnight. */
    fun refresh() {
        selectedDay.refresh()
    }

    /** Shows [date] within the editable days; what was typed for the day being left is not carried over. */
    fun onSelectDay(date: LocalDate) {
        selectedDay.select(date)
        typedMeasurements.value = null
    }

    fun onMeasurementsChange(form: MeasurementForm) {
        typedMeasurements.value = form
    }

    /** Saves the chosen day, and lets the profile's weight follow the newest logged weight. */
    fun saveMeasurements() {
        val day = selectedDay.value
        val entries = measurementsStore.entries.value ?: return
        val saved = entries.on(day)
        val measurements = (typedMeasurements.value ?: MeasurementForm.of(saved)).toMeasurements(saved)
        measurementsStore.save(day, measurements)
        // The whole history as it will be, so an older day measured again still keeps the newest weight on top.
        val logged = entries.filter { it.date != day } +
            listOfNotNull(MeasurementEntry(day, measurements).takeUnless { measurements.isEmpty })
        logged.latestWeightKg()?.let { weight ->
            val profile = savedProfile().copy(weightKg = weight)
            lastSaved = profile
            store.save(profile)
        }
    }
}
