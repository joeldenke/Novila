plugins {
    alias(libs.plugins.novila.android.library)
    alias(libs.plugins.novila.android.compose)
    alias(libs.plugins.novila.android.unittest)
}

novila {
    coroutines()
    glance()
    metro()
}

dependencies {
    implementation(projects.core.data)
    // NovilaGlanceTheme: the app's colors and tokens in Glance's form.
    implementation(projects.core.designsystem)
    // Shared units, such as "7.5 h".
    implementation(projects.core.ui)
}
