plugins {
    alias(libs.plugins.novila.jvm.library)
}

dependencies {
    // StateFlow is part of MovementRepository's API.
    api(libs.kotlinx.coroutines.core)
}
