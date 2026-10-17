package se.novila.feature.widget

import android.content.Context
import se.novila.data.GoalsStore
import se.novila.data.HealthStore

/**
 * What the widget needs from the app's Metro graph. The system creates the widget's receiver and actions itself, so
 * they reach the graph through the application ([Holder]); AppGraph in :app extends this interface.
 */
interface WidgetGraph {
    val healthStore: HealthStore
    val goalsStore: GoalsStore
    val widgetUpdates: WidgetUpdates

    /** The application, which holds the graph. */
    interface Holder {
        val widgetGraph: WidgetGraph
    }
}

internal val Context.widgetGraph: WidgetGraph get() = (applicationContext as WidgetGraph.Holder).widgetGraph
