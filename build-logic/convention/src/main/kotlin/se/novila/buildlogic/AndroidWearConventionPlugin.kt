package se.novila.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * `novila.android.wear`: the Wear OS app in `:wear`, a separate app next to the phone app. It reuses the same SDK,
 * Java level, lint policy and signing setup, but has its own application id (`se.novila.wear`) and a minimum of
 * Wear OS 3 (API 30), the oldest version Compose for Wear OS supports well. `:wear` applies
 * `novila.android.compose` and `novila.android.unittest` itself, like `:app`.
 */
class AndroidWearConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            novilaExtension()

            extensions.configure<ApplicationExtension> {
                configureAndroid(this)
                namespace = novilaNamespace
                defaultConfig {
                    applicationId = WEAR_APPLICATION_ID
                    minSdk = WEAR_MIN_SDK
                    targetSdk = libs.intVersion("targetSdk")
                    val versionCode = providers.environmentVariable("VERSION_CODE").orNull?.toIntOrNull() ?: 1
                    this.versionCode = versionCode
                    versionName = "$VERSION_NAME_PREFIX.$versionCode"
                }
                // The same reviewed debug key as the phone app; it is not secret and only signs debug builds.
                configureSigning(this, isolated.rootProject.projectDirectory.file("app/debug.keystore").asFile)
                buildTypes {
                    getByName("debug") {
                        applicationIdSuffix = ".debug"
                        versionNameSuffix = "-debug"
                    }
                    getByName("release") {
                        isMinifyEnabled = true
                        isShrinkResources = true
                        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
                        signingConfig = signingConfigs.findByName("release")
                    }
                }
                androidResources {
                    generateLocaleConfig = true
                    localeFilters += listOf("en", "sv")
                }
                packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
                lint {
                    abortOnError = true
                    warningsAsErrors = false
                    lintConfig = lintPolicy
                    ignoreTestSources = true
                }
            }
            extensions.configure<ApplicationAndroidComponentsExtension> { disableUnusedTests() }
        }
    }

    private companion object {
        const val WEAR_APPLICATION_ID = "se.novila.wear"

        /** Wear OS 3 is API 30; the watch app uses nothing older. */
        const val WEAR_MIN_SDK = 30

        /** The same milestone prefix as the phone app. */
        const val VERSION_NAME_PREFIX = "0.1"
    }
}
