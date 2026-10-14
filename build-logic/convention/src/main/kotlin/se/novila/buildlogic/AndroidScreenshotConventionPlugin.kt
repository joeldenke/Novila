package se.novila.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * `novila.android.screenshot`: Roborazzi screenshot tests on Robolectric (no emulator), on top
 * of `novila.android.unittest`. The module's `roborazzi {}` block turns its `@Preview`s into tests
 * and stores reference images in `src/test/screenshots`; record them in CI by commenting
 * `/record-screenshots` on a PR.
 *
 * Tasks: `recordRoborazziDebug` (write references), `verifyRoborazziDebug` (fail on diff),
 * `compareRoborazziDebug` (write diff images without failing).
 *
 * Roborazzi is configured in the module rather than here so build-logic needs no compile
 * dependency on the Roborazzi plugin, which may be built with a newer Kotlin than Gradle embeds.
 */
class AndroidScreenshotConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(AndroidUnitTestConventionPlugin::class.java)
            pluginManager.apply("io.github.takahirom.roborazzi")

            dependencies {
                add("testImplementation", libs.findLibrary("roborazzi").get())
                add("testImplementation", libs.findLibrary("roborazzi-compose").get())
                add("testImplementation", libs.findLibrary("roborazzi-junit-rule").get())
                add("testImplementation", libs.findLibrary("roborazzi-compose-preview-scanner-support").get())
                add("testImplementation", libs.findLibrary("composable-preview-scanner").get())
            }
        }
    }
}
