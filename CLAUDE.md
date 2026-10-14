# Novila – instruktioner för Claude

Novila är en svensk hälsoapp för Android som kombinerar träning, sömn och kost.
Ägaren utvecklar enbart från mobilen: ingen Android Studio, ingen emulator, ingen lokal adb.
CI är därför den enda sanningen om att något bygger och fungerar.

## Stack
- Kotlin 2.4 via AGP 9:s inbyggda Kotlin (ingen `kotlin-android` i modulerna)
- Jetpack Compose med eget designsystem ovanpå Material 3, compileSdk/targetSdk 37, minSdk 26
- Navigation 3 (`androidx.navigation3`) med en back stack per flik: Idag, Träning, Sömn, Kost
- Alla versioner i `gradle/libs.versions.toml`. Hårdkoda aldrig versioner i byggfiler.
- Data lokalt i SharedPreferences via `HealthStore`, kodat av `DayEntryCodec` (versionerat format)
- En delad `HealthViewModel` (`StateFlow`); skärmarna i `feature/*` är tillståndslösa

## Struktur
- `designsystem/theme` – `NovilaTheme`: färger, typografi, former och spacing-tokens
- `designsystem/component` – `Novila*`-komponenter som wrappar Material 3
- `navigation` – rutter (`NavKey`), `NavigationState`, `Navigator`
- `feature/<flik>` – en skärm per flik, med `@Preview`
- `ui/common` – delade hälsokort och listlayout

## Arbetsflöde
- Pusha aldrig direkt till `main`. Skapa en branch och öppna en PR.
- **Commit-meddelanden, branchnamn, PR-titlar och PR-beskrivningar skrivs på engelska.**
  PR-titeln blir release notes för testarna, så skriv den som en tydlig sammanfattning.
- `CI`-workflowen måste vara grön innan PR:en är klar: `./gradlew lintDebug testDebugUnitTest assembleDebug`.
  Release-bygget (R8) körs inte på PR:er. Ändrar du `proguard-rules.pro` eller R8-känslig kod,
  säg det i PR-beskrivningen. Fel syns då först vid merge.
- Kan din miljö inte nå Google Maven: pusha och följ körningen (`gh pr checks`, `gh run view --log-failed`)
  och rätta tills den är grön. Säg aldrig att något fungerar utan grönt CI.
- När PR:en mergas bygger `Distribute to testers` en signerad release-APK (`se.novila`) och skickar den via Firebase App Distribution.

## Kvalitet
- **Lint-policy:** `app/lint.xml` sätter projektets regler och orsaker. Höj inte lint-fel till varningar
  och lägg inte till `@SuppressLint` utan att fråga ägaren.
- **Kodstil:** `.editorconfig`. Inga wildcard-importer, trailing commas, max 120 tecken.
- **Lär av CI:** när CI hittar en ny typ av misstag, gör tre saker i samma PR. Fixa felet,
  lägg till regeln i `app/lint.xml` (eller i `ArchitectureTest` om lint inte kan fånga det),
  och lägg till en rad under *Kända fallgropar* nedan.

### Kända fallgropar
Misstag som redan har gjorts i det här repot. Gör dem inte igen.
- **Locale i composables:** använd aldrig `Locale.getDefault()` i en `@Composable` (lint `NonObservableLocale`).
  Läs den direkt med `LocalConfiguration.current.locales[0]`. Lint underkänner även
  `ConfigurationCompat.getLocales(...)` och fallbacks som `Locale.ROOT`.
- **Kotlin-version i AGP 9:** lägg aldrig till `org.jetbrains.kotlin.android`, inte heller med `apply false`.
  Kotlin-versionen höjs via `buildscript { classpath(libs.kotlin.gradle.plugin) }` och måste matcha `compose-compiler`.
- **Insets:** ge `innerPadding` till listans `contentPadding` via `ScreenList`, aldrig `Modifier.padding(innerPadding)`.
- **GitHub-loggar:** fullständiga Actions-loggar går inte att hämta härifrån. Läs CI-fel via annotations:
  `gh api repos/joeldenke/Novila/check-runs/<job-id>/annotations`.
- **GitHub API:** GraphQL och `gh pr create` fungerar inte här. Använd REST (`gh api repos/.../pulls`).

## Googles Android-skills
Repot aktiverar pluginet `android-skills` från github.com/android/skills via `.claude/settings.json`.
Använd rätt skill innan du ändrar respektive område:
- `agp-9-upgrade` vid ändringar i Gradle-bygget, AGP-, Kotlin- eller KSP-versioner
- `edge-to-edge` för alla nya skärmar, listor, FAB:ar och textfält
- `testing-setup` när testinfrastruktur byggs ut (Robolectric, Compose-tester, screenshot-tester)
- `navigation-3` när appen får fler skärmar
- `adaptive` för surfplattor och vikbara skärmar
- `jetpack-compose-performance` vid prestandaproblem eller onödiga omkompositioner
- `r8-analyzer` vid ändringar i `proguard-rules.pro` eller R8-problem
- `android-permissions-security` innan nya behörigheter läggs till (t.ex. Health Connect, notiser)
- `play-policy-insights` innan första Play-release. Appen hanterar hälsodata.
- `glance-canonical-layouts` om appen får hemskärmswidgets

## Firebase-pluginet
`.claude/settings.json` aktiverar också Firebase-pluginet (`firebase@firebase`) med Firebase MCP-servern.
- Den kräver inloggning (`firebase login --no-localhost`) och nätverksåtkomst till `*.googleapis.com`.
- Använd den för setup och felsökning av Firebase-projektet, aldrig för att lägga till Firebase-SDK:er i appen.
- CI använder aldrig MCP. CI ska autentisera via Workload Identity Federation (planerat, ersätter servicekonto-nyckeln).

## Regler
- **Designsystem:** feature-kod använder bara `se.novila.designsystem` och aldrig `androidx.compose.material3`
  direkt. Saknas en komponent, lägg till en `Novila*`-wrapper i `designsystem/component`.
  Använd `NovilaTheme.colors/typography/spacing/shapes`, aldrig hårdkodade färger eller dp-värden i features.
- **Inga hårdkodade texter:** all text som visas eller läses upp ska komma från `strings.xml`.
  Engelska i `values/`, svenska i `values-sv/`. Symboler och enheter markeras `translatable="false"`.
- `ArchitectureTest` bevakar båda reglerna i CI.
- **Navigation:** nya skärmar blir en `@Serializable`-rutt som implementerar `NavKey`, plus en `entry<…>`
  i `NovilaApp`. Navigera alltid via `Navigator`. Följ skillen `navigation-3`.
- Logik som går att testa utan Android ska ligga i rena Kotlin-klasser med enhetstester i `app/src/test`.
- Ändra lagringsformat bara genom att höja versionen i `DayEntryCodec` och migrera gammal data.
- Lägg till `contentDescription` på ikoner och emoji-knappar, eller `null` när de är dekorativa.
- `versionCode` sätts av CI. Ändra `versionName`-prefixet (`0.1`) bara vid större milstolpar.
- Signering: `app/debug.keystore` är bara för debug-byggen. Release signeras med en hemlig nyckel
  som enbart finns i GitHub-hemligheter. Lägg aldrig en release-nyckel eller dess lösenord i repot.
  Byts nyckeln kan testare inte uppdatera.
- Nya beroenden: lägg dem i versionskatalogen och motivera dem i PR-beskrivningen.
- Hälsodata stannar på enheten. Inga nätverksanrop, analysverktyg eller Firebase-SDK:er i appen utan att ägaren ber om det.
- Pinna GitHub Actions till commit-SHA med versionskommentar. Dependabot håller dem uppdaterade.
