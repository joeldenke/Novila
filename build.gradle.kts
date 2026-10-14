buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        // Lyfter AGP 9:s inbyggda Kotlin till versionen i libs.versions.toml.
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
