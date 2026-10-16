plugins {
    alias(libs.plugins.novila.android.library)
    alias(libs.plugins.novila.android.unittest)
}

novila {
    coroutines()
    metro()
}

dependencies {
    api(projects.core.model)
    api(projects.core.coach)
    implementation(libs.androidx.datastore.preferences)
    // Gemini Nano on the device, for the coach's message. See GeminiNanoCoachModel.
    implementation(libs.mlkit.genai.prompt)
}
