# Novila – instruktioner för Claude

Novila är en svensk hälsoapp för Android som kombinerar träning, sömn och kost.
Ägaren utvecklar enbart från mobilen: ingen Android Studio, ingen emulator, ingen lokal adb.
CI är därför den enda sanningen om att något bygger och fungerar.

## Stack
- Kotlin 2.4 via AGP 9:s inbyggda Kotlin (ingen `kotlin-android` i modulerna)
- Jetpack Compose med eget designsystem ovanpå Material 3 1.5 (beta), compileSdk/targetSdk 37, minSdk 26
- Navigation 3 (`androidx.navigation3`) med en back stack per flik: Idag, Träning, Sömn, Kost
- Alla versioner i `gradle/libs.versions.toml`. Hårdkoda aldrig versioner i byggfiler.
- Data lokalt i SharedPreferences via `HealthStore`, kodat av `DayEntryCodec` (versionerat format, v3 sedan passläget)
- DI med [Metro](https://zacsweers.github.io/metro/) (`dev.zacsweers.metro`, Kotlin-kompilatorplugin, ingen KSP).
  Varje skärm har en egen ViewModel (`StateFlow`) i sin feature, skapad av Metro och scopad till sin Navigation 3-entry.
  Ingen delad ViewModel. Skärmarna i `feature/*` är tillståndslösa.

## Struktur
Moduler. Namespace och paket följer sökvägen (`:core:ui` → `se.novila.ui`, `:feature:today` → `se.novila.feature.today`):
- `:app` – bara skalet: `NovilaApplication` (håller grafen), `MainActivity` (ger `LocalMetroViewModelFactory`),
  `NovilaApp` (flikar, toppbar, `NavDisplay`), `TopLevelDestination` och `di/`: `AppGraph` (Metro-grafen, där källor
  som `MovementRepository` väljs) och `NovilaViewModelFactory`. Ingen skärm, ViewModel eller logik som går att lägga
  i en modul.
- `:feature:today`, `:feature:training`, `:feature:sleep`, `:feature:nutrition` – en modul per flik med skärmar, kort,
  strängar, ViewModels och `@Preview`/skärmdumpar. Varje feature äger sin navigation (`navigation/`): `@Serializable`-rutter
  (`NavKey`) och `EntryProviderScope<NavKey>.<feature>Entries(...)`, som `:app` lägger i sin `entryProvider`.
  Features beror aldrig på varandra (`ArchitectureTest`); flikbyten går via callbacks från `:app`.
- `:core:designsystem` – `se.novila.designsystem`, enda modulen som använder Material 3 direkt
  - `theme` – `NovilaTheme`: färger, typografi, former och spacing-tokens
  - `component` – `Novila*`-komponenter som wrappar Material 3
- `:core:ui` – det flera features delar: `ScreenList`, formatering (`hoursText`, `stepsText` …), veckokorten,
  flikikonerna och `PreviewData` (samma person i alla previews)
- `:core:navigation` – `NavigationState` (en back stack per flik, varje entry med egen sparad state och
  `ViewModelStore` via `rememberViewModelStoreNavEntryDecorator`) och `Navigator`
- `:core:data` – lagring i SharedPreferences (`HealthStore`, `CoachStore`, singletons i `AppScope` vars `changes` får
  alla ViewModels att läsa om efter en sparning) och `GeminiNanoCoachModel` (bunden som `CoachLanguageModel`)
- `:core:healthconnect` – Health Connect som `MovementSource`, behörigheterna i modulens manifest och integritetsskärmen
  (`HealthDataInfoActivity`)
- Ren Kotlin, utan Android (`novila.jvm.library`, testas på sekunder):
  - `:core:model` – `DayEntry` (`DayEntryCodec`), kost, dagens balans och insikter
  - `:core:movement` – rörelsedata oberoende av källa: `MovementSource` (källa), `MovementCache` (cache) och
    `MovementRepository` (det enda UI:t läser). Källa och cache väljs på ett ställe, i `AppGraph`.
  - `:core:exercise` – träningspass oberoende av källa: `Exercise`, `ExerciseSource` och `TrainingProgramGenerator`, som
    bygger veckans program (WHO:s råd: 150–300 min kondition och styrka två dagar, högst +10 % kondition per vecka).
    `StrengthSessions` fyller styrkedagarna med övningar, set, repetitioner och vila efter nivå. `Workout` är passläget
    (set, vila och nedräkning). Timern körs i `WorkoutViewModel`. Avklarade pass sparas som `strengthMinutes` per dag
    och blir generatorns historik.
  - `:core:coach` – träningscoachen, helt på enheten: `CoachPreferences` (mål, dagar, tid per pass, fokus, hänsyn,
    rehabområde och fas), `CoachCatalog` (rörelsemönster, dosering och belastning per övning) och `CoachPlanner`, som
    bygger veckans program ur svaren, senaste passen och övningsbiblioteket och förklarar valen (`CoachNote`). Målet
    `Rehab` ger ett handvalt, fasindelat program (knä eller ländrygg, fas 1–3) med smärtgräns och vårdråd. Svaren
    sparas i `CoachStore`; är coachen inte inställd gäller `TrainingProgramGenerator`. Planen är alltid deterministisk
    och testad. Gemini Nano (`GeminiNanoCoachModel`, ML Kit Prompt API via AICore) skriver bara en kommentar om den
    färdiga planen (`CoachPrompt` bygger prompten och tvättar svaret), aldrig själva planen. Se *Gemini Nano* under Regler.
  - `:core:anatomy` – kroppsagnostiska övningsillustrationer: `Pose`/`Movement` (ledvinklar per keyframe), `Skeleton`
    (framåtkinematik, `BodyShape`), `MuscleMap`, `SceneBuilder` (ortografisk `Camera`, siluetter och de muskelband som
    syns), `ViewSelector` (varje övnings bästa vy) och `ExerciseLibrary`. `AnatomyFigure` i `:feature:training` ritar
    i Compose. Se `docs/anatomy.md`. `tools/anatomy/anatomy.sh` testar, renderar och löser vinklar utan Gradle.
  - `:core:googlehealth` – Google Health API (REST v4) som `ExerciseSource`: läser
    `users/me/dataTypes/exercise/dataPoints`. Ännu inte inkopplad i `:app`, se *Google Health API* under Regler.
- `build-logic` – inkluderat bygge med convention plugins (`novila.*`) och `novila { }`, se `build-logic/README.md`

Beroenden går nedåt: `:app` → `:feature:*` → `:core:ui`/`:core:navigation`/`:core:designsystem` → ren Kotlin.
Strängar och ikoner ligger i modulen som använder dem; det flera moduler använder ligger i `:core:ui`
(importeras som `import se.novila.ui.R as UiR`).

## Arbetsflöde
- Pusha aldrig direkt till `main`. Skapa en branch och öppna en PR.
- **Commit-meddelanden, branchnamn, PR-titlar och PR-beskrivningar skrivs på engelska.**
  PR-titeln (vid squash: commit-rubriken) blir en rad i testarnas release notes, så skriv den som en tydlig
  sammanfattning. `scripts/release-notes.sh` listar allt på `main` sedan förra lyckade distributionen.
- `CI`-workflowen måste vara grön innan PR:en är klar. Den kör fyra jobb parallellt: Format (`spotlessCheck`), Lint
  (`:app:lintDebug`, alla moduler), Tests (`test verifyRoborazziDebug koverXmlReportCoverage`) och Build
  (`:app:assembleDebug :app:checkDebugBadging`); kontrollen *Lint, test and build* samlar dem och är den som krävs. Gemensamma steg ligger
  i `.github/actions/`. Cachen sköts av `gradle/actions/setup-gradle` (standardcachen, sparas bara från `main`), och
  CI-flaggorna för 2-kärniga runners ligger i `.github/ci-gradle.properties`. Bygg aldrig egen cachelogik.
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

### Manifest och badging
- `app/badging/debug-badging.txt` är den granskade bilden av APK:n: `aapt2 dump badging` (paket, SDK-nivåer,
  behörigheter, features, locales) plus exporterade komponenter och `<queries>` från det sammanslagna manifestet.
  Build-jobbet kör `checkDebugBadging` och fallerar på minsta skillnad, även när ett nytt bibliotek smyger in
  en behörighet. Logiken finns i `build-logic` (`Badging.kt`).
- Är ändringen avsiktlig: kommentera `/record-badging` på PR:en (workflowen `Record references`), eller kör
  `./gradlew :app:updateDebugBadging`. Skriv i PR-beskrivningen varför varje ny rad behövs.
- Uppdatera aldrig badging-filen bara för att få grönt. En ny behörighet eller exporterad komponent kräver
  ägarens ok, och skillen `android-permissions-security` först.

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
- **Spotless och Isolated Projects:** ge Spotless filträd (`fileTree(...)`) som `target`, aldrig glob-strängar som
  `"**/*.kt"`. För en glob slår Spotless upp varje undermoduls `build`-katalog, och då fallerar bygget med
  "cannot access 'Project.layout' functionality on subprojects". Isolated Projects stoppar sådant i varje bygge.

## Repots egna skills
- `add-exercise` (`.claude/skills/add-exercise`) när en övning läggs till eller ändras, eller styrkepassen byter övningar.

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

## Google Cloud-, Firebase- och Developer Knowledge-MCP
`.mcp.json` startar tre MCP-servrar, godkända i `.claude/settings.json`. De fungerar även i molnsessioner,
där pluginet `firebase@firebase` inte alltid installeras.
- `gcloud` (`@google-cloud/gcloud-mcp`) kör gcloud-kommandon med den inloggade användaren.
- `firebase` (`firebase-tools mcp`) för Firebase-projekt, appar och App Distribution.
- `google-developer-knowledge` (`https://developerknowledge.googleapis.com/mcp`) söker i Googles aktuella
  utvecklardokumentation (Android, Health API, Firebase m.m.). Använd den före minnet när du rör Googles API:er.
  Kräver en API-nyckel för *Developer Knowledge API* i miljövariabeln `GOOGLE_DEVELOPER_KNOWLEDGE_API_KEY`
  (miljöns hemligheter i molnsessioner). Lägg aldrig nyckeln i repot.
- Båda kräver inloggning: `gcloud auth login --update-adc` räcker för båda (Firebase använder ADC),
  annars `firebase login --no-localhost`. Servrarna läses in när sessionen startar.
- Versionerna är pinnade och samma `firebase-tools` som i `distribute.yml`. Höj dem tillsammans.
- Använd MCP för setup och felsökning av Firebase-projektet, aldrig för att lägga till Firebase-SDK:er i appen.
- CI använder aldrig MCP. CI autentiserar via Workload Identity Federation (`google-github-actions/auth`),
  aldrig med servicekonto-nycklar. Ändra GCP/Firebase-setupen via `scripts/bootstrap-firebase.sh` och håll den idempotent.

## Gradle-bygget
- Varje modul använder convention plugins: `novila.android.application` (bara `:app`), `novila.android.feature`
  (`:feature:*`), `novila.android.library` eller `novila.jvm.library` (ren Kotlin), plus `novila.android.compose` för
  Compose. Tester: `novila.android.unittest` (JUnit, Robolectric, Compose UI-tester, Kover) och
  `novila.android.screenshot` (Roborazzi, varje `@Preview` i modulen blir ett skärmdumpstest).
  SDK-nivåer, Java-nivå, namespace, signering, lint och testuppsättning sätts bara i `build-logic`.
- Modulens `build.gradle.kts` innehåller bara `plugins`, `novila { }` och `dependencies`. Bibliotek som flera moduler
  använder hämtas via `novila { coroutines(); lifecycle(); … }` (`NovilaExtension`), så varje modul tar bara det den
  använder. Lägg till en funktion där hellre än att kopiera samma beroenden mellan moduler. Metro: `novila { metro() }`
  för moduler med `@Inject`-klasser, `novila { viewModels() }` för moduler med ViewModels (Metro, lifecycle och
  `metrox-viewmodel-compose`). Lägg aldrig till Metro-pluginet eller `metrox`-artefakter direkt i en modul.
- Logik utan Android blir en `novila.jvm.library`-modul. Den kompilerar och testar utan resurser och Robolectric.
- Isolated Projects är påslaget (`org.gradle.isolated-projects=true`): modulerna konfigureras parallellt, och varje bygge
  fallerar om bygglogik når in i en annan modul. Aldrig `allprojects {}`, `subprojects {}`, `rootProject.<…>` eller
  `project(":x")` för att läsa eller ändra en annan modul; beroenden deklareras med `projects.<…>` eller
  `dependencies.project(...)`. Behövs en fil i roten, använd `project.isolated.rootProject.projectDirectory`.
  KSP (t.ex. för Room) kräver `ksp.project.isolation.enabled=true` i `gradle.properties`, annars fallerar
  bygget med "cannot access task dependencies directly" (google/ksp#2844). Flaggan är redan satt, så att KSP fungerar
  den dag ett bibliotek kräver det; ta aldrig bort den. Metro (DI) är en Kotlin-kompilatorplugin, inte KSP, och
  behöver ingen flagga. CI har `workers.max=3` i stället för 2:
  Gradle 9.8 fryser med Isolated Projects när workers ≤ antalet byggen (roten och `build-logic`), gradle#39040.
  Tillbaka till 2 efter uppgradering till Gradle 9.9.
- Nya moduler: lägg till `include(...)` i `settings.gradle.kts`, beroende via `projects.<…>` (typsäkra accessorer).
- Nya Gradle-plugins deklareras med `apply false` i rotens `build.gradle.kts`, så att alla moduler delar en classloader.

## Tester och kodstil
- **Logik:** rena Kotlin-tester i `src/test`. **UI:** Compose UI-tester med `createComposeRule()` på Robolectric
  (`@RunWith(RobolectricTestRunner::class)`), också i `src/test`. Ingen emulator behövs.
- **Skärmdumpar:** varje `@Preview` testas mot referensbilder i modulens `src/test/screenshots`
  (t.ex. `feature/training/src/test/screenshots`). Filnamnet är paket, fil och preview.
  Ändrar du UI med flit: kommentera `/record-screenshots` på PR:en. Workflowen `Record references` tar nya bilder,
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
  Förlagan är riktning D i designkanvasen *Novila designriktningar*. Idag är en dashboard: hälsning, `NovilaHeroCard`
  (balansen som ringar per del, skogssiluett och solens sken, `NovilaTheme.hero`), en `NovilaStatTile` per del med
  veckans trend, insikter, veckans sömn (`NovilaLineChart`) och kost (`NovilaBarChart`) och mående.
  Kort är vita med hårfin kant (`NovilaTheme.surfaces`).
- **Inga emojis i UI:t.** Novila ska kännas som en professionell hälsoapp: använd vektorikoner och ritade komponenter
  (t.ex. `NovilaLevelPicker` för mående), aldrig emoji-tecken.
- **Övningsillustrationer:** en övning är data (keyframes med ledvinklar i `ExerciseLibrary`), aldrig video eller
  handritad animation, så samma rörelse fungerar för varje kropp och kamera. Ny eller ändrad övning: följ skillen
  `add-exercise` (lösta vinklar, kamera från `ViewSelector`, texter på båda språken). `AnatomyTest` kontrollerar att
  planterade händer och fötter står still och säger vilken kamera som gäller. Figuren färgas bara med `NovilaTheme.anatomy` (sjöblå kropp, glödande muskler) och
  spelar ett bestämt antal repetitioner, aldrig i loop.
- **Rörelse:** lugn och engångs. Animationer spelas när innehållet visas (`NovilaReveal`, diagram som ritar in sig)
  via `NovilaMotion` i `:core:designsystem`. Inga oändliga animationer: de får skärmdumpstesterna att hänga.
- **Kostpoäng:** `nutritionScore()` (0–100) viktar livsmedelsgrupper efter Livsmedelsverkets kostråd (`FoodGroup`) med
  avdrag för sött och snacks. Ändra vikterna bara medvetet: `NutritionTest` och dagens balans bygger på dem.
- **Inga hårdkodade texter:** all text som visas eller läses upp ska komma från `strings.xml`.
  Engelska i `values/`, svenska i `values-sv/`. Symboler och enheter markeras `translatable="false"`.
- `ArchitectureTest` (i `:app`, läser alla moduler) bevakar båda reglerna i CI, och att features inte beror på varandra.
- **Navigation:** nya skärmar blir en `@Serializable`-rutt som implementerar `NavKey` i featurens `navigation/`, plus en
  `entry<…>` i featurens `…Entries`-funktion. Navigation inom en flik går via `Navigator` (`:core:navigation`); till
  en annan flik via callbacks som `:app` kopplar. Entryn hämtar sin ViewModel och skickar state och callbacks till en
  tillståndslös skärm, så skärmen går att förhandsgranska. Följ skillen `navigation-3`.
- **ViewModels och DI (Metro):** en ViewModel per skärm, i skärmens feature, aldrig i `:app` och aldrig delad mellan
  skärmar. Annotera `@ViewModelKey @ContributesIntoMap(AppScope::class) @Inject`; behöver den ruttens argument, använd
  `@AssistedInject` och en `@AssistedFactory @ManualViewModelAssistedFactoryKey @ContributesIntoMap(AppScope::class)`
  `Factory : ManualViewModelAssistedFactory` (se `WorkoutViewModel`). Hämta den i entryn med `metroViewModel()` eller
  `assistedMetroViewModel<VM, VM.Factory> { create(route.x) }`, aldrig `viewModel()`, så den scopas till entryn och
  rensas när entryn poppas. Det skärmar delar (lagring, källor) ligger i `:core:*` som `@SingleIn(AppScope::class)`
  med en `Flow` som berättar om ändringar; ViewModels delar aldrig state direkt. Läs om "idag" i `refresh()` från
  `LifecycleResumeEffect` i entryn. `ArchitectureTest` bevakar `viewModel()` och ViewModels i `:app`.
- **Metro-grafen:** allt som bidrar till `AppScope` måste ligga på `:app`:s classpath (direkt `implementation`),
  annars hittar `AppGraph` det inte. Nya bindningar: `@Inject` på klassen, `@ContributesBinding` för ett gränssnitt,
  eller `@Provides` i `AppGraph` när en källa väljs.
- Logik som går att testa utan Android ska ligga i rena Kotlin-moduler (`novila.jvm.library`) med enhetstester.
- Ändra lagringsformat bara genom att höja versionen i `DayEntryCodec` och migrera gammal data.
- Lägg till `contentDescription` på ikoner och emoji-knappar, eller `null` när de är dekorativa.
- `versionCode` sätts av CI. Ändra `versionName`-prefixet (`0.1`) bara vid större milstolpar.
- Signering: `app/debug.keystore` är bara för debug-byggen. Release signeras med en hemlig nyckel
  som enbart finns i GitHub-hemligheter. Lägg aldrig en release-nyckel eller dess lösenord i repot.
  Byts nyckeln kan testare inte uppdatera.
- Nya beroenden: lägg dem i versionskatalogen och motivera dem i PR-beskrivningen.
- **Health Connect:** appen läser bara dagens steg och aktiva kalorier (`READ_STEPS`,
  `READ_ACTIVE_CALORIES_BURNED`), i förgrunden. Inget skrivs, ingen historik eller bakgrundsläsning, och cachen ligger
  bara i minnet (`InMemoryMovementCache`). En cache på disk ändrar vad integritetsskärmen och Play-deklarationen
  ska säga, så den kräver ägarens ok. Ny källa: implementera `MovementSource` och byt den i `AppGraph`.
  Varje datatyp måste motiveras i Play Consoles *Health apps declaration*, så lägg inte till typer eller
  behörigheter utan att ägaren ber om det. `HealthConnectManifestTest` håller manifestet och `MOVEMENT_PERMISSIONS`
  i synk. `HealthDataInfoActivity` (integritetsskärmen Health Connect länkar till) är obligatorisk och ska
  beskriva exakt vad appen läser; ändras läsningen ändras texten.
- **Google Health API:** `GoogleHealthExerciseSource` läser bara träningspass (scope
  `googlehealth.activity_and_fitness.readonly`, restricted: kräver Googles verifiering före publik release). Den är inte
  inkopplad än. Att koppla in den kräver ägarens ok och, i samma PR: Google-inloggning som `AccessTokenProvider`,
  behörigheten `INTERNET` (ny rad i badging), uppdaterad integritetsskärm och Play-deklaration, och skillen
  `android-permissions-security`. Svaren tolkas i `GoogleHealthJson`; okända fält ignoreras.
- **Gemini Nano:** coachens kommentar körs med Gemini Nano i AICore på telefonen; prompten (veckans plan) lämnar
  aldrig enheten. Modellen får aldrig bestämma övningar, doser eller rehabråd: den förklarar bara planen från
  `CoachPlanner`, och svaret visas med en AI-reservation. Inga emojis eller markdown (`CoachPrompt.clean`). Att använda
  modellen till mer än så, eller byta till en molnmodell, kräver ägarens ok.
- Hälsodata stannar på enheten. Inga nätverksanrop, analysverktyg eller Firebase-SDK:er i appen utan att ägaren ber om det.
- Pinna GitHub Actions till commit-SHA med versionskommentar. Dependabot håller dem uppdaterade.
