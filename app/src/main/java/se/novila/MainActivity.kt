package se.novila

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import se.novila.model.withGlassOfWater
import java.time.LocalDate
import java.time.LocalTime

class MainActivity : ComponentActivity() {
    /** The launcher shortcut the app was opened with, until the tabs have shown where it leads. */
    private var shortcut by mutableStateOf<AppShortcut?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val graph = (application as NovilaApplication).graph
        // A restored activity already showed its shortcut; a new start publishes them in the current language.
        if (savedInstanceState == null) {
            AppShortcut.publish(this)
            open(AppShortcut.of(intent))
        }
        val viewModelFactory = graph.metroViewModelFactory
        val settingsStore = graph.settingsStore
        // The widget redraws when the app saves something from here on.
        graph.widgetUpdates.start()
        setContent {
            // Nothing to show until the settings are read, a moment at app start, so neither the theme nor the intro
            // flickers.
            val settings = settingsStore.settings.collectAsStateWithLifecycle().value ?: return@setContent
            val darkTheme = settings.theme.isDark(systemDark = isSystemInDarkTheme())
            // The system bars follow the app's theme, not the phone's, when the user picked light or dark.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(LightScrim, DarkScrim) { darkTheme },
                )
                onDispose { }
            }
            // metroViewModel() in each Navigation 3 entry creates its ViewModels with this factory.
            CompositionLocalProvider(LocalMetroViewModelFactory provides viewModelFactory) {
                NovilaApp(
                    darkTheme = darkTheme,
                    dynamicColor = settings.dynamicColor,
                    onboarded = settings.onboarded,
                    entryLayout = settings.entryLayout,
                    shortcut = shortcut,
                    onShortcutShown = { shortcut = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        open(AppShortcut.of(intent))
    }

    /** Does what [opened] is for: logging water logs a glass right away, and the tabs then show where it leads. */
    private fun open(opened: AppShortcut?) {
        if (opened == AppShortcut.LogWater) {
            (application as NovilaApplication).graph.healthStore.update(LocalDate.now()) {
                it.withGlassOfWater(LocalTime.now())
            }
        }
        shortcut = opened
    }

    override fun onStop() {
        super.onStop()
        // Health Connect is read again when the app comes back: the user may have taken access away meanwhile. Within
        // the app, moving between tabs reuses the last reading for a little while.
        if (!isChangingConfigurations) {
            val graph = (application as NovilaApplication).graph
            graph.nights.expire()
            graph.exercises.expire()
            graph.recovery.expire()
        }
    }

    private companion object {
        // The scrims enableEdgeToEdge() uses by default, for three-button navigation.
        val LightScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DarkScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
