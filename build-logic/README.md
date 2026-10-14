# build-logic

Convention plugins for Novila, built as an included build (`includeBuild("build-logic")` in the
root `settings.gradle.kts`). This is Gradle's recommended way to share build logic, used by
Google's Now in Android. It replaces copy-paste between modules and `allprojects`/`subprojects`
blocks, which Isolated Projects forbids.

| Plugin id | Use it for | What it sets |
|---|---|---|
| `novila.android.application` | `:app` | AGP application, compileSdk/minSdk/targetSdk, Java 17, lint policy |
| `novila.android.library` | every Android library module | AGP library, compileSdk/minSdk, Java 17, lint policy |
| `novila.android.compose` | modules with Compose UI | Compose compiler, Compose BOM, preview tooling |

Plugins are small and composable: a Compose library applies `novila.android.library` and
`novila.android.compose`. One-off settings stay in that module's `build.gradle.kts`.

Versions come from the root catalog (`gradle/libs.versions.toml`), which `settings.gradle.kts`
here imports as `libs`. AGP's public `gradle-api` is a `compileOnly` dependency (never AGP internals, which AGP 10 will hide); the root build puts the real plugins
on the classpath with `apply false`, so every module shares one copy.
