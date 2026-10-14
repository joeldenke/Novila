package se.novila.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.TestOptions
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * `novila.android.unittest`: JVM unit tests with Robolectric, so Android and Compose UI tests
 * run in CI without an emulator. Adds Compose UI test APIs when the module uses Compose,
 * and Kover for coverage.
 */
class AndroidUnitTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlinx.kover")

            pluginManager.withPlugin("com.android.application") {
                extensions.configure<ApplicationExtension> { testOptions.configureUnitTests() }
            }
            pluginManager.withPlugin("com.android.library") {
                extensions.configure<LibraryExtension> { testOptions.configureUnitTests() }
            }

            dependencies {
                add("testImplementation", libs.findLibrary("junit").get())
                add("testImplementation", libs.findLibrary("robolectric").get())
            }
            pluginManager.withPlugin("org.jetbrains.kotlin.plugin.compose") {
                dependencies {
                    add("testImplementation", platform(libs.findLibrary("compose-bom").get()))
                    add("testImplementation", libs.findLibrary("compose-ui-test-junit4").get())
                    // Registers the empty activity that createComposeRule() launches.
                    add("debugImplementation", libs.findLibrary("compose-ui-test-manifest").get())
                }
            }
        }
    }

    private fun TestOptions.configureUnitTests() {
        // Robolectric needs merged resources, manifests and assets to inflate real UI.
        unitTests.isIncludeAndroidResources = true
        unitTests.all { test ->
            // Real Skia rendering instead of no-op drawing: required for screenshots, and makes
            // layout/drawing tests meaningful.
            test.systemProperty("robolectric.graphicsMode", "NATIVE")
            // Robolectric 4.17 reaches into jdk.internal.access for SDK 36+ on JDK 17+; without this
            // every Robolectric test fails with IllegalAccessException (robolectric#11434).
            test.jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
        }
    }
}
