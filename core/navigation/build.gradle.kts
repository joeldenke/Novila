plugins {
    alias(libs.plugins.novila.android.library)
    alias(libs.plugins.novila.android.compose)
    alias(libs.plugins.novila.android.unittest)
}

novila {
    // A ViewModelStore per entry (rememberViewModelStoreNavEntryDecorator), see NavigationState.toEntries.
    lifecycle()
}

dependencies {
    api(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
}
