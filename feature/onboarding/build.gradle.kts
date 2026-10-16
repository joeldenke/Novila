plugins {
    alias(libs.plugins.novila.android.feature)
}

novila {
    // System back steps through the intro's pages (BackHandler).
    activity()
    viewModels()
}

dependencies {
    // The coach's goals and time, saved when the intro is done.
    implementation(projects.core.coach)
    implementation(projects.core.data)
    // Health Connect's permission dialog, its settings and install page.
    implementation(projects.core.healthconnect)
}
