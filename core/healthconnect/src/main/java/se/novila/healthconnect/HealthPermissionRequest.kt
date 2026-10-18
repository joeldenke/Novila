package se.novila.healthconnect

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.health.connect.client.PermissionController

/**
 * Health Connect's own permission dialog for [permissions] (all of [HEALTH_PERMISSIONS] by default: steps, active
 * calories, workouts, sleep and recovery), which links to [HealthDataInfoActivity] for the details. Health Connect
 * only shows what isn't granted yet. Returns the function that opens it; [onResult] gets whether the user gave
 * access to anything.
 */
@Composable
fun rememberHealthPermissionRequest(
    onResult: (granted: Boolean) -> Unit,
    permissions: Set<String> = HEALTH_PERMISSIONS,
): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        remember { PermissionController.createRequestPermissionResultContract() },
    ) { granted -> onResult(granted.any { it in permissions }) }
    return remember(launcher, permissions) { { launcher.launch(permissions) } }
}
