package se.novila.healthconnect

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.StepsRecord

/**
 * The only Health Connect permissions the app asks for: read today's steps and active calories.
 * Must match the `uses-permission` entries in AndroidManifest.xml (HealthConnectManifestTest).
 */
val MOVEMENT_PERMISSIONS: Set<String> = setOf(
    HealthPermission.getReadPermission(StepsRecord::class),
    HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
)

/** Today's movement from Health Connect. A value is null when it isn't shared or nothing was recorded. */
data class TodayMovement(
    val steps: Long? = null,
    val activeKilocalories: Int? = null,
)

/** What the training part can show from Health Connect. */
sealed interface MovementState {
    /** No Health Connect on this device. The app works as before without it. */
    data object Unavailable : MovementState

    /** Health Connect has to be installed or updated from Google Play first. */
    data object NeedsInstall : MovementState

    /**
     * Not connected yet. [denied] after the user said no: Health Connect then stops showing its
     * dialog, so access can only be given in Health Connect's own settings.
     */
    data class NeedsPermission(val denied: Boolean = false) : MovementState

    data class Connected(val today: TodayMovement) : MovementState
}

/**
 * The state to show without reading any data, or null when there is something to read.
 * Partial access is fine: whatever the user shared is shown, the rest stays empty.
 */
fun accessState(sdkStatus: Int, granted: Set<String>, denied: Boolean): MovementState? = when (sdkStatus) {
    HealthConnectClient.SDK_AVAILABLE ->
        if (granted.any { it in MOVEMENT_PERMISSIONS }) null else MovementState.NeedsPermission(denied)

    HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> MovementState.NeedsInstall

    else -> MovementState.Unavailable
}
