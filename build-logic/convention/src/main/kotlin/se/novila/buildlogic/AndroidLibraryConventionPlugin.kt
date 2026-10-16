package se.novila.buildlogic

import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * `novila.android.library`: an Android library with the same SDK, Java and lint setup as the app. The namespace
 * comes from the module path (see [novilaNamespace]), so the module's build file needs no `android {}` block.
 */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            novilaExtension()

            extensions.configure<LibraryExtension> {
                configureAndroid(this)
                namespace = novilaNamespace
                lint.targetSdk = libs.intVersion("targetSdk")
            }
            extensions.configure<LibraryAndroidComponentsExtension> {
                disableUnusedTests()
            }
        }
    }
}
