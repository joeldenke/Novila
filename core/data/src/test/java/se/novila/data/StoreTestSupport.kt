package se.novila.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

/** A store's scope in a test. [close] ends it, which frees the store's file for the next store. */
class StoreScope {
    var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private set

    /** Closes the current store, as when the app's process ends, so a new one can open the same file. */
    fun restart() {
        close()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    fun close() = runBlocking { scope.coroutineContext.job.cancelAndJoin() }
}

/** Waits for the first read, or a later save, that matches [predicate]. */
fun <T : Any> StateFlow<T?>.await(predicate: (T) -> Boolean = { true }): T =
    runBlocking { withTimeout(5_000) { filterNotNull().first(predicate) } }
