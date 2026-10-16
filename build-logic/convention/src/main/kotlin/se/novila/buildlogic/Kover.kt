package se.novila.buildlogic

import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Coverage with Kover. Every module gets the same report variant, [COVERAGE_VARIANT], made of its debug unit tests
 * (Android) or its tests (JVM), so `./gradlew koverXmlReportCoverage` covers all modules without compiling a release
 * variant, which Kover's default total report would do. CI sums the per-module reports.
 */
internal fun Project.configureKover(variant: String) {
    pluginManager.apply("org.jetbrains.kotlinx.kover")
    extensions.configure<KoverProjectExtension> {
        currentProject {
            createVariant(COVERAGE_VARIANT) { add(variant) }
        }
    }
}

internal const val COVERAGE_VARIANT = "coverage"
