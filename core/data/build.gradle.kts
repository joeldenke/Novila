plugins {
    alias(libs.plugins.novila.android.library)
}

novila {
    coroutines()
}

dependencies {
    api(projects.core.model)
    api(projects.core.coach)
    // Gemini Nano on the device, for the coach's message. See GeminiNanoCoachModel.
    implementation(libs.mlkit.genai.prompt)
}
