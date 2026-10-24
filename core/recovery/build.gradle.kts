plugins {
    alias(libs.plugins.novila.jvm.library)
}

dependencies {
    // StateFlow is part of RecoveryRepository's API.
    api(libs.kotlinx.coroutines.core)
    // The personal baseline the readings are judged against.
    implementation(projects.core.science)
}
