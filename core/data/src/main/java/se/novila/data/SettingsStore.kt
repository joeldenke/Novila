package se.novila.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import se.novila.model.AppSettings
import se.novila.model.ThemeMode

/**
 * The app's settings in Preferences DataStore (file `settings`), on the device only. One instance for the whole app,
 * so the theme in `MainActivity` changes as soon as the Settings screen saves, and the app opens as soon as the intro
 * is done.
 */
@SingleIn(AppScope::class)
class SettingsStore internal constructor(context: Context, scope: CoroutineScope, cipher: DatastoreCipher) {
    @Inject constructor(context: Context, cipher: DatastoreCipher) : this(context, storeScope(), cipher)

    private val store = PreferencesStore(context, NAME, scope, cipher)

    /** The saved settings, kept in memory. Null until the first read is done. */
    val settings: StateFlow<AppSettings?> = store.snapshots {
        AppSettings(
            theme = ThemeMode.fromName(it[THEME]),
            dynamicColor = it[DYNAMIC_COLOR] ?: false,
            onboarded = it[ONBOARDED] ?: false,
        )
    }

    fun setTheme(theme: ThemeMode) = store.edit { it[THEME] = theme.name }

    fun setDynamicColor(enabled: Boolean) = store.edit { it[DYNAMIC_COLOR] = enabled }

    /** The intro is done, and the app opens on Today; or, from Settings, not done, to show it again. */
    fun setOnboarded(done: Boolean) = store.edit { it[ONBOARDED] = done }

    private companion object {
        const val NAME = "settings"
        val THEME = stringPreferencesKey("theme")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val ONBOARDED = booleanPreferencesKey("onboarded")
    }
}
