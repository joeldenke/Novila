plugins {
    alias(libs.plugins.novila.android.feature)
}

novila {
    viewModels()
}

dependencies {
    implementation(projects.core.data)
}
