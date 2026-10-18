package se.novila.recovery

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

class RecoveryRepositoryTest {
    private val today = LocalDate.of(2026, 10, 15)
    private val day = RecoveryDay(today, restingHeartRate = 52, hrvMillis = 48)

    /** A source that answers with the queued results, in order, and remembers the days asked for. */
    // A list, since Result (a value class) can't be a vararg type.
    private class FakeSource(results: List<Result<RecoveryState>>) : RecoverySource {
        private val queue = ArrayDeque(results)
        val asked = mutableListOf<LocalDate>()
        override suspend fun read(from: LocalDate): Result<RecoveryState> {
            asked += from
            return queue.removeFirst()
        }
    }

    private val connected = Result.success<RecoveryState>(RecoveryState.Connected(listOf(day)))
    private val failed = Result.failure<RecoveryState>(IOException("rate limited"))
    private val needsPermission = Result.success<RecoveryState>(RecoveryState.NeedsPermission())

    @Test fun `reads two weeks up to today`() = runBlocking {
        val source = FakeSource(listOf(connected))
        val repository = RecoveryRepository(source)
        repository.refresh(today)
        assertEquals(RecoveryState.Connected(listOf(day)), repository.state.value)
        assertEquals(listOf(today.minusDays(13)), source.asked)
    }

    @Test fun `a temporary failure keeps the last reading`() = runBlocking {
        val repository = RecoveryRepository(FakeSource(listOf(connected, failed)))
        repository.refresh(today)
        repository.refresh(today)
        assertEquals(RecoveryState.Connected(listOf(day)), repository.state.value)
    }

    @Test fun `revoked access forgets the readings`() = runBlocking {
        val repository = RecoveryRepository(FakeSource(listOf(connected, needsPermission)))
        repository.refresh(today)
        repository.refresh(today)
        assertEquals(RecoveryState.NeedsPermission(), repository.state.value)
    }

    @Test fun `a declined request is remembered until access is given`() = runBlocking {
        val repository = RecoveryRepository(FakeSource(listOf(needsPermission, connected)))
        repository.onPermissionResult(granted = false, today)
        assertEquals(RecoveryState.NeedsPermission(denied = true), repository.state.value)
        repository.onPermissionResult(granted = true, today)
        assertEquals(RecoveryState.Connected(listOf(day)), repository.state.value)
    }
}
