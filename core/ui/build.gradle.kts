plugins {
    alias(libs.plugins.novila.android.library)
    alias(libs.plugins.novila.android.compose)
    alias(libs.plugins.novila.android.unittest)
}

dependencies {
    api(projects.core.model)
    api(projects.core.movement)
    // Diet and allergen names, shared by Settings and Nutrition.
    api(projects.core.recipes)
    implementation(projects.core.designsystem)
}
