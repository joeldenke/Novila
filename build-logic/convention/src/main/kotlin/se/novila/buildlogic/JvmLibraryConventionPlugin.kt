package se.novila.buildlogic

import com.android.build.api.dsl.Lint
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * `novila.jvm.library`: plain Kotlin with no Android, for logic that can be tested without Android (models,
 * planning, anatomy). Compiles and tests in seconds, without resource merging or Robolectric. Android lint still
 * checks it (`com.android.lint`), as part of the app's lint run, and Kover measures its tests.
 */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            pluginManager.apply("com.android.lint")
            novilaExtension()

            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = NOVILA_JAVA_VERSION
                targetCompatibility = NOVILA_JAVA_VERSION
            }
            extensions.configure<KotlinJvmProjectExtension> {
                compilerOptions.jvmTarget.set(JvmTarget.fromTarget(NOVILA_JAVA_VERSION.toString()))
            }
            extensions.configure<Lint> {
                abortOnError = true
                warningsAsErrors = false
                lintConfig = lintPolicy
                ignoreTestSources = true
            }
            configureKover("jvm")
            tasks.withType<Test>().configureEach {
                useJUnit()
                jvmArgs(TEST_JVM_ARGS)
            }
            dependencies {
                add("testImplementation", libs.library("junit"))
            }
        }
    }
}
