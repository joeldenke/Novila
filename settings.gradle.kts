pluginManagement {
    // Convention plugins (novila.android.*). See build-logic/README.md.
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "Novila"

// Type-safe module references: implementation(projects.core.designsystem).
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")

include(":core:anatomy")
include(":core:coach")
include(":core:data")
include(":core:designsystem")
include(":core:exercise")
include(":core:googlehealth")
include(":core:healthconnect")
include(":core:model")
include(":core:movement")
include(":core:navigation")
include(":core:ui")

include(":feature:nutrition")
include(":feature:settings")
include(":feature:sleep")
include(":feature:today")
include(":feature:training")
