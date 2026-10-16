plugins {
    alias(libs.plugins.novila.android.library)
    alias(libs.plugins.novila.android.compose)
}

dependencies {
    api(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
}
