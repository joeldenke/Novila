# build-logic

Convention plugins for Novila, built as an included build (`includeBuild("build-logic")` in the
root `settings.gradle.kts`). This is Gradle's recommended way to share build logic, used by
Google's Now in Android. It replaces copy-paste between modules and `allprojects`/`subprojects`
blocks, which Isolated Projects forbids.

A module's `build.gradle.kts` contains only plugins, the `novila { }` block and dependencies. Everything else
(SDK levels, namespace, signing, test setup, lint, Roborazzi, coverage) is decided here.

| Plugin id | Use it for | What it sets |
|---|---|---|
| `novila.android.application` | `:app` | AGP application: application id, version, signing, build types, locales, lint over all modules (`checkDependencies`), badging tasks (`Badging.kt`) |
| `novila.android.library` | Android library modules | AGP library, compileSdk/minSdk, Java 17, lint policy, namespace from the module path |
| `novila.android.feature` | `:feature:*` | library + Compose + screenshot tests + navigation, and `:core:designsystem` and `:core:ui` |
| `novila.android.compose` | modules with Compose UI | Compose compiler, Compose BOM, preview tooling, compiler reports with `-Pnovila.composeReports=true` |
| `novila.android.unittest` | modules with unit tests | JUnit, Robolectric (native graphics, merged resources), Compose UI tests, Kover |
| `novila.android.screenshot` | modules whose previews are screenshot-tested | Roborazzi on top of `unittest`: every `@Preview` in the module is a test, references in `src/test/screenshots` |
| `novila.jvm.library` | plain Kotlin modules (no Android) | Kotlin/JVM 17, JUnit, Android lint (`com.android.lint`), Kover |

Plugins are small and composable: a tested Compose library applies `novila.android.library`,
`novila.android.compose` and `novila.android.unittest`.

## Namespaces

The namespace, and the package of the module's code, comes from the module path: `:core:ui` is `se.novila.ui`,
`:feature:today` is `se.novila.feature.today`, `:app` is `se.novila`. Modules never set it.

## `novila { }`

Libraries that several modules use, by what the module does (`NovilaExtension.kt`). Each call adds the artifacts
and compiler plugins it needs, so modules only take what they use:

```kotlin
novila {
    coroutines()               // kotlinx.coroutines
    serialization(json = true) // @Serializable: compiler plugin, runtime and JSON
    navigation()               // Navigation 3 routes and entries, :core:navigation (every feature has it)
    lifecycle()                // ViewModels and collectAsStateWithLifecycle
    activity()                 // ComponentActivity and setContent
    healthConnect()            // the Health Connect client (see CLAUDE.md first)
}
```

Project modules stay in `dependencies { }` with type-safe accessors (`implementation(projects.core.coach)`).
One-off libraries do too, with a comment saying why.

## Tests

Only the debug variant has unit tests (`./gradlew test` would otherwise run every test twice), and no module has
instrumented tests. Coverage: every module has the Kover variant `coverage`; `./gradlew koverXmlReportCoverage`
writes one report per module, which CI sums.

## Versions

Versions come from the root catalog (`gradle/libs.versions.toml`), which `settings.gradle.kts`
here imports as `libs`. AGP's public `gradle-api` is a `compileOnly` dependency (never AGP internals, which AGP 10
will hide); the root build puts the real plugins on the classpath with `apply false` (Kotlin through the
buildscript classpath), so every module shares one copy.
