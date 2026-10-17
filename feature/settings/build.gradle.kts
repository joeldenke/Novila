plugins {
    alias(libs.plugins.novila.android.feature)
}

novila {
    // The permission dialog for notifications (rememberLauncherForActivityResult, LocalActivity).
    activity()
    viewModels()
}

dependencies {
    implementation(projects.core.data)
    // Health Connect's permission dialog, its settings and the screen that says what Novila reads.
    implementation(projects.core.healthconnect)
    // Planning the reminders' work and checking whether Novila may notify.
    implementation(projects.core.reminders)
}
