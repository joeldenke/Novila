# Novila – instruktioner för Claude

Novila är en svensk hälsoapp för Android som kombinerar träning, sömn och kost.
Ägaren utvecklar enbart från mobilen: ingen Android Studio, ingen emulator, ingen lokal adb.
CI är därför den enda sanningen om att något bygger och fungerar.

## Stack
- Kotlin 2.4 via AGP 9:s inbyggda Kotlin (ingen `kotlin-android` i modulerna)
- Jetpack Compose med eget designsystem ovanpå Material 3 1.5 (beta), compileSdk/targetSdk 37, minSdk 26
- Navigation 3 (`androidx.navigation3`) med en back stack per flik: Idag, Träning, Sömn, Kost
- Alla versioner i `gradle/libs.versions.toml`. Hårdkoda aldrig versioner i byggfiler.
- Data lokalt i SharedPreferences via `HealthStore`, kodat av `DayEntryCodec` (versionerat format)
- En delad `HealthViewModel` (`StateFlow`); skärmarna i `feature/*` är tillståndslösa

## Struktur
Moduler:
- `:app` – appen: navigation, features, data och `ui/common`
- `:core:designsystem` – `se.novila.designsystem`, enda modulen som använder Material 3 direkt
  - `theme` – `NovilaTheme`: färger, typografi, former och spacing-tokens
  - `component` – `Novila*`-komponenter som wrappar Material 3
- `build-logic` – inkluderat bygge med convention plugins (`novila.android.*`), se `build-logic/README.md`

I `:app`:
- `navigation` – rutter (`NavKey`), `NavigationState`, `Navigator`
- `feature/<flik>` – en skärm per flik, med `@Preview`
- `ui/common` – delade hälsokort och listlayout

## Arbetsflöde
- Pusha aldrig direkt till `main`. Skapa en branch och öppna en PR.
- **Commit-meddelanden, branchnamn, PR-titlar och PR-beskrivningar skrivs på engelska.**
  PR-titeln (vid squash: commit-rubriken) blir en rad i testarnas release notes, så skriv den som en tydlig
  sammanfattning. `scripts/release-notes.sh` listar allt på `main` sedan förra lyckade distributionen.
- `CI`-workflowen måste vara grön innan PR:en är klar. Den kör jobben Format (`spotlessCheck`), Lint (`lintDebug`),
  Tests (`testDebugUnitTest verifyRoborazziDebug`) och Build (`assembleDebug`) parallellt; kontrollen
  *Lint, test and build* samlar dem och är den som krävs. Gemensamma steg ligger i `.github/actions/`.
  Release-bygget (R8) körs inte på PR:er. Ändrar du `proguard-rules.pro` eller R8-känslig kod,
  säg det i PR-beskrivningen. Fel syns då först vid merge.
- Kan din miljö inte nå Google Maven: pusha och följ körningen (`gh pr checks`, `gh run view --log-failed`)
  och rätta tills den är grön. Säg aldrig att något fungerar utan grönt CI.
- När PR:en mergas skickar `Distribute to testers` debug-APK:n (`se.novila.debug`) via Firebase App Distribution.
  Samma körning bygger även release (R8), signerad bara om release-nyckeln finns i GitHub-hemligheterna.

## Kvalitet
- **Lint-policy:** `lint.xml` i roten sätter projektets regler och orsaker. Höj inte lint-fel till varningar
  och lägg inte till `@SuppressLint` utan att fråga ägaren.
- **Kodstil:** `.editorconfig`. Inga wildcard-importer, trailing commas, max 120 tecken.
- **Lär av CI:** när CI hittar en ny typ av misstag, gör tre saker i samma PR. Fixa felet,
  lägg till regeln i `lint.xml` (eller i `ArchitectureTest` om lint inte kan fånga det),
  och lägg till en rad under *Kända fallgropar* nedan.

### Kända fallgropar
Misstag som redan har gjorts i det här repot. Gör dem inte igen.
- **Locale i composables:** använd aldrig `Locale.getDefault()` i en `@Composable` (lint `NonObservableLocale`).
  Läs den direkt med `LocalConfiguration.current.locales[0]`. Lint underkänner även
  `ConfigurationCompat.getLocales(...)` och fallbacks som `Locale.ROOT`.
- **Kotlin-version i AGP 9:** lägg aldrig till `org.jetbrains.kotlin.android`, inte heller med `apply false`.
  Kotlin-versionen höjs via `buildscript { classpath(libs.kotlin.gradle.plugin) }` och måste matcha `compose-compiler`.
- **Insets:** ge `innerPadding` till listans `contentPadding` via `ScreenList`, aldrig `Modifier.padding(innerPadding)`.
- **Robolectric på JDK 21:** Robolectric 4.17 kräver `--add-exports=java.base/jdk.internal.access=ALL-UNNAMED`,
  annars faller alla Robolectric-tester med `IllegalAccessException` i `AndroidInterceptors`. Flaggan sätts i
  `novila.android.unittest`; använd det pluginet i stället för egen testkonfiguration.
- **Robolectric-SDK:** Robolectric-tester körs mot API 36 via `src/test/resources/robolectric.properties`.
  På API 37 faller Compose UI-tester med `NoSuchMethodException: InputManager.getInstance()` (AndroidX Test).
  Nya moduler med Robolectric-tester behöver samma fil.
- **Designspråket måste användas:** tokens som `NovilaTheme.pillars` och `accent` fanns men användes inte, så appen såg
  generisk ut. Komponenterna tar en `role`: ge den delens pillar (`NovilaTheme.pillars.sleep` osv.), och använd
  `NovilaAccentButton` för skärmens huvudåtgärd.
- **GitHub-loggar:** fullständiga Actions-loggar går inte att hämta härifrån. Läs CI-fel via annotations:
  `gh api repos/joeldenke/Novila/check-runs/<job-id>/annotations`.
- **gcloud i molnsessioner:** miljön sätter `CLOUDSDK_AUTH_ACCESS_TOKEN`, som skriver över `gcloud auth login`.
  Kör `unset CLOUDSDK_AUTH_ACCESS_TOKEN` efter inloggningen, annars svarar alla anrop `CREDENTIALS_MISSING`.
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

## Google Cloud- och Firebase-MCP
`.mcp.json` startar två MCP-servrar, godkända i `.claude/settings.json`. De fungerar även i molnsessioner,
där pluginet `firebase@firebase` inte alltid installeras.
- `gcloud` (`@google-cloud/gcloud-mcp`) kör gcloud-kommandon med den inloggade användaren.
- `firebase` (`firebase-tools mcp`) för Firebase-projekt, appar och App Distribution.
- Båda kräver inloggning: `gcloud auth login --update-adc` räcker för båda (Firebase använder ADC),
  annars `firebase login --no-localhost`. Servrarna läses in när sessionen startar.
- Versionerna är pinnade och samma `firebase-tools` som i `distribute.yml`. Höj dem tillsammans.
- Använd MCP för setup och felsökning av Firebase-projektet, aldrig för att lägga till Firebase-SDK:er i appen.
- CI använder aldrig MCP. CI autentiserar via Workload Identity Federation (`google-github-actions/auth`),
  aldrig med servicekonto-nycklar. Ändra GCP/Firebase-setupen via `scripts/bootstrap-firebase.sh` och håll den idempotent.

## Gradle-bygget
- Varje modul använder convention plugins: `novila.android.application` eller `novila.android.library`,
  plus `novila.android.compose` för Compose. SDK-nivåer, Java-nivå och lint-policy sätts bara där.
  Tester: `novila.android.unittest` (JUnit, Robolectric, Compose UI-tester, Kover) och
  `novila.android.screenshot` (Roborazzi, varje `@Preview` blir ett skärmdumpstest).
  Modulens egen `build.gradle.kts` innehåller bara `namespace`, beroenden och det som är unikt för modulen.
- Bygget ska vara kompatibelt med Isolated Projects: aldrig `allprojects {}`, `subprojects {}`,
  `rootProject.<…>` eller `project(":x")` för att läsa eller ändra en annan modul. Behövs en fil i roten,
  använd `project.isolated.rootProject.projectDirectory`. Workflowen *Isolated Projects* kontrollerar detta, men körs bara
  manuellt från Actions-fliken (den är långsam och har hängt sig på AGP 9.x). Kör den efter ändringar i bygglogiken.
- Nya moduler: lägg till `include(...)` i `settings.gradle.kts`, beroende via `projects.<…>` (typsäkra accessorer).
- Nya Gradle-plugins deklareras med `apply false` i rotens `build.gradle.kts`, så att alla moduler delar en classloader.

## Tester och kodstil
- **Logik:** rena Kotlin-tester i `src/test`. **UI:** Compose UI-tester med `createComposeRule()` på Robolectric
  (`@RunWith(RobolectricTestRunner::class)`), också i `src/test`. Ingen emulator behövs.
- **Skärmdumpar:** varje `@Preview` i `:app` testas mot referensbilder i `app/src/test/screenshots`.
  Ändrar du UI med flit: kommentera `/record-screenshots` på PR:en. Workflowen `Record screenshots` tar nya bilder,
  pushar dem till branchen och startar CI. Ta aldrig fram referensbilder lokalt; de ska renderas i CI.
- **Kodstil:** Spotless med ktlint (`android_studio`-stil) kontrolleras i CI. `./gradlew spotlessApply` rättar.
- **Täckning:** Kover, sammanfattning i CI-körningens summary. **Compose-stabilitet:** artefakten `compose-reports`.

## Regler
- **Designsystem:** feature-kod använder bara `se.novila.designsystem` och aldrig `androidx.compose.material3`
  direkt. Saknas en komponent, lägg till en `Novila*`-wrapper i `:core:designsystem` (`component/`).
  Använd `NovilaTheme.colors/typography/spacing/shapes`, aldrig hårdkodade färger eller dp-värden i features.
- **Designspråk "Skog & sol":** nordiskt lugn. Typsnittet är Hanken Grotesk (Google Font, inbakad i
  `:core:designsystem`, licens i `assets/licenses`). Ladda aldrig ner typsnitt i runtime.
  Varje del har sin färg via `NovilaTheme.pillars`: träning = gran (primary), sömn = sjö (secondary),
  kost = falurött (tertiary). Solen (`NovilaTheme.accent`) används för högst en huvudknapp per skärm.
  Hörnradie från basen 14 dp via `NovilaTheme.shapes`. Nya färgpar läggs till i `ColorContrastTest` (WCAG 2.2).
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
