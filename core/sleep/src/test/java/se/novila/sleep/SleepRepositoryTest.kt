package se.novila.sleep

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime
import kotlin.time.TestTimeSource

class SleepRepositoryTest {
    private val today = LocalDate.of(2026, 10, 15)
    private val night = Night(today, LocalTime.of(23, 0), LocalTime.of(7, 0), asleepMinutes = 450)

    /** A source that answers with the queued results, in order, and remembers the days asked for. */
    // A list, since Result (a value class) can't be a vararg type.
    private class FakeSource(results: List<Result<SleepState>>) : SleepSource {
        private val queue = ArrayDeque(results)
        val asked = mutableListOf<LocalDate>()
        override suspend fun read(from: LocalDate): Result<SleepState> {
            asked += from
            return queue.removeFirst()
        }
    }

    private val connected = Result.success<SleepState>(SleepState.Connected(listOf(night)))
    private val failed = Result.failure<SleepState>(IOException("rate limited"))
    private val needsPermission = Result.success<SleepState>(SleepState.NeedsPermission())

    @Test fun `reads two years up to today, for the year chart and the year before`() = runBlocking {
        val source = FakeSource(listOf(connected))
        val repository = SleepRepository(source)
        repository.refresh(today)
        assertEquals(SleepState.Connected(listOf(night)), repository.state.value)
        assertEquals(listOf(today.minusDays(730)), source.asked)
    }

    @Test fun `a temporary failure keeps the last reading`() = runBlocking {
        val clock = TestTimeSource()
        val repository = SleepRepository(FakeSource(listOf(connected, failed)), clock)
        repository.refresh(today)
        clock += SleepRepository.FRESH_FOR
        repository.refresh(today)
        assertEquals(SleepState.Connected(listOf(night)), repository.state.value)
    }

    @Test fun `revoked access forgets the nights`() = runBlocking {
        val clock = TestTimeSource()
        val repository = SleepRepository(FakeSource(listOf(connected, needsPermission)), clock)
        repository.refresh(today)
        clock += SleepRepository.FRESH_FOR
        repository.refresh(today)
        assertEquals(SleepState.NeedsPermission(), repository.state.value)
    }

    @Test fun `a recent reading is kept instead of asking again`() = runBlocking {
        val clock = TestTimeSource()
        val source = FakeSource(listOf(connected))
        val repository = SleepRepository(source, clock)
        repository.refresh(today)
        clock += SleepRepository.FRESH_FOR / 2
        repository.refresh(today)
        assertEquals(1, source.asked.size)
    }

    @Test fun `an expired reading is read again`() = runBlocking {
        val repository = SleepRepository(FakeSource(listOf(connected, needsPermission)))
        repository.refresh(today)
        repository.expire()
        repository.refresh(today)
        assertEquals(SleepState.NeedsPermission(), repository.state.value)
    }

    @Test fun `a declined request is remembered until access is given`() = runBlocking {
        val repository = SleepRepository(FakeSource(listOf(needsPermission, connected)))
        repository.onPermissionResult(granted = false, today)
        assertEquals(SleepState.NeedsPermission(denied = true), repository.state.value)
        repository.onPermissionResult(granted = true, today)
        assertEquals(SleepState.Connected(listOf(night)), repository.state.value)
    }
}
