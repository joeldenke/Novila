plugins {
    alias(libs.plugins.novila.jvm.library)
}

dependencies {
    // StateFlow is part of ExerciseRepository's API.
    api(libs.kotlinx.coroutines.core)
    // The strength sessions are checked against the exercise library.
    testImplementation(projects.core.anatomy)
}
