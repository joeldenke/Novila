plugins {
    alias(libs.plugins.novila.jvm.library)
}

dependencies {
    api(projects.core.anatomy)
    api(projects.core.exercise)
    // The status that can lower today's intensity (RecoveryStatus).
    api(projects.core.recovery)
    // The unified daily coach reads the day's logs (DayEntry, Goals) and the training of today (TrainingToday).
    api(projects.core.model)
    // Its bedtime suggestion comes from the sleep coach's goal (SleepCoachPreferences).
    api(projects.core.sleep)
}
