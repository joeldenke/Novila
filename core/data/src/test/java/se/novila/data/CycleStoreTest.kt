package se.novila.data

import android.content.Context
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import se.novila.model.CycleLog
import se.novila.model.CycleSymptom
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class CycleStoreTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val stores = StoreScope()
    private val day = LocalDate.of(2026, 10, 22)

    @After fun close() = stores.close()

    @Test fun `off by default, and nothing is kept while it is off`() {
        val store = CycleStore(context, stores.scope, stores.cipher)
        assertFalse(store.cycle.await().enabled)
        store.update { it.withPeriod(day, on = true) }
        store.setEnabled(true)
        assertEquals(CycleLog(), store.cycle.await { it.enabled }.log)
    }

    @Test fun `the log survives a restart, and turning it off deletes it`() {
        val store = CycleStore(context, stores.scope, stores.cipher)
        store.setEnabled(true)
        store.update { it.withPeriod(day, on = true).withSymptom(day, CycleSymptom.Cramps, on = true) }
        store.cycle.await { it.log.on(day).period }

        stores.restart()
        val restarted = CycleStore(context, stores.scope, stores.cipher)
        assertEquals(setOf(CycleSymptom.Cramps), restarted.cycle.await().log.on(day).symptoms)

        restarted.setEnabled(false)
        restarted.cycle.await { !it.enabled }
        restarted.setEnabled(true)
        assertEquals(CycleLog(), restarted.cycle.await { it.enabled }.log)
    }

    @Test fun `a reset forgets the cycle and turns it off`() {
        val store = CycleStore(context, stores.scope, stores.cipher)
        store.setEnabled(true)
        store.update { it.withPeriod(day, on = true) }
        store.cycle.await { it.log.periodDays.isNotEmpty() }

        store.clear()
        assertEquals(SavedCycle(), store.cycle.await { !it.enabled })
    }
}
