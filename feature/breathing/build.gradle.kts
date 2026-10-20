plugins {
    alias(libs.plugins.novila.android.feature)
}

novila {
    viewModels()
}

dependencies {
    // The sessions' phases and timings: pure Kotlin, tested without Android.
    implementation(projects.core.breathing)
}
