plugins {
    alias(libs.plugins.novila.android.library)
    alias(libs.plugins.novila.android.compose)
    alias(libs.plugins.novila.android.unittest)
}

dependencies {
    // api: the public API exposes Compose UI and Foundation types (Modifier, TextStyle, shapes, RowScope).
    api(platform(libs.compose.bom))
    api(libs.compose.foundation)
    api(libs.compose.ui)
    // implementation: Material 3 stays inside this module. NovilaTheme and the Novila* components expose only
    // Novila types, so other modules can't import Material and it can be replaced component by component.
    implementation(libs.compose.material3)
    // NovilaGlanceTheme: the same color schemes for the home-screen widget. Only modules with a widget use it.
    implementation(libs.androidx.glance.material3)
}
