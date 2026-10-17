package se.novila.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SelectedDayTest {
    private var today = LocalDate.of(2026, 10, 17)
    private val day = SelectedDay { today }

    @Test fun `follows today past midnight until an earlier day is chosen`() = runBlocking {
        assertEquals(today, day.value)
        today = today.plusDays(1)
        day.refresh()
        assertEquals(today, day.value)
        assertEquals(today, day.dates.first())
    }

    @Test fun `an earlier day stays put, and choosing today follows today again`() {
        val yesterday = today.minusDays(1)
        day.select(yesterday)
        today = today.plusDays(1)
        day.refresh()
        assertEquals(yesterday, day.value)
        day.select(today)
        today = today.plusDays(1)
        day.refresh()
        assertEquals(today, day.value)
    }

    @Test fun `days outside the editable range are moved into it`() {
        day.select(today.plusDays(2))
        assertEquals(today, day.value)
        day.select(today.minusDays(100))
        assertEquals(today.minusDays(30), day.value)
    }
}
