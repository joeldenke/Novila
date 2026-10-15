plugins {
    alias(libs.plugins.novila.android.application)
    alias(libs.plugins.novila.android.compose)
    alias(libs.plugins.novila.android.screenshot)
    alias(libs.plugins.kotlin.serialization)
}

/**
 * versionCode is set by CI (VERSION_CODE = workflow run number) so every Firebase
 * distribution gets a unique, increasing number. Falls back to 1 for local builds.
 */
val ciVersionCode: Int = providers.environmentVariable("VERSION_CODE").orNull?.toIntOrNull() ?: 1

/**
 * Release signing comes only from the environment (CI secrets). The keystore is never in the repo.
 * Without these variables the release build is unsigned, which is fine for local builds.
 */
val releaseKeystore: String? = providers.environmentVariable("NOVILA_RELEASE_KEYSTORE").orNull
val releaseStorePassword: String? = providers.environmentVariable("NOVILA_RELEASE_STORE_PASSWORD").orNull
val releaseKeyAlias: String? = providers.environmentVariable("NOVILA_RELEASE_KEY_ALIAS").orNull
val releaseKeyPassword: String? = providers.environmentVariable("NOVILA_RELEASE_KEY_PASSWORD").orNull
val hasReleaseSigning = listOf(releaseKeystore, releaseStorePassword, releaseKeyAlias, releaseKeyPassword)
    .all { !it.isNullOrBlank() }

android {
    namespace = "se.novila"

    defaultConfig {
        applicationId = "se.novila"
        versionCode = ciVersionCode
        versionName = "0.1.$ciVersionCode"
    }

    signingConfigs {
        // Debug key checked into the repo (same pattern as Google's Now in Android) so every
        // debug build is signed identically. It is NOT secret and is never used for releases.
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseKeystore!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    androidResources {
        // Per-app language support (Android 13+), generated from the res/values-* folders.
        generateLocaleConfig = true
        localeFilters += listOf("en", "sv")
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

// Every @Preview in the app becomes a screenshot test. See novila.android.screenshot.
roborazzi {
    outputDir.set(layout.projectDirectory.dir("src/test/screenshots"))
    generateComposePreviewRobolectricTests {
        enable = true
        packages = listOf("se.novila")
        includePrivatePreviews = true
    }
}

dependencies {
    implementation(projects.core.designsystem)
    // Coverage report for the app includes the modules it depends on.
    kover(projects.core.designsystem)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.health.connect.client)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.compose.ui)
}
