package se.novila.healthconnect

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.health.connect.client.PermissionController

/**
 * Health Connect's own permission dialog for [MOVEMENT_PERMISSIONS], which links to [HealthDataInfoActivity] for the
 * details. Returns the function that opens it; [onResult] gets whether the user gave access to anything.
 */
@Composable
fun rememberMovementPermissionRequest(onResult: (granted: Boolean) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        remember { PermissionController.createRequestPermissionResultContract() },
    ) { granted -> onResult(granted.any { it in MOVEMENT_PERMISSIONS }) }
    return remember(launcher) { { launcher.launch(MOVEMENT_PERMISSIONS) } }
}
