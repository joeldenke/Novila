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
import se.novila.model.latestWeightKg
import se.novila.model.on
import java.time.LocalDate

/**
 * The measurements as typed, the chosen day ([SelectedDay]: today and the 30 days before it) and every logged day
 * newest first, or null before the stores' first reads.
 */
data class MeasurementsUiState(
    val form: MeasurementForm,
    val day: LocalDate,
    val today: LocalDate,
    val entries: List<MeasurementEntry>,
)

/**
 * The measurements screen. Scoped to its entry. The day is the one being logged, today unless the user went back to
 * fill in a forgotten day, and the fields start with what that day already has. Saving a day also moves the
 * profile's current weight to the newest logged weight, so the goal suggestions follow the scale and the profile
 * does not keep a second, separate number.
 */
@ViewModelKey
@ContributesIntoMap(AppScope::class)
@Inject
class MeasurementsViewModel(
    private val store: MeasurementsStore,
    private val profileStore: ProfileStore,
) : ViewModel() {
    private val selectedDay = SelectedDay()

    /** What the user has typed for the chosen day, or null to show that day's saved measurements. */
    private val typed = MutableStateFlow<MeasurementForm?>(null)

    /** The form, the chosen day and the history, or null before the stores' first reads. */
    val state: StateFlow<MeasurementsUiState?> =
        combine(typed, store.entries, selectedDay.dates, selectedDay.today) { typed, entries, day, today ->
            if (entries == null) return@combine null
            val saved = entries.on(day)
            MeasurementsUiState(typed ?: MeasurementForm.of(saved), day, today, entries)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Called on resume, so "today" stays correct after midnight. */
    fun refresh() {
        selectedDay.refresh()
    }

    /** Shows [date] within the editable days; what was typed for the day being left is not carried over. */
    fun onSelectDay(date: LocalDate) {
        selectedDay.select(date)
        typed.value = null
    }

    fun onChange(form: MeasurementForm) {
        typed.value = form
    }

    /** Saves the chosen day, and lets the profile's weight follow the newest logged weight. */
    fun save() {
        val day = selectedDay.value
        val entries = store.entries.value ?: return
        val saved = entries.on(day)
        val measurements = (typed.value ?: MeasurementForm.of(saved)).toMeasurements(saved)
        store.save(day, measurements)
        // The whole history as it will be, so an older day measured again still keeps the newest weight on top.
        val logged = entries.filter { it.date != day } +
            listOfNotNull(MeasurementEntry(day, measurements).takeUnless { measurements.isEmpty })
        logged.latestWeightKg()?.let { weight ->
            profileStore.save((profileStore.profile.value ?: Profile()).copy(weightKg = weight))
        }
    }
}
