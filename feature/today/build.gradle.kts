plugins {
    alias(libs.plugins.novila.android.feature)
}

novila {
    viewModels()
}

dependencies {
    implementation(projects.core.data)
    // Nights a watch recorded fill in the dashboard's sleep.
    implementation(projects.core.sleep)
}
