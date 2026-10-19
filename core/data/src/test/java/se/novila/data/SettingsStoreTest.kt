package se.novila.data

import android.content.Context
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import se.novila.model.AppSettings
import se.novila.model.ThemeMode

@RunWith(RobolectricTestRunner::class)
class SettingsStoreTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val stores = StoreScope()

    @After fun close() = stores.close()

    @Test fun `defaults until something is chosen`() {
        assertEquals(AppSettings(), SettingsStore(context, stores.scope, stores.cipher).settings.await())
    }

    @Test fun `chosen settings survive a restart`() {
        val store = SettingsStore(context, stores.scope, stores.cipher)
        store.setTheme(ThemeMode.Dark)
        store.setDynamicColor(true)
        val chosen = AppSettings(theme = ThemeMode.Dark, dynamicColor = true)
        store.settings.await { it == chosen }

        stores.restart()
        assertEquals(chosen, SettingsStore(context, stores.scope, stores.cipher).settings.await())
    }

    @Test fun `the intro stays done after a restart`() {
        val store = SettingsStore(context, stores.scope, stores.cipher)
        store.setOnboarded(true)
        store.settings.await { it.onboarded }

        stores.restart()
        assertEquals(true, SettingsStore(context, stores.scope, stores.cipher).settings.await().onboarded)
    }
}
