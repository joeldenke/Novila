---
name: add-exercise
description: Lägg till eller ändra en övning i Novilas anatomibibliotek (ExerciseLibrary), med lösta ledvinklar, muskler, bästa kameravy, texter på svenska och engelska och, om den ska ingå i veckans program, en plats i StrengthSessions. Använd när någon vill ha en ny övning, ändra hur en övning ser ut eller rör sig, eller byta övningar i styrkepassen.
---

# Lägg till en övning

Bakgrund och konventioner finns i `docs/anatomy.md`. Läs avsnitten *Koordinater och vinklar* och *Kontakter*
först. Allt nedan går att verifiera utan Gradle med `tools/anatomy/anatomy.sh`.

## 0. Passar övningen riggen?
- Riggen kan böja och lyfta lemmar framåt, bakåt och åt sidan, böja bålen och nacken, luta hela kroppen
  fram och bak (stående, liggande på mage eller rygg, fyrfota) och ställa fötter och händer mot golvet.
- Den kan **inte** rulla kroppen åt sidan (sidoplanka, sidoliggande), vrida lemmar eller bålen, eller
  rita redskap (hantlar, stol, gummiband). Behövs det, säg det till ägaren och föreslå en utbyggnad av
  `Pose` och `Skeleton` i en egen PR i stället för att fuska med vinklarna.
- Välj övningar som fungerar hemma med kroppsvikt, i linje med resten av biblioteket.

## 1. Skriv rörelsen i `ExerciseLibrary.kt` (`core/anatomy`)
- Lägg till en `val` i `ExerciseLibrary` och i listan `all`. `id` skrivs i snake_case och ändras aldrig efteråt,
  eftersom program och texter använder det.
- Gör en repetition av `Keyframe(at, pose)` från 0 till 1, där den sista keyframen är samma pose som den första.
  Pausa i toppläget med två keyframes som har samma pose (till exempel 0.45 och 0.58), och sätt `peak`
  mitt i pausen.
- Statiska övningar (planka) har två identiska keyframes. `isHold` döljer då *Spela igen*.
- Sätt `contacts` (det som vilar på golvet hela repetitionen) och `anchor` (det som aldrig rör sig).
  Rör sig fötterna, som i stjärnhopp, sätt `anchor = null`. Lämnar vissa punkter golvet ibland, sätt
  `shadowUnder` så att skuggan ändå hamnar under hela kroppen.
- `primary` är de muskler som arbetar mest och `secondary` de som hjälper till. Grupperna får inte överlappa.
- Tempo: `repetitionMillis` är 3 200 som standard, lugnt och kontrollerat. Snabba konditionsövningar har
  1 400–1 600.
- Lägg gemensamma poser som `private val` längst ner i filen med en KDoc-rad som beskriver ställningen.
  Återanvänd befintliga poser där det går (`PUSH_UP[0]` är planka, `BRIDGE[0]` ligger på rygg).

## 2. Lös vinklarna när fler än ankaret står i golvet
Gissa inte. Skriv en liten fil i scratchpad och lös vinklarna med `PoseSolver`:

```kotlin
import se.novila.anatomy.BodyPart
import se.novila.anatomy.Landmark
import se.novila.anatomy.LimbPose
import se.novila.anatomy.Pose

fun main() {
    val knee = Landmark.LeftKnee
    val hand = Landmark.LeftHand
    // Okända: lean, armens raise. Villkor: handen i golvet och armen lodrät.
    val (lean, raise) = PoseSolver.minimize(doubleArrayOf(80.0, 80.0)) { (lean, raise) ->
        val pose = Pose.symmetric(
            leg = LimbPose(raise = lean.toFloat(), bend = 90f),
            arm = LimbPose(raise = raise.toFloat()),
            lean = lean.toFloat(),
            flatHands = true,
        )
        val arm = PoseSolver.body(pose, knee).segments.first { it.part == BodyPart.UpperArm }
        PoseSolver.squared(PoseSolver.heightAboveFloor(pose, hand, knee)) * 1000 +
            PoseSolver.squared(arm.start.z - arm.end.z) * 100
    }
    println("lean %.1f raise %.1f".format(lean, raise))
}
```

```bash
tools/anatomy/anatomy.sh run /path/to/Solve.kt
```

- Residualen ska vara nära 0. Avrunda till en decimal i biblioteket och skriv i KDoc att vinklarna är lösta.
- Glider en kontakt mellan två keyframes, lös en mellanliggande keyframe med samma metod. Armhävningen
  har sex poser av det skälet.

## 3. Låt `ViewSelector` välja kameran
```bash
tools/anatomy/anatomy.sh views mitt_id
```
- Sätt `camera` till den kamera skriptet anger. `AnatomyTest` fallerar annars och anger rätt `Camera(...)`.
- Visar vyn inte huvudmusklerna, fallerar testet *the chosen view shows the main muscles at the peak*.
  Ändra då posen så att lemmarna inte skymmer musklerna (crunchen sträcker armarna mot knäna av det skälet).
  Ändra inte vikterna i `ViewSelector` för en enskild övning. De är kalibrerade på hela biblioteket.

## 4. Granska bilden
```bash
tools/anatomy/anatomy.sh render /path/to/ark.png mitt_id
```
Öppna PNG:en och kontrollera:
- att ingen kroppsdel går ner i golvet
- att händer och fötter står still
- att rörelsen går att känna igen
- att tumnageln (sista rutan) visar övningen i dess tyngsta läge

## 5. Texter
- Lägg till `move_<id>`, `move_<id>_steps` (en `string-array` med tre steg) och `move_<id>_tip` i både
  `values/strings.xml` och `values-sv/strings.xml` i `feature/training/src/main/res`. Skriv kort och lugnt, som en fysioterapeut, i du-form och
  utan emojis.
- Lägg till id:t i `moveText()` i `ExerciseCards.kt` (`:feature:training`). `ExerciseCardsTest` kontrollerar att varje
  övning har texter.
- Behövs en ny muskelgrupp: lägg till den i `MuscleGroup`, ge den en `MuscleRegion` i `MuscleMap` (testet
  *every muscle group has a place on the body*), en `muscle_<namn>`-sträng på båda språken och en gren i
  `MuscleGroup.label`.

## 6. Ska övningen ingå i veckans program?
- Byt eller lägg till den i `SESSION_A` eller `SESSION_B` i `StrengthSessions.kt` (`:core:exercise`), med rätt `Dosage`
  (`Seconds` för hållningar och kondition) och `perSide` för ensidiga övningar.
- `StrengthSessionsTest` kräver att lår, säte, bröst och mage tränas båda styrkedagarna på varje nivå, och att
  pass B skiljer sig från pass A.
- **Alltid:** lägg till övningen i `CoachCatalog.kt` (`:core:coach`) med rörelsemönster (`MovementPattern`), `Dosage`, `perSide`,
  lägsta nivå och om den belastar knän, rygg eller är ett hopp. Coachen väljer bara bland övningar i katalogen, och
  `CoachPlannerTest` kräver att varje övning i biblioteket finns där.

## 7. Verifiera och skicka
```bash
tools/anatomy/anatomy.sh test
```
- Kör ktlint, eller `./gradlew spotlessApply` om Gradle fungerar.
- Öppna en PR enligt CLAUDE.md med `/record-screenshots` i beskrivningen, eftersom övningsbanken och
  förhandsvisningarna ändras. CI spelar in de ändrade skärmdumparna, pushar dem och kör igen; granska bilderna i diffen. Säg aldrig att det fungerar förrän CI är grönt.
