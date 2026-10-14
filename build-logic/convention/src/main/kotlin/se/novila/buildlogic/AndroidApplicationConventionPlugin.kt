package se.novila.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** `novila.android.application`: SDK levels, Java level and lint policy for the app module. */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")

            extensions.configure<ApplicationExtension> {
                compileSdk = libs.intVersion("compileSdk")
                defaultConfig {
                    minSdk = libs.intVersion("minSdk")
                    targetSdk = libs.intVersion("targetSdk")
                }
                compileOptions {
                    sourceCompatibility = NOVILA_JAVA_VERSION
                    targetCompatibility = NOVILA_JAVA_VERSION
                }
                lint {
                    abortOnError = true
                    checkReleaseBuilds = true
                    warningsAsErrors = false
                    lintConfig = lintPolicy
                }
            }
        }
    }
}
