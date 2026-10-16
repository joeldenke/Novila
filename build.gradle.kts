buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        // Lyfter AGP 9:s inbyggda Kotlin till versionen i libs.versions.toml.
        classpath(libs.kotlin.gradle.plugin)
    }
}

// Plugins are declared once here so all modules (and the convention plugins in build-logic)
// share one classloader. Never configure subprojects from here: Isolated Projects forbids it.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.metro) apply false
    alias(libs.plugins.spotless)
}

// Formatting and ktlint for every Kotlin file in the repo, configured from .editorconfig.
// Runs on the root project only and just reads files. The targets are file trees, not glob strings: for a glob
// Spotless looks up every subproject's build directory, which Isolated Projects forbids.
spotless {
    val ktlintVersion = libs.versions.ktlint.get()
    fun sources(pattern: String) = fileTree(layout.projectDirectory) {
        include(pattern)
        exclude("**/build/**", "**/.gradle/**", "**/.kotlin/**")
    }
    kotlin {
        target(sources("**/*.kt"))
        ktlint(ktlintVersion)
    }
    kotlinGradle {
        target(sources("**/*.gradle.kts"))
        ktlint(ktlintVersion)
    }
}
