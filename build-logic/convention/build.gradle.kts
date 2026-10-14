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
    // compileOnly: the root build puts AGP on the classpath (apply false), so every module shares
    // one copy. Compiles against AGP's public API only. Other plugins (Compose compiler) are
    // applied by id and need no compile dependency.
    compileOnly(libs.android.gradle.api)
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
    }
}
