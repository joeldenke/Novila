package se.novila.recipes

/**
 * What the user eats, from most to least: each diet also eats everything the ones after it do. A pescetarian eats
 * vegetarian food, and everyone eats vegan food, so a recipe fits a diet when its own diet is the same or later.
 */
enum class Diet { Everything, Pescetarian, Vegetarian, Vegan }

/** The 14 allergens EU law requires food to be labelled with (Regulation (EU) No 1169/2011, Annex II). */
enum class Allergen {
    Gluten,
    Crustaceans,
    Eggs,
    Fish,
    Peanuts,
    Soy,
    Milk,
    TreeNuts,
    Celery,
    Mustard,
    Sesame,
    Sulphites,
    Lupin,
    Molluscs,
}

/**
 * What recipe suggestions must take into account: the user's [diet], [allergens] they avoid and at most how many
 * minutes a recipe may take ([maxMinutes], null for any time).
 */
data class FoodPreferences(
    val diet: Diet = Diet.Everything,
    val allergens: Set<Allergen> = emptySet(),
    val maxMinutes: Int? = null,
) {
    fun toggle(allergen: Allergen) =
        copy(allergens = if (allergen in allergens) allergens - allergen else allergens + allergen)

    /** [minutes] if it is one of [MINUTES], otherwise any time. */
    fun withMaxMinutes(minutes: Int?) = copy(maxMinutes = minutes?.takeIf { it in MINUTES })

    /** Whether [recipe] can be suggested: the diet fits, no allergen the user avoids and quick enough. */
    fun allows(recipe: Recipe): Boolean = recipe.diet >= diet &&
        recipe.allergens.none { it in allergens } &&
        (maxMinutes == null || recipe.minutes <= maxMinutes)

    companion object {
        /** The time limits the user can choose from, besides any time. */
        val MINUTES = listOf(15, 30)
    }
}

/** [FoodPreferences] as one line of text, versioned so that the format can change. */
object FoodPreferencesCodec {
    private const val VERSION = "f1"
    private const val FIELDS = 4

    fun encode(preferences: FoodPreferences): String = with(preferences) {
        listOf(VERSION, diet.name, allergens.sorted().joinToString(",") { it.name }, maxMinutes ?: "")
            .joinToString(";")
    }

    /** The preferences in [raw], or null for nothing saved or a format this version does not know. */
    fun decode(raw: String?): FoodPreferences? {
        val parts = raw?.split(';') ?: return null
        if (parts[0] != VERSION || parts.size != FIELDS) return null
        val diet = Diet.entries.firstOrNull { it.name == parts[1] } ?: return null
        val allergens = parts[2].split(',').mapNotNull { name -> Allergen.entries.firstOrNull { it.name == name } }
        return FoodPreferences(diet, allergens.toSet()).withMaxMinutes(parts[3].toIntOrNull())
    }
}
