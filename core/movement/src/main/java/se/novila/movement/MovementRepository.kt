package se.novila.movement

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

/**
 * The single place the UI gets movement from. Combines a [MovementSource] with a [MovementCache]:
 * - the cached reading for the day is shown at once, then replaced by a fresh read (stale while revalidate)
 * - a temporary failure keeps showing the cached reading instead of hiding the numbers
 * - when access is gone or the source is missing, the cache is cleared so old data never shows
 *
 * Swap the source or the cache where the repository is created (HealthViewModel); nothing else changes.
 */
class MovementRepository(
    private val source: MovementSource,
    private val cache: MovementCache,
) {
    private val _state = MutableStateFlow<MovementState>(MovementState.Unavailable)
    val state: StateFlow<MovementState> = _state.asStateFlow()

    /** Set when the user declined the permission request, until they grant something. */
    private var declined = false
    private val reading = Mutex()

    /** Reads [date] from the source. Overlapping calls run one at a time, so the latest read wins. */
    suspend fun refresh(date: LocalDate): Unit = reading.withLock {
        cache.get(date)?.let { _state.value = MovementState.Connected(it) }
        val result = source.read(date).getOrElse { return@withLock } // Keep what is shown.
        when (result) {
            is MovementState.Connected -> {
                declined = false
                cache.put(date, result.today)
                _state.value = result
            }

            is MovementState.NeedsPermission -> {
                cache.clear()
                _state.value = MovementState.NeedsPermission(denied = declined)
            }

            MovementState.NeedsInstall, MovementState.Unavailable -> {
                cache.clear()
                _state.value = result
            }
        }
    }

    /** Result of the permission request: whether the user gave access to anything. */
    suspend fun onPermissionResult(granted: Boolean, date: LocalDate) {
        declined = !granted
        refresh(date)
    }
}
