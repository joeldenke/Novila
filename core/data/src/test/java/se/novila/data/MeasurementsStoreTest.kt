package se.novila.data

import android.content.Context
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import se.novila.model.BodyMeasurements
import se.novila.model.MeasurementEntry
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class MeasurementsStoreTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val stores = StoreScope()
    private val today = LocalDate.of(2026, 10, 16)
    private val all = BodyMeasurements(weightKg = 74, waistCm = 82, neckCm = 37, hipsCm = 98)

    @After fun close() = stores.close()

    @Test fun `nothing is logged until something is measured`() {
        assertEquals(emptyList<MeasurementEntry>(), MeasurementsStore(context, stores.scope).entries.await())
    }

    @Test fun `logged days are kept, newest first, and survive a restart`() {
        val store = MeasurementsStore(context, stores.scope)
        store.save(today.minusDays(2), all)
        store.save(today, all.copy(weightKg = 73))
        store.entries.await { it.size == 2 }

        stores.restart()
        assertEquals(
            listOf(
                MeasurementEntry(today, all.copy(weightKg = 73)),
                MeasurementEntry(today.minusDays(2), all),
            ),
            MeasurementsStore(context, stores.scope).entries.await(),
        )
    }

    @Test fun `saving a day again replaces it, and an empty day is forgotten`() {
        val store = MeasurementsStore(context, stores.scope)
        store.save(today, all)
        store.entries.await { it.singleOrNull()?.measurements == all }

        store.save(today, BodyMeasurements(weightKg = 73))
        store.entries.await { it.singleOrNull()?.measurements == BodyMeasurements(weightKg = 73) }

        store.save(today, BodyMeasurements())
        store.entries.await { it.isEmpty() }
    }

    @Test fun `cleared measurements are forgotten`() {
        val store = MeasurementsStore(context, stores.scope)
        store.save(today, all)
        store.entries.await { it.isNotEmpty() }

        store.clear()
        store.entries.await { it.isEmpty() }
        stores.restart()
        assertTrue(MeasurementsStore(context, stores.scope).entries.await().isEmpty())
    }
}
