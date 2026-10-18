package se.novila.feature.nutrition

import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import se.novila.feature.nutrition.R
import se.novila.recipes.Recipe

/** A recipe's [name], [ingredients] for one serving and [steps], in English and Swedish. */
data class RecipeText(@StringRes val name: Int, @ArrayRes val ingredients: Int, @ArrayRes val steps: Int)

val Recipe.text: RecipeText
    get() = when (this) {
        Recipe.OvernightOats -> RecipeText(
            R.string.recipe_overnight_oats,
            R.array.recipe_overnight_oats_ingredients,
            R.array.recipe_overnight_oats_steps,
        )

        Recipe.EggSandwich -> RecipeText(
            R.string.recipe_egg_sandwich,
            R.array.recipe_egg_sandwich_ingredients,
            R.array.recipe_egg_sandwich_steps,
        )

        Recipe.BerrySmoothie -> RecipeText(
            R.string.recipe_berry_smoothie,
            R.array.recipe_berry_smoothie_ingredients,
            R.array.recipe_berry_smoothie_steps,
        )

        Recipe.LentilBolognese -> RecipeText(
            R.string.recipe_lentil_bolognese,
            R.array.recipe_lentil_bolognese_ingredients,
            R.array.recipe_lentil_bolognese_steps,
        )

        Recipe.BakedSalmon -> RecipeText(
            R.string.recipe_baked_salmon,
            R.array.recipe_baked_salmon_ingredients,
            R.array.recipe_baked_salmon_steps,
        )

        Recipe.ChickpeaCurry -> RecipeText(
            R.string.recipe_chickpea_curry,
            R.array.recipe_chickpea_curry_ingredients,
            R.array.recipe_chickpea_curry_steps,
        )

        Recipe.ChickenStirFry -> RecipeText(
            R.string.recipe_chicken_stir_fry,
            R.array.recipe_chicken_stir_fry_ingredients,
            R.array.recipe_chicken_stir_fry_steps,
        )

        Recipe.VegetableOmelette -> RecipeText(
            R.string.recipe_vegetable_omelette,
            R.array.recipe_vegetable_omelette_ingredients,
            R.array.recipe_vegetable_omelette_steps,
        )

        Recipe.TunaWheatSalad -> RecipeText(
            R.string.recipe_tuna_wheat_salad,
            R.array.recipe_tuna_wheat_salad_ingredients,
            R.array.recipe_tuna_wheat_salad_steps,
        )

        Recipe.BeanSoup -> RecipeText(
            R.string.recipe_bean_soup,
            R.array.recipe_bean_soup_ingredients,
            R.array.recipe_bean_soup_steps,
        )

        Recipe.ChickenBulgurSalad -> RecipeText(
            R.string.recipe_chicken_bulgur_salad,
            R.array.recipe_chicken_bulgur_salad_ingredients,
            R.array.recipe_chicken_bulgur_salad_steps,
        )

        Recipe.AppleAndNuts -> RecipeText(
            R.string.recipe_apple_and_nuts,
            R.array.recipe_apple_and_nuts_ingredients,
            R.array.recipe_apple_and_nuts_steps,
        )

        Recipe.CarrotsAndHummus -> RecipeText(
            R.string.recipe_carrots_and_hummus,
            R.array.recipe_carrots_and_hummus_ingredients,
            R.array.recipe_carrots_and_hummus_steps,
        )

        Recipe.YogurtWithBerries -> RecipeText(
            R.string.recipe_yogurt_with_berries,
            R.array.recipe_yogurt_with_berries_ingredients,
            R.array.recipe_yogurt_with_berries_steps,
        )
    }
