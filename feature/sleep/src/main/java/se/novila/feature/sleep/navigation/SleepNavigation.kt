package se.novila.feature.sleep.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import se.novila.feature.sleep.SleepScreen
import se.novila.model.DayEntry

/** The Sleep tab. */
@Serializable data object SleepRoute : NavKey

fun EntryProviderScope<NavKey>.sleepEntry(
    today: DayEntry,
    history: List<DayEntry>,
    onSleep: (Float) -> Unit,
    contentPadding: PaddingValues,
) {
    entry<SleepRoute> { SleepScreen(today, history, onSleep, contentPadding) }
}
