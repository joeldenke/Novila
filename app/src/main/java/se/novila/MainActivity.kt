package se.novila

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val viewModelFactory = (application as NovilaApplication).graph.metroViewModelFactory
        setContent {
            // metroViewModel() in each Navigation 3 entry creates its ViewModels with this factory.
            CompositionLocalProvider(LocalMetroViewModelFactory provides viewModelFactory) { NovilaApp() }
        }
    }
}
