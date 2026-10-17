package se.novila.data

import android.content.Context
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import se.novila.model.Goals

@RunWith(RobolectricTestRunner::class)
class GoalsStoreTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val stores = StoreScope()

    @After fun close() = stores.close()

    @Test fun `the general advice until the user sets a goal`() {
        assertEquals(Goals(), GoalsStore(context, stores.scope).goals.await())
    }

    @Test fun `goals survive a restart and are forgotten when cleared`() {
        val store = GoalsStore(context, stores.scope)
        val saved = Goals(steps = 10000, sleepHours = 7.5f, waterGlasses = 6)
        store.save(saved)
        store.goals.await { it == saved }

        stores.restart()
        val reopened = GoalsStore(context, stores.scope)
        assertEquals(saved, reopened.goals.await())

        reopened.clear()
        reopened.goals.await { it == Goals() }
    }
}
