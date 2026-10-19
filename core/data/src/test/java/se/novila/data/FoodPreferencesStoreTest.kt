package se.novila.data

import android.content.Context
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import se.novila.recipes.Allergen
import se.novila.recipes.Diet
import se.novila.recipes.FoodPreferences

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
}
