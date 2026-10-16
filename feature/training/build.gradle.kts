plugins {
    alias(libs.plugins.novila.android.feature)
}

dependencies {
    implementation(projects.core.anatomy)
    implementation(projects.core.coach)
    implementation(projects.core.exercise)
}
