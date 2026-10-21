package se.novila.model

/**
 * What a free-text meal translates into: one portion per recognized word ([portions] per group, [treats] for sweets),
 * and the words that were not recognized ([unknown]), listed rather than guessed at.
 */
data class MealLog(
    val portions: Map<FoodGroup, Int> = emptyMap(),
    val treats: Int = 0,
    val unknown: List<String> = emptyList(),
) {
    /** Nothing in the text filled in a group, so logging it would change nothing. */
    val isEmpty: Boolean get() = portions.isEmpty() && treats == 0
}

/**
 * One meal written in the user's own words, "Breakfast: porridge, berries", as the food groups it fills in: the meal
 * name before the colon counts as one [FoodGroup.Meals], and each recognized food as one portion of its group. Sweets
 * become treats. Both of the app's languages are understood, the same food written twice is one portion, and a word
 * outside the dictionary is reported in [MealLog.unknown] so the user can see what was left out and add it with the
 * steppers instead.
 */
fun parseMeal(text: String): MealLog {
    val named = text.indexOf(':').takeIf { it >= 0 && text.substring(0, it).trim().lowercase() in MEAL_NAMES }
    val foods = if (named == null) text else text.substring(named + 1)
    val portions = mutableMapOf<FoodGroup, Int>()
    if (named != null) portions[FoodGroup.Meals] = 1
    var treats = 0
    val unknown = mutableListOf<String>()
    val counted = mutableSetOf<String>()
    for (word in foodWords(foods)) {
        if (!counted.add(word)) continue
        val group = foodGroupOf(word)
        when {
            group != null -> portions[group] = (portions[group] ?: 0) + 1
            isSweet(word) -> treats++
            else -> unknown += word
        }
    }
    return MealLog(portions, treats, unknown)
}

/** Adds a parsed [meal] to the day: its portions on top of what is already logged, its sweets on top of the treats. */
fun DayEntry.withMeal(meal: MealLog): DayEntry = meal.portions.entries
    .fold(this) { day, (group, portions) -> day.withPortions(group, portions) }
    .withTreats(meal.treats)

/** The food group [word] (lowercased, as [foodWords] gives it) belongs to in the dictionary, or null. */
fun foodGroupOf(word: String): FoodGroup? = FOODS[word]

/** Whether [word] (lowercased) is a sweet or a snack, which counts as a treat. */
fun isSweet(word: String): Boolean = word in SWEETS

/** Every food and sweet the dictionary knows, in both of the app's languages. */
val knownFoods: Set<String> get() = FOODS.keys + SWEETS

/** The words of a meal, lowercased and split on whitespace and punctuation, with "and"/"och" dropped as filler. */
fun foodWords(text: String): List<String> = text
    .lowercase()
    .split(SEPARATOR)
    .map { it.trim(*WORD_EDGE) }
    .filter { it.isNotEmpty() && it !in FILLER }

private val SEPARATOR = Regex("""[\s,;:/|·]+""")
private val WORD_EDGE = ".,;:!?\"'()…".toCharArray()

/** A meal's name, the word before the colon: it counts as a portion of [FoodGroup.Meals]. */
private val MEAL_NAMES = setOf(
    "frukost", "lunch", "middag", "mellanmål", "mellanmal", "kvällsmat", "kvallsmat", "nattmat", "efterrätt",
    "efterrett", "brunch", "breakfast", "dinner", "supper", "snack", "snacks",
)

/** Words that only glue a meal together: they neither count nor are reported. */
private val FILLER = setOf(
    "med", "with", "samt", "eller", "or", "plus", "på", "on", "till", "to", "i", "in", "en", "a",
)

/** Foods by their common names in the app's two languages, counted once each per meal. */
private val FOODS: Map<String, FoodGroup> = sequenceOf(
    FoodGroup.Vegetables to listOf(
        "avokado", "äpple", "äpplen", "apple", "apples", "banan", "banana", "bananas", "björnbär", "blåbär",
        "blueberry", "blueberries", "blomkål", "blomkal", "broccoli", "bär", "berries", "berry", "carrot",
        "carrots", "cauliflower", "clementin", "frukt", "fruit", "gurka", "grönsak", "grönsaker", "vegetable",
        "vegetables", "hallon", "jordgubbar", "jordgubbe", "kiwi", "kålrot", "lingon", "mandarin", "melon",
        "morot", "morötter", "paprika", "päron", "pear", "potatis", "potatoes", "radish", "rädisa", "rödbeta",
        "salad", "sallad", "selleri", "sparris", "spenat", "spinach", "strawberry", "strawberries", "svamp",
        "tomat", "tomater", "vattenmelon", "zucchini",
    ),
    FoodGroup.Protein to listOf(
        "biff", "baljväxt", "baljväxter", "beef", "böna", "bönor", "beans", "chicken", "chickpea", "chickpeas",
        "egg", "eggs", "fisk", "fish", "fläsk", "herring", "humus", "hummus", "kalv", "kalkon", "kyckling",
        "kött", "lamb", "lamm", "lax", "lentil", "lentils", "linser", "makrill", "mackerel", "meat", "nötkött",
        "pork", "prawns", "quorn", "räkor", "salmon", "sardiner", "sardines", "seitan", "sill", "shrimp",
        "tempeh", "tofu", "tonfisk", "tuna", "turkey", "veal", "ägg",
    ),
    FoodGroup.WholeGrain to listOf(
        "barley", "bread", "bröd", "bulgur", "crispbread", "gryn", "grain", "granola", "gröt", "havregryn",
        "knäcke", "knäckebröd", "korn", "muesli", "müsli", "noodles", "nudlar", "oat", "oats", "pasta",
        "porridge", "råg", "rice", "ris", "rye", "spaghetti", "wrap",
    ),
    FoodGroup.Water to listOf("kranvatten", "mineralvatten", "vatten", "water"),
).flatMap { (group, names) -> names.map { it to group } }.toMap()

/** Sweets and snacks: one a day is free, each one after that costs points. */
private val SWEETS = setOf(
    "brownie", "brownies", "bulle", "bullar", "cake", "candy", "cheesecake", "chips", "choklad", "chocolate", "cola",
    "cookie", "cookies", "croissant", "croissants", "donut", "donuts", "glass", "godis", "icecream", "kaka", "kakor",
    "kex", "läsk", "munk", "pastry", "pastries", "soda", "sweets", "tårta", "wienerbröd",
)
