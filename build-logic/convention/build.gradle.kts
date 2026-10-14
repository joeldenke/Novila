import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "se.novila.buildlogic"

// Targets the JDK that runs Gradle, not what runs on the device.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    // compileOnly: the root build puts these plugins on the classpath (apply false), so every
    // module shares one copy. AGP is compiled against its public API only. Kover and Roborazzi
    // are applied by id and need no compile dependency.
    compileOnly(libs.android.gradle.api)
    compileOnly(libs.compose.gradle.plugin)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = libs.plugins.novila.android.application.get().pluginId
            implementationClass = "se.novila.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = libs.plugins.novila.android.library.get().pluginId
            implementationClass = "se.novila.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = libs.plugins.novila.android.compose.get().pluginId
            implementationClass = "se.novila.buildlogic.AndroidComposeConventionPlugin"
        }
        register("androidUnitTest") {
            id = libs.plugins.novila.android.unittest.get().pluginId
            implementationClass = "se.novila.buildlogic.AndroidUnitTestConventionPlugin"
        }
        register("androidScreenshot") {
            id = libs.plugins.novila.android.screenshot.get().pluginId
            implementationClass = "se.novila.buildlogic.AndroidScreenshotConventionPlugin"
        }
    }
}
