plugins {
    alias(libs.plugins.novila.jvm.library)
}

dependencies {
    // The status that can lower today's intensity (RecoveryStatus).
    api(projects.core.recovery)
    // StateFlow is part of ExerciseRepository's API.
    api(libs.kotlinx.coroutines.core)
    // The exponentially weighted usual week and Foster's monotony.
    implementation(projects.core.science)
    // The strength sessions are checked against the exercise library.
    testImplementation(projects.core.anatomy)
}
