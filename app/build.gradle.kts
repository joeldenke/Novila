plugins {
    alias(libs.plugins.novila.android.application)
    alias(libs.plugins.novila.android.compose)
    alias(libs.plugins.novila.android.unittest)
}

novila {
    activity()
    coroutines()
    healthConnect()
    lifecycle()
    navigation()
}

dependencies {
    implementation(projects.feature.today)
    implementation(projects.feature.training)
    implementation(projects.feature.sleep)
    implementation(projects.feature.nutrition)

    implementation(projects.core.coach)
    implementation(projects.core.data)
    implementation(projects.core.designsystem)
    implementation(projects.core.exercise)
    implementation(projects.core.healthconnect)
    implementation(projects.core.model)
    implementation(projects.core.movement)
    implementation(projects.core.ui)

    implementation(libs.androidx.navigation3.ui)
}
