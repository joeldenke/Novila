package se.novila.feature.nutrition

import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import se.novila.feature.nutrition.R
import se.novila.recipes.Recipe

/** A recipe's [name], [ingredients] for one serving and [steps], in English and Swedish. */
data class RecipeText(@StringRes val name: Int, @ArrayRes val ingredients: Int, @ArrayRes val steps: Int)

/**
 * Maps each recipe to its localized text. Adding a recipe means adding one branch here and the three resources in
 * both `values/strings.xml` and `values-sv/strings.xml`; the exhaustive `when` makes a missing branch a build error.
 */
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

        Recipe.RyePorridgeWithLingonberries -> RecipeText(
            R.string.recipe_rye_porridge_with_lingonberries,
            R.array.recipe_rye_porridge_with_lingonberries_ingredients,
            R.array.recipe_rye_porridge_with_lingonberries_steps,
        )

        Recipe.AvocadoToast -> RecipeText(
            R.string.recipe_avocado_toast,
            R.array.recipe_avocado_toast_ingredients,
            R.array.recipe_avocado_toast_steps,
        )

        Recipe.ScrambledEggsWithSpinach -> RecipeText(
            R.string.recipe_scrambled_eggs_with_spinach,
            R.array.recipe_scrambled_eggs_with_spinach_ingredients,
            R.array.recipe_scrambled_eggs_with_spinach_steps,
        )

        Recipe.SkyrWithMuesli -> RecipeText(
            R.string.recipe_skyr_with_muesli,
            R.array.recipe_skyr_with_muesli_ingredients,
            R.array.recipe_skyr_with_muesli_steps,
        )

        Recipe.SmokedSalmonRye -> RecipeText(
            R.string.recipe_smoked_salmon_rye,
            R.array.recipe_smoked_salmon_rye_ingredients,
            R.array.recipe_smoked_salmon_rye_steps,
        )

        Recipe.BananaOatPancakes -> RecipeText(
            R.string.recipe_banana_oat_pancakes,
            R.array.recipe_banana_oat_pancakes_ingredients,
            R.array.recipe_banana_oat_pancakes_steps,
        )

        Recipe.MangoSmoothieBowl -> RecipeText(
            R.string.recipe_mango_smoothie_bowl,
            R.array.recipe_mango_smoothie_bowl_ingredients,
            R.array.recipe_mango_smoothie_bowl_steps,
        )

        Recipe.TofuScrambleOnRye -> RecipeText(
            R.string.recipe_tofu_scramble_on_rye,
            R.array.recipe_tofu_scramble_on_rye_ingredients,
            R.array.recipe_tofu_scramble_on_rye_steps,
        )

        Recipe.BreakfastBurrito -> RecipeText(
            R.string.recipe_breakfast_burrito,
            R.array.recipe_breakfast_burrito_ingredients,
            R.array.recipe_breakfast_burrito_steps,
        )

        Recipe.QuickBeanWrap -> RecipeText(
            R.string.recipe_quick_bean_wrap,
            R.array.recipe_quick_bean_wrap_ingredients,
            R.array.recipe_quick_bean_wrap_steps,
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

        Recipe.SwedishMeatballs -> RecipeText(
            R.string.recipe_swedish_meatballs,
            R.array.recipe_swedish_meatballs_ingredients,
            R.array.recipe_swedish_meatballs_steps,
        )

        Recipe.FalafelWithCouscous -> RecipeText(
            R.string.recipe_falafel_with_couscous,
            R.array.recipe_falafel_with_couscous_ingredients,
            R.array.recipe_falafel_with_couscous_steps,
        )

        Recipe.SalmonSoup -> RecipeText(
            R.string.recipe_salmon_soup,
            R.array.recipe_salmon_soup_ingredients,
            R.array.recipe_salmon_soup_steps,
        )

        Recipe.ShrimpPasta -> RecipeText(
            R.string.recipe_shrimp_pasta,
            R.array.recipe_shrimp_pasta_ingredients,
            R.array.recipe_shrimp_pasta_steps,
        )

        Recipe.TofuVegetableCurry -> RecipeText(
            R.string.recipe_tofu_vegetable_curry,
            R.array.recipe_tofu_vegetable_curry_ingredients,
            R.array.recipe_tofu_vegetable_curry_steps,
        )

        Recipe.SwedishPeaSoup -> RecipeText(
            R.string.recipe_swedish_pea_soup,
            R.array.recipe_swedish_pea_soup_ingredients,
            R.array.recipe_swedish_pea_soup_steps,
        )

        Recipe.RootVegetableLentilSoup -> RecipeText(
            R.string.recipe_root_vegetable_lentil_soup,
            R.array.recipe_root_vegetable_lentil_soup_ingredients,
            R.array.recipe_root_vegetable_lentil_soup_steps,
        )

        Recipe.ChickenTikkaWithRice -> RecipeText(
            R.string.recipe_chicken_tikka_with_rice,
            R.array.recipe_chicken_tikka_with_rice_ingredients,
            R.array.recipe_chicken_tikka_with_rice_steps,
        )

        Recipe.CodWithDill -> RecipeText(
            R.string.recipe_cod_with_dill,
            R.array.recipe_cod_with_dill_ingredients,
            R.array.recipe_cod_with_dill_steps,
        )

        Recipe.HalloumiSkewers -> RecipeText(
            R.string.recipe_halloumi_skewers,
            R.array.recipe_halloumi_skewers_ingredients,
            R.array.recipe_halloumi_skewers_steps,
        )

        Recipe.JacketPotatoWithBeans -> RecipeText(
            R.string.recipe_jacket_potato_with_beans,
            R.array.recipe_jacket_potato_with_beans_ingredients,
            R.array.recipe_jacket_potato_with_beans_steps,
        )

        Recipe.BuckwheatSalad -> RecipeText(
            R.string.recipe_buckwheat_salad,
            R.array.recipe_buckwheat_salad_ingredients,
            R.array.recipe_buckwheat_salad_steps,
        )

        Recipe.FishCakesWithPotatoes -> RecipeText(
            R.string.recipe_fish_cakes_with_potatoes,
            R.array.recipe_fish_cakes_with_potatoes_ingredients,
            R.array.recipe_fish_cakes_with_potatoes_steps,
        )

        Recipe.SpinachRicottaLasagna -> RecipeText(
            R.string.recipe_spinach_ricotta_lasagna,
            R.array.recipe_spinach_ricotta_lasagna_ingredients,
            R.array.recipe_spinach_ricotta_lasagna_steps,
        )

        Recipe.BeefAndRootVegetableStew -> RecipeText(
            R.string.recipe_beef_and_root_vegetable_stew,
            R.array.recipe_beef_and_root_vegetable_stew_ingredients,
            R.array.recipe_beef_and_root_vegetable_stew_steps,
        )

        Recipe.MinuteSteakWithTomatoSalad -> RecipeText(
            R.string.recipe_minute_steak_with_tomato_salad,
            R.array.recipe_minute_steak_with_tomato_salad_ingredients,
            R.array.recipe_minute_steak_with_tomato_salad_steps,
        )

        Recipe.RiceNoodlesWithPrawns -> RecipeText(
            R.string.recipe_rice_noodles_with_prawns,
            R.array.recipe_rice_noodles_with_prawns_ingredients,
            R.array.recipe_rice_noodles_with_prawns_steps,
        )

        Recipe.ChanaMasala -> RecipeText(
            R.string.recipe_chana_masala,
            R.array.recipe_chana_masala_ingredients,
            R.array.recipe_chana_masala_steps,
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

        Recipe.BananaWithPeanutButter -> RecipeText(
            R.string.recipe_banana_with_peanut_butter,
            R.array.recipe_banana_with_peanut_butter_ingredients,
            R.array.recipe_banana_with_peanut_butter_steps,
        )

        Recipe.Edamame -> RecipeText(
            R.string.recipe_edamame,
            R.array.recipe_edamame_ingredients,
            R.array.recipe_edamame_steps,
        )

        Recipe.RoastedChickpeas -> RecipeText(
            R.string.recipe_roasted_chickpeas,
            R.array.recipe_roasted_chickpeas_ingredients,
            R.array.recipe_roasted_chickpeas_steps,
        )

        Recipe.CottageCheeseWithTomato -> RecipeText(
            R.string.recipe_cottage_cheese_with_tomato,
            R.array.recipe_cottage_cheese_with_tomato_ingredients,
            R.array.recipe_cottage_cheese_with_tomato_steps,
        )

        Recipe.BoiledEggWithCrispbread -> RecipeText(
            R.string.recipe_boiled_egg_with_crispbread,
            R.array.recipe_boiled_egg_with_crispbread_ingredients,
            R.array.recipe_boiled_egg_with_crispbread_steps,
        )

        Recipe.TunaRyeCrispbread -> RecipeText(
            R.string.recipe_tuna_rye_crispbread,
            R.array.recipe_tuna_rye_crispbread_ingredients,
            R.array.recipe_tuna_rye_crispbread_steps,
        )
    }
