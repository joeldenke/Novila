package se.novila.feature.widget

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import se.novila.data.HealthStore
import se.novila.model.withGlassOfWater
import java.time.LocalDate
import java.time.LocalTime
import se.novila.ui.R as UiR

/**
 * A Quick Settings tile that logs a glass of water with one tap, the same glass as the Nutrition tab's, and shows the
 * day's glasses against the goal. On the lock screen it asks to unlock first and shows no count, so health data is
 * neither logged nor seen without the phone being unlocked. The system creates the service, so it reaches the stores
 * through the application ([WidgetGraph]); it only runs while the Quick Settings panel is open.
 */
class WaterTileService : TileService() {
    private val scope = MainScope()

    override fun onStartListening() {
        super.onStartListening()
        scope.launch { show(widgetGraph.healthStore.days.filterNotNull().first()) }
    }

    override fun onClick() {
        super.onClick()
        if (isLocked) unlockAndRun(::logGlass) else logGlass()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun logGlass() {
        val store = widgetGraph.healthStore
        val today = LocalDate.now()
        val before = store.days.value?.day(today)?.drinks?.size
        store.update(today) { it.withGlassOfWater(LocalTime.now()) }
        scope.launch {
            // Shown once the save is done; a full day saves nothing, and then the tile shows the day as it is.
            val saved = withTimeoutOrNull(SAVE_WAIT_MILLIS) {
                store.days.filterNotNull().first { it.day(today).drinks.size != before }
            } ?: store.days.filterNotNull().first()
            show(saved)
            try {
                NovilaWidget().updateAll(this@WaterTileService)
            } catch (e: IllegalStateException) {
                // The widget host can be busy; the widget catches up with the next save or its hourly update.
                Log.w(TAG, "Widget update failed", e)
            }
        }
    }

    private suspend fun show(saved: HealthStore.SavedDays) {
        val tile = qsTile ?: return
        val goals = widgetGraph.goalsStore.goals.filterNotNull().first()
        val glasses = saved.day(LocalDate.now()).waterGlasses
        tile.state = Tile.STATE_ACTIVE
        tile.label = getString(R.string.tile_water_label)
        tile.icon = Icon.createWithResource(this, UiR.drawable.ic_water)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (isLocked) null else getString(R.string.tile_water_glasses, glasses, goals.waterGlasses)
        }
        tile.updateTile()
    }

    private companion object {
        const val TAG = "NovilaTile"

        /** How long the tile waits for the glass to be saved before showing the day as it is. */
        const val SAVE_WAIT_MILLIS = 2_000L
    }
}
