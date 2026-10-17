# Novila

[![CI](https://github.com/joeldenke/Novila/actions/workflows/ci.yml/badge.svg)](https://github.com/joeldenke/Novila/actions/workflows/ci.yml)

*[English](README.md)*

Novila är en hälsoapp för Android som samlar träning, sömn och kost på ett lugnt, nordiskt ställe. Den planerar din
träningsvecka, följer hur du sover och äter och visar hur delarna balanserar varandra varje dag. Allt stannar på
telefonen: inget konto, inget moln, inga analysverktyg. Appen finns på svenska och engelska.

## Vad appen gör

| Del | Funktioner |
|---|---|
| **Idag** | Hälsning med namn, dagens balans som en ring per del, en ruta per del med veckans trend, insikter, veckans sömn och kost i diagram, och mående |
| **Träning** | Veckoprogram efter WHO:s råd (150–300 min kondition, styrka två dagar i veckan, högst +10 % kondition per vecka). En coach på enheten som bygger veckan ur ditt mål, dina dagar, tid per pass, fokus och hänsyn, med fasindelade rehabprogram för knä och ländrygg. 13 illustrerade övningar och ett passläge med set, vila och nedräkning. Konditionspass loggas med "Jag gjorde det" eller läses från Health Connect, och efter passet ändrar ansträngning (RPE) och smärta dosen och ger förslag på nästa rehabfas |
| **Sömn** | Läggtid och uppvakning (eller bara timmar), hur natten kändes och vad som kan ha påverkat den, och vad som påverkar din sömn: samband mellan dagen före (kost, vatten, träning, sött), kvällen och natten, prövade på dina egna loggar och visade med nätterna bakom. En graf över veckan, fyra veckor eller ett år jämförd med perioden innan |
| **Kost** | Portioner per livsmedelsgrupp, vatten och sött, poäng 0–100 efter Livsmedelsverkets kostråd, ett förslag på nästa steg, och poängen över veckan, fyra veckor eller ett år jämförd med perioden innan |
| **Rörelse** | Dagens steg och aktiva kalorier, och de senaste fem veckornas träningspass, från Health Connect, bara medan appen är öppen |
| **Påminnelser** | Logga sömnen, träna och logga kosten, var och en på egen tid och bara om det inte redan är loggat. Ett WorkManager-jobb per påminnelse och dag, inget annat i bakgrunden |
| **Widget** | I dag på hemskärmen: dagens balans, sömn, kostpoäng och vatten, det senaste du loggat, något uppmuntrande om det du gjort (en sommarpromenad nämner D-vitamin, ett styrkepass nattens sömn) och ett glas vatten med ett tryck |
| **Inställningar** | Tema (automatiskt, ljust, mörkt, bakgrundsfärger), profil, Health Connect, ansvarsfriskrivning, radera all data och introduktionen igen |

En kort illustrerad introduktion vid första start ställer in mål, profil, coach och Health Connect, så att planen
finns från dag ett. Vad som kommer härnäst står i [roadmapen](docs/roadmap.md).

## Integritet från grunden

- **Datan stannar på enheten.** Dagar, coachens svar och inställningar ligger i Preferences DataStore. Appen gör
  inga nätverksanrop och har inga analysverktyg eller Firebase-SDK:er.
- **Health Connect läses minimalt och bara läses:** dagens steg och aktiva kalorier (`READ_STEPS`,
  `READ_ACTIVE_CALORIES_BURNED`) och de senaste fem veckornas träningspass (`READ_EXERCISE`: typ, dag och längd, för
  träningsprogrammet), medan appen är öppen. Inget skrivs, ingen bakgrundsläsning, ingen extra historikåtkomst, och
  det som läses ligger bara i minnet. Integritetsskärmen som Health Connect länkar till säger exakt det.
- **Påminnelserna är av tills du slår på dem.** Notiser frågas först då. Varje påminnelse är ett WorkManager-jobb
  om dagen som läser det som redan finns sparat på telefonen och visar en notis bara om något saknas.
- **AI:n körs på telefonen.** Träningsplanen är deterministisk, testad Kotlin. Gemini Nano (via AICore) skriver
  bara en kort kommentar om den färdiga planen, prompten lämnar aldrig enheten och svaret visas med en
  AI-reservation. Modellen väljer aldrig övningar, doser eller rehabråd.
- **APK:n granskas.** CI jämför APK:ns behörigheter, SDK-nivåer, features och exporterade komponenter med en
  incheckad fil och fallerar på minsta skillnad, så inget bibliotek kan smyga in en behörighet.

## Teknik

| Område | Val |
|---|---|
| Språk | Kotlin 2.4 (AGP 9:s inbyggda Kotlin) |
| Bygge | Gradle 9.8, AGP 9.4, convention plugins i `build-logic`, versionskatalog, configuration cache, build cache och Isolated Projects |
| UI | Jetpack Compose (BOM 2026.09) med eget designsystem ovanpå Material 3 1.5 |
| Navigation | Navigation 3, en back stack per flik |
| Arkitektur | En ViewModel (`StateFlow`) per skärm, scopad till sin navigationsentry; tillståndslösa skärmar som går att förhandsgranska |
| Dependency injection | [Metro](https://zacsweers.github.io/metro/), en Kotlin-kompilatorplugin som kopplar ihop allt vid kompilering |
| Lagring | Preferences DataStore med versionerat format som migreras |
| Hälsodata | Health Connect; en källa för Google Health API (REST) finns men är inte inkopplad än |
| AI på enheten | Gemini Nano via ML Kit GenAI Prompt API |
| Plattform | minSdk 26, compileSdk och targetSdk 37 |
| Tester | JUnit, Robolectric och Compose UI-tester, Roborazzi-skärmdumpar av varje `@Preview`, täckning med Kover |
| Kodkvalitet | Spotless med ktlint, Android lint med projektets policy (`lint.xml`), arkitekturtester |
| CI/CD | GitHub Actions (SHA-pinnade, Dependabot), Firebase App Distribution till testare, R8 på release |

### Design

Designspråket *Skog & sol* är nordiskt lugn: typsnittet Hanken Grotesk inbakat i appen, vita kort med hårfin kant och
en färg per del: gran för träning, sjö för sömn och falurött för kost. Solen markerar högst en huvudknapp per skärm.
Animationer spelas en gång när innehållet visas, aldrig i loop. Färgparen testas mot WCAG 2.2-kontrast. Övningarna
ritas från data, inte video: varje övning är keyframes med ledvinklar på ett kroppsagnostiskt skelett, ritat med de
muskler som arbetar ([docs/anatomy.md](docs/anatomy.md)).

### Moduler

Beroenden går nedåt: `:app` → `:feature:*` → UI-modulerna i `:core:*` → ren Kotlin. Features beror aldrig på
varandra, vilket ett test kontrollerar.

```
:app                     Bara skalet: Metro-grafen, aktiviteten, flikarna och navigationen
:feature:today           Dashboarden: balans, rutor, insikter, diagram, mående
:feature:training        Veckoprogram, coach, övningar, passläge
:feature:sleep           Sömnlogg och historik
:feature:nutrition       Livsmedelsgrupper, vatten, poäng
:feature:onboarding      Introduktionen vid första start
:feature:settings        Inställningar
:feature:widget          Widgeten på hemskärmen (Glance)
:core:designsystem       NovilaTheme och Novila*-komponenter (enda modulen som använder Material 3 direkt)
:core:ui                 Delat UI: listor, formatering, historikkort, previewdata
:core:reminders          Påminnelser: WorkManager-jobb och notiser
:core:navigation         Back stack per flik, Navigator
:core:data               Lagring i DataStore och coachens Gemini Nano-modell
:core:healthconnect      Health Connect som källa för rörelse och träningspass, integritetsskärmen
:core:model              Ren Kotlin: dagar, kostpoäng, dagens balans, insikter, inställningar
:core:movement           Ren Kotlin: rörelsedata oberoende av källa
:core:exercise           Ren Kotlin: övningar, veckoprogrammet, passläget
:core:coach              Ren Kotlin: träningscoachen och rehabprogrammen
:core:anatomy            Ren Kotlin: poser, skelett, muskler och kameror för övningsillustrationerna
:core:googlehealth       Ren Kotlin: Google Health API som träningskälla (inte inkopplad än)
```

Logik som inte behöver Android ligger i rena Kotlin-moduler, som kompilerar och testar på sekunder.

## Så byggs appen

Appen utvecklas helt från mobilen: ingen Android Studio, ingen emulator. [Claude Code](https://claude.ai/code)
skriver koden i en branch och öppnar en PR, och CI är det enda beviset på att den bygger och fungerar:

1. **CI** kör format och lint, enhets- och skärmdumpstester med täckning och ett debug-bygge med APK-kontrollen,
   som parallella jobb. Säger PR:en att UI:t ändras med flit spelar CI in de nya skärmdumparna och pushar dem till PR:en för granskning.
2. PR:en mergas när CI är grön.
3. **Distribute to testers** bygger debug och release (R8) och skickar debug-appen till testarna via Firebase App
   Distribution, med release notes från de mergade PR:erna.

`CLAUDE.md` innehåller projektets regler och kända fallgropar, och Googles
[Android-skills](https://github.com/android/skills) är aktiverade för Claude Code i repot.
