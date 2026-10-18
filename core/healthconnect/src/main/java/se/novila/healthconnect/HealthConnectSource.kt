package se.novila.healthconnect

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import se.novila.movement.MovementSource
import se.novila.movement.MovementState
import se.novila.movement.TodayMovement
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.roundToInt

/**
 * [MovementSource] backed by Health Connect, where Google Fit, Fitbit, Samsung Health and others put
 * their data. Read-only and only while the app is open: nothing is written to Health Connect or sent
 * anywhere. Permissions are checked on every read (never cached), since the user can revoke them in
 * Health Connect at any time.
 */
class HealthConnectSource(private val context: Context) : MovementSource {

    override suspend fun read(date: LocalDate): Result<MovementState> {
        val sdkStatus = HealthConnectClient.getSdkStatus(context)
        return healthConnectRead(
            context = context,
            sdkStatus = sdkStatus,
            unavailable = checkNotNull(accessState(sdkStatus, emptySet())),
            needsPermission = MovementState.NeedsPermission(),
            required = { granted -> granted.any { it in MOVEMENT_PERMISSIONS } },
        ) { client, granted ->
            MovementState.Connected(readDay(client, date, granted))
        }
    }

    private suspend fun readDay(client: HealthConnectClient, date: LocalDate, granted: Set<String>): TodayMovement {
        val steps = HealthPermission.getReadPermission(StepsRecord::class) in granted
        val calories = HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class) in granted
        val metrics = buildSet {
            if (steps) add(StepsRecord.COUNT_TOTAL)
            if (calories) add(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)
        }
        // Local time, so "today" follows the user's day. Aggregation removes duplicates between apps.
        val result = client.aggregate(
            AggregateRequest(
                metrics = metrics,
                timeRangeFilter = TimeRangeFilter.between(date.atStartOfDay(), LocalDateTime.now()),
            ),
        )
        return TodayMovement(
            steps = if (steps) result[StepsRecord.COUNT_TOTAL] ?: 0L else null,
            activeKilocalories = if (calories) {
                result[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories?.roundToInt() ?: 0
            } else {
                null
            },
        )
    }

    companion object {
        private const val TAG = "HealthConnect"
        private const val PROVIDER_PACKAGE = "com.google.android.apps.healthdata"

        /** Health Connect's settings, where access can be given after the dialog was declined. */
        fun openSettings(context: Context) = start(context, Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS))

        /** Health Connect in Google Play, for Android 13 and lower where it is a separate app. */
        fun openInstall(context: Context) = start(
            context,
            Intent(Intent.ACTION_VIEW).apply {
                setPackage("com.android.vending")
                data = "market://details?id=$PROVIDER_PACKAGE&url=healthconnect%3A%2F%2Fonboarding".toUri()
                putExtra("overlay", true)
                putExtra("callerId", context.packageName)
            },
        )

        private fun start(context: Context, intent: Intent) {
            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Log.w(TAG, "No activity for ${intent.action}", e)
            }
        }
    }
}
