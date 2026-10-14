# build-logic

Convention plugins for Novila, built as an included build (`includeBuild("build-logic")` in the
root `settings.gradle.kts`). This is Gradle's recommended way to share build logic, used by
Google's Now in Android. It replaces copy-paste between modules and `allprojects`/`subprojects`
blocks, which Isolated Projects forbids.

| Plugin id | Use it for | What it sets |
|---|---|---|
| `novila.android.application` | `:app` | AGP application, compileSdk/minSdk/targetSdk, Java 17, lint policy |
| `novila.android.library` | every Android library module | AGP library, compileSdk/minSdk, Java 17, lint policy |
| `novila.android.compose` | modules with Compose UI | Compose compiler, Compose BOM, preview tooling, compiler reports with `-Pnovila.composeReports=true` |
| `novila.android.unittest` | modules with unit tests | JUnit, Robolectric (native graphics, merged resources), Compose UI tests, Kover |
| `novila.android.screenshot` | modules whose previews are screenshot-tested | Roborazzi on top of `unittest`; configure `roborazzi {}` in the module |

Plugins are small and composable: a tested Compose library applies `novila.android.library`,
`novila.android.compose` and `novila.android.unittest`. One-off settings stay in that module's `build.gradle.kts`.

Versions come from the root catalog (`gradle/libs.versions.toml`), which `settings.gradle.kts`
here imports as `libs`. AGP's public `gradle-api` is a `compileOnly` dependency (never AGP internals, which AGP 10 will hide); the root build puts the real plugins
on the classpath with `apply false`, so every module shares one copy.
