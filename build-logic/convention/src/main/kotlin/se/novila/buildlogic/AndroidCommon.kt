package se.novila.buildlogic

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.HostTestBuilder
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.Project

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
