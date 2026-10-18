package se.novila.healthconnect

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.ExerciseSessionRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.novila.exercise.ExerciseType
import se.novila.movement.MovementState

// Robolectric, because the Health Connect record classes may touch Android APIs when loaded.
@RunWith(RobolectricTestRunner::class)
class HealthConnectAccessTest {
    private val steps = "android.permission.health.READ_STEPS"
    private val calories = "android.permission.health.READ_ACTIVE_CALORIES_BURNED"

    private val exercise = "android.permission.health.READ_EXERCISE"
    private val sleep = "android.permission.health.READ_SLEEP"
    private val restingHeartRate = "android.permission.health.READ_RESTING_HEART_RATE"
    private val hrv = "android.permission.health.READ_HEART_RATE_VARIABILITY"

    @Test fun `asks only for steps, active calories, workouts, sleep and recovery`() {
        assertEquals(setOf(steps, calories), MOVEMENT_PERMISSIONS)
        assertEquals(setOf(restingHeartRate, hrv), RECOVERY_PERMISSIONS)
        assertEquals(setOf(steps, calories, exercise, sleep, restingHeartRate, hrv), HEALTH_PERMISSIONS)
    }

    @Test fun `access to workouts, sleep or recovery alone doesn't connect movement`() {
        assertEquals(
            MovementState.NeedsPermission(),
            accessState(HealthConnectClient.SDK_AVAILABLE, setOf(exercise, sleep, restingHeartRate)),
        )
    }

    @Test fun `Health Connect's workout types map to Novila's`() {
        mapOf(
            ExerciseSessionRecord.EXERCISE_TYPE_WALKING to ExerciseType.Walking,
            ExerciseSessionRecord.EXERCISE_TYPE_HIKING to ExerciseType.Walking,
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL to ExerciseType.Running,
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY to ExerciseType.Cycling,
            ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER to ExerciseType.Swimming,
            ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING to ExerciseType.Strength,
            ExerciseSessionRecord.EXERCISE_TYPE_YOGA to ExerciseType.Mobility,
            ExerciseSessionRecord.EXERCISE_TYPE_TENNIS to ExerciseType.Other,
        ).forEach { (type, expected) -> assertEquals(type.toString(), expected, exerciseType(type)) }
    }

    @Test fun `no Health Connect hides movement`() {
        assertEquals(MovementState.Unavailable, accessState(HealthConnectClient.SDK_UNAVAILABLE, emptySet()))
    }

    @Test fun `outdated Health Connect asks for an update`() {
        assertEquals(
            MovementState.NeedsInstall,
            accessState(HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED, emptySet()),
        )
    }

    @Test fun `without access the user is asked to connect`() {
        assertEquals(
            MovementState.NeedsPermission(),
            accessState(HealthConnectClient.SDK_AVAILABLE, emptySet()),
        )
    }

    @Test fun `other apps' permissions don't count as access`() {
        val unrelated = setOf("android.permission.health.READ_HEART_RATE")
        assertEquals(
            MovementState.NeedsPermission(),
            accessState(HealthConnectClient.SDK_AVAILABLE, unrelated),
        )
    }

    @Test fun `partial access is enough to read`() {
        assertNull(accessState(HealthConnectClient.SDK_AVAILABLE, setOf(steps)))
        assertNull(accessState(HealthConnectClient.SDK_AVAILABLE, setOf(steps, calories)))
    }
}
