package se.novila.healthconnect

import android.content.Context
import android.os.RemoteException
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import java.io.IOException

/**
 * One Health Connect read for every source: the SDK check, the permission check and the errors that make a read
 * impossible, which used to be repeated in each source. [connected] runs with the client and the granted permissions
 * when there is something to read; otherwise the result is [unavailable] (Health Connect missing or outdated) or
 * [needsPermission] (nothing shared, or access revoked between the check and the read). Failures are logged here,
 * once, so a broken read is diagnosable from a single place.
 */
internal suspend fun <T> healthConnectRead(
    context: Context,
    sdkStatus: Int,
    unavailable: T,
    needsPermission: T,
    required: (Set<String>) -> Boolean,
    connected: suspend (HealthConnectClient, Set<String>) -> T,
): Result<T> {
    if (sdkStatus != HealthConnectClient.SDK_AVAILABLE) return Result.success(unavailable)
    return try {
        val client = HealthConnectClient.getOrCreate(context)
        val granted = client.permissionController.getGrantedPermissions()
        Result.success(if (required(granted)) connected(client, granted) else needsPermission)
    } catch (e: SecurityException) {
        // Revoked between the check and the read.
        Log.w(TAG, "Health Connect access revoked", e)
        Result.success(needsPermission)
    } catch (e: RemoteException) {
        Log.w(TAG, "Health Connect not reachable", e)
        Result.failure(e)
    } catch (e: IOException) {
        Log.w(TAG, "Health Connect read failed", e)
        Result.failure(e)
    } catch (e: IllegalStateException) {
        // For example the rate limit, or Health Connect being updated.
        Log.w(TAG, "Health Connect read failed", e)
        Result.failure(e)
    }
}

private const val TAG = "HealthConnect"
