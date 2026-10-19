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
    api(projects.core.sleep)
    // The status that can lower today's intensity (RecoveryStatus) in trainingWeek.
    api(projects.core.recovery)
    // The user's diet and allergens for recipe suggestions (FoodPreferencesStore).
    api(projects.core.recipes)
    implementation(libs.androidx.datastore.preferences)
    // Every preference file is sealed with a key from the Android Keystore: datastore-tink's
    // AeadSerializer plus Tink itself (Aead, AndroidKeysetManager). See EncryptingPreferencesSerializer.
    implementation(libs.androidx.datastore.tink)
    implementation(libs.tink.android)
    // Gemini Nano on the device, for the coach's message. See GeminiNanoCoachModel.
    implementation(libs.mlkit.genai.prompt)
    // Restores a real DataStore file with the device-side actions backup tests use, see EncryptedDataStoreRestoreTest.
    testImplementation(libs.androidx.test.backup)
}
