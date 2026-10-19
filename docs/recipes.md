# Recipes

Novila's recipe suggestions are a curated, on-device library in `:core:recipes`. They are never generated and never
come from Gemini Nano or a cloud service: a generated recipe could contain something the user is allergic to.

The library is one source of truth, the `Recipe` enum in
`core/recipes/src/main/java/se/novila/recipes/Recipe.kt`. The Nutrition tab only reads it; there is no per-screen
recipe logic.

## Schema

Every `Recipe` entry describes one serving and has these fields:

| Field | Meaning |
|---|---|
| enum name | Stable id. It names the string resources (`recipe_overnight_oats`) and is the key in `RecipeText.kt`. |
| `slot` | `MealSlot.Breakfast`, `Main` or `Snack`. Breakfast and main meals also give one `Meals` portion; a snack does not. |
| `diet` | The least strict diet that may eat it. `Diet` runs `Everything`, `Pescetarian`, `Vegetarian`, `Vegan`; a recipe fits a user when `recipe.diet >= user.diet`, so a vegan recipe fits everyone. |
| `minutes` | Preparation and cooking time. Used for the user's time limit and for `RecipeTime`: `Quick` (< 15), `Medium` (15–30), `Long` (> 30). |
| `vegetables` | Portions of vegetables and fruit in one serving, about 100 g each. Potatoes do not count. |
| `protein` | Portions of fish, meat, poultry, eggs, beans or lentils. |
| `wholeGrain` | Portions of whole grain bread, pasta, rice, oats or similar. |
| `allergens` | The 14 labelled allergens (EU 1169/2011) the ingredients contain as written. |
| `kinds` | Ingredient families: vegetables, fruit, fish/seafood, meat, poultry, dairy, eggs, grains, potatoes, legumes, nuts/seeds and fats. The score does not count these separately; they describe the library and are validated against diet and allergens. |

`Recipe.portions(group)` turns the three portion counts into the nutrition score's `FoodGroup`s and adds `Meals` from
`slot`. `DayEntry.withRecipe(recipe)` logs one serving.

Text is not stored in `:core:recipes` because that module has no Android resources. Each recipe has three resources in
`feature/nutrition/src/main/res/values/strings.xml` (English) and `values-sv/strings.xml` (Swedish):

- `recipe_<id>` – the title
- `recipe_<id>_ingredients` – a string array for one serving
- `recipe_<id>_steps` – a string array of steps

The card shows the title, a generated summary (time, points, food groups, allergens) and the ingredient and step
arrays. There is no separate free-text description: the ingredients, steps and generated summary already describe the
dish, and one fewer translatable field is one fewer place to drift.

## Selection

`suggestRecipes(day, hour, preferences, goals)` is deterministic:

1. Pick the slot: breakfast before 10 while no meal is logged, a snack once all meals are logged, otherwise a main
   meal.
2. Keep recipes whose `slot` matches and that `FoodPreferences.allows` accepts (diet, allergens and maximum time).
3. Score each by how many nutrition points one serving adds to the day, measured against the user's goals.
4. Sort by points, then rotate each equal-points group by `day.toEpochDay() % group.size`. The same input always
   gives the same result, and with several equally good recipes the first choice changes from day to day instead of
   repeating. (An affine shift of the sort key would not work: it leaves the order inside a tie group unchanged.)

Allergen exclusion and diet filtering happen before scoring, so a suggestion can never violate them.

## How to add a recipe

1. Add an entry to `Recipe` with all fields. Use an existing entry as a template and keep the ingredient list and the
   allergen set in sync (as written, including ready-made products).
2. Add the three string resources in both `values/strings.xml` and `values-sv/strings.xml`. Keep the Swedish and
   English versions equivalent; no machine translation on the device.
3. Add the branch to `Recipe.text` in `RecipeText.kt`. The exhaustive `when` fails the build if a branch is missing.
4. Run `./gradlew :core:recipes:test` and `./gradlew :feature:nutrition:test`. `RecipeDataTest` checks portions, diets,
   allergen families, coverage and alternatives; `RecipeSuggestionsTest` checks exclusion and selection.
5. If the recipe contains a new allergen, verify `Allergen` and the allergen name resources already cover it.

## Coverage target and rationale

The library targets **at least 40 recipes**, with **at least five the user's diet allows in every meal slot**
(`RecipeDataTest`). The reasoning:

- Three suggestions are shown at a time (`SUGGESTED_RECIPES`), so a slot needs at least a handful of alternatives for
  ties to rotate and for a time or allergen filter not to empty the card.
- Each of the four diets should see several options in every slot, including vegan and pescetarian; vegan recipes also
  serve the stricter end of every other diet.
- The three time buckets and all ingredient families should be present, so a user who limits time still gets variety
  and the library does not quietly become "pasta and oats only".
- More than about 60 recipes adds translation and review work without changing what the three-item card can show.

The current library has more than 40 recipes, including Swedish/Nordic everyday dishes (rye porridge, pea soup,
meatballs, cod with dill, salmon soup) and country-agnostic staples (curries, salads, pasta, soups, breakfast bowls,
snacks). Nordic dishes are part of the library, not an assumption that the user lives in Sweden.

## Out of scope

User-created recipes are not part of this library. They would need a separate store, user-entered allergen data and a
review path, and the deterministic suggestions would have to treat them with the same validation. That is a larger
feature than extending the curated library. Recipe text stays in the app's existing two languages; adding a third
language means adding the same resources under another `values-*` directory, not changing the schema.
