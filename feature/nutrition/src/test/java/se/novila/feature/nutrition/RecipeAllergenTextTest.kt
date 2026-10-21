package se.novila.feature.nutrition

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import se.novila.recipes.Allergen
import se.novila.recipes.Recipe

/**
 * The allergens a recipe declares must cover what its English ingredients say, since a missed allergen means a recipe
 * is suggested to someone who must avoid it. The words are deliberately plain; a recipe may declare more than they
 * find (stock with celery, muesli with nuts), never less.
 */
@RunWith(RobolectricTestRunner::class)
class RecipeAllergenTextTest {
    private val words = mapOf(
        Allergen.Milk to Regex(
            "(?<!coconut )(?<!oat )(?<!soy )\\bmilk\\b|yogh?urt|cheese|(?<!peanut )\\bbutter\\b|cream|skyr|quark|" +
                "feta|ricotta|parmesan|cottage|halloumi",
        ),
        Allergen.Eggs to Regex("\\beggs?\\b"),
        Allergen.Fish to Regex("salmon|tuna|\\bcod\\b|mackerel|herring|\\bfish\\b"),
        Allergen.Crustaceans to Regex("prawn|shrimp|crab"),
        Allergen.Gluten to Regex(
            "(?<!buck)wheat|\\brye\\b|barley|spelt|bread|pasta|bulgur|couscous|soy sauce|flour|muesli|\\boats?\\b|" +
                "tortilla|lasagn",
        ),
        Allergen.Peanuts to Regex("peanut"),
        Allergen.Sesame to Regex("sesame|tahini|hummus"),
        Allergen.TreeNuts to Regex("almond|walnut|hazelnut|cashew|\\bnuts\\b"),
        Allergen.Soy to Regex("\\bsoy|tofu|edamame"),
        Allergen.Mustard to Regex("mustard"),
        Allergen.Celery to Regex("celery"),
    )

    @Test fun `every allergen named in the ingredients is declared`() {
        val resources = RuntimeEnvironment.getApplication().resources
        Recipe.entries.forEach { recipe ->
            val ingredients = resources.getStringArray(recipe.text.ingredients).joinToString("\n").lowercase()
            words.forEach { (allergen, pattern) ->
                val found = pattern.find(ingredients)?.value ?: return@forEach
                assertTrue(
                    "${recipe.name} contains \"$found\" but does not declare $allergen",
                    allergen in recipe.allergens,
                )
            }
        }
    }
}
