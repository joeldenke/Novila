package se.novila.exercise

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.time.LocalDate
import kotlin.time.Duration.Companion.minutes

class ExerciseRepositoryTest {
    private val today = LocalDate.of(2026, 10, 15)
    private val walk = Exercise(ExerciseType.Walking, today, 30.minutes)

    /** A source that answers with the queued results, in order, and remembers the days asked for. */
    // A list, since Result (a value class) can't be a vararg type.
    private class FakeSource(results: List<Result<ExerciseState>>) : ExerciseSource {
        private val queue = ArrayDeque(results)
        val asked = mutableListOf<LocalDate>()
        override suspend fun read(from: LocalDate): Result<ExerciseState> {
            asked += from
            return queue.removeFirst()
        }
    }

    private val connected = Result.success<ExerciseState>(ExerciseState.Connected(listOf(walk)))
    private val failed = Result.failure<ExerciseState>(IOException("rate limited"))
    private val needsPermission = Result.success<ExerciseState>(ExerciseState.NeedsPermission())

    @Test fun `reads five weeks up to today`() = runBlocking {
        val source = FakeSource(listOf(connected))
        val repository = ExerciseRepository(source)
        repository.refresh(today)
        assertEquals(ExerciseState.Connected(listOf(walk)), repository.state.value)
        assertEquals(listOf(today.minusDays(34)), source.asked)
    }

    @Test fun `a temporary failure keeps the last reading`() = runBlocking {
        val repository = ExerciseRepository(FakeSource(listOf(connected, failed)))
        repository.refresh(today)
        repository.refresh(today)
        assertEquals(ExerciseState.Connected(listOf(walk)), repository.state.value)
    }

    @Test fun `revoked access forgets the workouts`() = runBlocking {
        val repository = ExerciseRepository(FakeSource(listOf(connected, needsPermission)))
        repository.refresh(today)
        repository.refresh(today)
        assertEquals(ExerciseState.NeedsPermission(), repository.state.value)
    }

    @Test fun `a declined request is remembered until access is given`() = runBlocking {
        val repository = ExerciseRepository(FakeSource(listOf(needsPermission, connected)))
        repository.onPermissionResult(granted = false, today)
        assertEquals(ExerciseState.NeedsPermission(denied = true), repository.state.value)
        repository.onPermissionResult(granted = true, today)
        assertEquals(ExerciseState.Connected(listOf(walk)), repository.state.value)
    }
}
