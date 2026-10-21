package se.novila.data

import android.content.Context
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import se.novila.model.MealMemory
import se.novila.recipes.Allergen
import se.novila.recipes.Diet
import se.novila.recipes.FoodPreferences
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class FoodPreferencesStoreTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val stores = StoreScope()

    @After fun close() = stores.close()

    @Test fun `everything and any time until the user chooses`() {
        assertEquals(FoodPreferences(), FoodPreferencesStore(context, stores.scope, stores.cipher).preferences.await())
    }

    @Test fun `preferences survive a restart and are forgotten when cleared`() {
        val store = FoodPreferencesStore(context, stores.scope, stores.cipher)
        val saved = FoodPreferences(Diet.Vegetarian, setOf(Allergen.Peanuts, Allergen.TreeNuts), maxMinutes = 30)
        store.save(saved)
        store.preferences.await { it == saved }

        stores.restart()
        val reopened = FoodPreferencesStore(context, stores.scope, stores.cipher)
        assertEquals(saved, reopened.preferences.await())

        reopened.clear()
        reopened.preferences.await { it == FoodPreferences() }
    }

    @Test fun `remembered meals survive a restart and are forgotten when cleared`() {
        val store = FoodPreferencesStore(context, stores.scope, stores.cipher)
        val date = LocalDate.of(2026, 10, 21)
        store.updateMeals { it.withLogged("Frukost: gröt, bär", date) }
        store.updateMeals { it.toggledFavorite("Frukost: gröt, bär") }
        val saved = MealMemory().withLogged("Frukost: gröt, bär", date).toggledFavorite("Frukost: gröt, bär")
        store.meals.await { it == saved }

        stores.restart()
        val reopened = FoodPreferencesStore(context, stores.scope, stores.cipher)
        assertEquals(saved, reopened.meals.await())

        reopened.clear()
        reopened.meals.await { it == MealMemory() }
    }
}
