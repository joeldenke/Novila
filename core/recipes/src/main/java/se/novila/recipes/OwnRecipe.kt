package se.novila.recipes

import se.novila.model.DayEntry
import se.novila.model.FoodGroup
import se.novila.model.MealLog
import se.novila.model.QUICK_PICK_DAYS
import se.novila.model.withMeal
import se.novila.model.withPortions
import java.time.LocalDate

/**
 * A recipe the user wrote themselves: its [name], when it is eaten ([slot]), how many [minutes] it takes, one
 * ingredient per line ([ingredients]) and one step per line ([steps]), for one serving. What a serving gives is
 * counted from the ingredients ([contents], [parseIngredients]), never entered or guessed. The [allergens] are the
 * ones the user confirmed when saving, starting from those found in the ingredients ([ingredientAllergens]). [id] is
 * 0 until the recipe is saved in a [RecipeBook].
 */
data class OwnRecipe(
    val name: String,
    val slot: MealSlot = MealSlot.Main,
    val minutes: Int = DEFAULT_MINUTES,
    val ingredients: List<String> = emptyList(),
    val steps: List<String> = emptyList(),
    val allergens: Set<Allergen> = emptySet(),
    val id: Int = 0,
) {
    /** What one serving gives, counted from the ingredients. */
    val contents: MealLog by lazy { parseIngredients(ingredients) }

    /** Portions of [group] in one serving. A breakfast or main meal is one of the day's meals. */
    fun portions(group: FoodGroup): Int = when (group) {
        FoodGroup.Meals -> if (slot.isMeal) 1 else 0
        else -> contents.portions[group] ?: 0
    }

    /** The key it is remembered by when cooked, apart from Novila's own recipes. */
    val key: String get() = "$OWN_PREFIX$id"

    /** Whether it can be saved: it has a name and at least one ingredient. */
    val isComplete: Boolean get() = name.isNotBlank() && ingredients.isNotEmpty()

    /** The recipe as it is saved: trimmed lines, no blank ones, and within the limits. */
    fun tidy(): OwnRecipe = copy(
        name = line(name, MAX_NAME_LENGTH),
        minutes = minutes.coerceIn(1, MAX_MINUTES),
        ingredients = ingredients.map { line(it, MAX_LINE_LENGTH) }.filter { it.isNotEmpty() }.take(MAX_LINES),
        steps = steps.map { line(it, MAX_STEP_LENGTH) }.filter { it.isNotEmpty() }.take(MAX_LINES),
    )

    companion object {
        const val DEFAULT_MINUTES = 20
        const val MAX_MINUTES = 240
        const val MAX_NAME_LENGTH = 60
        const val MAX_LINE_LENGTH = 80
        const val MAX_STEP_LENGTH = 300
        const val MAX_LINES = 30
    }
}

/**
 * A recipe being written: the [recipe] so far, and the allergens the user [added] to or [removed] from the ones found
 * in its ingredients ([found]). New ingredients bring their allergens with them, so a found allergen is only left out
 * when the user takes it away. The recipe can be saved once it is complete and the user has [checked] its allergens;
 * any change to them asks for a new check.
 */
data class OwnRecipeDraft(
    val recipe: OwnRecipe = OwnRecipe(name = ""),
    val added: Set<Allergen> = emptySet(),
    val removed: Set<Allergen> = emptySet(),
    val checked: Boolean = false,
) {
    /** The allergens found in the ingredients as written. */
    val found: Set<Allergen> get() = ingredientAllergens(recipe.ingredients)

    /** The allergens the recipe will be saved with. */
    val allergens: Set<Allergen> get() = (found - removed + added).toSortedSet()

    /** Whether it can be saved: it has a name and an ingredient, and the allergens are checked. */
    val canSave: Boolean get() = recipe.tidy().isComplete && checked

    /** The recipe as it is saved, with the confirmed [allergens]. */
    fun toRecipe(): OwnRecipe = recipe.copy(allergens = allergens).tidy()

    /** With [change] made to the recipe; when that changes the allergens, they need checking again. */
    fun edited(change: (OwnRecipe) -> OwnRecipe): OwnRecipeDraft = rechecked(copy(recipe = change(recipe)))

    /** With [line] added as the last ingredient, unless it is blank or the list is full. */
    fun withIngredient(line: String): OwnRecipeDraft = edited { it.copy(ingredients = it.ingredients.plusLine(line)) }

    fun withoutIngredient(index: Int): OwnRecipeDraft = edited { it.copy(ingredients = it.ingredients.minusAt(index)) }

    /** With [line] added as the last step, unless it is blank or the list is full. */
    fun withStep(line: String): OwnRecipeDraft = edited { it.copy(steps = it.steps.plusLine(line)) }

    fun withoutStep(index: Int): OwnRecipeDraft = edited { it.copy(steps = it.steps.minusAt(index)) }

    /** With [allergen] taken away when the recipe has it, or added when it does not. */
    fun toggled(allergen: Allergen): OwnRecipeDraft = rechecked(
        if (allergen in allergens) {
            copy(added = added - allergen, removed = removed + allergen)
        } else {
            copy(added = added + allergen, removed = removed - allergen)
        },
    )

    private fun rechecked(next: OwnRecipeDraft) = if (next.allergens == allergens) next else next.copy(checked = false)

    private fun List<String>.plusLine(line: String): List<String> {
        val tidy = line.replace('\n', ' ').trim()
        return if (tidy.isEmpty() || size >= OwnRecipe.MAX_LINES) this else this + tidy
    }

    private fun List<String>.minusAt(index: Int): List<String> = filterIndexed { i, _ -> i != index }

    companion object {
        /** A draft of a saved [recipe], its allergens as the user confirmed them. */
        fun of(recipe: OwnRecipe): OwnRecipeDraft {
            val found = ingredientAllergens(recipe.ingredients)
            return OwnRecipeDraft(recipe, recipe.allergens - found, found - recipe.allergens, checked = true)
        }
    }
}

/** The key a recipe from Novila's library is remembered by when cooked. */
val Recipe.key: String get() = name

/** Whether [recipe] can be suggested: no allergen the user avoids and quick enough. The user wrote it, so any diet. */
fun FoodPreferences.allows(recipe: OwnRecipe): Boolean =
    recipe.allergens.none { it in allergens } && (maxMinutes == null || recipe.minutes <= maxMinutes)

/** The day with one serving of [recipe] logged: its portions and treats, and a meal unless it is a snack. */
fun DayEntry.withRecipe(recipe: OwnRecipe): DayEntry =
    withMeal(recipe.contents).withPortions(FoodGroup.Meals, recipe.portions(FoodGroup.Meals))

/** One serving of the recipe with [key] ([Recipe.key] or [OwnRecipe.key]) logged on [date]. */
data class CookedRecipe(val key: String, val date: LocalDate)

/**
 * The user's own [recipes] (at most [MAX_OWN_RECIPES], in the order they were written) and what they have [cooked]
 * lately, from both their own recipes and Novila's, which steers the suggestions like the quick picks.
 */
data class RecipeBook(val recipes: List<OwnRecipe> = emptyList(), val cooked: List<CookedRecipe> = emptyList()) {
    /** The recipe with [id], or null. */
    fun recipe(id: Int): OwnRecipe? = recipes.firstOrNull { it.id == id }

    /** Whether another recipe can be written. */
    val isFull: Boolean get() = recipes.size >= MAX_OWN_RECIPES

    /**
     * Saves [recipe] tidied: in its place when it has an id, otherwise last with a new id. An incomplete recipe, or a
     * new one when the book is full, changes nothing.
     */
    fun saved(recipe: OwnRecipe): RecipeBook {
        val tidy = recipe.tidy()
        if (!tidy.isComplete) return this
        return if (recipe(tidy.id) != null) {
            copy(recipes = recipes.map { if (it.id == tidy.id) tidy else it })
        } else if (isFull) {
            this
        } else {
            copy(recipes = recipes + tidy.copy(id = (recipes.maxOfOrNull { it.id } ?: 0) + 1))
        }
    }

    /** Without the recipe with [id], and without its servings in [cooked]. */
    fun removed(id: Int): RecipeBook {
        val key = recipe(id)?.key ?: return this
        return copy(recipes = recipes.filterNot { it.id == id }, cooked = cooked.filterNot { it.key == key })
    }

    /** Remembers one serving of [key] on [date], forgetting servings older than [QUICK_PICK_DAYS] before it. */
    fun withCooked(key: String, date: LocalDate): RecipeBook {
        val since = date.minusDays(QUICK_PICK_DAYS.toLong())
        return copy(cooked = (cooked.filter { it.date > since } + CookedRecipe(key, date)).takeLast(MAX_COOKED))
    }

    /** On how many of the last [QUICK_PICK_DAYS] days up to [today] the recipe with [key] was cooked. */
    fun cookedDays(key: String, today: LocalDate): Int {
        val since = today.minusDays(QUICK_PICK_DAYS.toLong())
        return cooked.filter { it.key == key && it.date > since && it.date <= today }.distinctBy { it.date }.size
    }
}

/** How many recipes the user can write. */
const val MAX_OWN_RECIPES = 50

/** How many cooked servings are remembered, newest kept. */
const val MAX_COOKED = 60

private const val OWN_PREFIX = "own:"

private fun line(text: String, max: Int): String =
    text.replace('\r', ' ').replace('\n', ' ').trim().take(max).trim()

/**
 * The [RecipeBook] as text: "b1" on the first line, then "R|id|slot|minutes|allergens|name|ingredients|steps" per
 * recipe (lists joined by ";") and "C|date|key" per cooked serving. Text fields are escaped, so a name or step may
 * hold any character. Unreadable lines are skipped, and anything not starting with a known version decodes to an
 * empty book.
 */
object RecipeBookCodec {
    private const val VERSION = "b1"
    private const val RECIPE_FIELDS = 8

    fun encode(book: RecipeBook): String {
        val recipes = book.recipes.map { recipe ->
            listOf(
                "R",
                recipe.id.toString(),
                recipe.slot.name,
                recipe.minutes.toString(),
                recipe.allergens.sorted().joinToString(",") { it.name },
                escape(recipe.name),
                recipe.ingredients.joinToString(";", transform = ::escape),
                recipe.steps.joinToString(";", transform = ::escape),
            ).joinToString("|")
        }
        val cooked = book.cooked.map { "C|${it.date}|${escape(it.key)}" }
        return (listOf(VERSION) + recipes + cooked).joinToString("\n")
    }

    fun decode(raw: String?): RecipeBook {
        val lines = raw?.split('\n') ?: return RecipeBook()
        if (lines.first() != VERSION) return RecipeBook()
        val recipes = lines.drop(1).filter { it.startsWith("R|") }.mapNotNull(::recipe).distinctBy { it.id }
        val cooked = lines.drop(1).filter { it.startsWith("C|") }.mapNotNull { line ->
            val fields = line.split('|')
            val date = fields.getOrNull(1)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            val key = fields.getOrNull(2)?.let(::unescape)
            if (date != null && !key.isNullOrEmpty()) CookedRecipe(key, date) else null
        }
        return RecipeBook(recipes.take(MAX_OWN_RECIPES), cooked.takeLast(MAX_COOKED))
    }

    private fun recipe(line: String): OwnRecipe? {
        val fields = line.split('|')
        if (fields.size != RECIPE_FIELDS) return null
        val id = fields[1].toIntOrNull()?.takeIf { it > 0 } ?: return null
        val slot = MealSlot.entries.firstOrNull { it.name == fields[2] } ?: return null
        val minutes = fields[3].toIntOrNull() ?: return null
        val allergens = fields[4].split(',').mapNotNull { name -> Allergen.entries.firstOrNull { it.name == name } }
        val recipe = OwnRecipe(
            name = unescape(fields[5]),
            slot = slot,
            minutes = minutes,
            ingredients = list(fields[6]),
            steps = list(fields[7]),
            allergens = allergens.toSet(),
            id = id,
        ).tidy()
        return recipe.takeIf { it.isComplete }
    }

    private fun list(field: String): List<String> =
        if (field.isEmpty()) emptyList() else field.split(';').map(::unescape)

    private fun escape(text: String): String =
        text.replace("%", "%25").replace("|", "%7C").replace(";", "%3B").replace("\n", "%0A")

    private fun unescape(text: String): String =
        text.replace("%0A", "\n").replace("%3B", ";").replace("%7C", "|").replace("%25", "%")
}
