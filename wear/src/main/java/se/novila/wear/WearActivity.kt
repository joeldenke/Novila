package se.novila.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.metroViewModel

/** The watch app's only activity: one workout screen, created by Metro and scoped to it. */
class WearActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val viewModelFactory = (application as WearApplication).graph.metroViewModelFactory
        setContent {
            CompositionLocalProvider(LocalMetroViewModelFactory provides viewModelFactory) {
                NovilaWearTheme {
                    val viewModel = metroViewModel<WearWorkoutViewModel>()
                    val state by viewModel.state.collectAsStateWithLifecycle()
                    WearWorkoutScreen(
                        state = state,
                        onLevel = viewModel::setLevel,
                        onSession = viewModel::setSecond,
                        onStart = viewModel::start,
                        onNext = viewModel::next,
                        onNewSession = viewModel::reset,
                    )
                }
            }
        }
    }
}
