package se.novila.feature.nutrition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaHeroBar
import se.novila.designsystem.component.NovilaHeroBars
import se.novila.designsystem.component.NovilaHeroPanel
import se.novila.designsystem.component.NovilaHeroPart
import se.novila.designsystem.component.NovilaHeroRing
import se.novila.designsystem.component.NovilaHeroText
import se.novila.designsystem.component.NovilaMeter
import se.novila.designsystem.component.NovilaSky
import se.novila.designsystem.component.NovilaStepper
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.feature.nutrition.R
import se.novila.model.DayEntry
import se.novila.model.FISH_TIMES_A_WEEK
import se.novila.model.FoodGroup
import se.novila.model.Goals
import se.novila.model.dailyGoal
import se.novila.model.nextPortion
import se.novila.model.nutritionScore
import se.novila.model.portions
import se.novila.ui.common.nextPortionText
import se.novila.ui.common.weekdayLabels
import java.time.format.TextStyle
import kotlin.math.roundToInt
import se.novila.ui.R as UiR

/**
 * The day at a glance at the top of Nutrition, on the falu red field under the [sky]: the shown [day]'s nutrition
 * score, the smartest next step, and the scores of the [week] up to the day. Titled as today's when [isToday].
 */
@Composable
internal fun NutritionOverview(
    day: DayEntry,
    week: List<DayEntry>,
    goals: Goals,
    isToday: Boolean,
    modifier: Modifier = Modifier,
    sky: NovilaSky = NovilaSky.Day,
) {
    val score = day.nutritionScore(goals)
    NovilaHeroPanel(
        title = stringResource(if (isToday) R.string.nutrition_score_title else R.string.nutrition_score_title_day),
        modifier = modifier,
        sky = sky,
        part = NovilaHeroPart.Nutrition,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.l),
        ) {
            NovilaHeroRing(
                progress = score?.progress ?: 0f,
                value = score?.total?.toString() ?: stringResource(UiR.string.value_missing),
                unit = stringResource(UiR.string.balance_unit),
            )
            val next = day.nextPortion(goals)
            NovilaHeroText(
                headline = stringResource(
                    if (next != null) R.string.nutrition_next_title else R.string.nutrition_done_title,
                ),
                body = if (next != null) nextPortionText(next) else stringResource(R.string.nutrition_done_body),
                modifier = Modifier.weight(1f),
            )
        }
        NovilaHeroBars(bars = scores(week, day, goals), max = 100f)
    }
}

/** A bar per day of the [week] with its score, the [shown] one in the sun's color. */
@Composable
private fun scores(week: List<DayEntry>, shown: DayEntry, goals: Goals): List<NovilaHeroBar> {
    val locale = LocalConfiguration.current.locales[0]
    val labels = weekdayLabels(week)
    return week.mapIndexed { i, day ->
        val name = day.date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
        val score = day.nutritionScore(goals)?.total
        NovilaHeroBar(
            label = labels[i],
            value = score?.toFloat(),
            current = day.date == shown.date,
            description = if (score != null) {
                stringResource(R.string.nutrition_hero_day, name, score)
            } else {
                stringResource(R.string.nutrition_hero_day_missing, name)
            },
        )
    }
}

/**
 * How the shown day's score adds up: each food group's points, what sweets and snacks took off, and what the
 * weights follow. Last in the food sheet, under what is logged.
 */
@Composable
fun ScoreBreakdownCard(today: DayEntry, goals: Goals, isToday: Boolean = true, fishDays: Int? = null) {
    val nutrition = NovilaTheme.pillars.nutrition
    val score = today.nutritionScore(goals)
    NovilaCard(
        title = stringResource(R.string.nutrition_breakdown_title),
        icon = painterResource(UiR.drawable.ic_nutrition),
        role = nutrition,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.m)) {
            FoodGroup.entries.forEach { group ->
                val part = score?.parts?.firstOrNull { it.group == group }
                NovilaMeter(
                    label = foodName(group),
                    value = stringResource(R.string.fraction, (part?.points ?: 0f).roundToInt(), group.points),
                    progress = part?.progress ?: 0f,
                    role = nutrition,
                )
            }
        }
        val penalty = score?.treatPenalty ?: 0
        if (penalty > 0) {
            NovilaText(
                stringResource(
                    if (isToday) R.string.nutrition_treat_penalty else R.string.nutrition_treat_penalty_day,
                    penalty,
                ),
                style = NovilaTheme.typography.bodySmall,
                color = NovilaTheme.colors.onSurfaceVariant,
            )
        }
        if (fishDays != null) {
            NovilaMeter(
                label = stringResource(R.string.nutrition_fish_week),
                value = stringResource(R.string.fraction, fishDays, FISH_TIMES_A_WEEK),
                progress = (fishDays.toFloat() / FISH_TIMES_A_WEEK).coerceAtMost(1f),
                role = nutrition,
            )
        }
        val redMeatPenalty = score?.redMeatPenalty ?: 0
        if (redMeatPenalty > 0) {
            NovilaText(
                stringResource(
                    if (isToday) R.string.nutrition_red_meat_penalty else R.string.nutrition_red_meat_penalty_day,
                    redMeatPenalty,
                ),
                style = NovilaTheme.typography.bodySmall,
                color = NovilaTheme.colors.onSurfaceVariant,
            )
        }
        NovilaText(
            stringResource(R.string.nutrition_about),
            style = NovilaTheme.typography.bodySmall,
            color = NovilaTheme.colors.onSurfaceVariant,
        )
    }
}

/**
 * Steppers for each food group, for red meat and charcuterie, and for treats, for today or an earlier day. Water is
 * logged as drinks ([DrinksCard]).
 */
@Composable
fun FoodLogCard(
    today: DayEntry,
    onPortions: (FoodGroup, Int) -> Unit,
    onTreats: (Int) -> Unit,
    isToday: Boolean = true,
    onRedMeat: (Int) -> Unit = {},
    onFish: (Int) -> Unit = {},
) {
    val nutrition = NovilaTheme.pillars.nutrition
    NovilaCard(title = stringResource(if (isToday) R.string.food_log_title else R.string.food_log_title_day)) {
        Column(verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.m)) {
            FoodGroup.entries.filter { it != FoodGroup.Water }.forEach { group ->
                val name = foodName(group)
                val count = today.portions(group)
                NovilaStepper(
                    title = name,
                    value = stringResource(R.string.food_progress, count, group.dailyGoal()),
                    onDecrement = { onPortions(group, -1) },
                    onIncrement = { onPortions(group, +1) },
                    decrementDescription = stringResource(R.string.food_remove, name),
                    incrementDescription = stringResource(R.string.food_add, name),
                    role = nutrition,
                    hint = servingHint(group),
                    decrementEnabled = count > 0,
                )
            }
            val fish = stringResource(R.string.food_fish)
            NovilaStepper(
                title = fish,
                value = stringResource(
                    if (isToday) R.string.food_treats_value else R.string.food_treats_value_day,
                    today.food.fish,
                ),
                onDecrement = { onFish(-1) },
                onIncrement = { onFish(+1) },
                decrementDescription = stringResource(R.string.food_remove, fish),
                incrementDescription = stringResource(R.string.food_add, fish),
                role = nutrition,
                hint = stringResource(R.string.serving_fish),
                decrementEnabled = today.food.fish > 0,
            )
            val redMeat = stringResource(R.string.food_red_meat)
            NovilaStepper(
                title = redMeat,
                value = stringResource(
                    if (isToday) R.string.food_treats_value else R.string.food_treats_value_day,
                    today.food.redMeat,
                ),
                onDecrement = { onRedMeat(-1) },
                onIncrement = { onRedMeat(+1) },
                decrementDescription = stringResource(R.string.food_remove, redMeat),
                incrementDescription = stringResource(R.string.food_add, redMeat),
                role = nutrition,
                hint = stringResource(R.string.serving_red_meat),
                decrementEnabled = today.food.redMeat > 0,
            )
            val treats = stringResource(R.string.food_treats)
            NovilaStepper(
                title = treats,
                value = stringResource(
                    if (isToday) R.string.food_treats_value else R.string.food_treats_value_day,
                    today.food.treats,
                ),
                onDecrement = { onTreats(-1) },
                onIncrement = { onTreats(+1) },
                decrementDescription = stringResource(R.string.food_remove, treats),
                incrementDescription = stringResource(R.string.food_add, treats),
                role = nutrition,
                decrementEnabled = today.food.treats > 0,
            )
        }
    }
}

@Composable
@ReadOnlyComposable
fun foodName(group: FoodGroup): String = stringResource(
    when (group) {
        FoodGroup.Vegetables -> R.string.food_vegetables
        FoodGroup.Meals -> R.string.food_meals
        FoodGroup.Protein -> R.string.food_protein
        FoodGroup.WholeGrain -> R.string.food_whole_grain
        FoodGroup.Water -> R.string.water_title
    },
)

/** A short, plain explanation of what one serving is for [group], after the Swedish Food Agency's dietary advice. */
@Composable
@ReadOnlyComposable
fun servingHint(group: FoodGroup): String = stringResource(
    when (group) {
        FoodGroup.Vegetables -> R.string.serving_vegetables
        FoodGroup.Meals -> R.string.serving_meals
        FoodGroup.Protein -> R.string.serving_protein
        FoodGroup.WholeGrain -> R.string.serving_whole_grain
        FoodGroup.Water -> R.string.serving_water
    },
)
