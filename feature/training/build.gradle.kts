plugins {
    alias(libs.plugins.novila.android.feature)
}

novila {
    viewModels()
}

dependencies {
    implementation(projects.core.anatomy)
    implementation(projects.core.coach)
    implementation(projects.core.data)
    implementation(projects.core.exercise)
    // Health Connect's permission dialog and settings, opened from the movement card.
    implementation(projects.core.healthconnect)
    // The watch's resting heart rate and heart rate variability, shown on the recovery card.
    implementation(projects.core.recovery)
}
