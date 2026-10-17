package se.novila.feature.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import se.novila.designsystem.glance.NovilaGlance
import se.novila.feature.widget.R
import se.novila.model.CardioType
import se.novila.model.DayCheer
import se.novila.model.DayEntry
import se.novila.model.FoodGroup
import se.novila.model.Goals
import se.novila.model.LatestLog
import se.novila.model.balance
import se.novila.model.cheers
import se.novila.model.formatHours
import se.novila.model.latestLog
import se.novila.model.nutritionScore
import se.novila.ui.R as UiR

/**
 * The widget for [day] measured against [goals]: the balance, a value per part, the latest log and the day's first
 * cheer, and a button for a glass of water. A tap anywhere else opens the app. Only the background before the stores
 * are read (either is null).
 */
@Composable
fun WidgetContent(day: DayEntry?, goals: Goals?) {
    val context = LocalContext.current
    val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(NovilaGlance.widgetCornerRadius)
            .background(GlanceTheme.colors.surface)
            .padding(NovilaGlance.spacing.l)
            .let { if (launch != null) it.clickable(actionStartActivity(launch)) else it },
    ) {
        if (day == null || goals == null) return@Column
        Label(context.getString(R.string.widget_balance), GlanceTheme.colors.onSurfaceVariant)
        Text(
            text = context.getString(R.string.widget_balance_value, day.balance(steps = null, goals = goals)),
            style = TextStyle(
                color = GlanceTheme.colors.primary,
                fontSize = NovilaGlance.headlineSize,
                fontWeight = FontWeight.Bold,
            ),
        )
        Row(modifier = GlanceModifier.fillMaxWidth().padding(vertical = NovilaGlance.spacing.s)) {
            Part(R.string.widget_sleep, sleepValue(context, day), GlanceTheme.colors.secondary)
            Part(R.string.widget_food, foodValue(context, day, goals), GlanceTheme.colors.tertiary)
            Part(
                R.string.widget_water,
                context.getString(R.string.widget_water_value, day.waterGlasses, goals.waterGlasses),
                GlanceTheme.colors.tertiary,
            )
        }
        day.latestLog()?.let { Label(latestText(context, it), GlanceTheme.colors.onSurfaceVariant) }
        Text(
            text = cheerText(context, day.cheers(goals).first()),
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = NovilaGlance.bodySize),
            maxLines = 3,
        )
        Spacer(modifier = GlanceModifier.defaultWeight())
        Button(
            text = context.getString(R.string.widget_log_water),
            onClick = actionRunCallback<LogWaterAction>(),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = GlanceTheme.colors.tertiary,
                contentColor = GlanceTheme.colors.onTertiary,
            ),
        )
    }
}

@Composable
private fun Label(text: String, color: ColorProvider) {
    Text(
        text = text,
        style = TextStyle(color = color, fontSize = NovilaGlance.labelSize, fontWeight = FontWeight.Medium),
        maxLines = 1,
    )
}

/** One part of the day in its pillar's [color]: a small title over the value. */
@Composable
private fun RowScope.Part(title: Int, value: String, color: ColorProvider) {
    Column(modifier = GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.Start) {
        Label(LocalContext.current.getString(title), color)
        Spacer(modifier = GlanceModifier.height(NovilaGlance.spacing.xxs))
        Text(
            text = value,
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = NovilaGlance.titleSize,
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 1,
        )
    }
}

private fun hours(context: Context, hours: Float): String {
    val locale = context.resources.configuration.locales[0]
    return formatHours(hours, locale)?.let { context.getString(UiR.string.hours_value, it) }
        ?: context.getString(UiR.string.value_missing)
}

private fun sleepValue(context: Context, day: DayEntry) = hours(context, day.sleepHours)

private fun foodValue(context: Context, day: DayEntry, goals: Goals): String =
    day.nutritionScore(goals)?.let { context.getString(R.string.widget_food_value, it.total) }
        ?: context.getString(UiR.string.value_missing)

private fun latestText(context: Context, log: LatestLog): String {
    val what = when (log) {
        is LatestLog.Cardio -> context.getString(cardioName(log.log.type), log.log.minutes)

        is LatestLog.Strength -> context.getString(R.string.widget_latest_strength, log.minutes)

        is LatestLog.Food ->
            context.resources.getQuantityString(R.plurals.widget_latest_food, log.portions, log.portions)

        is LatestLog.Water ->
            context.resources.getQuantityString(R.plurals.widget_latest_water, log.glasses, log.glasses)

        is LatestLog.Sleep -> context.getString(R.string.widget_latest_sleep, hours(context, log.hours))
    }
    return context.getString(R.string.widget_latest, what)
}

private fun cardioName(type: CardioType): Int = when (type) {
    CardioType.Walking -> R.string.widget_latest_walk
    CardioType.Running -> R.string.widget_latest_run
    CardioType.Cycling -> R.string.widget_latest_ride
    CardioType.Swimming -> R.string.widget_latest_swim
}

private fun cheerText(context: Context, cheer: DayCheer): String = when (cheer) {
    is DayCheer.OutdoorsSummer -> context.getString(R.string.widget_cheer_outdoors_summer, cheer.log.minutes)
    is DayCheer.OutdoorsDaylight -> context.getString(R.string.widget_cheer_outdoors_daylight, cheer.log.minutes)
    is DayCheer.Swim -> context.getString(R.string.widget_cheer_swim, cheer.minutes)
    is DayCheer.Strength -> context.getString(R.string.widget_cheer_strength)
    is DayCheer.RestedNight -> context.getString(R.string.widget_cheer_rested, hours(context, cheer.hours))
    is DayCheer.ShortNight -> context.getString(R.string.widget_cheer_short_night, hours(context, cheer.hours))
    DayCheer.Vegetables -> context.getString(R.string.widget_cheer_vegetables, FoodGroup.Vegetables.goal)
    DayCheer.Water -> context.getString(R.string.widget_cheer_water)
    is DayCheer.GoodFood -> context.getString(R.string.widget_cheer_good_food, cheer.score)
    DayCheer.Start -> context.getString(R.string.widget_cheer_start)
}
