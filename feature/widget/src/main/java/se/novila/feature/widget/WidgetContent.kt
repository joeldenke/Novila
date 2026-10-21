package se.novila.feature.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import se.novila.designsystem.component.NovilaSky
import se.novila.designsystem.glance.NovilaGlance
import se.novila.designsystem.glance.NovilaGlanceHero
import se.novila.designsystem.glance.NovilaGlancePillar
import se.novila.designsystem.glance.NovilaGlanceRing
import se.novila.feature.widget.R
import se.novila.model.DayCheer
import se.novila.model.DayEntry
import se.novila.model.FoodGroup
import se.novila.model.Goals
import se.novila.model.balance
import se.novila.model.cheers
import se.novila.model.formatHours
import se.novila.model.nutritionScore
import se.novila.model.sleepProgress
import se.novila.ui.R as UiR

/** The sky's sun or moon in the corner, like the hero card. */
private val SkyIconSize = 24.dp

/** A part's coloured dot in the legend. */
private val DotSize = 10.dp

/** The header row: the title and the sky icon. */
private val HeaderHeight = 24.dp

/** Two lines of encouragement under the rings. */
private val CheerHeight = 40.dp

/** The hero card's rings; the widget never draws them larger. */
private val MaxRingSize = 148.dp

/** Below this the rings would be too small to read. */
private val MinRingSize = 56.dp

/** Narrower than this, the rings stand alone without a legend. */
private val LegendMinWidth = 230.dp

/** Lower than this, the encouragement is left out and the legend gets one line per part. */
private val CheerMinHeight = 190.dp

/** The rings' share of the width beside the legend. */
private const val RINGS_SHARE = 0.46f

/**
 * The widget for [day] measured against [goals]: the app's hero card on the home screen. The pine panel with the
 * sky at [hour], the balance rings with the score in the middle, a legend with sleep, food and water, and the day's
 * first encouragement. The layout follows the widget's size: a small widget keeps the rings, a large one gets the
 * legend and the encouragement too. A tap anywhere opens the app. Only the panel is drawn before the stores are read
 * (either is null).
 */
@Composable
fun WidgetContent(day: DayEntry?, goals: Goals?, hour: Int) {
    val context = LocalContext.current
    val size = LocalSize.current
    val hero = NovilaGlanceHero.of(context)
    val sky = NovilaSky.at(hour)
    val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(NovilaGlance.widgetCornerRadius)
            .let { if (launch != null) it.clickable(actionStartActivity(launch)) else it },
    ) {
        Image(
            provider = ImageProvider(hero.panel(size.width, size.height, sky)),
            contentDescription = null,
            modifier = GlanceModifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
        if (day != null && goals != null) Hero(context, hero, sky, size, day, goals)
    }
}

/** What the panel shows: the title and sky, the rings and legend, and the encouragement when there is room. */
@Composable
private fun Hero(context: Context, hero: NovilaGlanceHero, sky: NovilaSky, size: DpSize, day: DayEntry, goals: Goals) {
    val padding = NovilaGlance.spacing.l
    val legend = size.width >= LegendMinWidth
    val cheer = size.height >= CheerMinHeight
    val ringRoom = size.height - padding * 2 - HeaderHeight - NovilaGlance.spacing.m -
        if (cheer) CheerHeight + NovilaGlance.spacing.m else 0.dp
    val ringWidth = if (legend) size.width * RINGS_SHARE else size.width - padding * 2
    val ringSize = minOf(ringRoom, ringWidth, MaxRingSize).coerceAtLeast(MinRingSize)
    Column(modifier = GlanceModifier.fillMaxSize().padding(padding)) {
        Header(context, hero, sky)
        Spacer(modifier = GlanceModifier.height(NovilaGlance.spacing.m))
        Row(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = if (legend) Alignment.Start else Alignment.CenterHorizontally,
        ) {
            Rings(context, hero, ringSize, day, goals)
            if (legend) {
                Spacer(modifier = GlanceModifier.width(NovilaGlance.spacing.l))
                Legend(context, hero, day, goals, compact = !cheer)
            }
        }
        if (cheer) {
            Spacer(modifier = GlanceModifier.height(NovilaGlance.spacing.m))
            Text(
                text = cheerText(context, day.cheers(goals).first()),
                style = TextStyle(color = hero.content, fontSize = NovilaGlance.bodySize),
                maxLines = 2,
            )
        }
    }
}

/** The card's title and the sky's sun or moon, like the hero card in the app. */
@Composable
private fun Header(context: Context, hero: NovilaGlanceHero, sky: NovilaSky) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().height(HeaderHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = context.getString(R.string.widget_balance),
            style = TextStyle(color = hero.content, fontSize = NovilaGlance.bodySize, fontWeight = FontWeight.Medium),
            modifier = GlanceModifier.defaultWeight(),
            maxLines = 1,
        )
        Image(
            provider = ImageProvider(hero.skyIcon(sky)),
            contentDescription = null,
            modifier = GlanceModifier.size(SkyIconSize),
            colorFilter = ColorFilter.tint(hero.sun),
        )
    }
}

/** The balance as rings for sleep and food, with the score in the middle, drawn like the hero card's. */
@Composable
private fun Rings(context: Context, hero: NovilaGlanceHero, size: Dp, day: DayEntry, goals: Goals) {
    val value = day.balance(steps = null, goals = goals)
    val unit = context.getString(UiR.string.balance_unit)
    val rings = listOf(
        NovilaGlanceRing(day.sleepProgress(goals), NovilaGlancePillar.Sleep),
        NovilaGlanceRing(day.nutritionScore(goals)?.progress ?: 0f, NovilaGlancePillar.Nutrition),
    )
    val score = context.getString(R.string.widget_balance_value, value)
    Image(
        provider = ImageProvider(hero.rings(size, rings, score, unit)),
        contentDescription = context.getString(R.string.widget_balance_description, value),
        modifier = GlanceModifier.size(size),
    )
}

/** Sleep, food and water, each with its pillar's dot. [compact] puts each on one line, for a low widget. */
@Composable
private fun Legend(context: Context, hero: NovilaGlanceHero, day: DayEntry, goals: Goals, compact: Boolean) {
    val parts = listOf(
        Triple(R.string.widget_sleep, sleepValue(context, day), NovilaGlancePillar.Sleep),
        Triple(R.string.widget_food, foodValue(context, day, goals), NovilaGlancePillar.Nutrition),
        // Water belongs to food, the nutrition pillar.
        Triple(R.string.widget_water, waterValue(context, day, goals), NovilaGlancePillar.Nutrition),
    )
    Column(modifier = GlanceModifier.defaultWeight()) {
        parts.forEachIndexed { i, (title, value, pillar) ->
            if (i > 0) {
                Spacer(
                    modifier = GlanceModifier.height(if (compact) NovilaGlance.spacing.xs else NovilaGlance.spacing.m),
                )
            }
            LegendRow(hero, context.getString(title), value, hero.color(pillar), compact)
        }
    }
}

/** One part: a dot in its [color], its [title] and its [value], under the title or, when [compact], beside it. */
@Composable
private fun LegendRow(hero: NovilaGlanceHero, title: String, value: String, color: ColorProvider, compact: Boolean) {
    val titleStyle = TextStyle(color = hero.contentVariant, fontSize = NovilaGlance.labelSize)
    val valueStyle = TextStyle(color = hero.content, fontSize = NovilaGlance.titleSize, fontWeight = FontWeight.Medium)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(modifier = GlanceModifier.size(DotSize).cornerRadius(DotSize / 2).background(color))
        Spacer(modifier = GlanceModifier.width(NovilaGlance.spacing.s))
        if (compact) {
            Text(text = title, style = titleStyle, maxLines = 1)
            Spacer(modifier = GlanceModifier.width(NovilaGlance.spacing.s))
            Text(text = value, style = valueStyle, maxLines = 1)
        } else {
            Column {
                Text(text = title, style = titleStyle, maxLines = 1)
                Text(text = value, style = valueStyle, maxLines = 1)
            }
        }
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
