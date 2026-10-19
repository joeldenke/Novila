package se.novila.feature.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
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
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import se.novila.designsystem.glance.NovilaGlance
import se.novila.feature.widget.R
import se.novila.model.DayCheer
import se.novila.model.DayEntry
import se.novila.model.FoodGroup
import se.novila.model.Goals
import se.novila.model.balance
import se.novila.model.cheers
import se.novila.model.formatHours
import se.novila.model.nutritionScore
import se.novila.ui.R as UiR

/** The sun emblem's size. */
private val SunSize = 20.dp

/** A pillar's coloured dot. */
private val DotSize = 8.dp

/** The hairline's thickness. */
private val HairlineSize = 1.dp

/**
 * The widget for [day] measured against [goals]: the day's balance, a value per part (sleep, food and water), and
 * the day's first encouragement. Restrained on purpose: the cheer already says what the user did, so a separate
 * "latest log" line would only crowd it. A tap anywhere opens the app. Only the frosted background is drawn before
 * the stores are read (either is null).
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
            .background(NovilaGlance.cardBackground)
            .padding(NovilaGlance.spacing.l)
            .let { if (launch != null) it.clickable(actionStartActivity(launch)) else it },
    ) {
        if (day == null || goals == null) return@Column
        Header(context)
        Spacer(modifier = GlanceModifier.height(NovilaGlance.spacing.s))
        Balance(context, day.balance(steps = null, goals = goals))
        Spacer(modifier = GlanceModifier.height(NovilaGlance.spacing.m))
        Parts(context, day, goals)
        Spacer(modifier = GlanceModifier.defaultWeight())
        Hairline()
        Spacer(modifier = GlanceModifier.height(NovilaGlance.spacing.m))
        Text(
            text = cheerText(context, day.cheers(goals).first()),
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = NovilaGlance.bodySize),
            maxLines = 2,
        )
    }
}

/** The card's title and the midsummer sun, like the hero card in the app. */
@Composable
private fun Header(context: Context) {
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = context.getString(R.string.widget_balance),
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = NovilaGlance.labelSize,
                fontWeight = FontWeight.Medium,
            ),
            modifier = GlanceModifier.defaultWeight(),
            maxLines = 1,
        )
        Image(
            provider = ImageProvider(NovilaGlance.sunDrawable),
            contentDescription = null,
            modifier = GlanceModifier.size(SunSize),
            colorFilter = ColorFilter.tint(NovilaGlance.accentSun),
        )
    }
}

/** The day's balance as a large [value], the one number that sums the day up. */
@Composable
private fun Balance(context: Context, value: Int) {
    Text(
        text = context.getString(R.string.widget_balance_value, value),
        style = TextStyle(
            color = GlanceTheme.colors.primary,
            fontSize = NovilaGlance.headlineSize,
            fontWeight = FontWeight.Bold,
        ),
        maxLines = 1,
    )
}

/** A hairline between the balance block and the encouragement. */
@Composable
private fun Hairline() {
    Spacer(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(HairlineSize)
            .background(NovilaGlance.cardOutline),
    )
}

/** The day's parts, each with its pillar's colour: sleep (lake), food and water (falu red, both nutrition). */
@Composable
private fun Parts(context: Context, day: DayEntry, goals: Goals) {
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        Part(context, R.string.widget_sleep, sleepValue(context, day), GlanceTheme.colors.secondary)
        Part(context, R.string.widget_food, foodValue(context, day, goals), GlanceTheme.colors.tertiary)
        Part(context, R.string.widget_water, waterValue(context, day, goals), GlanceTheme.colors.tertiary)
    }
}

/** One part in its [color]: a small coloured dot, its title and the value. */
@Composable
private fun RowScope.Part(context: Context, title: Int, value: String, color: ColorProvider) {
    Column(modifier = GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.Start) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(
                modifier = GlanceModifier
                    .size(DotSize)
                    .background(color)
                    .cornerRadius(DotSize / 2),
            )
            Spacer(modifier = GlanceModifier.size(NovilaGlance.spacing.xs))
            Text(
                text = context.getString(title),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = NovilaGlance.labelSize),
                maxLines = 1,
            )
        }
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

private fun waterValue(context: Context, day: DayEntry, goals: Goals): String =
    context.getString(R.string.widget_water_value, day.waterGlasses, goals.waterGlasses)

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
