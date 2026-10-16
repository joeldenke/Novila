package se.novila.buildlogic

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/** Java level for app code. The built-in Kotlin compiler follows it as its JVM target. */
internal val NOVILA_JAVA_VERSION = JavaVersion.VERSION_17

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.intVersion(alias: String): Int = findVersion(alias).get().requiredVersion.toInt()

internal fun VersionCatalog.library(alias: String) = findLibrary(alias).get()

/**
 * The shared lint policy at the repository root. Read through the isolated root project,
 * which Isolated Projects allows, instead of reaching into another project's state.
 */
internal val Project.lintPolicy
    get() = isolated.rootProject.projectDirectory.file("lint.xml").asFile

/**
 * The namespace (and package) of a module, from its path: `:core:ui` is `se.novila.ui`, `:feature:today` is
 * `se.novila.feature.today`. Modules never set it themselves, so the path is the only place it is decided.
 */
internal val Project.novilaNamespace: String
    get() = "se.novila." + path.removePrefix(":").removePrefix("core:").replace(':', '.')

/** A dependency on another module by path. Declares the dependency only; never touches that project (Isolated Projects). */
internal fun Project.module(path: String) = dependencies.project(mapOf("path" to path))
