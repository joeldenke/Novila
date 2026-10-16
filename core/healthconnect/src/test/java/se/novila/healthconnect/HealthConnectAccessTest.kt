package se.novila.healthconnect

import androidx.health.connect.client.HealthConnectClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.novila.movement.MovementState

// Robolectric, because the Health Connect record classes may touch Android APIs when loaded.
@RunWith(RobolectricTestRunner::class)
class HealthConnectAccessTest {
    private val steps = "android.permission.health.READ_STEPS"
    private val calories = "android.permission.health.READ_ACTIVE_CALORIES_BURNED"

    @Test fun `asks only for today's steps and active calories`() {
        assertEquals(setOf(steps, calories), MOVEMENT_PERMISSIONS)
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
