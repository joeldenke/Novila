plugins {
    alias(libs.plugins.novila.android.library)
    alias(libs.plugins.novila.android.compose)
    alias(libs.plugins.novila.android.unittest)
}

dependencies {
    // api: NovilaTheme exposes Material 3 types (ColorScheme, Typography, Shapes) and Compose UI
    // types in its public API, so consumers need them on their compile classpath.
    api(platform(libs.compose.bom))
    api(libs.compose.material3)
    api(libs.compose.ui)
    // NovilaGlanceTheme: the same color schemes for the home-screen widget. Only modules with a widget use it.
    implementation(libs.androidx.glance.material3)
}
