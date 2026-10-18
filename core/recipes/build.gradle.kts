plugins {
    alias(libs.plugins.novila.jvm.library)
}

dependencies {
    // A recipe fills food groups in a DayEntry and is chosen by what it adds to the nutrition score.
    api(projects.core.model)
}
