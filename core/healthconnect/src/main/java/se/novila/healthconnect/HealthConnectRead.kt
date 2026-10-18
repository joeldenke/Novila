package se.novila.healthconnect

import android.content.Context
import android.os.RemoteException
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import java.io.IOException

/** The tag every Health Connect log line uses. */
internal const val HEALTH_CONNECT_TAG = "HealthConnect"

/**
 * One Health Connect read, shared by every source: the SDK status check, the client, the granted permissions and
 * the same error handling, so the three sources can never drift apart in what they do on a failure.
 *
 * - No Health Connect, or one that has to be updated, is [unavailable] with the SDK status, so the caller can tell
 *   a missing provider from one that needs an update.
 * - [needsPermission] both before the read (nothing granted) and when access is revoked between the check and the
 *   read (a [SecurityException]); either way the UI asks the user to connect.
 * - A temporary problem ([RemoteException], [IOException] or [IllegalStateException], such as the rate limit) is a
 *   failed [Result], so the repository keeps what it showed.
 *
 * [sdkStatus] and [open] are seams: tests drive the flow without a real Health Connect client.
 */
internal suspend fun <T> readHealthConnect(
    context: Context,
    unavailable: (sdkStatus: Int) -> T,
    needsPermission: () -> T,
    connected: suspend (client: HealthConnectClient, granted: Set<String>) -> T,
    sdkStatus: (Context) -> Int = { HealthConnectClient.getSdkStatus(it) },
    open: (Context) -> HealthConnectClient = { HealthConnectClient.getOrCreate(it) },
): Result<T> {
    val status = sdkStatus(context)
    if (status != HealthConnectClient.SDK_AVAILABLE) return Result.success(unavailable(status))
    return try {
        val client = open(context)
        val granted = client.permissionController.getGrantedPermissions()
        Result.success(connected(client, granted))
    } catch (e: SecurityException) {
        // Revoked between the check and the read.
        Log.w(HEALTH_CONNECT_TAG, "Health Connect access revoked", e)
        Result.success(needsPermission())
    } catch (e: RemoteException) {
        Log.w(HEALTH_CONNECT_TAG, "Health Connect not reachable", e)
        Result.failure(e)
    } catch (e: IOException) {
        Log.w(HEALTH_CONNECT_TAG, "Health Connect read failed", e)
        Result.failure(e)
    } catch (e: IllegalStateException) {
        // For example the rate limit, or Health Connect being updated.
        Log.w(HEALTH_CONNECT_TAG, "Health Connect read failed", e)
        Result.failure(e)
    }
}
