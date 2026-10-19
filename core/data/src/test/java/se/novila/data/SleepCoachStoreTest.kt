package se.novila.data

import android.content.Context
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import se.novila.sleep.SleepCoachPreferences
import se.novila.sleep.WindDownStep
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
class SleepCoachStoreTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val stores = StoreScope()
    private val evening = SleepCoachPreferences(windDown = setOf(WindDownStep.ScreensOff, WindDownStep.Read))

    @After fun close() = stores.close()

    @Test fun `no answers until the coach is set up`() {
        assertNull(SleepCoachStore(context, stores.scope, stores.cipher).answers.await().preferences)
    }

    @Test fun `saved answers survive a restart, and a reset forgets them`() {
        val store = SleepCoachStore(context, stores.scope, stores.cipher)
        store.save(SleepCoachPreferences(bedtime = LocalTime.of(23, 0)))
        store.save(evening)
        store.answers.await { it.preferences == evening }

        stores.restart()
        val restarted = SleepCoachStore(context, stores.scope, stores.cipher)
        assertEquals(evening, restarted.answers.await().preferences)

        restarted.clear()
        restarted.answers.await { it.preferences == null }
    }
}
