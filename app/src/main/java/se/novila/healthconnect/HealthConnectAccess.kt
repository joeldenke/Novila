package se.novila.healthconnect

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.StepsRecord
import se.novila.movement.MovementState

/**
 * The only Health Connect permissions the app asks for: read today's steps and active calories.
 * Must match the `uses-permission` entries in AndroidManifest.xml (HealthConnectManifestTest).
 */
val MOVEMENT_PERMISSIONS: Set<String> = setOf(
    HealthPermission.getReadPermission(StepsRecord::class),
    HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
)

/**
 * The state to show without reading any data, or null when there is something to read.
 * Partial access is fine: whatever the user shared is shown, the rest stays empty.
 */
fun accessState(sdkStatus: Int, granted: Set<String>): MovementState? = when (sdkStatus) {
    HealthConnectClient.SDK_AVAILABLE ->
        if (granted.any { it in MOVEMENT_PERMISSIONS }) null else MovementState.NeedsPermission()

    HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> MovementState.NeedsInstall

    else -> MovementState.Unavailable
}
