package se.novila.sleep

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

/**
 * The single place the UI gets recorded nights from. Kept in memory only: nothing from the source is written to
 * disk, so revoking access (or the app's process ending) forgets it.
 * - a temporary failure keeps showing the last reading
 * - when access is gone or the source is missing, the last reading is dropped so old data never shows
 *
 * The source is chosen where the repository is created (AppGraph in :app).
 */
class SleepRepository(private val source: SleepSource) {
    private val _state = MutableStateFlow<SleepState>(SleepState.Unavailable)
    val state: StateFlow<SleepState> = _state.asStateFlow()

    /** Set when the user declined the permission request, until they grant something. */
    private var declined = false
    private val reading = Mutex()

    /** Reads the [days] up to [today]. Overlapping calls run one at a time, so the latest read wins. */
    suspend fun refresh(today: LocalDate, days: Long = HISTORY_DAYS): Unit = reading.withLock {
        val result = source.read(today.minusDays(days - 1)).getOrElse { return@withLock } // Keep what is shown.
        _state.value = when (result) {
            is SleepState.Connected -> {
                declined = false
                result
            }

            is SleepState.NeedsPermission -> SleepState.NeedsPermission(denied = declined)

            SleepState.Unavailable -> result
        }
    }

    /** Result of the permission request: whether the user gave access to anything. */
    suspend fun onPermissionResult(granted: Boolean, today: LocalDate) {
        declined = !granted
        refresh(today)
    }

    companion object {
        /** The year chart and the year before it, which it is compared with. */
        const val HISTORY_DAYS = 731L
    }
}
