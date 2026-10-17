plugins {
    alias(libs.plugins.novila.android.library)
    alias(libs.plugins.novila.android.compose)
    alias(libs.plugins.novila.android.screenshot)
}

novila {
    activity()
    healthConnect()
}

dependencies {
    api(projects.core.exercise)
    api(projects.core.movement)
    implementation(projects.core.designsystem)
    implementation(projects.core.ui)
}
