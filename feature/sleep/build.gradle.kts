plugins {
    alias(libs.plugins.novila.android.feature)
}

novila {
    viewModels()
}

dependencies {
    implementation(projects.core.data)
    // Nights a watch recorded, and Health Connect's permission dialog and settings, opened from the sleep card.
    implementation(projects.core.healthconnect)
    implementation(projects.core.sleep)
}
