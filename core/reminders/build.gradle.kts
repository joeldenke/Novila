plugins {
    alias(libs.plugins.novila.android.library)
    alias(libs.plugins.novila.android.unittest)
}

novila {
    coroutines()
    metro()
    workManager()
}

dependencies {
    api(projects.core.data)
    // The pillar icons, as the notifications' small icons.
    implementation(projects.core.ui)
    // NotificationCompat and NotificationManagerCompat.
    implementation(libs.androidx.core.ktx)
}
