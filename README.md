# Novila

[![CI](https://github.com/joeldenke/Novila/actions/workflows/ci.yml/badge.svg)](https://github.com/joeldenke/Novila/actions/workflows/ci.yml)

*[Svenska](README.sv.md)*

Novila is an Android health app that brings training, sleep and nutrition together in one calm, Nordic-styled
place. It plans your training week, follows how you sleep and eat, and shows how the parts balance out each day.
Everything stays on the phone: no account, no cloud, no analytics. The app is in English and Swedish.

## What it does

| Part | Features |
|---|---|
| **Today** | A greeting by name, the day's balance as one ring per part, a tile per part with the week's trend, insights, the week's sleep and nutrition charts, and mood |
| **Training** | A weekly program following WHO's guidelines (150–300 min cardio, strength two days a week, at most +10 % cardio per week). An on-device coach that builds the week from your goal, days, time per session, focus and limitations, including phased rehab programs for knee and lower back. 13 illustrated exercises and a workout mode with sets, rests and countdowns |
| **Sleep** | Hours slept per night and the week as a chart |
| **Nutrition** | Servings per food group, water and sweets, scored 0–100 after the Swedish Food Agency's dietary guidelines, with a suggested next step |
| **Movement** | Today's steps and active calories from Health Connect, read in the foreground only |
| **Settings** | Theme (system, light, dark, wallpaper colors), profile, Health Connect access, health disclaimer, deleting all data, and the intro again |

A short illustrated intro at first start sets up the goal, profile, coach and Health Connect, so the plan is there
from day one. What comes next is in the [roadmap](docs/roadmap.md) (Swedish).

## Privacy by design

- **Data stays on the device.** Days, coach answers and settings live in Preferences DataStore. The app makes no
  network calls and has no analytics or Firebase SDKs.
- **Health Connect is read-only and minimal:** only today's steps and active calories (`READ_STEPS`,
  `READ_ACTIVE_CALORIES_BURNED`), while the app is open. Nothing is written, no history, no background reads, and
  the cache is in memory only. The privacy screen Health Connect links to says exactly that.
- **AI runs on the phone.** The training plan is deterministic, tested Kotlin. Gemini Nano (through AICore)
  only writes a short comment about the finished plan, the prompt never leaves the device, and the answer is
  shown with an AI disclaimer. The model never picks exercises, doses or rehab advice.
- **The APK is reviewed.** CI compares the APK's permissions, SDK levels, features and exported components with
  a committed file and fails on any difference, so no library can add a permission unnoticed.

## Tech stack

| Area | Choice |
|---|---|
| Language | Kotlin 2.4 (AGP 9's built-in Kotlin) |
| Build | Gradle 9.8, AGP 9.4, convention plugins in `build-logic`, version catalog, configuration cache, build cache and Isolated Projects |
| UI | Jetpack Compose (BOM 2026.09) with an own design system on top of Material 3 1.5 |
| Navigation | Navigation 3, one back stack per tab |
| Architecture | One ViewModel (`StateFlow`) per screen, scoped to its navigation entry; stateless, previewable screens |
| Dependency injection | [Metro](https://zacsweers.github.io/metro/), a compile-time Kotlin compiler plugin |
| Storage | Preferences DataStore with a versioned, migrated format |
| Health data | Health Connect; a Google Health API (REST) source exists but is not wired in yet |
| On-device AI | Gemini Nano via the ML Kit GenAI Prompt API |
| Platform | minSdk 26, compileSdk and targetSdk 37 |
| Testing | JUnit, Robolectric and Compose UI tests, Roborazzi screenshot tests of every `@Preview`, Kover coverage |
| Code quality | Spotless with ktlint, Android lint with a project policy (`lint.xml`), architecture tests |
| CI/CD | GitHub Actions (SHA-pinned, Dependabot), Firebase App Distribution for testers, R8 on release builds |

### Design

The design language, *Forest & sun*, is Nordic calm: the Hanken Grotesk typeface bundled with the app, white cards
with hairline borders and one color per part: spruce green for training, lake blue for sleep and falu red for
nutrition. The sun accent marks at most one main action per screen. Animations play once as content appears, never
in a loop. Color pairs are checked against WCAG 2.2 contrast in tests. Exercises are drawn from data, not video:
each one is keyframes of joint angles on a body-agnostic skeleton, rendered with the muscles that work
([docs/anatomy.md](docs/anatomy.md), Swedish).

### Modules

Dependencies point downwards: `:app` → `:feature:*` → `:core:*` UI modules → pure Kotlin. Features never depend on
each other, which a test checks.

```
:app                     Shell only: Metro graph, activity, tabs and navigation display
:feature:today           Dashboard: balance, tiles, insights, charts, mood
:feature:training        Weekly program, coach, exercises, workout mode
:feature:sleep           Sleep log and week
:feature:nutrition       Food groups, water, score
:feature:onboarding      The intro at first start
:feature:settings        Settings
:core:designsystem       NovilaTheme and Novila* components (the only module using Material 3 directly)
:core:ui                 Shared UI: lists, formatting, week cards, preview data
:core:navigation         Back stack per tab, Navigator
:core:data               DataStore stores and the Gemini Nano coach model
:core:healthconnect      Health Connect as a movement source, privacy screen
:core:model              Pure Kotlin: day entries, nutrition score, daily balance, insights, settings
:core:movement           Pure Kotlin: movement data independent of source
:core:exercise           Pure Kotlin: exercises, weekly program generator, workout mode
:core:coach              Pure Kotlin: the training coach and its rehab programs
:core:anatomy            Pure Kotlin: poses, skeleton, muscles and cameras for exercise illustrations
:core:googlehealth       Pure Kotlin: Google Health API as an exercise source (not wired in yet)
```

Logic that needs no Android lives in pure Kotlin modules, which compile and test in seconds.

## How it's built

The app is developed entirely from a phone: no Android Studio, no emulator. [Claude Code](https://claude.ai/code)
writes the code in a branch and opens a pull request, and CI is the only proof that it builds and works:

1. **CI** runs formatting and lint, unit and screenshot tests with coverage, and a debug build with the APK check,
   as parallel jobs. When the pull request says the UI changes on purpose, CI records the new screenshots and pushes them to it for review.
2. The pull request is merged when CI is green.
3. **Distribute to testers** builds debug and release (R8) and sends the debug app to testers through Firebase App
   Distribution, with release notes from the merged pull requests.

`CLAUDE.md` holds the project's rules and known pitfalls (Swedish), and Google's
[Android skills](https://github.com/android/skills) are enabled for Claude Code in this repository.
