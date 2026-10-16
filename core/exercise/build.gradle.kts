plugins {
    alias(libs.plugins.novila.jvm.library)
}

dependencies {
    // The strength sessions are checked against the exercise library.
    testImplementation(projects.core.anatomy)
}
