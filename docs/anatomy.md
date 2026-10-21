# Anatomifiguren och övningarna

Novila visar övningar på en kroppsagnostisk figur: en sjöblå kropp med de tränade musklerna i glödande orange,
sedd från varje övnings bästa vinkel. Allt är data och ren Kotlin. Det finns ingen video, inga handritade
animationer och ingen 3D-motor. Samma rörelse fungerar därför för varje kroppsform och kameravinkel.

Vill du lägga till en övning, följ skillen [`add-exercise`](../.claude/skills/add-exercise/SKILL.md). Den här
sidan förklarar hur det fungerar.

## Kedjan

```
Keyframe(at, Pose) ──► Movement.poseAt(progress)      ledvinklar, utjämnade mellan keyframes
        Pose ──► Skeleton.pose(pose, BodyShape)       framåtkinematik: segment och landmärken
  PosedBody ──► ExerciseMotion.bodyAt()                flyttad så att ankaret står still
  PosedBody ──► SceneBuilder.build(camera, muskler)    siluetter bakifrån och fram, synliga muskelband
      Scene ──► AnatomyFigure (Compose Canvas)         färger från NovilaTheme.anatomy
```

| Fil (`core/anatomy/`, ren Kotlin, `se.novila.anatomy`) | Ansvar |
|---|---|
| `Geometry.kt` | `Vec3`, `Point2`, `Rotation` (`aboutX/Y/Z`) |
| `Pose.kt` | `LimbPose`, `Pose`, `Keyframe`, `Movement` |
| `Body.kt` | `BodyShape`, `Skeleton` (proportioner och kinematik), `Segment`, `Landmark` |
| `Muscles.kt` | `MuscleGroup` och `MuscleMap` (var varje grupp sitter på kroppen) |
| `Scene.kt` | `Camera`, `SceneBuilder`, `Layer`, golvskugga |
| `ViewSelector.kt` | väljer varje övnings bästa kamera |
| `ExerciseMotion.kt` | en övning: rörelse, muskler, kontakter, ankare, kamera, redskap |
| `Gear.kt` | redskapen (`Equipment`, `Gear`, `Prop`): hantlar, gummiband och skivstång i händerna |
| `ExerciseLibrary.kt` | alla övningar och deras lösta vinklar |
| `RepetitionCounter.kt` | räknar repetitioner ur observerade ledvinklar, se *Räkna repetitioner* |

`AnatomyFigure` (Compose, i `:feature:training`) ritar en `Scene`: still på toppen (tumnagel) eller ett antal
repetitioner, aldrig loop.

Programmet bygger på detta i `StrengthSessions.kt` (`:core:exercise`): veckans två helkroppspass, en övning per
*slot* (ben, press, höftfällning, bål, rygg, kondition) och set, repetitioner och vila efter nivå.
Texterna (namn, steg och tips) finns i `ExerciseCards.kt` (`moveText`) och i `strings.xml`, båda i `:feature:training`.

## Koordinater och vinklar

- Längder anges i **kroppslängder**. Figuren är 1,0 hög, så användarens längd ändrar aldrig teckningen.
  `BodyShape` (axlar, höfter, omfång) är kopplingen till användarprofilen.
- Axlar: x pekar åt kroppens vänster, y uppåt och z framåt (dit kroppen tittar). Bäckenet är roten.
- `LimbPose` för armar och ben, i grader från stående med lemmen rakt nedåt:
  - `raise`: framåt, och bakåt om negativ.
  - `spread`: ut åt sidan.
  - `bend`: knä eller armbåge.
- Vinklarna är relativa till bålen, som en fysioterapeut mäter. Lutar bålen fram med `lean` följer benen med,
  så en knäböj med `lean = 38` behöver `raise = 135` för att låret ska vara vågrätt.
- `Pose.lean` tippar hela kroppen runt höften: 90 är med ansiktet ned, -90 är på rygg. `trunk` böjer överkroppen
  och `neck` huvudet.
- Fötterna (`leftFoot`, `rightFoot`) är tårnas vinkel nedåt *mot golvet*, oberoende av benen, så att plantade
  fötter förblir platta. `flatHands` lägger händerna platt i golvet, som i en armhävning.

## Kontakter, ankare och lösta vinklar

- `contacts` är landmärken som vilar på golvet: tår, hälar, händer, knän, mage och övre rygg.
- `anchor` står helt still. Resten av kroppen flyttas runt den. Utan ankare hålls den lägsta kontakten mot
  golvet, som i stjärnhopp där fötterna rör sig.
- När flera punkter står i golvet (armhävning, höftlyft, fyrfota) måste vinklarna lösas så att alla står kvar.
  Använd `PoseSolver` i `tools/anatomy`. Mellanliggande keyframes behövs där interpolationen annars skulle
  låta en hand glida eller sjunka.
- `AnatomyTest` kontrollerar att kontakterna flyttar sig mindre än 2 % av kroppslängden och aldrig sjunker
  ner i golvet.

## Redskap

En övning kan ha ett redskap (`ExerciseMotion.gear`). Redskapet har inga egna vinklar: det ritas ur den poserade
kroppens händer, så samma keyframes fungerar för varje kropp och kamera.

- `Gear.Dumbbells`: en hantel i varje hand, handtaget tvärs över handflatan.
- `Gear.Goblet`: en hantel upprätt mot bröstet, mellan händerna (gobletknäböj).
- `Gear.Barbell`: en skivstång mellan händerna med en viktskiva i varje ände.
- `Gear.Band(anchor)`: ett gummiband från varje hand till en fast punkt, i första keyframens koordinater (där
  kroppen står vid start). Punkten gäller vänster hand; höger hands ände sitter i spegelbilden, så med `x = 0`
  möts banden i en punkt (dörrankare framför), och med `x` utanför axlarna går de förbi kroppen.

Varje del är en cylinder (`Prop`) som ritas som det konvexa höljet av sina två ändcirklar, så en viktskiva är rund
framifrån och smal från sidan. Delarna sorteras in bland kroppsdelarna efter djup (`Layer.Gear`), så en hantel på
bortre sidan hamnar bakom kroppen. Viktskivor ritas som en kant runt en genomskinlig yta, så de aldrig döljer
huvudet eller musklerna. Färgerna är `NovilaTheme.anatomy.gear`, `weight` och `band`.

Riggen kan fortfarande inte vrida överarmen, så rörelser som kräver utåtrotation (knäböj med stången på ryggen,
sidolyft med raka armar framåt) går inte att rita. Välj då en variant som går, som frontböj.

`Equipment` (hantlar, gummiband, skivstång) är det coachen frågar efter: `CoachPreferences.equipment` är vad
användaren har, och `CoachCatalog` väljer bara övningar vars redskap finns där.

## Muskler

Varje grupp är ett område på en kroppsdels ovala tvärsnitt (`MuscleRegion`):
- `from` och `to` anger var längs delen området sitter. 0 är änden närmast bålen, och under 0 når över leden,
  som sätet över höften.
- `around` och `halfWidth` anger var runt delen det sitter, i grader. 0 är fram, 90 är utsidan (på bålen
  kroppens vänster) och 180 är bak.

Bara den del som vetter mot kameran ritas, och den smalnar av mot ändarna som en muskelbuk. Därför försvinner
musklerna ur sikte när de vänds bort. Huvudmuskler ritas fyllda. Stödmuskler ritas med ljus fyllning och
orange kant, så att de skiljer sig på form och inte bara färg.

## Bästa vyn

`ViewSelector` jämför 36 kameror: var 30:e grad runt kroppen, ovanifrån (10°), i ögonhöjd och nedifrån (-8°,
för kroppar som vetter mot golvet). Poängen viktar:
- hur mycket av de tränade musklerna som syns, efter att den närmaste sidan har skymt den bortre (0,15)
- hur mycket rörelsen syns på skärmen (0,25)
- hur lite lemmarna förkortas, i kvadrat (0,5)
- en liten bonus för vyer framifrån, eftersom figuren saknar ansikte (0,1)

Vid jämnt läge vinner en kropp som vetter åt höger, sedd ovanifrån. Övningen sparar sin kamera, och
`AnatomyTest` fallerar med rätt `Camera(...)` om den inte längre är bäst. Valet kostar alltså inget när appen
körs.

## Räkna repetitioner

Eftersom en övning är ledvinklar kan samma data känna igen den. `RepetitionCounter(motion)` tar emot
observerade `Pose` med tid (till exempel det en posmodell ser i kameran, omräknat till vinklar) och lägger varje
observation på linjen från övningens första keyframe till vändläget, den keyframe som ligger längst därifrån.
Bara vinklarna som rör sig i övningen räknas, så leder övningen inte använder, eller som kameran inte ser,
spelar ingen roll. Poser långt från linjen ignoreras (benet som inte jobbar i mountain climbers, att gå fram
till telefonen).

- En repetition räknas när kroppen når minst 80 % av vägen (`FULL_DEPTH`) och kommer tillbaka till start.
- Försök som bara når halvvägs ger en `Repetition` som inte är `complete`, så appen kan säga "hela vägen ner".
- `rushed` är sant när repetitionen tog mindre än halva tiden mot övningens lugna tempo (`repetitionMillis`).
- Hållövningar (plankan) har inget att räkna.

`RepetitionCounterTest` spelar varje övning ur biblioteket med brus på varje vinkel och kräver rätt antal.
Kameran och posmodellen är inte inkopplade än: de kräver behörigheten `CAMERA` och ägarens ok.

## Testa och förhandsgranska utan Gradle

Google Maven kan vara blockerat i molnsessioner. Den rena Kotlin-koden går ändå att köra:

```bash
tools/anatomy/anatomy.sh test                    # AnatomyTest, RepetitionCounterTest, StrengthSessionsTest m.fl.
tools/anatomy/anatomy.sh views                   # bästa kamera mot sparad, per övning
tools/anatomy/anatomy.sh render ark.png squat    # kontaktark: fyra ögonblick och tumnageln
tools/anatomy/anatomy.sh run Solve.kt            # egen fil, t.ex. en vinkellösare
```

Skriptet hämtar Kotlin-kompilatorn (samma version som `libs.versions.toml`) och JUnit från Maven Central via
`mvn`. Kontaktarket ritas med Java2D och ungefärliga färger. Det är bara en kontroll, aldrig en referensbild:
skärmdumparna tas alltid i CI (`/record-screenshots` i PR-beskrivningen).
