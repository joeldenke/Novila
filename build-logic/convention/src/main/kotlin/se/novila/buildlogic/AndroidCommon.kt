package se.novila.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.HostTestBuilder
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.Project
import java.io.File

/** SDK levels, Java level and the lint policy, the same for the app and every library. */
internal fun Project.configureAndroid(android: CommonExtension) {
    android.apply {
        compileSdk = libs.intVersion("compileSdk")
        defaultConfig.minSdk = libs.intVersion("minSdk")
        compileOptions.sourceCompatibility = NOVILA_JAVA_VERSION
        compileOptions.targetCompatibility = NOVILA_JAVA_VERSION
        lint.abortOnError = true
        lint.warningsAsErrors = false
        lint.lintConfig = lintPolicy
        // lint.xml is about app code (UI, strings, locale). Analyzing every module's tests doubled lint's work.
        lint.ignoreTestSources = true
    }
}

/**
 * Only debug variants get host tests, and no variant gets instrumented tests (CI has no emulator). Release unit
 * tests would compile and run the same tests a second time (`./gradlew test` runs every variant) without testing
 * anything new; R8 is checked by the release build. Variants that don't exist aren't configured either.
 */
internal fun ApplicationAndroidComponentsExtension.disableUnusedTests() {
    beforeVariants { variant ->
        variant.androidTest.enable = false
        if (variant.buildType == "release") variant.hostTests[HostTestBuilder.UNIT_TEST_TYPE]?.enable = false
    }
}

/** See the application variant above. */
internal fun LibraryAndroidComponentsExtension.disableUnusedTests() {
    beforeVariants { variant ->
        variant.androidTest.enable = false
        if (variant.buildType == "release") variant.hostTests[HostTestBuilder.UNIT_TEST_TYPE]?.enable = false
    }
}

/**
 * Debug builds use the key checked into the repo (same pattern as Google's Now in Android) so every debug build is
 * signed identically; it is not secret and never used for releases. The release key comes only from the environment
 * (CI secrets); without it the release build is unsigned, which is fine locally.
 */
internal fun Project.configureSigning(android: ApplicationExtension, debugKeystore: File) {
    val env = { name: String -> providers.environmentVariable(name).orNull?.takeIf { it.isNotBlank() } }
    val keystore = env("NOVILA_RELEASE_KEYSTORE")
    val storePassword = env("NOVILA_RELEASE_STORE_PASSWORD")
    val keyAlias = env("NOVILA_RELEASE_KEY_ALIAS")
    val keyPassword = env("NOVILA_RELEASE_KEY_PASSWORD")
    android.signingConfigs {
        getByName("debug") {
            storeFile = debugKeystore
            this.storePassword = "android"
            this.keyAlias = "androiddebugkey"
            this.keyPassword = "android"
        }
        if (keystore != null && storePassword != null && keyAlias != null && keyPassword != null) {
            create("release") {
                storeFile = file(keystore)
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }
}
