package se.novila.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MeasurementsTest {
    private val day = LocalDate.of(2026, 10, 14)
    private val all = BodyMeasurements(weightKg = 74, waistCm = 82, neckCm = 37, hipsCm = 98)

    private fun entry(daysAgo: Long, measurements: BodyMeasurements) =
        MeasurementEntry(day.minusDays(daysAgo), measurements)

    @Test fun `a new day of measurements is empty`() {
        assertTrue(BodyMeasurements().isEmpty)
        assertTrue(BodyMeasurements(weightKg = 74).isEmpty.not())
    }

    @Test fun `encoded measurements read back the same`() {
        assertEquals(all, MeasurementsCodec.decode(MeasurementsCodec.encode(all)))
        assertEquals(BodyMeasurements(), MeasurementsCodec.decode(MeasurementsCodec.encode(BodyMeasurements())))
        assertEquals("v1;74;82;37;98", MeasurementsCodec.encode(all))
        assertEquals("v1;;;;", MeasurementsCodec.encode(BodyMeasurements()))
    }

    @Test fun `nothing or an unknown version reads as nothing logged, a damaged number is dropped`() {
        assertEquals(BodyMeasurements(), MeasurementsCodec.decode(null))
        assertEquals(BodyMeasurements(), MeasurementsCodec.decode(""))
        assertEquals(BodyMeasurements(), MeasurementsCodec.decode("v9;74;82;37;98"))
        assertEquals(BodyMeasurements(), MeasurementsCodec.decode("v1;74;82;37"))
        assertEquals(
            BodyMeasurements(waistCm = 82, neckCm = 37, hipsCm = 98),
            MeasurementsCodec.decode("v1;x;82;37;98"),
        )
    }

    @Test fun `a number outside the range is not logged, the rest of the line still is`() {
        assertEquals(
            BodyMeasurements(waistCm = 82, neckCm = 37, hipsCm = 98),
            MeasurementsCodec.decode("v1;999;82;37;98"),
        )
        assertEquals(
            BodyMeasurements(weightKg = 74, neckCm = 37, hipsCm = 98),
            MeasurementsCodec.decode("v1;74;5;37;98"),
        )
        assertEquals(
            BodyMeasurements(weightKg = 74, waistCm = 82, hipsCm = 98),
            MeasurementsCodec.decode("v1;74;82;900;98"),
        )
        assertEquals(
            BodyMeasurements(weightKg = 74, waistCm = 82, neckCm = 37),
            MeasurementsCodec.decode("v1;74;82;37;5"),
        )
    }

    @Test fun `the newest day with a weight is the latest weight`() {
        assertEquals(72, listOf(entry(0, all.copy(weightKg = 72)), entry(3, all)).latestWeightKg())
        assertEquals(74, listOf(entry(0, all.copy(weightKg = null)), entry(3, all)).latestWeightKg())
        assertNull(listOf(entry(0, all.copy(weightKg = null)), entry(3, all.copy(weightKg = null))).latestWeightKg())
        assertNull(emptyList<MeasurementEntry>().latestWeightKg())
    }

    @Test fun `correcting an older day does not move the newest weight`() {
        val newest = entry(0, all.copy(weightKg = 72))
        val older = entry(5, all.copy(weightKg = 80))

        assertEquals(72, listOf(newest, older).latestWeightKg())
        // Forgetting the newest day falls back to the newest one that still has a weight.
        assertEquals(80, listOf(older).latestWeightKg())
    }

    @Test fun `a day is read from the history, and a day without measurements is empty`() {
        val entry = entry(0, all)
        assertEquals(all, listOf(entry).on(day))
        assertEquals(BodyMeasurements(), listOf(entry).on(day.minusDays(1)))
        assertEquals(BodyMeasurements(), emptyList<MeasurementEntry>().on(day))
    }

    @Test fun `the form shows what is saved`() {
        assertEquals(MeasurementForm(weight = "74", waist = "82", neck = "37", hips = "98"), MeasurementForm.of(all))
        assertEquals(MeasurementForm(), MeasurementForm.of(BodyMeasurements()))
    }

    @Test fun `the form saves valid and empty fields and keeps a saved one when the number is wrong`() {
        val saved = all

        assertEquals(
            BodyMeasurements(weightKg = 72, waistCm = 84, neckCm = 36, hipsCm = 99),
            MeasurementForm(weight = "72", waist = "84", neck = "36", hips = "99").toMeasurements(saved),
        )
        assertEquals(BodyMeasurements(waistCm = 82), MeasurementForm(waist = "82").toMeasurements(BodyMeasurements()))
        assertEquals(
            BodyMeasurements(weightKg = 72, waistCm = 82, neckCm = 37, hipsCm = 98),
            MeasurementForm(weight = "72", waist = "8", neck = "37", hips = "98").toMeasurements(saved),
        )
        // A wrong weight keeps the saved one, while the fields left empty clear what was measured.
        assertEquals(BodyMeasurements(weightKg = 74), MeasurementForm(weight = "7").toMeasurements(saved))
    }

    @Test fun `each field is checked against its own range`() {
        val form = MeasurementForm(weight = "72", waist = "84", neck = "36", hips = "99")
        assertEquals(NumberInput.Valid(72), form.weightInput)
        assertEquals(NumberInput.Valid(84), form.waistInput)
        assertEquals(NumberInput.Valid(36), form.neckInput)
        assertEquals(NumberInput.Valid(99), form.hipsInput)

        val wrong = MeasurementForm(weight = "7", waist = "8", neck = "3", hips = "9")
        assertEquals(NumberInput.Invalid, wrong.weightInput)
        assertEquals(NumberInput.Invalid, wrong.waistInput)
        assertEquals(NumberInput.Invalid, wrong.neckInput)
        assertEquals(NumberInput.Invalid, wrong.hipsInput)
    }
}
