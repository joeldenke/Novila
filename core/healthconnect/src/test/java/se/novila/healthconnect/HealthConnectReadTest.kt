package se.novila.healthconnect

import android.content.Context
import android.os.RemoteException
import androidx.health.connect.client.HealthConnectClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.IOException

// Robolectric, because the Health Connect client classes may touch Android APIs when loaded.
@RunWith(RobolectricTestRunner::class)
class HealthConnectReadTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    /** No test reaches [connected]: the status and error paths are what this covers. */
    private fun read(
        sdkStatus: Int,
        unavailable: (Int) -> String = { "unavailable" },
        needsPermission: () -> String = { "permission" },
        open: (Context) -> HealthConnectClient = { error("open must not be called") },
        connected: suspend (HealthConnectClient, Set<String>) -> String =
            { _, _ -> error("connected must not be called") },
    ): Result<String> = runBlocking {
        readHealthConnect(
            context = context,
            unavailable = unavailable,
            needsPermission = needsPermission,
            connected = connected,
            sdkStatus = { sdkStatus },
            open = open,
        )
    }

    @Test fun `no Health Connect is the unavailable state`() {
        assertEquals("unavailable", read(sdkStatus = HealthConnectClient.SDK_UNAVAILABLE).getOrThrow())
    }

    @Test fun `an outdated Health Connect passes its status to unavailable`() {
        var seen = -1
        val result = read(
            sdkStatus = HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED,
            unavailable = { seen = it; "unavailable" },
        )
        assertEquals(HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED, seen)
        assertEquals("unavailable", result.getOrThrow())
    }

    @Test fun `access revoked between the check and the read asks for permission`() {
        val result = read(
            sdkStatus = HealthConnectClient.SDK_AVAILABLE,
            open = { throw SecurityException("revoked") },
        )
        assertEquals("permission", result.getOrThrow())
    }

    @Test fun `a temporary problem fails the read`() {
        val problems = listOf(RemoteException("gone"), IOException("offline"), IllegalStateException("rate limit"))
        problems.forEach { problem ->
            val result = read(sdkStatus = HealthConnectClient.SDK_AVAILABLE, open = { throw problem })
            assertTrue(problem.toString(), result.isFailure)
            assertEquals(problem.toString(), problem, result.exceptionOrNull())
        }
    }
}
