package se.novila.movement

import java.time.LocalDate

/** Today's movement. A value is null when it isn't shared or nothing was recorded. */
data class TodayMovement(
    val steps: Long? = null,
    val activeKilocalories: Int? = null,
)

/** What the app can show about movement, whatever the data source is. */
sealed interface MovementState {
    /** The source isn't on this device. The app works as before without it. */
    data object Unavailable : MovementState

    /** The source has to be installed or updated first. */
    data object NeedsInstall : MovementState

    /**
     * Not connected yet. [denied] after the user said no: the source then stops asking, so access
     * can only be given in its own settings.
     */
    data class NeedsPermission(val denied: Boolean = false) : MovementState

    data class Connected(val today: TodayMovement) : MovementState
}

/**
 * Where movement data comes from (Health Connect today). Implementations never throw: a temporary
 * problem (rate limit, the provider being updated) is a failed [Result], so the repository can fall
 * back to its cache. Everything else is a [MovementState].
 */
fun interface MovementSource {
    suspend fun read(date: LocalDate): Result<MovementState>
}

/** Last good reading per day. In memory today; a persistent cache only has to implement this. */
interface MovementCache {
    fun get(date: LocalDate): TodayMovement?

    fun put(date: LocalDate, movement: TodayMovement)

    fun clear()
}

/** Keeps only the latest day, in memory: nothing survives the process, nothing is written to disk. */
class InMemoryMovementCache : MovementCache {
    private var latest: Pair<LocalDate, TodayMovement>? = null

    @Synchronized override fun get(date: LocalDate): TodayMovement? = latest?.takeIf { it.first == date }?.second

    @Synchronized override fun put(date: LocalDate, movement: TodayMovement) {
        latest = date to movement
    }

    @Synchronized override fun clear() {
        latest = null
    }
}
