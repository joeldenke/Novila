package se.novila.feature.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.provideContent
import se.novila.designsystem.glance.NovilaGlanceTheme
import se.novila.model.FoodGroup
import se.novila.model.withPortions
import java.time.LocalDate

/**
 * Today on the home screen: the day's balance, sleep, food and water, the latest log, something encouraging about
 * what the user did, and a glass of water in one tap. Reads the same stores as the app, while the widget is shown;
 * nothing is read from Health Connect here (it only allows reads while the app is open), so the balance is sleep and
 * food, like Today without a movement source.
 */
class NovilaWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val graph = context.widgetGraph
        provideContent {
            val days by graph.healthStore.days.collectAsState()
            val goals by graph.goalsStore.goals.collectAsState()
            NovilaGlanceTheme {
                // Nothing but the background until the stores are read, a moment after the phone starts.
                WidgetContent(day = days?.day(LocalDate.now()), goals = goals)
            }
        }
    }
}

/** Gives the system the widget. Registered in this module's manifest. */
class NovilaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NovilaWidget()
}

/** The widget's button: one more glass of water today, saved like in the app. */
class LogWaterAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        context.widgetGraph.healthStore.update(LocalDate.now()) { it.withPortions(FoodGroup.Water, 1) }
    }
}
