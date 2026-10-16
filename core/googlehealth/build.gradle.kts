plugins {
    alias(libs.plugins.novila.jvm.library)
}

novila {
    coroutines()
    serialization(json = true)
}

dependencies {
    api(projects.core.exercise)
}
