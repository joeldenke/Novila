package se.novila.feature.nutrition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import se.novila.designsystem.component.NovilaButton
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaChoiceChip
import se.novila.designsystem.component.NovilaMeter
import se.novila.designsystem.component.NovilaStepper
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.model.DRINK_SIZES_ML
import se.novila.model.DRINK_STEP_ML
import se.novila.model.DayEntry
import se.novila.model.Drink
import se.novila.model.DrinkKind
import se.novila.model.Goals
import se.novila.model.MAX_DRINKS
import se.novila.model.MAX_DRINK_ML
import se.novila.model.MIN_DRINK_ML
import se.novila.model.drunkMl
import se.novila.model.lateCaffeine
import se.novila.model.waterMl
import se.novila.model.withDrink
import se.novila.ui.common.clockText
import se.novila.ui.common.decimalText
import se.novila.ui.preview.PreviewData
import java.time.LocalTime

/** Logging a drink on the shown day, and taking back the last one. */
data class DrinkActions(
    val onAdd: (DrinkKind, Int) -> Unit = { _, _ -> },
    val onUndo: () -> Unit = {},
)

/** How many of the day's drinks are listed, newest first. */
private const val SHOWN_DRINKS = 5

/**
 * What the user drank on [day]: millilitres towards the water goal in [goals], any kind of drink in any amount (a
 * glass, a cup, a bottle or anything in between), and the latest drinks with a way to take the last one back. When
 * caffeine was logged late today, a word on what it can do to sleep; the Sleep tab says whether it does for the user.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DrinksCard(day: DayEntry, goals: Goals, actions: DrinkActions, isToday: Boolean = true) {
    val nutrition = NovilaTheme.pillars.nutrition
    var kind by rememberSaveable { mutableStateOf(DrinkKind.Water) }
    var ml by rememberSaveable { mutableIntStateOf(DrinkKind.Water.defaultMl) }
    NovilaCard(
        title = stringResource(if (isToday) R.string.drinks_title else R.string.drinks_title_day),
        role = nutrition,
    ) {
        val goalMl = goals.waterGlasses * Goals.GLASS_ML
        NovilaMeter(
            label = stringResource(R.string.drinks_water_goal),
            value = stringResource(R.string.drinks_progress, litersText(day.waterMl), litersText(goalMl)),
            progress = (day.waterMl.toFloat() / goalMl).coerceIn(0f, 1f),
            role = nutrition,
        )
        if (day.drunkMl > day.waterMl) {
            NovilaText(
                stringResource(R.string.drinks_total, litersText(day.drunkMl)),
                style = NovilaTheme.typography.bodySmall,
                color = NovilaTheme.colors.onSurfaceVariant,
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s),
            verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s),
        ) {
            DrinkKind.entries.forEach { option ->
                NovilaChoiceChip(
                    label = drinkName(option),
                    selected = option == kind,
                    onClick = {
                        kind = option
                        ml = option.defaultMl
                    },
                    role = nutrition,
                )
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s),
            verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s),
        ) {
            DRINK_SIZES_ML.forEach { size ->
                NovilaChoiceChip(
                    label = decilitersText(size),
                    selected = size == ml,
                    onClick = { ml = size },
                    role = nutrition,
                )
            }
        }
        val amount = stringResource(R.string.drinks_amount)
        NovilaStepper(
            title = amount,
            value = decilitersText(ml),
            onDecrement = { ml = (ml - DRINK_STEP_ML).coerceAtLeast(MIN_DRINK_ML) },
            onIncrement = { ml = (ml + DRINK_STEP_ML).coerceAtMost(MAX_DRINK_ML) },
            decrementDescription = stringResource(R.string.drinks_less),
            incrementDescription = stringResource(R.string.drinks_more),
            role = nutrition,
            hint = stringResource(R.string.drinks_amount_hint),
            decrementEnabled = ml > MIN_DRINK_ML,
            incrementEnabled = ml < MAX_DRINK_ML,
        )
        NovilaButton(
            text = stringResource(R.string.drinks_add, decilitersText(ml), drinkName(kind).lowercase()),
            onClick = { actions.onAdd(kind, ml) },
            enabled = day.drinks.size < MAX_DRINKS,
            role = nutrition,
        )
        if (day.drinks.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.xxs)) {
                day.drinks.asReversed().take(SHOWN_DRINKS).forEach { drink -> DrinkLine(drink) }
                val more = day.drinks.size - SHOWN_DRINKS
                if (more > 0) {
                    NovilaText(
                        stringResource(R.string.drinks_more_logged, more),
                        style = NovilaTheme.typography.bodySmall,
                        color = NovilaTheme.colors.onSurfaceVariant,
                    )
                }
            }
            NovilaButton(
                text = stringResource(R.string.drinks_undo),
                onClick = actions.onUndo,
                role = nutrition,
            )
        }
        if (isToday && day.lateCaffeine() == true) {
            NovilaText(
                stringResource(R.string.drinks_late_caffeine),
                style = NovilaTheme.typography.bodySmall,
                color = NovilaTheme.colors.onSurfaceVariant,
            )
        }
        NovilaText(
            stringResource(R.string.drinks_about),
            style = NovilaTheme.typography.bodySmall,
            color = NovilaTheme.colors.onSurfaceVariant,
        )
    }
}

/** One logged drink: "Coffee · 1.5 dl", after the time when it is known. */
@Composable
private fun DrinkLine(drink: Drink) {
    val text = stringResource(R.string.drinks_line, drinkName(drink.kind), decilitersText(drink.ml))
    val time = drink.time
    NovilaText(
        if (time != null) stringResource(R.string.drinks_line_time, clockText(time), text) else text,
        style = NovilaTheme.typography.bodyMedium,
    )
}

@Composable
private fun decilitersText(ml: Int): String = stringResource(R.string.drinks_dl, decimalText(ml / 100.0))

@Composable
private fun litersText(ml: Int): String = stringResource(R.string.drinks_liters, decimalText(ml / 1000.0))

@Composable
@ReadOnlyComposable
fun drinkName(kind: DrinkKind): String = stringResource(
    when (kind) {
        DrinkKind.Water -> R.string.drink_water
        DrinkKind.Coffee -> R.string.drink_coffee
        DrinkKind.Tea -> R.string.drink_tea
        DrinkKind.HerbalTea -> R.string.drink_herbal_tea
        DrinkKind.Milk -> R.string.drink_milk
        DrinkKind.Juice -> R.string.drink_juice
        DrinkKind.SoftDrink -> R.string.drink_soft_drink
        DrinkKind.EnergyDrink -> R.string.drink_energy_drink
    },
)

/** A morning coffee, a bottle of water and a tea late in the afternoon. */
@Preview(showBackground = true)
@Composable
private fun DrinksCardPreview() {
    val day = DayEntry(PreviewData.date)
        .withDrink(Drink(DrinkKind.Coffee, 150, LocalTime.of(7, 45)))
        .withDrink(Drink(DrinkKind.Water, 500, LocalTime.of(10, 30)))
        .withDrink(Drink(DrinkKind.SoftDrink, 330, LocalTime.of(12, 15)))
        .withDrink(Drink(DrinkKind.Tea, 250, LocalTime.of(15, 0)))
    NovilaTheme { DrinksCard(day, Goals(), DrinkActions()) }
}

/** Nothing drunk logged yet. */
@Preview(showBackground = true)
@Composable
private fun DrinksCardEmptyPreview() {
    NovilaTheme { DrinksCard(DayEntry(PreviewData.date), Goals(), DrinkActions()) }
}
