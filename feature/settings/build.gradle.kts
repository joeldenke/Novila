plugins {
    alias(libs.plugins.novila.android.feature)
}

novila {
    viewModels()
}

dependencies {
    implementation(projects.core.data)
    // Health Connect's permission dialog, its settings and the screen that says what Novila reads.
    implementation(projects.core.healthconnect)
}
