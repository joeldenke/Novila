package se.novila.healthconnect

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import se.novila.movement.MovementState

/** Read today's steps and active calories, for the movement cards. */
val MOVEMENT_PERMISSIONS: Set<String> = setOf(
    HealthPermission.getReadPermission(StepsRecord::class),
    HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
)

/** Read the last five weeks' workouts, the history the training program builds on. */
val EXERCISE_PERMISSION: String = HealthPermission.getReadPermission(ExerciseSessionRecord::class)

/** Read when the user went to bed, woke up and slept, so a recorded night fills in Sleep. */
val SLEEP_PERMISSION: String = HealthPermission.getReadPermission(SleepSessionRecord::class)

/**
 * The only Health Connect permissions the app asks for, all in one dialog. Must match the `uses-permission` entries
 * in AndroidManifest.xml (HealthConnectManifestTest).
 */
val HEALTH_PERMISSIONS: Set<String> = MOVEMENT_PERMISSIONS + EXERCISE_PERMISSION + SLEEP_PERMISSION

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
