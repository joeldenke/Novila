package se.novila.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** `novila.android.library`: the same SDK, Java and lint setup as the app, for library modules. */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")

            extensions.configure<LibraryExtension> {
                compileSdk = libs.intVersion("compileSdk")
                defaultConfig {
                    minSdk = libs.intVersion("minSdk")
                }
                compileOptions {
                    sourceCompatibility = NOVILA_JAVA_VERSION
                    targetCompatibility = NOVILA_JAVA_VERSION
                }
                lint {
                    abortOnError = true
                    warningsAsErrors = false
                    targetSdk = libs.intVersion("targetSdk")
                    lintConfig = lintPolicy
                }
            }
        }
    }
}
