plugins {
    alias(libs.plugins.novila.jvm.library)
}

dependencies {
    api(projects.core.anatomy)
    api(projects.core.exercise)
}
