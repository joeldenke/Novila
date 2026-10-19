package se.novila.data

import com.google.crypto.tink.Aead
import com.google.crypto.tink.subtle.AesGcmJce
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
import java.security.SecureRandom

/** A store's scope in a test. [close] ends it, which frees the store's file for the next store. */
class StoreScope {
    var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private set

    /**
     * The cipher the stores under test are sealed with. The same one across a [restart], so a reopened store can
     * read what the previous one wrote.
     */
    val cipher: DatastoreCipher = testCipher()

    /** Closes the current store, as when the app's process ends, so a new one can open the same file. */
    fun restart() {
        close()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    fun close() = runBlocking { scope.coroutineContext.job.cancelAndJoin() }
}

/**
 * A cipher for tests: a real AES-GCM key generated in-process, so no Android Keystore (which Robolectric does not
 * provide) is touched, while the code under test still seals and opens the file for real.
 */
fun testCipher(): DatastoreCipher = object : DatastoreCipher {
    private val aead: Aead = AesGcmJce(ByteArray(32).also { SecureRandom().nextBytes(it) })

    override fun aead(): Aead = aead
}

/** Waits for the first read, or a later save, that matches [predicate]. */
fun <T : Any> StateFlow<T?>.await(predicate: (T) -> Boolean = { true }): T =
    runBlocking { withTimeout(30_000) { filterNotNull().first(predicate) } }
