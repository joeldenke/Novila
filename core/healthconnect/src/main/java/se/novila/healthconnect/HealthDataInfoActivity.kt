package se.novila.healthconnect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.res.stringResource
import se.novila.designsystem.component.NovilaScaffold
import se.novila.designsystem.component.NovilaTopAppBar
import se.novila.designsystem.theme.NovilaTheme
import se.novila.healthconnect.R

/**
 * Opened by Health Connect from the privacy link in its permission dialog (see AndroidManifest.xml).
 * Exported, so it shows static text only and ignores the intent it was started with.
 */
class HealthDataInfoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NovilaTheme {
                NovilaScaffold(topBar = { NovilaTopAppBar(title = stringResource(R.string.health_info_title)) }) {
                    HealthDataInfoScreen(
                        onOpenHealthSettings = { HealthConnectSource.openSettings(this) },
                        contentPadding = it,
                    )
                }
            }
        }
    }
}
