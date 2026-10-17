package se.novila.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class EditableDaysTest {
    private val today = LocalDate.of(2026, 10, 17)

    @Test fun `days are kept within the last month and never in the future`() {
        assertEquals(today, EditableDays.clamp(today.plusDays(3), today))
        assertEquals(today.minusDays(30), EditableDays.clamp(today.minusDays(90), today))
        assertEquals(today.minusDays(4), EditableDays.clamp(today.minusDays(4), today))
    }

    @Test fun `the ends of the range stop the arrows`() {
        assertFalse(EditableDays.canGoForward(today, today))
        assertTrue(EditableDays.canGoBack(today, today))
        assertTrue(EditableDays.canGoForward(today.minusDays(1), today))
        assertFalse(EditableDays.canGoBack(today.minusDays(30), today))
    }
}
