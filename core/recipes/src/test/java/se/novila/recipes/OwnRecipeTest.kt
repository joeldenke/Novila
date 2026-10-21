package se.novila.recipes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import se.novila.model.DayEntry
import se.novila.model.FoodGroup
import se.novila.model.FoodLog
import se.novila.model.QUICK_PICK_DAYS
import se.novila.model.nutritionScore
import java.time.LocalDate

class OwnRecipeTest {
    private val date = LocalDate.of(2026, 10, 18)
    private val empty = DayEntry(date)
    private val soup = OwnRecipe(
        name = "Linssoppa",
        ingredients = listOf("2 dl röda linser", "1 morot", "1 burk tomater", "5 dl vatten", "1 tsk spiskummin"),
        steps = listOf("Koka allt i 20 minuter.", "Mixa."),
    )

    @Test fun `the ingredients are counted in the meal dictionary, amounts and cooking water left out`() {
        val contents = parseIngredients(soup.ingredients)
        assertEquals(mapOf(FoodGroup.Protein to 1, FoodGroup.Vegetables to 2), contents.portions)
        assertEquals(0, contents.treats)
        assertEquals(listOf("röda", "spiskummin"), contents.unknown)
    }

    @Test fun `one serving gives at most two portions of a group, and sweets are treats`() {
        val contents = parseIngredients(listOf("broccoli", "morot, paprika", "tomat", "broccoli", "choklad"))
        assertEquals(MAX_PORTIONS_PER_GROUP, contents.portions[FoodGroup.Vegetables])
        assertEquals(1, contents.treats)
    }

    @Test fun `allergens are looked up from whole words in both languages, never from part of a word`() {
        assertEquals(
            setOf(Allergen.Gluten, Allergen.Milk, Allergen.Eggs),
            ingredientAllergens(listOf("3 dl vetemjöl", "2 dl mjölk", "1 ägg")),
        )
        assertEquals(setOf(Allergen.Gluten), ingredientAllergens(listOf("havremjölk")))
        assertEquals(setOf(Allergen.Gluten, Allergen.Soy), ingredientAllergens(listOf("2 tbsp soy sauce")))
        assertEquals(setOf(Allergen.Crustaceans, Allergen.Sesame), ingredientAllergens(listOf("prawns, tahini")))
        assertEquals(emptySet<Allergen>(), ingredientAllergens(soup.ingredients))
        // Allergen words are known foods, not reported as unknown.
        assertEquals(emptyList<String>(), parseIngredients(listOf("2 dl mjölk", "smör")).unknown)
    }

    @Test fun `the last word is completed from the dictionary, shortest first`() {
        assertEquals(listOf("lax"), completeIngredient("200 g la").filter { it == "lax" })
        assertTrue(completeIngredient("1 dl havreg").contains("havregryn"))
        assertTrue(completeIngredient("mj").contains("mjölk"))
        val words = completeIngredient("b")
        assertEquals(emptyList<String>(), words)
        assertEquals(emptyList<String>(), completeIngredient("lax"))
        val broc = completeIngredient("bro")
        assertEquals(broc.sortedWith(compareBy<String> { it.length }.thenBy { it }), broc)
        assertTrue(broc.size <= MAX_COMPLETIONS)
        assertEquals("200 g lax ", completedIngredient("200 g la", "lax"))
    }

    @Test fun `logging an own recipe adds what its ingredients give and a meal unless it is a snack`() {
        assertEquals(FoodLog(vegetables = 2, protein = 1, meals = 1), empty.withRecipe(soup).food)
        val snack = soup.copy(slot = MealSlot.Snack)
        assertEquals(0, empty.withRecipe(snack).food.meals)
    }

    @Test fun `the book saves new recipes with new ids, replaces edited ones and removes them with their servings`() {
        val book = RecipeBook().saved(soup).saved(soup.copy(name = "Gröt", ingredients = listOf("havregryn")))
        assertEquals(listOf(1, 2), book.recipes.map { it.id })
        val edited = book.saved(book.recipe(1)!!.copy(name = "  Röd linssoppa \n"))
        assertEquals("Röd linssoppa", edited.recipe(1)?.name)
        assertEquals(2, edited.recipes.size)
        val cooked = edited.withCooked(edited.recipe(1)!!.key, date)
        val removed = cooked.removed(1)
        assertNull(removed.recipe(1))
        assertTrue(removed.cooked.isEmpty())
        assertEquals(book, book.saved(OwnRecipe(name = " ", ingredients = listOf("lax"))))
        assertEquals(book, book.saved(OwnRecipe(name = "Inget", ingredients = listOf("  "))))
    }

    @Test fun `a full book takes no new recipe but can still edit one`() {
        val full = (1..MAX_OWN_RECIPES).fold(RecipeBook()) { book, n -> book.saved(soup.copy(name = "Soppa $n")) }
        assertTrue(full.isFull)
        assertEquals(full, full.saved(soup))
        assertEquals("Ny", full.saved(full.recipes.first().copy(name = "Ny")).recipes.first().name)
    }

    @Test fun `cooked servings are counted per day over the last month`() {
        val book = RecipeBook()
            .withCooked("own:1", date.minusDays(QUICK_PICK_DAYS.toLong()))
            .withCooked("own:1", date.minusDays(3))
            .withCooked("own:1", date.minusDays(3))
            .withCooked("own:1", date)
        assertEquals(2, book.cookedDays("own:1", date))
        assertEquals(0, book.cookedDays(Recipe.BakedSalmon.key, date))
    }

    @Test fun `own recipes are suggested for the slot, never with an avoided allergen or over the time limit`() {
        val book = RecipeBook()
            .saved(soup)
            .saved(soup.copy(name = "Pasta", ingredients = listOf("pasta", "lax"), allergens = setOf(Allergen.Gluten)))
            .saved(soup.copy(name = "Gröt", slot = MealSlot.Breakfast, ingredients = listOf("havregryn")))
            .saved(soup.copy(name = "Gryta", minutes = 60, ingredients = listOf("bönor")))
        val all = suggestRecipes(empty, hour = 12, preferences = FoodPreferences(), book = book).own
        assertEquals(setOf("Pasta", "Linssoppa"), all.map { it.recipe.name }.toSet())
        assertTrue(all.zipWithNext().all { (a, b) -> a.points >= b.points })
        val noGluten = FoodPreferences(allergens = setOf(Allergen.Gluten), maxMinutes = 30)
        val allowed = suggestRecipes(empty, hour = 12, preferences = noGluten, book = book).own
        assertEquals(listOf("Linssoppa"), allowed.map { it.recipe.name })
        all.forEach {
            val after = empty.withRecipe(it.recipe).nutritionScore()?.total ?: 0
            assertEquals(after - (empty.nutritionScore()?.total ?: 0), it.points)
        }
    }

    @Test fun `what the user cooks goes first among recipes worth as much`() {
        val preferences = FoodPreferences()
        val first = suggestRecipes(empty, 12, preferences).recipes
        val tied = first.filter { it.points == first.first().points }
        assertTrue(tied.size >= 2)
        val favorite = tied.last().recipe
        val book = RecipeBook().withCooked(favorite.key, date.minusDays(1))
        assertEquals(favorite, suggestRecipes(empty, 12, preferences, book = book).recipes.first().recipe)
        val twin = soup.copy(name = "Tvilling")
        val twins = RecipeBook().saved(soup).saved(twin)
        val cookedTwin = twins.withCooked(twins.recipes.last().key, date)
        assertEquals("Linssoppa", suggestRecipes(empty, 12, preferences, book = twins).own.first().recipe.name)
        assertEquals("Tvilling", suggestRecipes(empty, 12, preferences, book = cookedTwin).own.first().recipe.name)
    }

    @Test fun `the book survives encoding, with any character in its text`() {
        val tricky = soup.copy(
            name = "Soppa | 100% ; god",
            steps = listOf("Koka; rör om | smaka av 50%", "Servera"),
            allergens = setOf(Allergen.Celery, Allergen.Milk),
        )
        val book = RecipeBook().saved(tricky).saved(soup).withCooked("own:1", date).withCooked("BakedSalmon", date)
        assertEquals(book, RecipeBookCodec.decode(RecipeBookCodec.encode(book)))
        assertEquals(RecipeBook(), RecipeBookCodec.decode(null))
        assertEquals(RecipeBook(), RecipeBookCodec.decode("x9\nR|1|Main|20||A|lax|"))
        assertFalse(RecipeBookCodec.decode("b1\nR|broken").recipes.any())
    }

    @Test fun `a draft brings the allergens of new ingredients, and changed allergens need a new check`() {
        val draft = OwnRecipeDraft().edited { it.copy(name = "Pannkakor") }.withIngredient("3 dl vetemjöl")
        assertEquals(setOf(Allergen.Gluten), draft.allergens)
        assertFalse(draft.canSave)
        val checked = draft.copy(checked = true)
        assertTrue(checked.canSave)
        val withMilk = checked.withIngredient("6 dl mjölk")
        assertEquals(setOf(Allergen.Gluten, Allergen.Milk), withMilk.allergens)
        assertFalse(withMilk.checked)
        // A line without new allergens keeps the check.
        assertTrue(checked.withIngredient("1 nypa salt").checked)
        val glutenFree = withMilk.toggled(Allergen.Gluten).toggled(Allergen.Mustard)
        assertEquals(setOf(Allergen.Milk, Allergen.Mustard), glutenFree.allergens)
        assertEquals(setOf(Allergen.Milk, Allergen.Mustard), glutenFree.toRecipe().allergens)
        val undone = glutenFree.toggled(Allergen.Gluten).toggled(Allergen.Mustard)
        assertEquals(setOf(Allergen.Gluten, Allergen.Milk), undone.allergens)
        assertEquals(listOf("6 dl mjölk"), glutenFree.withoutIngredient(0).recipe.ingredients)
        assertEquals(draft, draft.withIngredient("   "))
        assertEquals(listOf("Vispa.", "Stek."), draft.withStep("Vispa.").withStep("Stek.").recipe.steps)
    }

    @Test fun `a saved recipe opens as a checked draft with the allergens as confirmed`() {
        val saved = soup.copy(ingredients = listOf("pasta", "lax"), allergens = setOf(Allergen.Fish, Allergen.Milk))
        val draft = OwnRecipeDraft.of(saved)
        assertTrue(draft.checked)
        assertEquals(saved.allergens, draft.allergens)
        assertEquals(saved.tidy(), draft.toRecipe())
    }
}
