plugins {
    alias(libs.plugins.novila.android.library)
    alias(libs.plugins.novila.android.compose)
    alias(libs.plugins.novila.android.screenshot)
}

novila {
    activity()
    healthConnect()
    // The reads are suspend functions; coroutines is also on the test classpath for runBlocking.
    coroutines()
}

dependencies {
    api(projects.core.exercise)
    api(projects.core.movement)
    api(projects.core.recovery)
    api(projects.core.sleep)
    implementation(projects.core.designsystem)
    implementation(projects.core.ui)
}
