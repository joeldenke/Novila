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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import se.novila.data.RemindersStore
import se.novila.model.Reminder
import se.novila.model.ReminderSetting
import se.novila.model.Reminders
import se.novila.reminders.ReminderNotifications
import se.novila.reminders.ReminderScheduler
import java.time.LocalTime

/** The user's [reminders], and whether Novila may show notifications right now. */
data class RemindersUiState(val reminders: Reminders, val notificationsAllowed: Boolean)

/** The Reminders screen: saves each change and plans or cancels the reminder's work with it. Scoped to its entry. */
@ViewModelKey
@ContributesIntoMap(AppScope::class)
@Inject
class RemindersViewModel(
    private val store: RemindersStore,
    private val scheduler: ReminderScheduler,
    private val notifications: ReminderNotifications,
) : ViewModel() {
    private val allowed = MutableStateFlow(notifications.canNotify())

    /** The reminders, or null before the store's first read. */
    val state: StateFlow<RemindersUiState?> =
        combine(store.reminders.filterNotNull(), allowed, ::RemindersUiState)
            .stateIn(viewModelScope, SharingStarted.Eagerly, initial())

    private fun initial(): RemindersUiState? = store.reminders.value?.let { RemindersUiState(it, allowed.value) }

    /** Called on resume and after the permission dialog: the user may have changed it in the phone's settings. */
    fun refresh() {
        allowed.value = notifications.canNotify()
    }

    fun setOn(reminder: Reminder, on: Boolean) = change(reminder) { it.copy(on = on) }

    fun setTime(reminder: Reminder, time: LocalTime) = change(reminder) { it.copy(time = time) }

    private fun change(reminder: Reminder, update: (ReminderSetting) -> ReminderSetting) {
        val current = store.reminders.value ?: return
        val setting = update(current[reminder])
        store.save(reminder, setting)
        scheduler.apply(reminder, setting)
    }
}
