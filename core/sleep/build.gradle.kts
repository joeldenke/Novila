plugins {
    alias(libs.plugins.novila.jvm.library)
}

dependencies {
    // A night fills in a DayEntry.
    api(projects.core.model)
    // StateFlow is part of SleepRepository's API.
    api(libs.kotlinx.coroutines.core)
}
