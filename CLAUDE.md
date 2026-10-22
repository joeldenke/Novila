# Novila – instruktioner för Claude

Novila är en svensk hälsoapp för Android som kombinerar träning, sömn och kost.
Ägaren utvecklar enbart från mobilen: ingen Android Studio, ingen emulator, ingen lokal adb.
CI är därför den enda sanningen om att något bygger och fungerar.

## Stack
- Kotlin 2.4 via AGP 9:s inbyggda Kotlin (ingen `kotlin-android` i modulerna)
- Jetpack Compose med eget designsystem ovanpå Material 3 1.5 (beta), compileSdk/targetSdk 37, minSdk 26
- Navigation 3 (`androidx.navigation3`) med en back stack per flik: Idag, Träning, Sömn, Kost
- Alla versioner i `gradle/libs.versions.toml`. Hårdkoda aldrig versioner i byggfiler.
- Data lokalt i Preferences DataStore via `HealthStore`, kodat av `DayEntryCodec` (versionerat format, v7 sedan drycker)
- DI med [Metro](https://zacsweers.github.io/metro/) (`dev.zacsweers.metro`, Kotlin-kompilatorplugin, ingen KSP).
  Varje skärm har en egen ViewModel (`StateFlow`) i sin feature, skapad av Metro och scopad till sin Navigation 3-entry.
  Ingen delad ViewModel. Skärmarna i `feature/*` är tillståndslösa.

## Struktur
Moduler. Namespace och paket följer sökvägen (`:core:ui` → `se.novila.ui`, `:feature:today` → `se.novila.feature.today`):
- `:app` – bara skalet: `NovilaApplication` (håller grafen), `MainActivity` (ger `LocalMetroViewModelFactory`),
  `NovilaApp` (flikar, toppbar, `NavDisplay`), `TopLevelDestination`, `AppShortcut` (genvägarna från appikonen,
  dynamiska: "Starta dagens pass" öppnar passläget, "Logga ett glas vatten" loggar och visar Kost) och `di/`:
  `AppGraph` (Metro-grafen, där källor som `MovementRepository` väljs) och `NovilaViewModelFactory`. Ingen skärm,
  ViewModel eller logik som går att lägga i en modul.
- `:feature:today`, `:feature:training`, `:feature:sleep`, `:feature:nutrition` – en modul per flik med skärmar, kort,
  strängar, ViewModels och `@Preview`/skärmdumpar. Varje feature äger sin navigation (`navigation/`): `@Serializable`-rutter
  (`NavKey`) och `EntryProviderScope<NavKey>.<feature>Entries(...)`, som `:app` lägger i sin `entryProvider`.
  Features beror aldrig på varandra (`ArchitectureTest`); flikbyten går via callbacks från `:app`.
- `:feature:breathing` – Andning och avslappning: korta guidade andningsövningar (lugn andning, boxandning, 4-7-8)
  med en visuell pacer i Skog & sol-stil, fas för fas (andas in, håll, andas ut). Öppnas från Sömnens ingångar,
  bredvid sömncoachen, och från sömncoachens kvällsrutin. Sessionerna är data i `:core:breathing`; spelarens
  ViewModel räknar tiden. Inget loggas.
- `:feature:onboarding` – introduktionen vid första start: en karusell genom appens delar (målade scener i
  `NovilaSceneImage` med parallax), namn, mål och tid för coachen, Health Connect och en sammanfattning i `NovilaHeroBanner`.
  Visas ensam (utan flikar) tills `AppSettings.onboarded` är satt; `NovilaApp` ger den en egen entry via
  `rememberStandaloneEntries`. Kan visas igen från Inställningar.
- `:feature:settings` – Inställningar (profil med kroppsmått och vägen till *Mina mål*, tema, påminnelser, Health Connect, ansvarsfriskrivning, radera data, integritetspolicy, licenser, introduktionen, version).
  Ingen flik: öppnas med kugghjulet i toppraden (på Idag i hälsningen) och läggs på aktuell fliks back stack. `Navigator` flyttar
  den om den öppnas från en annan flik, så samma nyckel aldrig ligger i två stackar som visas samtidigt.
- `:feature:widget` – widgeten på hemskärmen (Glance): `NovilaWidget` visar dagens balans, sömn, kost och vatten samt
  dagens första uppmuntran (`cheers`) som appens hero-kort: granpanelen med himlen efter timmen, höjdkurvor och
  balansringarna (`NovilaGlanceHero`), anpassat efter widgetens storlek (`SizeMode.Exact`). Ingen knapp och ingen
  vattenloggning; ett tryck öppnar appen. Ingen flik och ingen ViewModel: receivern skapas av systemet och når storarna
  via `WidgetGraph` (som `AppGraph` utökar, genom `NovilaApplication`). `WidgetUpdates` ritar om widgeten när appen
  sparar. Läser aldrig Health Connect (bara i förgrunden), så balansen är sömn och kost. Här ligger också rutan i
  snabbinställningarna (`WaterTileService`): ett tryck loggar ett glas vatten och rutan visar dagens glas mot målet,
  men aldrig på låsskärmen, där den ber om upplåsning först. Den är en exporterad tjänst som bara systemet får binda
  (`BIND_QUICK_SETTINGS_TILE`), godkänd av ägaren.
- `:wear` – Wear OS-appen (`se.novila.wear`, egen app med `novila.android.wear`): en fristående klockapp som kör ett
  styrkepass och dess vila. `StandaloneSession` i `:core:exercise` bygger dagens pass (A/B, nivå), och klockan stegar
  genom samma `Workout`-steg som telefonens passläge, med nedräkning på skärmen, håll skärmen tänd och en summering.
  Den är medvetet fristående: ingen Wearable Data Layer, ingen delning med telefonen, inget sparat och ingen
  Health Connect på klockan. Temat är `NovilaTheme` mappat till Compose for Wear OS Material 3 (`NovilaWearTheme`),
  så klockan ser ut som appen, alltid i mörkt läge. Egen Metro-graf (`WearGraph`). Se *Wear OS* under Regler.
  Ingår bara i bygget med `-Pnovila.wear=true` (`settings.gradle.kts`; Spotless kontrollerar då bara `wear/`, annars
  allt utom `wear/`), så telefonens flöden aldrig ser den; bara workflowen `Wear OS` (`wear.yml`) gör det, se
  *Arbetsflöde*. `ArchitectureTest` läser inte `wear/`.
- `:core:designsystem` – `se.novila.designsystem`, enda modulen som använder Material 3 direkt
  - `theme` – `NovilaTheme`: färger, typografi, former och spacing-tokens
  - `component` – `Novila*`-komponenter som wrappar Material 3
  - `glance` – `NovilaGlanceTheme` och `NovilaGlance`: samma färger, spacing, hörn och textstorlekar för widgets,
    som inte kan använda Compose-komponenterna, och `NovilaGlanceHero`, som ritar hero-kortets panel och ringar som
    bitmappar med samma mått och höjdkurvor (`contourLines`) som `NovilaHeroCard`
- `:core:ui` – det flera features delar: `ScreenList`, formatering (`hoursText`, `stepsText` …), `DayPicker`, historikkorten
  (`SleepHistoryCard`, `NutritionHistoryCard`: vecka, fyra veckor eller år, jämfört med perioden innan),
  flikikonerna och `PreviewData` (samma person i alla previews)
- `:core:navigation` – `NavigationState` (en back stack per flik, varje entry med egen sparad state och
  `ViewModelStore` via `rememberViewModelStoreNavEntryDecorator`), `Navigator` och `SharedScreen` (nycklarna som
  låter en ingång växa till skärmen den öppnar, se *Rörelse* under Regler)
- `:core:data` – lagring i Preferences DataStore (`HealthStore`, `CoachStore`, `SettingsStore`, `ProfileStore`, `GoalsStore`, `CycleStore`,
  singletons i `AppScope` vars `StateFlow` `days`/`answers`/`settings`/`profile`/`goals` får alla ViewModels att läsa om efter en sparning). `PreferencesStore` flyttar gamla SharedPreferences hit vid första läsningen, håller datan i
  minnet och köar sparningar i storens egen scope (i ordning, klara även när ViewModeln rensas). `SavedDays` avkodar
  varje dag en gång per ögonblicksbild, även genom `showing`. Tills första läsningen
  är klar är ViewModelns state `null` och entryn ritar inget. Använd aldrig SharedPreferences för ny data.
  `RemindersStore` håller påminnelserna (på/av och tid). `trainingWeek` planerar veckan på samma sätt för
  Träningsfliken och träningspåminnelsen. `SelectedDay` är dagen en skärm visar och ändrar (i dag, eller en tidigare dag inom `EditableDays`, 30 dagar
  bakåt); Sömn och Kost har varsin i sin ViewModel och sparar på den dagen.
- `:core:reminders` – påminnelserna (logga sömn, träning, logga kost): `ReminderScheduler` lägger en engångsjobb i
  WorkManager per påminnelse vid nästa tid, `ReminderWorker` läser dagen, visar notisen bara om något fortfarande
  saknas (träning bara de dagar `trainingWeek` har ett pass) och lägger nästa dag själv. `ReminderWorkerFactory` ger
  workern storarna från Metro via `NovilaApplication` (`Configuration.Provider`). Se *Påminnelser* under Regler.
- `:core:healthconnect` – Health Connect som `MovementSource` (dagens steg och kalorier), `ExerciseSource`
  (`HealthConnectExerciseSource`, träningspass) och `SleepSource` (`HealthConnectSleepSource`, nätter), behörigheterna i modulens manifest (`HEALTH_PERMISSIONS`, frågas i en
  dialog via `rememberHealthPermissionRequest`) och integritetsskärmen (`HealthDataInfoActivity`)
- Ren Kotlin, utan Android (`novila.jvm.library`, testas på sekunder):
  - `:core:model` – `DayEntry` (`DayEntryCodec`), cykeln (`Cycle.kt`: `CycleLog`, `cyclePhase`, se *Cykeln* under Regler), sömnens regelbundenhet (`sleepRegularity`: Sleep Regularity Index och social jetlag över fyra veckor), kost, drycker (`Drink`, se *Drycker* under Regler), dagens balans, insikter, dagens sista logg (`LatestLog`) och uppmuntran (`cheers`), `analyzeSleep` (se *Sömnens samband*
    under Regler), `History` (dagarna över en
    `HistoryRange` i perioder, plus lika många dagar före att jämföra med; Sömn och Kost läser den via `HealthStore`), `Profile`, `Goals` (egna mål för
    steg, sömn och vatten; balans, insikter och kostpoäng mäts mot dem) och `AppSettings` (`ThemeMode`)
  - `:core:movement` – rörelsedata oberoende av källa: `MovementSource` (källa), `MovementCache` (cache) och
    `MovementRepository` (det enda UI:t läser). Källa och cache väljs på ett ställe, i `AppGraph`.
  - `:core:exercise` – träningspass oberoende av källa: `Exercise`, `ExerciseSource` och `TrainingProgramGenerator`, som
    bygger veckans program (WHO:s råd: 150–300 min kondition och styrka två dagar, högst +10 % kondition per vecka).
    `StrengthSessions` fyller styrkedagarna med övningar, set, repetitioner och vila efter nivå. `Workout` är passläget
    (uppvärmning, set, vila, nedvarvning och nedräkning). Timern körs i `WorkoutViewModel`, som också visar varje
    timed step som notis (`WorkoutNotifications`): nedräkningen tyst, och nästa set med ljud när tiden är slut.
    Avklarade pass sparas som `strengthMinutes` per dag,
    med ansträngning (RPE 1–10), smärta (0–10) och rehabfas om användaren svarar. Konditionspass loggas för hand
    ("Jag gjorde det", `DayEntry.cardio`). `ExerciseRepository` håller passen från en källa (Health Connect) i minnet, och
    `TrainingHistory` slår ihop dem med appens egna (per dag och slag räknas den sida som har mest tid, så samma
    promenad räknas en gång). Det blir generatorns historik.
    `trainingMinutesByDate` (i `:core:data`) ger minuterna per dag på samma sätt, för Träningens *Trender*
    (`TrainingTrendsScreen`: vecka, fyra veckor eller år med `TrainingHistoryCard`); klockans pass finns bara för de
    fem veckor som läses, så längre bak räknas det som loggats i Novila.
    `trainingLoad` är veckans belastning (session-RPE, Foster: ansträngning × minuter, monotoni och strain) mot de tre
    veckorna innan; pass utan användarens ansträngning räknas som måttliga, och jämförelsen är bara lugn vägledning,
    aldrig en varning (ACWR är omstritt som skadeprediktor).
    `Progression` höjer varje övning steg för steg (dubbel progression): passläget frågar hur många repetitioner
    varje set blev (`Workout.done`), resultaten sparas i `CoachStore` (`ExerciseLog`), och nådde alla set målet blir
    det en repetition (eller fem sekunder) mer nästa gång, ett set klart under ger ett steg tillbaka. Det läggs på
    styrkepassen i `trainingWeek`; rehabpassen doseras av smärtan och rörs inte.
  - `:core:sleep` – nätter från en klocka oberoende av källa: `Night`, `SleepSource`, `SleepRepository` (bara i minnet)
    och `nightsOf` (dagens längsta session på minst tre timmar, på morgonen användaren vaknade). `withNight` fyller
    i en dag utan egen sömn; användarens egen logg vinner alltid. Sömn och Idag visar dagarna så via
    `SavedDays.showing`, som aldrig sparar något.
  - `:core:recovery` – återhämtning från en klocka oberoende av källa: `RecoveryDay` (vilopuls och HRV per dag),
    `RecoverySource`, `RecoveryRepository` (bara i minnet) och `analyzeRecovery`, som jämför dagens värden med
    användarens egen baslinje (medianen av de senaste dagarna) och ger `RecoveryStatus`. Träning och coachen sänker
    dagens pass när läget är lågt.
  - `:core:coach` – coachen, helt på enheten: `CoachPreferences` (mål, dagar, tid per pass, fokus, hänsyn,
    redskap användaren har, rehabområde och fas; utan redskap blir passen kroppsvikt), `CoachCatalog`
    (rörelsemönster, dosering och belastning per övning) och `CoachPlanner`, som
    bygger veckans program ur svaren, senaste passen och övningsbiblioteket och förklarar valen (`CoachNote`). Målet
    `Rehab` ger ett handvalt, fasindelat program (knä, ländrygg, axel, nacke eller höft, fas 1–3) med smärtgräns och vårdråd.
    `CoachFeedback` läser svaren efter passen: ansträngning och smärta ändrar dosen (ett set mindre eller två
    repetitioner mer), och smärtan i rehabpassen ger ett förslag på fas (smärtmonitoreringsmodellen: högst 3 av 10 är
    okej, över 5 är för mycket). Coachen föreslår bara en ny fas; användaren byter. Svaren
    sparas i `CoachStore`; är coachen inte inställd gäller `TrainingProgramGenerator`. Planen är alltid deterministisk
    och testad, och förklarar sig själv med `CoachNote`. Dagens gemensamma förslag för sömn, träning och kost är
    `dailyCoach` (`DailyCoach`, `CoachAdvice`), som väger delarna mot varandra och är deterministisk och enhetstestad.
    Dagsformen (`readiness`, `Readiness`) läser natten, återhämtningen, träningsbelastningen och cykelns symtom som
    signaler som drar ner, upp eller inte alls, och räknar dem: två ner är en lugn dag, en ner en dag som planerat,
    ingen ner och två upp en kropp som verkar redo. Den visas aldrig som en poäng, bara med sina skäl i coachens
    mening på Idag (`CoachAdvice.CalmDay`, `ReadyDay`); det som inte är känt räknas aldrig som dåligt.
    Se *Ingen AI* under Regler.
  - `:core:breathing` – de guidade andningsövningarna som data: `BreathingSession` (faser med längd i sekunder och
    antal rundor), `BreathingCatalog` (lugn andning, boxandning, 4-7-8) och `momentAt`, som deterministiskt räknar ut
    runda, fas, sekunder kvar och hur långt in i fasen man är vid en förfluten tid. `breathScale` ger cirkelns storlek.
    Ren Kotlin och enhetstestad; spelarens timer i `:feature:breathing` ritar bara det modellen säger.
  - `:core:anatomy` – kroppsagnostiska övningsillustrationer: `Pose`/`Movement` (ledvinklar per keyframe), `Skeleton`
    (framåtkinematik, `BodyShape`), `MuscleMap`, `SceneBuilder` (ortografisk `Camera`, siluetter och de muskelband som
    syns), `ViewSelector` (varje övnings bästa vy), `Gear` (hantlar, gummiband och skivstång, ritade ur händerna),
    `ExerciseLibrary` och `RepetitionCounter` (räknar repetitioner, djup och tempo ur observerade ledvinklar; ingen
    kamera inkopplad än). `AnatomyFigure` i `:feature:training` ritar
    i Compose. Se `docs/anatomy.md`. `tools/anatomy/anatomy.sh` testar, renderar och löser vinklar utan Gradle.
  - `:core:recipes` – receptförslag på Kost: `Recipe` (Novilas egna recept, en portion: portioner per livsmedelsgrupp,
    `Diet`, de 14 märkningspliktiga `Allergen`, tid, `RecipeTime` och `FoodKind` för ingrediensfamiljerna),
    `FoodPreferences` (kosthållning, allergener och tid, sparas i `FoodPreferencesStore`) och `suggestRecipes`, som
    väljer recepten som höjer dagens kostpoäng mest för måltiden just nu (`MealSlot`) och roterar lika bra recept
    mellan dagar. Namn, ingredienser och steg ligger i `:feature:nutrition`s strängar. `RecipeDataTest` kräver minst
    40 recept och minst fem per måltid och kosthållning. Användarens egna recept (`OwnRecipe`, `RecipeBook`, sparade i
    `FoodPreferencesStore`) skrivs på Receptskärmen med autokomplettering ur måltidsloggningens ordlista
    (`completeIngredient`); portionerna räknas ur ingredienserna (`parseIngredients`) och allergenerna slås upp
    (`ingredientAllergens`) och bekräftas av användaren. Det användaren lagar (`withCooked`) styr förslagen.
    Se `docs/recipes.md` och *Recept* under Regler.
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
- `CI`-workflowen måste vara grön innan PR:en är klar. Den kör tre jobb parallellt: Format and lint (`spotlessCheck`
  och `:app:lintDebug`, alla moduler), Tests (`test verifyAndRecordRoborazziDebug koverXmlReportCoverage`) och Build
  (`:app:assembleDebug :app:checkDebugBadging :app:compileReleaseKotlin`); kontrollen *Lint, test and build* samlar dem och är den som krävs. Gemensamma steg ligger
  i `.github/actions/`. Cachen sköts av `gradle/actions/setup-gradle` (standardcachen, sparas bara från `main`), och
  CI-flaggorna för de publika runnerna (4 kärnor, 16 GB) ligger i `.github/ci-gradle.properties`. Bygg aldrig egen cachelogik.
  Release-bygget (R8) körs inte på PR:er. Ändrar du `proguard-rules.pro` eller R8-känslig kod,
  säg det i PR-beskrivningen. Fel syns då först vid merge.
- `Wear OS` (`wear.yml`) är klockappens egen workflow och den enda som rör `:wear`. Inga andra workflows bygger,
  testar, formaterar eller spelar in klockan, och `wear.yml` använder inget i `.github/actions/`. Lägg aldrig in
  klockan i de andra flödena eller tvärtom. **En push kör en pipeline, aldrig båda:** på en PR körs `wear.yml` bara
  när `wear/` eller `wear.yml` ändras, och `CI`:s jobb *Changes* hoppar över lint, tester och bygge när inget annat
  ändrats (den krävda kontrollen blir ändå grön; vid osäkerhet körs allt). `distribute.yml` ignorerar sådana merges.
  Det klockan bygger på (`:core:exercise`, `:core:recovery`, `:core:designsystem`, bygget) kontrolleras av `CI` på
  PR:en och av `wear.yml` efter merge till `main`. `wear.yml` kör med `-Pnovila.wear=true`: Spotless på `wear/`,
  lint, tester, skärmdumpar och debug-APK:n, och kollar i APK:n att klockappen aldrig kan ersätta telefonappen
  (paketet `se.novila.wear.debug`, kräver `android.hardware.type.watch`, inga egna behörigheter) och att klockan inte
  importerar telefonens Material 3. Klockans skärmdumpar spelas in med `/record-wear-screenshots` i PR-beskrivningen,
  eller genom att köra workflowen med *record* från Actions-fliken; saknas referenserna helt spelas de in vid första
  körningen. Telefonens `/record-screenshots` rör dem aldrig.
  Efter en sådan push startar den sig själv och `CI` (som på en ren klock-branch bara rapporterar). `wear.yml` är
  ingen obligatorisk kontroll, så titta på den i PR:er som rör klockan.
- Kan din miljö inte nå Google Maven: pusha och följ körningen (`gh pr checks`, `gh run view --log-failed`)
  och rätta tills den är grön. Säg aldrig att något fungerar utan grönt CI.
- `Dependency submission` skickar bibliotekens beroendegraf till GitHub vid varje push till `main`, så Dependabot larmar
  om sårbara bibliotek (fliken Security). Testklassvägar räknas inte. Kör bara upplösning, ingen kompilering.
- När PR:en mergas skickar `Distribute to testers` debug-APK:n (`se.novila.debug`) via Firebase App Distribution.
  Samma körning bygger även release (R8) som APK och som App Bundle (`.aab`, det Google Play tar emot), båda
  signerade bara om release-nyckeln (uppladdningsnyckeln i Play App Signing) finns i GitHub-hemligheterna. Bundlen
  och R8-mappningen ligger i körningens artefakt `novila-<nummer>`.

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
- **`dataStore.data` efter en skrivning:** lita inte på att `dataStore.data` sänder ut en sparad ändring om flödet
  börjar samlas in medan skrivningen pågår. Flödet hoppar då över versionen skrivningen gav, och den som väntar på
  värdet hänger tills tidsgränsen. Sätt snapshotten från värdet `edit`/`updateData` returnerar i samma enda köare som
  kör skrivningarna (se `PreferencesStore`), inte från en ny utsändning från `dataStore.data`.
- **Designspråket måste användas:** tokens som `NovilaTheme.pillars` och `accent` fanns men användes inte, så appen såg
  generisk ut. Komponenterna tar en `role`: ge den delens pillar (`NovilaTheme.pillars.sleep` osv.), och använd
  `NovilaAccentButton` för skärmens huvudåtgärd.
- **GitHub-loggar:** fullständiga Actions-loggar går inte att hämta härifrån. Läs CI-fel via annotations:
  `gh api repos/joeldenke/Novila/check-runs/<job-id>/annotations`.
- **gcloud i molnsessioner:** miljön sätter `CLOUDSDK_AUTH_ACCESS_TOKEN`, som skriver över `gcloud auth login`.
  Kör `unset CLOUDSDK_AUTH_ACCESS_TOKEN` efter inloggningen, annars svarar alla anrop `CREDENTIALS_MISSING`.
- **GitHub API:** GraphQL och `gh pr create` fungerar inte här. Använd REST (`gh api repos/.../pulls`).
- **Modifier-parametrar:** en composable får bara ha en `Modifier`-parameter, och den heter `modifier` (lint
  `ModifierParameter`). Behöver en del inuti egen form, ta värden (t.ex. `aspectRatio`) i stället för en andra
  `Modifier` som `figureModifier`.
- **Providers från tasks:** ett värde härlett ur en `TaskProvider` (`update.map { it.path }`) ger ett beroende på
  den tasken. Så körde `checkDebugBadging` först `updateDebugBadging`, som skrev över den granskade filen, och
  kontrollen gick alltid igenom (från #16 till #41). Ge tasks vanliga värden (strängar) för sådant som bara är text.
  CI jämför dessutom badgingen mot filen i git, och fallerar om bygget skriver till `app/badging`.
- **Beroenden som bara debug får:** `NovilaGlanceHero` använde `ResourcesCompat` utan att `:core:designsystem`
  berodde på `androidx.core`. Debug-bygget fick det transitivt via ett debug-beroende, så PR:en var grön men
  release-bygget i `Distribute to testers` kompilerade inte (#91). Deklarera varje bibliotek en modul importerar
  direkt i modulen. Build-jobbet kompilerar nu även release (`:app:compileReleaseKotlin`, utan R8).
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
  behöver ingen flagga. Sätt aldrig `workers.max` till 2 eller lägre på Gradle 9.8: med Isolated Projects fryser
  bygget när workers ≤ antalet byggen (roten och `build-logic`), gradle#39040. Standardvärdet är antalet kärnor,
  så på en 2-kärnig maskin blir det 2 och bygget står still i *Calculating task graph*: kör då
  `--max-workers=4`. CI har 4 kärnor och klarar sig med standardvärdet.
- Nya moduler: lägg till `include(...)` i `settings.gradle.kts`, beroende via `projects.<…>` (typsäkra accessorer).
- Nya Gradle-plugins deklareras med `apply false` i rotens `build.gradle.kts`, så att alla moduler delar en classloader.

## Tester och kodstil
- **Logik:** rena Kotlin-tester i `src/test`. **UI:** Compose UI-tester med `createComposeRule()` på Robolectric
  (`@RunWith(RobolectricTestRunner::class)`), också i `src/test`. Ingen emulator behövs.
- **Skärmdumpar:** varje `@Preview` testas mot referensbilder i modulens `src/test/screenshots`
  (t.ex. `feature/training/src/test/screenshots`). Filnamnet är paket, fil och preview.
  Ändrar du UI med flit: skriv `/record-screenshots` i PR-beskrivningen. Då spelar Tests-jobbet in ändrade bilder,
  pushar dem till branchen som en egen commit (*Record screenshot references*) och startar CI på den. Granska bilderna
  i PR:ens diff. Utan raden fallerar en ändrad skärmdump som vanligt. Beskrivningen läses vid varje push; har du lagt
  till raden efteråt, kommentera `/record-screenshots` på PR:en (workflowen `Record references`) i stället. Ta aldrig fram referensbilder lokalt; de ska renderas i CI.
- **Kodstil:** Spotless med ktlint (`android_studio`-stil) kontrolleras i CI. `./gradlew spotlessApply` rättar.
- **Täckning:** Kover, sammanfattning i CI-körningens summary. **Compose-stabilitet:** artefakten `compose-reports`.

## Regler
- **Designsystem:** feature-kod använder bara `se.novila.designsystem` och aldrig `androidx.compose.material3`
  direkt. Saknas en komponent, lägg till en `Novila*`-wrapper i `:core:designsystem` (`component/`).
  Använd `NovilaTheme.colors/typography/spacing/shapes`, aldrig hårdkodade färger eller dp-värden i features.
  Tokens är Novilas egna typer (`NovilaColors`, `NovilaTypography`, `NovilaShapes`), aldrig Material-typer, och
  `material3` är ett `implementation`-beroende i `:core:designsystem`. Wrappers exponerar inga Material-typer i
  sitt API (inga `ButtonColors`, `SheetState` o.d.). Behöver en skärm en färgroll som saknas, lägg till den i
  `NovilaColors` och `toNovilaColors()`.
- **Designspråk "Skog & sol":** nordiskt lugn. Typsnittet är Hanken Grotesk (Google Font, inbakad i
  `:core:designsystem`, licens i `assets/licenses`). Ladda aldrig ner typsnitt i runtime.
  Varje del har sin färg via `NovilaTheme.pillars`: träning = gran (primary), sömn = sjö (secondary),
  kost = falurött (tertiary). Solen (`NovilaTheme.accent`) används för högst en huvudknapp per skärm.
  Hörnradie från basen 14 dp via `NovilaTheme.shapes`. Nya färgpar läggs till i `ColorContrastTest` (WCAG 2.2).
  Förlagan är riktning D i designkanvasen *Novila designriktningar*. Idag svarar på "vad gör jag nu": hälsning,
  `NovilaHeroCard` (balansen som ringar per del, höjdkurvor och himlens sken, `NovilaTheme.hero`), *Din dag*
  (`dayChecklist` i `:core:model`: sömn, dagens pass ur `TrainingWeek.trainingOn`, kost och vatten som
  `NovilaTaskRow`, där varje öppen rad har en knapp som gör det och den första är skärmens `NovilaAccentButton`;
  coachens förslag `dailyCoach` står överst i kortet, utan det raderna redan säger) och veckans insikter med vägen
  till *Upptäckter* (alla insikter och sambanden mellan delarna) och *Trender* (`TrendsScreen`: sömn, kost och
  träning över samma period, med `trendFeedback` som jämför med perioden innan och väger delarna mot varandra).
  Insiktskortet visar också vad loggarna snart låser upp (`discoveryProgress`), som staplar som fylls.
  Veckodiagrammen hör till delarnas egna flikar, och mående (humör, energi, stress, dagbok) togs bort för att syftet var oklart;
  lägg inte tillbaka dem på Idag utan att ägaren ber om det.
  Hero-korten har himlen efter tid på dygnet (`NovilaSky.at(hour)`): morgonsol i ljusringar 05–10, sol med strålar
  10–17, solnedgång med appikonens snitt 17–22 och måne 22–05. Timmen kommer från skärmens ViewModel, aldrig från
  klockan i en composable, så previews och skärmdumpar visar en bestämd himmel. Rita aldrig granar eller annat
  landskap i hero-korten; bakgrunden är linjer (höjdkurvor på Idag, rörelselinjer på Träning).
  Kort är vita med hårfin kant (`NovilaTheme.surfaces`). Träning börjar med veckan i överblick på samma granpanel,
  med rörelselinjer i stället för höjdkurvor (`NovilaHeroPanel`: minuter mot veckans plan, `NovilaWeekStrip` och
  dagens enda huvudåtgärd ur `TodayPlan`). Sömn och Kost börjar på samma sätt (`NovilaHeroPanel` med
  `NovilaHeroPart`), var och en på sin egen botten med egna linjer: Sömn en nattsjö i sjöblått (krusningar mot en
  horisont och månens spegling i streck som smalnar av, alltid månen eftersom översikten gäller natten), Kost falurött
  med fåror som löper mot en horisont. Ringen visar timmar mot målet respektive kostpoängen, och
  `NovilaHeroBars` veckans nätter mot målet respektive dagarnas poäng. Poängens delar per livsmedelsgrupp ligger sist
  i matens sheet (`ScoreBreakdownCard`). Rörelse och återhämtning från Health Connect är ett kort
  (`HealthDataCard`) med en statuspill (`NovilaStatus`) och streck för värden som saknas; utan anslutning ligger det
  sist med en enda knapp.
- **Flikarnas startsidor:** en flik är en startsida på ungefär en skärm, inga formulär och inga lösa taggar
  (chips) direkt på den. Sömn, Kost och Träning byggs i samma ordning: dagväljaren (om man kan gå bakåt i dagar),
  *översikten* (läget och delens huvudåtgärd), *snabbloggning* (`NovilaQuickActions`: ett kort med rubrik och
  knappar som loggar med ett tryck eller öppnar ett `NovilaBottomSheet` med formuläret; inga lösa knappar direkt på
  startsidan) och *ingångarna* (`NovilaEntries`: coachen, programmet, recepten, trenderna och liknande, var och en med en rad om läget). Det en ingång öppnar är en egen skärm (rutt och
  ViewModel i featuren) med titel i toppraden. Taggar som väljer något (orsaker, dryckessorter, intervall) hör hemma
  i ett sheet eller på en sådan skärm. Nya kort läggs där, inte på startsidan; ett sheets innehåll får en egen
  `@Preview` via `NovilaSheetContent`, eftersom ett sheet öppnas i ett eget fönster.
- **Ingångarnas layout:** användaren väljer rutor eller rader under Inställningar (`AppSettings.entryLayout`).
  `MainActivity` ger valet till `NovilaTheme(entryLayout = …)` och `NovilaEntries` följer det; skärmarna vet inget om
  det. Lägg en `@Preview` med `NovilaEntryLayout.Rows` för nya startsidor.
- **Adaptiv layout:** appen ska fungera på surfplattor, vikbara och i liggande läge. Från `NovilaRailMinWidth`
  (600 dp, storleksklassen *medium*) ligger flikarna i en `NovilaNavigationRail` i stället för bottenraden, och
  `ScreenList` centrerar innehållet på högst 720 dp. `NovilaEntries` lägger två till fyra rutor per rad efter bredden.
  Lägg en `@Preview` med `widthDp = 900` för nya startsidor. Följ skillen `adaptive`.
- **Mina mål:** allt appen planerar efter samlas på en skärm (`MyGoalsScreen`), som öppnas från profilen: dagliga mål,
  träningscoachen, sömncoachen, matpreferenserna och *Följ din cykel*, var och en med vad den står på och en rad dit den ändras. Samma
  skärmar öppnas från ingångarna på varje flik. Ny inställning som styr en plan läggs till här, inte som en egen rad i
  Inställningar, som bara har det som gäller appen.
- **Profil:** `ProfileScreen` är en sida för personen: avatar (`NovilaAvatar`, initialer på granen), namn, födelseår
  och längd, kroppsmåtten (senaste värdena, loggas per dag i ett sheet, historik med viktkurva) och raden till
  *Mina mål*. Vikten skrivs bara via måtten; profilens vikt följer den senaste mätningen. Inställningar har en rad dit.
- **Målade scener:** introduktionens scener är målade bilder i samma nordiska stil (falröd stuga, björk, gran, sjö,
  midsommarljus), WebP i featurens `drawable-nodpi`, beskurna till 1,4:1, högst cirka 1120 px breda och helst under 200 kB. De visas bara via
  `NovilaSceneImage`, som sköter hörn, entré och parallax. I mörkt läge blir dagscener skymning (`ColorMatrix`:
  mindre mättnad och ljus, aldrig en grå slöja) och nederkanten flyter ut i ytan; nattskogen och solnedgången behåller sitt ljus. Rita inte landskap i kod: det har
  provats och når inte upp till målade bilder. Nya scener tas fram i samma stil som de befintliga.
- **Inga medicinska påståenden:** appen ger allmänna råd och ska inte låta som vård (medicintekniska regler, MDR).
  I texter och på butikssidan heter målet `Rehab` *skonsam träning* och dess faser *nivåer*;
  skriv aldrig rehab, rehabilitering, behandling, terapi eller att appen läker, lindrar eller hjälper mot en
  sjukdom eller besvär (sömnlöshet, skada). Råden om när man ska söka vård behålls. Koden får heta `Rehab`.
- **Appikonen:** ett vitt N på granplattan, med solen i skåran och solnedgången som tre snitt genom N:ets nedre del
  (snitten blir tjockare nedåt). Vektorerna i `app/src/main/res/drawable/ic_launcher_*` är källan och
  `mipmap-anydpi` den adaptiva ikonen. Ändras de: kör `python3 tools/icon/render_icon.py`, som ritar WebP-reserverna
  i `mipmap-*dpi` (Firebase App Distribution kan inte visa en adaptiv ikon) och Play-ikonen `docs/play-icon.png`.
  Rita aldrig om dem för hand.
- **Inga emojis i UI:t.** Novila ska kännas som en professionell hälsoapp: använd vektorikoner och ritade komponenter
  (t.ex. `NovilaLevelPicker` för nivåer), aldrig emoji-tecken.
- **Övningsillustrationer:** en övning är data (keyframes med ledvinklar i `ExerciseLibrary`), aldrig video eller
  handritad animation, så samma rörelse fungerar för varje kropp och kamera. Ny eller ändrad övning: följ skillen
  `add-exercise` (lösta vinklar, kamera från `ViewSelector`, texter på båda språken). `AnatomyTest` kontrollerar att
  planterade händer och fötter står still och säger vilken kamera som gäller. Figuren färgas bara med `NovilaTheme.anatomy` (sjöblå kropp, glödande muskler) och
  spelar ett bestämt antal repetitioner, aldrig i loop. I övningsvyn har den spela/pausa och ett reglage för att dra
  genom rörelsen (`AnatomyPlayer`), som en video men ritad ur samma data.
- **Tema:** användaren väljer Automatiskt/Ljust/Mörkt och bakgrundsfärger (Android 12+) under Inställningar.
  `MainActivity` läser `SettingsStore` och ger `NovilaApp` `darkTheme`/`dynamicColor`, och systemfälten följer appens
  tema. Ritar inget förrän inställningarna är lästa, så temat aldrig blinkar.
- **Rörelse:** lugn och engångs. Animationer spelas när innehållet visas (`NovilaReveal`, diagram som ritar in sig)
  via `NovilaMotion` i `:core:designsystem`. Inga oändliga animationer: de får skärmdumpstesterna att hänga.
  Mellan skärmar (`NovilaTransitions`, kopplat i `NovilaApp`): djupare in i en flik och tillbaka glider skärmarna
  några procent längs samma axel och tonar över varandra, flikbyten tonar igenom utan sidled (`Navigator` sätter
  `switchedTab`), och prediktiv bakåt följer fingret. En ingång (`NovilaEntry.sharedKey`) växer till skärmen den
  öppnar och krymper tillbaka (`NovilaSharedBounds`): ge rutan och entryn samma `SharedScreen`, entryn via
  `entry<Rutt>(metadata = sharedScreen(SharedScreen.X))`. Utan app-navigation (previews, skärmdumpar) är det en vanlig
  ruta. Lägg aldrig egna `transitionSpec` per skärm.
  I en pager styrs rörelsen av fingret, läst när det ritas: `Modifier.novilaPagerDepth` (block i lager som tonar
  över), `NovilaPagerBackdrop` (delens färg tonar till nästa) och `NovilaPageIndicator(offset = …)`. Lagren och
  indikatorn står still när sidan gör det, så previews och skärmdumpar visar slutläget.
- **Sömnens samband:** `analyzeSleep` (`SleepAnalysis`) parar varje natt med dagen före (kostpoäng, vatten mot målet,
  träning, sött) och med orsakerna användaren angav för natten (`SleepFactor`), över de senaste 90 nätterna. Ett
  samband (`SleepLink`) visas bara med minst `MIN_NIGHTS` nätter på varje sida, en skillnad som märks (20 min eller ett
  halvt steg i kvalitet) och Welchs t ≥ 2, och alltid med antalet nätter och "samband, inte bevis". En dag utan loggar
  är okänd, aldrig en dag utan träning. Veckans förändring (`SleepChange`) visas bara tillsammans med det som ändrades
  med den, och säger om de tidigare nätterna bekräftar det. Ändra trösklarna bara medvetet: `SleepAnalysisTest`.
- **Kostpoäng:** `nutritionScore()` (0–100) viktar livsmedelsgrupper efter Livsmedelsverkets kostråd (`FoodGroup`) med
  avdrag för sött och snacks. Ändra vikterna bara medvetet: `NutritionTest` och dagens balans bygger på dem.
- **Drycker:** vätska loggas som `Drink` (slag, ml, klockslag) i `DayEntry.drinks`, per glas, flaska eller valfri mängd
  (`DrinksCard` på Kost). Vatten, kaffe, te och mjölk räknas mot vattenmålet (`DrinkKind.hydrates`, `waterGlasses` är
  hela glas à `Goals.GLASS_ML`); juice, läsk och energidryck loggas men räknas inte. Koffein från kl. 14 räknas som sent
  koffein i `analyzeSleep` (`DayEntry.lateCaffeine`), med samma trösklar som andra samband. Appen räknar aldrig ut
  koffein i mg, näringsämnen eller kosttillskott: det kräver en livsmedelsdatabas och närmar sig hälsopåståenden
  (förordning 1924/2006). Lägg inte till det utan att ägaren ber om det.
- **Recept:** förslagen väljs alltid deterministiskt ur `Recipe` och användarens egna recept (testat i
  `RecipeSuggestionsTest` och `OwnRecipeTest`), aldrig genererade av en språkmodell: ett påhittat
  recept kan innehålla något användaren är allergisk mot. Ett recept som innehåller en av användarens allergener föreslås aldrig, och kortet ber alltid användaren
  kontrollera ingredienserna själv. Nytt recept: ange portioner (grönsaker cirka 100 g per portion, potatis räknas
  inte), diet, alla allergener i ingredienserna, tid och ingrediensfamiljer (`FoodKind`), plus namn, ingredienser och
  steg på båda språken i `Recipe`-enumen, `RecipeText.kt` och strängarna. Följ `docs/recipes.md`. Inga recept "för"
  en sjukdom eller ett besvär (se *Inga medicinska påståenden*). Matpreferenserna är hälsodata och raderas med
  resten.
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
  `READ_ACTIVE_CALORIES_BURNED`), de senaste fem veckornas träningspass (`READ_EXERCISE`: typ, dag och längd, för
  träningsprogrammet) och nätterna upp till två år bakåt (`READ_SLEEP`: läggtid, uppvakning och tid i sömn, för
  Sömn och Idag), samt vilopuls och HRV de senaste två veckorna (`READ_RESTING_HEART_RATE`,
  `READ_HEART_RATE_VARIABILITY`, för Träningens återhämtning), i förgrunden. Inget skrivs, ingen bakgrundsläsning och
  ingen `READ_HEALTH_DATA_HISTORY`
  (Health Connect ger 30 dagar före första godkännandet, det räcker). Varje flik läser vid resume, så samma läsning
  återanvänds i `FRESH_FOR` (två minuter) inom appen; den glöms när appen lämnar skärmen (`MainActivity.onStop`) och
  efter en behörighetsdialog, så en återkallad behörighet aldrig visar gammal data. Cachen ligger bara i minnet
  (`InMemoryMovementCache`, `ExerciseRepository`, `SleepRepository`, `RecoveryRepository`); pass, nätter och
  återhämtning från Health Connect sparas
  aldrig i `HealthStore`. En cache
  på disk ändrar vad integritetsskärmen och Play-deklarationen ska säga, så den kräver ägarens ok. Ny källa:
  implementera `MovementSource`, `ExerciseSource` eller `SleepSource` och byt den i `AppGraph`.
  Varje datatyp måste motiveras i Play Consoles *Health apps declaration*, så lägg inte till typer eller
  behörigheter utan att ägaren ber om det. `HealthConnectManifestTest` håller manifestet och `HEALTH_PERMISSIONS`
  i synk. `HealthDataInfoActivity` (integritetsskärmen Health Connect länkar till) är obligatorisk och ska
  beskriva exakt vad appen läser; ändras läsningen ändras texten.
- **Google Health API:** `GoogleHealthExerciseSource` läser bara träningspass (scope
  `googlehealth.activity_and_fitness.readonly`, restricted: kräver Googles verifiering före publik release). Den är inte
  inkopplad än. Att koppla in den kräver ägarens ok och, i samma PR: Google-inloggning som `AccessTokenProvider`,
  behörigheten `INTERNET` (ny rad i badging), uppdaterad integritetsskärm och Play-deklaration, och skillen
  `android-permissions-security`. Svaren tolkas i `GoogleHealthJson`; okända fält ignoreras.
- **Ingen AI:** appen har ingen språkmodell. Coachens kommentar med Gemini Nano togs bort i oktober 2026: den gav
  lite utöver coachens egna skäl, och Play kräver att AI-genererat innehåll går att rapportera inifrån appen, vilket
  inte går utan `INTERNET`. Planen, skälen och insikterna är deterministiska och förklarar sig själva. Att lägga
  tillbaka en modell, på enheten eller i molnet, kräver ägarens ok, rapportering enligt Plays policy för AI-genererat
  innehåll, ny integritetstext och ny Play-deklaration.
- **Cykeln:** *Följ din cykel* i *Mina mål* (`CycleScreen`) är avslagen från början, ligger i en egen fil
  (`CycleStore`) och raderas när den stängs av. Användaren loggar mensdagar och symtom i ett sheet på Träning; en dag
  med symtom och ett pass erbjuder coachen ett lättare pass (`SessionChoice`, `CoachNote.Lighter`) men byter aldrig
  själv. Återhämtningen (`recoveryStatus`) jämför en dag bara med dagar i samma halva av cykeln (`cyclePhase`), så en
  vanlig lutealfas inte läses som dålig återhämtning; Health Connect ger bara två veckor, så aldrig "samma fas förra
  cykeln". Appen förutsäger aldrig ägglossning eller fertila dagar (preventivmedels- och fertilitetsappar är
  medicintekniska), planerar aldrig träning efter fas och frågar aldrig om kön. Mensdagar från Health Connect
  (`READ_MENSTRUATION`) kräver ägarens ok.
- **Påminnelser:** bara WorkManager, ett engångsjobb per påminnelse och dag (`ReminderScheduler`). Inga
  periodiska jobb, ingen tjänst i förgrunden, inga exakta larm (`SCHEDULE_EXACT_ALARM`) och inget som körs mellan
  påminnelserna. Workern läser bara det som redan är sparat, visar notisen om något saknas och lägger nästa dag; den
  lyckas alltid, eftersom nästa dag hänger på den. Kommer den mer än två timmar sent skippas notisen. `POST_NOTIFICATIONS`
  frågas först när användaren slår på en påminnelse, aldrig vid start, och om notiser får visas kollas varje gång
  (aldrig sparat). WorkManagers standardstart är borttagen i appens manifest (lint `RemoveWorkManagerInitializer`);
  appen ger konfigurationen.
  Nya workers skapas av en fabrik i grafen och behöver en `-keepnames`-regel i `app/proguard-rules.pro`.
- **Widget:** Glance, färgad bara med `NovilaGlanceTheme`/`NovilaGlance`/`NovilaGlanceHero` (pillars som
  färgroller), texter från `strings.xml`. Den ska se ut som Idags hero-kort: rita panelen och ringarna i
  `NovilaGlanceHero` (RemoteViews kan inte rita), aldrig en egen, enklare variant. Skärmdumparna är
  `WidgetScreenshotTest` (Glance komponerar RemoteViews som fångas med Roborazzi), eftersom Glance saknar
  `@Preview` som skannern hittar. Det widgeten säger om dagen kommer från `cheers` i `:core:model` (testat i
  `DayHighlightsTest`) och påstår aldrig mer än appen vet: ingen väderdata (ingen `INTERNET`), så solen nämns bara
  via årstiden. Uppdateras när appen sparar (`WidgetUpdates`) och av systemet en gång i timmen, för midnatt; inga
  egna jobb. Nya knappar är en `ActionCallback` med en keep-regel i `app/proguard-rules.pro`.
- **Integritetspolicy och Play:** policyn finns i appen (`PrivacyPolicyScreen`, strängarna `privacy_*`) och som
  publik text i `docs/privacy-policy.md`; ändra båda och datumet. `docs/play-console.md` håller svaren för Data
  safety, Health apps declaration och Health Connect-behörigheterna. Ändras vad appen läser, sparar eller skickar,
  eller tillkommer en behörighet, uppdatera policyn, `HealthDataInfoActivity`s text och `docs/play-console.md` i
  samma PR. Text före en Health Connect-dialog ska nämna alla typer dialogen frågar om.
- **Wear OS:** `:wear` är en egen app (`se.novila.wear`, minSdk 30) med samma designspråk men utan flikar, navigation
  eller sparade data. Den får bara köra ett pass ur `StandaloneSession` och visa `Workout`-stegen; ingen Wearable Data
  Layer, ingen Health Connect och ingen lagring på klockan, eftersom det skulle ändra integritets- och Play-deklarationen.
  Delning med telefonen (Wearable Data Layer eller Health Connect) kräver ägarens ok. `NovilaWearTheme` mappar
  `NovilaTheme` till Compose for Wear OS Material 3, alltid mörkt (Wear OS har inget ljust tema); klockan får inte
  importera `androidx.compose.material3` själv. Skärmen ligger i `AppScaffold` och `ScreenScaffold` och listan tar
  scaffoldens `contentPadding`, så inget hamnar under klockan eller utanför en rund skärm. Rader med flera knappar
  delar bredden (`weight`). Varje `@Preview` blir en skärmdump, på alla klockformer för skärmar med mycket text.
- Hälsodata stannar på enheten. Inga nätverksanrop, analysverktyg eller Firebase-SDK:er i appen utan att ägaren ber om det.
  Appen har ingen `INTERNET`-behörighet, och den tas bort i appens manifest (`tools:node="remove"`), så inget
  bibliotek kan be om den, till exempel för att skicka användningsstatistik. Lägg aldrig tillbaka den
  utan ägarens ok; Google Health API-källan kräver den, se ovan.
- Pinna GitHub Actions till commit-SHA med versionskommentar. Dependabot håller dem uppdaterade.
