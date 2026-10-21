package se.novila.sleep

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * The single place the UI gets recorded nights from. Kept in memory only: nothing from the source is written to
 * disk, so revoking access (or the app's process ending) forgets it.
 * - a temporary failure keeps showing the last reading
 * - when access is gone or the source is missing, the last reading is dropped so old data never shows
 *
 * The source is chosen where the repository is created (AppGraph in :app).
 */
class SleepRepository(
    private val source: SleepSource,
    private val clock: TimeSource = TimeSource.Monotonic,
) {
    private val _state = MutableStateFlow<SleepState>(SleepState.Unavailable)
    val state: StateFlow<SleepState> = _state.asStateFlow()

    /** Set when the user declined the permission request, until they grant something. */
    private var declined = false
    private val reading = Mutex()

    /** The last reading that connected: for which days and when, so a screen shown again soon after reuses it. */
    @Volatile private var lastRead: Triple<LocalDate, Long, TimeMark>? = null

    /**
     * Reads the [days] up to [today]. Overlapping calls run one at a time, so the latest read wins. Every tab reads
     * on resume, so a reading of the same days less than [FRESH_FOR] old is kept instead of asking Health Connect
     * again, which also keeps the app within Health Connect's read limits.
     */
    suspend fun refresh(today: LocalDate, days: Long = HISTORY_DAYS): Unit = reading.withLock {
        lastRead?.let { (date, count, at) ->
            if (date == today && count == days && at.elapsedNow() < FRESH_FOR) return@withLock
        }
        val result = source.read(today.minusDays(days - 1)).getOrElse { return@withLock } // Keep what is shown.
        lastRead = (result as? SleepState.Connected)?.let { Triple(today, days, clock.markNow()) }
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
        // What the user just allowed or took away is read at once.
        expire()
        refresh(today)
    }

    /**
     * The next [refresh] reads again, however recent the last reading. Called when the app leaves the screen: access
     * can be taken away in Health Connect meanwhile, and a reading must never outlive it.
     */
    fun expire() {
        lastRead = null
    }

    companion object {
        /** How long a reading is reused: long enough for moving between tabs, short enough for a watch's sync. */
        val FRESH_FOR = 2.minutes

        /** The year chart and the year before it, which it is compared with. */
        const val HISTORY_DAYS = 731L
    }
}
