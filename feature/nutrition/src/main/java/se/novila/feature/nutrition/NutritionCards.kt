package se.novila.feature.nutrition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaMeter
import se.novila.designsystem.component.NovilaScoreRing
import se.novila.designsystem.component.NovilaStepper
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.feature.nutrition.R
import se.novila.model.DayEntry
import se.novila.model.FoodGroup
import se.novila.model.Goals
import se.novila.model.dailyGoal
import se.novila.model.nextPortion
import se.novila.model.nutritionScore
import se.novila.model.portions
import se.novila.ui.common.nextPortionText
import kotlin.math.roundToInt
import se.novila.ui.R as UiR

/**
 * The day's nutrition score, the smartest next step and how each food group contributes. Titled as today's when
 * [isToday].
 */
@Composable
fun NutritionScoreCard(today: DayEntry, goals: Goals, isToday: Boolean = true) {
    val nutrition = NovilaTheme.pillars.nutrition
    val score = today.nutritionScore(goals)
    NovilaCard(
        title = stringResource(if (isToday) R.string.nutrition_score_title else R.string.nutrition_score_title_day),
        icon = painterResource(UiR.drawable.ic_nutrition),
        role = nutrition,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.l),
        ) {
            NovilaScoreRing(
                score = score?.total ?: 0,
                progress = score?.progress ?: 0f,
                caption = stringResource(UiR.string.balance_unit),
                role = nutrition,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.xs)) {
                val next = today.nextPortion(goals)
                NovilaText(
                    stringResource(if (next != null) R.string.nutrition_next_title else R.string.nutrition_done_title),
                    style = NovilaTheme.typography.labelLarge,
                    color = NovilaTheme.colors.onSurfaceVariant,
                )
                NovilaText(
                    if (next != null) nextPortionText(next) else stringResource(R.string.nutrition_done_body),
                    style = NovilaTheme.typography.bodyMedium,
                )
            }
        }
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
        NovilaText(
            stringResource(R.string.nutrition_about),
            style = NovilaTheme.typography.bodySmall,
            color = NovilaTheme.colors.onSurfaceVariant,
        )
    }
}

/** Steppers for each food group, water against the user's goal, and for treats, for today or an earlier day. */
@Composable
fun FoodLogCard(
    today: DayEntry,
    goals: Goals,
    onPortions: (FoodGroup, Int) -> Unit,
    onTreats: (Int) -> Unit,
    isToday: Boolean = true,
) {
    val nutrition = NovilaTheme.pillars.nutrition
    NovilaCard(title = stringResource(if (isToday) R.string.food_log_title else R.string.food_log_title_day)) {
        Column(verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.m)) {
            FoodGroup.entries.forEach { group ->
                val name = foodName(group)
                val count = today.portions(group)
                NovilaStepper(
                    title = name,
                    value = stringResource(R.string.food_progress, count, group.dailyGoal(goals)),
                    onDecrement = { onPortions(group, -1) },
                    onIncrement = { onPortions(group, +1) },
                    decrementDescription = stringResource(R.string.food_remove, name),
                    incrementDescription = stringResource(R.string.food_add, name),
                    role = nutrition,
                    decrementEnabled = count > 0,
                )
            }
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
