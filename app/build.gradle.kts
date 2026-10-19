plugins {
    alias(libs.plugins.novila.android.application)
    alias(libs.plugins.novila.android.compose)
    alias(libs.plugins.novila.android.unittest)
}

novila {
    activity()
    navigation()
    // The Metro graph (AppGraph) and the ViewModel factory every entry uses.
    viewModels()
    // WorkManager's configuration, with the reminders' worker factory (NovilaApplication).
    workManager()
}

dependencies {
    implementation(projects.feature.today)
    implementation(projects.feature.training)
    implementation(projects.feature.sleep)
    implementation(projects.feature.nutrition)
    implementation(projects.feature.onboarding)
    implementation(projects.feature.settings)
    implementation(projects.feature.widget)

    // Everything that contributes to the graph must be on the app's classpath, so Metro can find it.
    implementation(projects.core.coach)
    implementation(projects.core.data)
    implementation(projects.core.designsystem)
    implementation(projects.core.exercise)
    implementation(projects.core.healthconnect)
    implementation(projects.core.model)
    implementation(projects.core.movement)
    implementation(projects.core.recovery)
    implementation(projects.core.reminders)
    implementation(projects.core.sleep)
    implementation(projects.core.ui)

    implementation(libs.androidx.navigation3.ui)
}
