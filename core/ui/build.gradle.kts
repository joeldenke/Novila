plugins {
    alias(libs.plugins.novila.android.library)
    alias(libs.plugins.novila.android.compose)
    alias(libs.plugins.novila.android.unittest)
}

dependencies {
    api(projects.core.model)
    api(projects.core.movement)
    implementation(projects.core.designsystem)
}
