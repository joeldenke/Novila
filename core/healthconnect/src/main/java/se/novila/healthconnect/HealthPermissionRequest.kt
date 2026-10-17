package se.novila.healthconnect

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.health.connect.client.PermissionController

/**
 * Health Connect's own permission dialog for [HEALTH_PERMISSIONS] (steps, active calories and workouts), which links
 * to [HealthDataInfoActivity] for the details. Health Connect only shows what isn't granted yet. Returns the function
 * that opens it; [onResult] gets whether the user gave access to anything.
 */
@Composable
fun rememberHealthPermissionRequest(onResult: (granted: Boolean) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        remember { PermissionController.createRequestPermissionResultContract() },
    ) { granted -> onResult(granted.any { it in HEALTH_PERMISSIONS }) }
    return remember(launcher) { { launcher.launch(HEALTH_PERMISSIONS) } }
}
