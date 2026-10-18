plugins {
    alias(libs.plugins.novila.jvm.library)
}

dependencies {
    api(projects.core.anatomy)
    api(projects.core.exercise)
    // The status that can lower today's intensity (RecoveryStatus).
    api(projects.core.recovery)
}
