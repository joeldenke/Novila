plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

/**
 * versionCode is set by CI (VERSION_CODE = workflow run number) so every Firebase
 * distribution gets a unique, increasing number. Falls back to 1 for local builds.
 */
val ciVersionCode: Int = providers.environmentVariable("VERSION_CODE").orNull?.toIntOrNull() ?: 1

android {
    namespace = "se.novila"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "se.novila"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = ciVersionCode
        versionName = "0.1.$ciVersionCode"
    }

    // Debug key checked into the repo (same pattern as Google's Now in Android):
    // every CI build is signed identically, so testers can update without reinstalling.
    // It is NOT secret and must never be used for Play releases.
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
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
            // Unsigned for now: the release key goes into CI secrets once the app ships to Play.
            // PR checks build release unsigned to catch R8 issues early.
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    androidResources {
        // Per-app language support (Android 13+), generated from the res/values-* folders.
        generateLocaleConfig = true
        localeFilters += listOf("en", "sv")
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        warningsAsErrors = false
        // Dependency updates are handled by Dependabot; don't fail lint on them.
        disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "NewerVersionAvailable")
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
}
