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

| Fil (`app/src/main/java/se/novila/anatomy/`) | Ansvar |
|---|---|
| `Geometry.kt` | `Vec3`, `Point2`, `Rotation` (`aboutX/Y/Z`) |
| `Pose.kt` | `LimbPose`, `Pose`, `Keyframe`, `Movement` |
| `Body.kt` | `BodyShape`, `Skeleton` (proportioner och kinematik), `Segment`, `Landmark` |
| `Muscles.kt` | `MuscleGroup` och `MuscleMap` (var varje grupp sitter på kroppen) |
| `Scene.kt` | `Camera`, `SceneBuilder`, `Layer`, golvskugga |
| `ViewSelector.kt` | väljer varje övnings bästa kamera |
| `ExerciseMotion.kt` | en övning: rörelse, muskler, kontakter, ankare, kamera |
| `ExerciseLibrary.kt` | alla övningar och deras lösta vinklar |
| `AnatomyFigure.kt` | Compose: still på toppen (tumnagel) eller ett antal repetitioner, aldrig loop |

Programmet bygger på detta i `exercise/StrengthSessions.kt`: veckans två helkroppspass, en övning per
*slot* (ben, press, höftfällning, bål, rygg, kondition) och set, repetitioner och vila efter nivå.
Texterna (namn, steg och tips) finns i `ui/common/ExerciseCards.kt` (`moveText`) och i `strings.xml`.

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

## Testa och förhandsgranska utan Gradle

Google Maven kan vara blockerat i molnsessioner. Den rena Kotlin-koden går ändå att köra:

```bash
tools/anatomy/anatomy.sh test                    # AnatomyTest, StrengthSessionsTest, TrainingProgramGeneratorTest
tools/anatomy/anatomy.sh views                   # bästa kamera mot sparad, per övning
tools/anatomy/anatomy.sh render ark.png squat    # kontaktark: fyra ögonblick och tumnageln
tools/anatomy/anatomy.sh run Solve.kt            # egen fil, t.ex. en vinkellösare
```

Skriptet hämtar Kotlin-kompilatorn (samma version som `libs.versions.toml`) och JUnit från Maven Central via
`mvn`. Kontaktarket ritas med Java2D och ungefärliga färger. Det är bara en kontroll, aldrig en referensbild:
skärmdumparna tas alltid i CI med `/record-screenshots`.
