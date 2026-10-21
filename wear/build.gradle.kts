plugins {
    alias(libs.plugins.novila.android.wear)
    alias(libs.plugins.novila.android.compose)
    // Unit and Compose UI tests, and every @Preview as a Roborazzi screenshot on a round watch.
    alias(libs.plugins.novila.android.screenshot)
}

novila {
    activity()
    // The one ViewModel, created by Metro and scoped to the activity.
    viewModels()
    // Compose for Wear OS: Material 3 and the scaling layouts for a round watch.
    wear()
}

dependencies {
    // Novila's colors, type and shapes, so the watch looks like the phone app.
    implementation(projects.core.designsystem)
    // Workout, WorkoutStep and StandaloneSession: the same steps, rests and countdown as the phone.
    implementation(projects.core.exercise)

    // Steps the workout timer without waiting in the ViewModel's test.
    testImplementation(libs.kotlinx.coroutines.test)
}
