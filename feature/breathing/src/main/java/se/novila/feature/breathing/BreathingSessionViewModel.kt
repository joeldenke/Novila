package se.novila.feature.breathing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import se.novila.breathing.BreathingCatalog
import se.novila.breathing.BreathingMoment
import se.novila.breathing.BreathingSession
import kotlin.time.Clock
import kotlin.time.Instant

/** The guided session as the player shows it. */
data class BreathingUiState(
    val session: BreathingSession,
    val moment: BreathingMoment?,
    val elapsedMillis: Long,
    val running: Boolean,
) {
    /** True once the last round is over. */
    val finished: Boolean get() = moment == null

    /** Whole seconds left of the whole session, rounded up. */
    val remainingSeconds: Int
        get() {
            val left = (session.totalMillis - elapsedMillis).coerceAtLeast(0L)
            return ((left + 999L) / 1000L).toInt()
        }
}

/**
 * One guided session in [sessionId], scoped to its entry: the timer survives rotation, and leaving the screen pops the
 * entry, which clears the ViewModel and stops the timer. Nothing is saved; a session is a moment for the user.
 *
 * The clock makes the timer testable: a test can step time instead of waiting for it. The moment is recomputed from
 * elapsed time rather than counted down step by step, so drift can never build up.
 */
@AssistedInject
class BreathingSessionViewModel(
    @Assisted sessionId: String,
    private val clock: Clock,
) : ViewModel() {
    private val session: BreathingSession = BreathingCatalog.session(sessionId) ?: BreathingCatalog.calm

    private val _state = MutableStateFlow(BreathingUiState(session, session.momentAt(0L), 0L, running = true))

    /** The session, the phase it is in and how much is left. */
    val state: StateFlow<BreathingUiState> = _state.asStateFlow()

    private var ticker: Job? = null

    /** Time already spent in earlier runs of the timer, kept when the user pauses. */
    private var elapsedBeforeStart = 0L
    private var startedAt: Instant = clock.now()

    init {
        play()
    }

    /** Pause or continue. Pausing keeps the moment, so the user resumes where they stopped. */
    fun toggle() {
        if (_state.value.running) pause() else play()
    }

    /** Back to the first breath, running. */
    fun restart() {
        ticker?.cancel()
        elapsedBeforeStart = 0L
        startedAt = clock.now()
        _state.value = BreathingUiState(session, session.momentAt(0L), 0L, running = true)
        tick()
    }

    private fun pause() {
        ticker?.cancel()
        val elapsed = elapsed()
        elapsedBeforeStart = elapsed
        // Recompute the moment from the clock, so pausing shows the phase the user is really in.
        _state.value = BreathingUiState(
            session = session,
            moment = session.momentAt(elapsed),
            elapsedMillis = elapsed.coerceAtMost(session.totalMillis),
            running = false,
        )
    }

    private fun play() {
        if (_state.value.finished) return
        startedAt = clock.now()
        _state.value = _state.value.copy(running = true)
        tick()
    }

    /** Runs the timer while the user is breathing, one calm tick at a time. */
    private fun tick() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (true) {
                val elapsed = elapsed()
                val moment = session.momentAt(elapsed)
                _state.value = BreathingUiState(session, moment, elapsed.coerceAtMost(session.totalMillis), true)
                if (moment == null) break
                delay(TICK_MILLIS)
            }
            _state.value = _state.value.copy(running = false)
        }
    }

    private fun elapsed(): Long = elapsedBeforeStart + (clock.now() - startedAt).inWholeMilliseconds

    override fun onCleared() {
        ticker?.cancel()
    }

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(@Assisted sessionId: String): BreathingSessionViewModel
    }

    private companion object {
        /** Five ticks a second is smooth enough for a circle, and cheap. */
        const val TICK_MILLIS = 200L
    }
}
