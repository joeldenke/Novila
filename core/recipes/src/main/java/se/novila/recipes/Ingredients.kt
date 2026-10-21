package se.novila.recipes

import se.novila.model.FoodGroup
import se.novila.model.MealLog
import se.novila.model.foodGroupOf
import se.novila.model.foodWords
import se.novila.model.isSweet
import se.novila.model.knownFoods

/** At most this many portions of one food group per serving, however many foods of the group a recipe has. */
const val MAX_PORTIONS_PER_GROUP = 2

/**
 * What the [ingredients] of a recipe give in one serving, looked up word by word in the same dictionary as meal
 * logging: each food once, at most [MAX_PORTIONS_PER_GROUP] per group, and sweets as treats. Water used for cooking is
 * not a drink, so it fills nothing. Amounts and units ("2 dl") are neither counted nor reported; any other word outside
 * the dictionary is listed in [MealLog.unknown], never guessed at.
 */
fun parseIngredients(ingredients: List<String>): MealLog {
    val portions = mutableMapOf<FoodGroup, Int>()
    var treats = 0
    val unknown = mutableListOf<String>()
    val counted = mutableSetOf<String>()
    for (word in ingredients.flatMap(::foodWords)) {
        if (!counted.add(word) || isAmount(word)) continue
        val group = foodGroupOf(word)
        when {
            group == FoodGroup.Water -> Unit
            group != null -> portions[group] = minOf((portions[group] ?: 0) + 1, MAX_PORTIONS_PER_GROUP)
            isSweet(word) -> treats++
            word !in ALLERGENS -> unknown += word
        }
    }
    return MealLog(portions, treats, unknown)
}

/**
 * The labelled allergens the [ingredients] contain as written: every word, and every two words in a row ("soy
 * sauce"), looked up in a dictionary of foods that contain them (wheat, milk, eggs, nuts …), in both of the app's
 * languages. Only whole words count, so nothing is guessed from part of a word. When in doubt the dictionary says
 * more rather than less ("coconut milk" is milk); the user confirms the result, and can add what a ready-made product
 * contains.
 */
fun ingredientAllergens(ingredients: List<String>): Set<Allergen> = ingredients
    .flatMap { line ->
        val words = foodWords(line)
        words + words.zipWithNext { a, b -> "$a $b" }
    }
    .flatMap { ALLERGENS[it].orEmpty() }
    .toSortedSet()

/**
 * Foods from the dictionary that complete the last word of [text], the shortest first, at most [limit]. Nothing is
 * offered before two letters, or once the word is already complete.
 */
fun completeIngredient(text: String, limit: Int = MAX_COMPLETIONS): List<String> {
    val prefix = lastWord(text).lowercase()
    if (prefix.length < MIN_PREFIX) return emptyList()
    return INGREDIENT_WORDS
        .filter { it.startsWith(prefix) && it != prefix }
        .sortedWith(compareBy<String> { it.length }.thenBy { it })
        .take(limit)
}

/** [text] with its last word replaced by [word], and a space after it, ready for the next word. */
fun completedIngredient(text: String, word: String): String = text.dropLast(lastWord(text).length) + word + " "

/** How many completions are offered at a time. */
const val MAX_COMPLETIONS = 5

private const val MIN_PREFIX = 2

private fun lastWord(text: String): String = text.takeLastWhile { it.isLetter() || it == '-' }

/** A number, a fraction or a unit: how much, not what. */
private fun isAmount(word: String): Boolean = word.any { it.isDigit() } || word.all { it in FRACTIONS } || word in UNITS

private const val FRACTIONS = "½⅓⅔¼¾-"

private val UNITS = setOf(
    "g", "gram", "kg", "mg", "hg", "l", "dl", "cl", "ml", "msk", "tsk", "krm", "st", "stycken", "tbsp", "tsp", "cup",
    "cups", "oz", "lb", "nypa", "pinch", "burk", "burkar", "can", "cans", "paket", "pack", "klyfta", "klyftor",
    "clove", "cloves", "skiva", "skivor", "slice", "slices", "näve", "nävar", "handful", "bit", "bitar", "piece",
    "pieces", "ca", "cirka", "about", "liten", "litet", "små", "small", "stor", "stort", "stora", "large", "medium",
    "efter", "smak", "taste", "hackad", "hackade", "chopped", "riven", "rivet", "grated", "kokt", "kokta", "boiled",
    "färsk", "färska", "fresh", "fryst", "frysta", "frozen",
)

/** Foods that contain each labelled allergen, by their common names in both of the app's languages. */
private val ALLERGENS: Map<String, Set<Allergen>> = sequenceOf(
    Allergen.Gluten to listOf(
        "vete", "vetemjöl", "wheat", "mjöl", "flour", "dinkel", "spelt", "råg", "rågmjöl", "rye", "korn", "barley",
        "havre", "havregryn", "havremjölk", "oat", "oats", "gröt", "porridge", "granola", "muesli", "müsli", "bulgur",
        "couscous", "pasta", "spaghetti", "makaroner", "macaroni", "lasagne", "nudlar", "noodles", "bröd", "bread",
        "knäcke", "knäckebröd", "crispbread", "tortilla", "tortillas", "wrap", "pitabröd", "pita", "ströbröd",
        "breadcrumbs", "seitan", "matvete", "mannagryn", "semolina", "sojasås", "soy sauce",
    ),
    Allergen.Crustaceans to listOf(
        "räka", "räkor", "shrimp", "shrimps", "prawn", "prawns", "kräfta", "kräftor", "crayfish", "hummer", "lobster",
        "krabba", "crab",
    ),
    Allergen.Eggs to listOf("ägg", "egg", "eggs", "äggula", "äggvita", "majonnäs", "mayonnaise", "mayo"),
    Allergen.Fish to listOf(
        "fisk", "fish", "lax", "salmon", "torsk", "cod", "sej", "pollock", "sill", "herring", "makrill", "mackerel",
        "tonfisk", "tuna", "sardiner", "sardines", "ansjovis", "anchovies", "kaviar", "rödspätta", "plaice",
        "fiskbullar", "fiskpinnar",
    ),
    Allergen.Peanuts to listOf("jordnöt", "jordnötter", "jordnötssmör", "peanut", "peanuts", "peanut butter"),
    Allergen.Soy to listOf("soja", "sojasås", "soy", "tofu", "tempeh", "edamame", "sojabönor", "miso"),
    Allergen.Milk to listOf(
        "mjölk", "milk", "ost", "cheese", "smör", "butter", "grädde", "cream", "matlagningsgrädde", "yoghurt", "yogurt",
        "kvarg", "quark", "keso", "skyr", "fil", "filmjölk", "crème", "creme", "fraiche", "feta", "fetaost", "halloumi",
        "mozzarella", "parmesan", "ricotta", "mascarpone", "cheddar", "vassle", "whey",
    ),
    Allergen.TreeNuts to listOf(
        "nöt", "nötter", "nuts", "mandel", "mandlar", "almond", "almonds", "valnöt", "valnötter", "walnut", "walnuts",
        "hasselnöt", "hasselnötter", "hazelnut", "hazelnuts", "cashew", "cashewnötter", "cashews", "pistage",
        "pistachio", "pistachios", "pekannöt", "pekannötter", "pecan", "pecans", "paranöt", "paranötter", "macadamia",
    ),
    Allergen.Celery to listOf("selleri", "rotselleri", "blekselleri", "celery", "celeriac"),
    Allergen.Mustard to listOf("senap", "mustard"),
    Allergen.Sesame to listOf("sesam", "sesamfrö", "sesamfrön", "sesame", "tahini", "hummus", "humus"),
    Allergen.Sulphites to listOf("vin", "wine", "russin", "raisins"),
    Allergen.Lupin to listOf("lupin", "lupinmjöl"),
    Allergen.Molluscs to listOf(
        "mussla", "musslor", "blåmusslor", "mussels", "ostron", "oysters", "bläckfisk", "squid", "calamari",
        "octopus", "pilgrimsmusslor", "scallops",
    ),
).flatMap { (allergen, names) -> names.map { it to allergen } }
    .groupBy({ it.first }, { it.second })
    .mapValues { it.value.toSet() }

/** Every word the editor can complete: the meal dictionary's foods and the allergen dictionary's. */
private val INGREDIENT_WORDS: Set<String> = knownFoods + ALLERGENS.keys.filterNot { ' ' in it }
