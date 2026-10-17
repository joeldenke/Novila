package se.novila.feature.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import se.novila.data.GoalsStore
import se.novila.data.HealthStore

/**
 * Redraws the widget when the app saves a day or changes the goals, so it never shows an old value. Started once the
 * app is opened (MainActivity); the widget redraws itself while it is shown, and the system's hourly update covers
 * midnight. Nothing runs when nothing is saved.
 */
@SingleIn(AppScope::class)
class WidgetUpdates @Inject constructor(
    private val context: Context,
    private val store: HealthStore,
    private val goalsStore: GoalsStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var started = false

    @OptIn(FlowPreview::class)
    fun start() {
        if (started) return
        started = true
        scope.launch {
            combine(store.days.filterNotNull(), goalsStore.goals.filterNotNull()) { days, goals -> days to goals }
                .drop(1) // What the widget already shows.
                .debounce(SAVE_SETTLE_MILLIS)
                .collect {
                    try {
                        NovilaWidget().updateAll(context)
                    } catch (e: IllegalStateException) {
                        // The widget host can be busy, for example while the launcher restarts. The next save tries
                        // again.
                        Log.w(TAG, "Widget update failed", e)
                    }
                }
        }
    }

    private companion object {
        const val TAG = "NovilaWidget"

        /** Several taps in a row (portions, water) redraw once. */
        const val SAVE_SETTLE_MILLIS = 500L
    }
}
