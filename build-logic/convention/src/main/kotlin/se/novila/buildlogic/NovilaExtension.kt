package se.novila.buildlogic

import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findByType
import javax.inject.Inject

/**
 * The `novila { }` block in a module's build file: libraries that several modules use, by what the module does
 * rather than by artifact. Each call adds the right artifacts (and compiler plugins) as `implementation`, with
 * versions from the catalog, so modules only take what they use:
 *
 * ```
 * plugins {
 *     alias(libs.plugins.novila.android.feature)
 * }
 *
 * novila {
 *     lifecycle()
 * }
 *
 * dependencies {
 *     implementation(projects.core.coach)
 * }
 * ```
 *
 * Project modules stay in `dependencies {}` with type-safe accessors; this is for external libraries.
 */
abstract class NovilaExtension @Inject constructor(private val project: Project) {
    private fun implementation(alias: String) {
        project.dependencies.add("implementation", project.libs.library(alias))
    }

    /** kotlinx.coroutines: suspend functions, Flow and StateFlow. */
    fun coroutines() = implementation("kotlinx-coroutines-core")

    /** `@Serializable` classes: the kotlinx.serialization compiler plugin and runtime, plus JSON when [json]. */
    fun serialization(json: Boolean = false) {
        project.pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
        implementation("kotlinx-serialization-core")
        if (json) implementation("kotlinx-serialization-json")
    }

    /** Navigation 3: `NavKey` routes and entry providers. Every feature has this through `novila.android.feature`. */
    fun navigation() {
        serialization()
        implementation("androidx-navigation3-runtime")
        project.dependencies.add("implementation", project.module(":core:navigation"))
    }

    /**
     * ViewModels and lifecycle-aware state collection in Compose, and ViewModel stores scoped to Navigation 3
     * entries (`rememberViewModelStoreNavEntryDecorator`).
     */
    fun lifecycle() {
        implementation("androidx-lifecycle-runtime-compose")
        implementation("androidx-lifecycle-viewmodel-compose")
        implementation("androidx-lifecycle-viewmodel-navigation3")
    }

    /**
     * Metro, compile-time dependency injection: the compiler plugin and its runtime (`@Inject`, `@SingleIn`,
     * `@ContributesBinding` …). Every module with injected classes needs it, so Metro generates their factories.
     */
    fun metro() {
        project.pluginManager.apply("dev.zacsweers.metro")
    }

    /**
     * ViewModels created by Metro: `@ViewModelKey @ContributesIntoMap(AppScope::class)` on the ViewModel and
     * `metroViewModel()` in the Navigation 3 entry, so each entry gets its own instance. Includes [metro] and
     * [lifecycle].
     */
    fun viewModels() {
        metro()
        lifecycle()
        coroutines()
        implementation("metrox-viewmodel-compose")
    }

    /** Activities with Compose content (`setContent`). */
    fun activity() = implementation("androidx-activity-compose")

    /**
     * WorkManager for work that must run later, also after a restart (the reminders), and its test helpers. See
     * *Påminnelser* in CLAUDE.md before scheduling anything new.
     */
    fun workManager() {
        implementation("androidx-work-runtime")
        project.dependencies.add("testImplementation", project.libs.library("androidx-work-testing"))
    }

    /** The Health Connect client. See *Health Connect* in CLAUDE.md before using it anywhere new. */
    fun healthConnect() = implementation("androidx-health-connect-client")
}

/** Registers `novila { }` once, whichever Novila plugins the module applies. */
internal fun Project.novilaExtension(): NovilaExtension =
    extensions.findByType<NovilaExtension>() ?: extensions.create<NovilaExtension>("novila", this)
