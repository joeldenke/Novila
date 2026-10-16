package se.novila.data

import android.content.Context
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import se.novila.model.Profile

@RunWith(RobolectricTestRunner::class)
class ProfileStoreTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val stores = StoreScope()

    @After fun close() = stores.close()

    @Test fun `empty until something is filled in`() {
        assertEquals(Profile(), ProfileStore(context, stores.scope).profile.await())
    }

    @Test fun `the profile survives a restart`() {
        val store = ProfileStore(context, stores.scope)
        val saved = Profile(name = "Anna", birthYear = 1990, heightCm = 168, weightKg = 64)
        store.save(saved)
        store.profile.await { it == saved }

        stores.restart()
        assertEquals(saved, ProfileStore(context, stores.scope).profile.await())
    }

    @Test fun `cleared fields and a cleared profile are forgotten`() {
        val store = ProfileStore(context, stores.scope)
        store.save(Profile(name = "Anna", weightKg = 64))
        store.save(Profile(name = "Anna"))
        store.profile.await { it == Profile(name = "Anna") }

        store.clear()
        store.profile.await { it.isEmpty }
    }
}
