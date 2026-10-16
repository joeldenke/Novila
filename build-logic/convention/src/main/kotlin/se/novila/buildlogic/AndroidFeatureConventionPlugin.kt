package se.novila.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * `novila.android.feature`: a feature module (`:feature:<tab>`). One per tab, never depending on another feature.
 * A feature owns its screens, its strings and its navigation: `@Serializable` routes (`NavKey`) and an
 * `EntryProviderScope<NavKey>.<feature>Entries(...)` function that the app adds to its `NavDisplay`. Everything it
 * shows comes in as parameters, so the screens stay stateless and previewable; every `@Preview` is a screenshot
 * test.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(AndroidLibraryConventionPlugin::class.java)
            pluginManager.apply(AndroidComposeConventionPlugin::class.java)
            pluginManager.apply(AndroidScreenshotConventionPlugin::class.java)
            novilaExtension().navigation()

            // Only what every feature needs. Anything else the module asks for in `novila { }` or `dependencies { }`.
            dependencies {
                add("implementation", module(":core:designsystem"))
                add("implementation", module(":core:ui"))
            }
        }
    }
}
