package se.novila.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class QuickPicksTest {
    private val today = LocalDate.of(2026, 10, 21)

    private fun MealMemory.logged(text: String, daysAgo: Long) = withLogged(text, today.minusDays(daysAgo))

    @Test fun `nothing logged offers nothing`() {
        assertEquals(emptyList<MealPick>(), MealMemory().picks(today))
    }

    @Test fun `the meal logged on most days comes first, then the latest`() {
        val memory = MealMemory()
            .logged("Frukost: gröt, bär", daysAgo = 3)
            .logged("Lunch: lax, potatis", daysAgo = 2)
            .logged("Frukost: gröt, bär", daysAgo = 1)
            .logged("Middag: pasta, sallad", daysAgo = 0)

        assertEquals(
            listOf("Frukost: gröt, bär", "Middag: pasta, sallad", "Lunch: lax, potatis"),
            memory.picks(today).map { it.text },
        )
    }

    @Test fun `the same meal twice in a day counts as one day`() {
        val memory = MealMemory()
            .logged("Kaffe och bulle", daysAgo = 0)
            .logged("Kaffe och bulle", daysAgo = 0)
            .logged("Gröt", daysAgo = 2)
            .logged("Gröt", daysAgo = 1)

        assertEquals(listOf("Gröt", "Kaffe och bulle"), memory.picks(today).map { it.text })
    }

    @Test fun `two spellings are one meal, shown as written last`() {
        val memory = MealMemory()
            .logged("gröt,  bär.", daysAgo = 2)
            .logged("Gröt, bär", daysAgo = 1)

        assertEquals(listOf(MealPick("Gröt, bär", favorite = false)), memory.picks(today))
    }

    @Test fun `favorites come first, in the order they were starred, and are not offered twice`() {
        val memory = MealMemory()
            .logged("Gröt, bär", daysAgo = 2)
            .logged("Lax, potatis", daysAgo = 1)
            .toggledFavorite("Lax, potatis")
            .toggledFavorite("Smörgås med ägg")

        assertEquals(
            listOf(
                MealPick("Lax, potatis", favorite = true),
                MealPick("Smörgås med ägg", favorite = true),
                MealPick("Gröt, bär", favorite = false),
            ),
            memory.picks(today),
        )
    }

    @Test fun `a second star takes it away`() {
        val memory = MealMemory().toggledFavorite("Gröt, bär").toggledFavorite("gröt, bär.")

        assertFalse(memory.isFavorite("Gröt, bär"))
        assertEquals(emptyList<String>(), memory.favorites)
    }

    @Test fun `no more favorites than the limit`() {
        val full = (1..MAX_FAVORITE_MEALS).fold(MealMemory()) { memory, i -> memory.toggledFavorite("Måltid $i") }

        assertEquals(full, full.toggledFavorite("En till"))
        assertTrue(full.toggledFavorite("Måltid 1").favorites.size < MAX_FAVORITE_MEALS)
    }

    @Test fun `meals older than the window are forgotten and not offered`() {
        val memory = MealMemory()
            .withLogged("Gammal gröt", today.minusDays(QUICK_PICK_DAYS.toLong() + 5))
            .withLogged("Ny gröt", today)

        assertEquals(listOf("Ny gröt"), memory.recent.map { it.text })
        assertEquals(
            emptyList<MealPick>(),
            MealMemory(listOf(LoggedMeal("Gröt", today.minusDays(QUICK_PICK_DAYS.toLong())))).picks(today),
        )
    }

    @Test fun `only the latest meals are remembered`() {
        val memory = (1..MAX_RECENT_MEALS + 10).fold(MealMemory()) { memory, i ->
            memory.withLogged("Måltid $i", today)
        }

        assertEquals(MAX_RECENT_MEALS, memory.recent.size)
        assertEquals("Måltid ${MAX_RECENT_MEALS + 10}", memory.recent.last().text)
    }

    @Test fun `at most the limit besides the favorites`() {
        val memory = (1..10).fold(MealMemory()) { memory, i -> memory.logged("Måltid $i", daysAgo = 0) }

        assertEquals(MAX_MEAL_PICKS, memory.picks(today).size)
    }

    @Test fun `blank meals are not remembered, and a meal is one short line`() {
        assertEquals(MealMemory(), MealMemory().withLogged("  \n ", today))
        assertEquals("Gröt bär", mealLine(" Gröt\nbär "))
        assertEquals(MAX_MEAL_LENGTH, mealLine("a".repeat(MAX_MEAL_LENGTH + 20)).length)
    }

    @Test fun `the codec round-trips recent meals and favorites, a bar in the text too`() {
        val memory = MealMemory()
            .withLogged("Frukost: gröt | bär", today.minusDays(1))
            .withLogged("Lunch: lax", today)
            .toggledFavorite("Lunch: lax")

        val encoded = MealMemoryCodec.encode(memory)

        assertEquals("v1\nR|2026-10-20|Frukost: gröt | bär\nR|2026-10-21|Lunch: lax\nF|Lunch: lax", encoded)
        assertEquals(memory, MealMemoryCodec.decode(encoded))
    }

    @Test fun `missing, unknown or unreadable data remembers nothing it can't read`() {
        assertEquals(MealMemory(), MealMemoryCodec.decode(null))
        assertEquals(MealMemory(), MealMemoryCodec.decode("v9\nR|2026-10-20|Gröt"))
        assertEquals(
            MealMemory(listOf(LoggedMeal("Gröt", today)), listOf("Lax")),
            MealMemoryCodec.decode("v1\nR|igår|Gröt\nR|2026-10-21|Gröt\nR|2026-10-21|\nX|Lax\nF|Lax\nF|lax."),
        )
    }

    @Test fun `the drinks drunk on most days come first, without the glass of water`() {
        val days = listOf(
            DayEntry(today)
                .withDrink(Drink(DrinkKind.Coffee, 150, LocalTime.of(8, 0)))
                .withDrink(Drink(DrinkKind.Water, DrinkKind.Water.defaultMl))
                .withDrink(Drink(DrinkKind.Water, 500)),
            DayEntry(today.minusDays(1))
                .withDrink(Drink(DrinkKind.Coffee, 150))
                .withDrink(Drink(DrinkKind.Coffee, 150))
                .withDrink(Drink(DrinkKind.Tea, 250)),
            DayEntry(today.minusDays(2)).withDrink(Drink(DrinkKind.Juice, 200)),
        )

        assertEquals(
            listOf(Drink(DrinkKind.Coffee, 150), Drink(DrinkKind.Water, 500), Drink(DrinkKind.Tea, 250)),
            recentDrinks(days),
        )
    }

    @Test fun `no drinks offers nothing`() {
        assertEquals(emptyList<Drink>(), recentDrinks(listOf(DayEntry(today))))
    }
}
