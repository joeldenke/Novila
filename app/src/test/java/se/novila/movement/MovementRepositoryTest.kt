package se.novila.movement

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

class MovementRepositoryTest {
    private val today = LocalDate.of(2026, 10, 15)
    private val reading = TodayMovement(steps = 8412, activeKilocalories = 312)

    /** A source that answers with the queued results, in order. */
    private class FakeSource(vararg results: Result<MovementState>) : MovementSource {
        private val queue = ArrayDeque(results.toList())
        override suspend fun read(date: LocalDate) = queue.removeFirst()
    }

    private val connected = Result.success<MovementState>(MovementState.Connected(reading))
    private val failed = Result.failure<MovementState>(IOException("rate limited"))

    @Test fun `a fresh read is shown and cached`() = runBlocking {
        val cache = InMemoryMovementCache()
        val repository = MovementRepository(FakeSource(connected), cache)
        repository.refresh(today)
        assertEquals(MovementState.Connected(reading), repository.state.value)
        assertEquals(reading, cache.get(today))
    }

    @Test fun `a temporary failure keeps the cached numbers`() = runBlocking {
        val cache = InMemoryMovementCache().apply { put(today, reading) }
        val repository = MovementRepository(FakeSource(failed), cache)
        repository.refresh(today)
        assertEquals(MovementState.Connected(reading), repository.state.value)
    }

    @Test fun `a failure without cache shows nothing`() = runBlocking {
        val repository = MovementRepository(FakeSource(failed), InMemoryMovementCache())
        repository.refresh(today)
        assertEquals(MovementState.Unavailable, repository.state.value)
    }

    @Test fun `yesterday's cache is not shown today`() = runBlocking {
        val cache = InMemoryMovementCache().apply { put(today.minusDays(1), reading) }
        val repository = MovementRepository(FakeSource(failed), cache)
        repository.refresh(today)
        assertEquals(MovementState.Unavailable, repository.state.value)
    }

    @Test fun `revoked access clears the cache`() = runBlocking {
        val cache = InMemoryMovementCache()
        val repository = MovementRepository(
            FakeSource(connected, Result.success(MovementState.NeedsPermission())),
            cache,
        )
        repository.refresh(today)
        repository.refresh(today)
        assertEquals(MovementState.NeedsPermission(denied = false), repository.state.value)
        assertNull(cache.get(today))
    }

    @Test fun `declining sends the user to the source's settings until access is given`() = runBlocking {
        val needsPermission = Result.success<MovementState>(MovementState.NeedsPermission())
        val repository = MovementRepository(FakeSource(needsPermission, connected), InMemoryMovementCache())
        repository.onPermissionResult(granted = false, date = today)
        assertEquals(MovementState.NeedsPermission(denied = true), repository.state.value)
        repository.onPermissionResult(granted = true, date = today)
        assertEquals(MovementState.Connected(reading), repository.state.value)
    }
}
