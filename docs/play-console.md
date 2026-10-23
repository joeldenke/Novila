# Google Play: deklarationer och behörigheter

Underlag för att fylla i Play Console när Novila publiceras, och för att hålla svaren rätt efteråt. Texterna att
klistra in är på engelska, som formulären. Allt här bygger på hur appen fungerar i dag (oktober 2026): ingen
`INTERNET`-behörighet, inga SDK:er som skickar data, Health Connect bara i förgrunden och bara i minnet.

**Ändras något av det, ändras svaren här, i integritetspolicyn (`docs/privacy-policy.md` och `privacy_*`-strängarna)
och i Play Console i samma PR/release.** Typiska utlösare: en ny Health Connect-typ, Google Health API-källan
(`INTERNET`), en kraschrapporterare eller analys-SDK, en ändring i exporten (till exempel att Health Connect-data
kommer med, eller att appen själv skickar filen någonstans), en cache på disk för Health Connect-data.

## Ordning i Play Console

1. **Policy → App content**: integritetspolicy, annonser, appåtkomst, innehållsklassning, målgrupp, Data safety,
   Health apps declaration och Health Connect-behörigheterna (avsnitten nedan).
2. **Store presence → Main store listing**: kategori *Health & Fitness*, texter enligt *Butikssidan* nedan.
3. **Release**: Android App Bundle (`.aab`), inte APK. Play kräver AAB för nya appar och Play App Signing.
   `Distribute to testers` bygger den (`:app:bundleRelease`) vid varje merge till `main`: ladda ner artefakten
   `novila-<nummer>` från körningen och ladda upp `app-release.aab` och `mapping.txt` i Play Console. Bundlen är
   signerad med release-nyckeln, som i Play App Signing blir *uppladdningsnyckeln*; Google signerar appen med sin
   egen nyckel. Utan nyckeln i GitHub-hemligheterna är bundlen osignerad och Play tar inte emot den.

## Integritetspolicy

- Play kräver en publik adress (vanlig webbsida, inte PDF eller redigerbart dokument) och samma policy i appen.
  I appen: Inställningar → *Integritetspolicy* (`PrivacyPolicyScreen`). Publikt: texten i `docs/privacy-policy.md`.
- Adressen är `https://novila.app/privacy/` (svenska: `https://novila.app/sv/integritet/`), publicerad av
  workflowen `Website` med GitHub Pages ur `docs/privacy-policy.md`. Samma adress anges även i Health
  Connect-deklarationen. Flytta den aldrig.
- Policyn hänvisar till utvecklarens kontaktadress i Play. Fyll i *Developer contact → Email* i Play Console; det är
  den adressen användare skriver till.

## Användarvillkor

Play kräver inga användarvillkor för en gratis app utan konto, och Play Console har inget fält för dem. De finns ändå
i appen (Inställningar → *Användarvillkor*, `TermsScreen`) och som publik text i `docs/terms-of-use.md`, publicerad
på `https://novila.app/terms/` (svenska: `https://novila.app/sv/villkor/`). Inget i dem ändrar svaren nedan.

## App content: svar

| Fråga | Svar | Varför |
|---|---|---|
| Ads | No | Ingen reklam eller reklam-SDK. |
| App access | All functionality is available without special access | Inget konto eller inloggning. |
| Content rating (IARC) | Kategori *Reference, News, or Educational*/*Health*; nej på allt våld, sex, droger, spel, användarinteraktion | Appen nämner alkohol bara som en sömnfaktor användaren kan logga, ingen uppmaning till konsumtion. |
| Target audience | 18 and over | Träningsprogram med smärtskala, kroppsmått och vikt. Under 18 drar in Families-policyn. Policyn säger "made for adults". |
| News app | No | |
| Government app | No | |
| Financial features | None | |
| Health apps declaration | Se nedan | |
| Data safety | Se nedan | |

## Health apps declaration

Välj de hälsofunktioner appen har:

- **Activity and fitness** – träningsprogram, passläge, steg och kalorier från Health Connect.
- **Nutrition and weight management** – kostloggning per livsmedelsgrupp och måltider med egna ord, vatten, recept, vikt och kroppsmått.
- **Sleep management** – sömnlogg, sömncoach, samband mellan dag och natt.
- **Stress management, relaxation, mental acuity** – andningsövningarna (Andning och avslappning).
- **Menstrual cycle tracking** (om formuläret har en sådan kategori, annars reproduktiv hälsa) – *Mens och träning*:
  mensdagar och symtom som användaren själv loggar, avslaget från början. Ett lättare pass erbjuds en dag med
  symtom, och återhämtningen jämförs inom samma halva av cykeln. Inga förutsägelser av ägglossning eller fertila
  dagar och inget preventivmedel, så aldrig *Fertility* eller *Contraception*.

Välj **inte** medicinska kategorier (*Medical*, *Disease and condition management*, *Clinical decision support*,
*Medical device*). Novila är inte en medicinteknisk produkt; skonsam träning (`Rehab` i koden) är allmän träning,
inte vård. Om formuläret frågar om regulatorisk status:

> Novila is a general wellness app. It is not a medical device, does not diagnose, treat, cure or prevent any
> disease, and shows a health disclaimer in the app (Settings → Health disclaimer). The gentle training programs are
> general exercise with a pain limit and advice on when to seek care; they do not replace a physiotherapist's plan.

## Health Connect: behörigheter

Appen deklarerar exakt dessa (`core/healthconnect/src/main/AndroidManifest.xml`, `HEALTH_PERMISSIONS`,
`app/badging/debug-badging.txt`). Inga skrivbehörigheter, ingen `READ_HEALTH_DATA_IN_BACKGROUND`, ingen
`READ_HEALTH_DATA_HISTORY`.

| Behörighet | Vad läses | Hur länge bakåt | Används till | Var i appen |
|---|---|---|---|---|
| `READ_STEPS` | Antal steg (aggregerat) | I dag | Dagens rörelse, stegmål, dagens balans | Idag, Träning |
| `READ_ACTIVE_CALORIES_BURNED` | Aktiva kalorier (aggregerat) | I dag | Dagens rörelse | Träning |
| `READ_EXERCISE` | Typ, dag och längd per pass | 5 veckor | Veckans träningsprogram bygger på gjorda pass; samma pass räknas inte två gånger | Träning |
| `READ_SLEEP` | Läggtid, uppvakning, vaken tid | 2 år | Fyller i nätter användaren inte loggat; sömnhistorik upp till ett år jämfört med året innan | Sömn, Idag |
| `READ_RESTING_HEART_RATE` | Vilopuls per dag | 2 veckor | Återhämtning mot egen baslinje; dagens pass görs lättare | Träning, Idag (coachen) |
| `READ_HEART_RATE_VARIABILITY` | HRV (RMSSD) per dag | 2 veckor | Som vilopuls | Träning, Idag (coachen) |

Motiveringar att klistra in, en per behörighet:

- **READ_STEPS**: *Novila shows today's step count on the Today dashboard and under Training, and measures it
  against the user's own daily step goal. Only today's total is read, while the app is open, and it is kept in
  memory only.*
- **READ_ACTIVE_CALORIES_BURNED**: *Novila shows today's active calories next to the steps under Training, so the
  user sees how much they have moved today. Only today's total is read, while the app is open, and it is kept in
  memory only.*
- **READ_EXERCISE**: *Novila's training coach builds the week's program from the workouts the user has done. It reads
  the type, date and duration of workouts from the last five weeks, so workouts a watch or another app recorded
  count towards the program and are not planned again. Read only while the app is open, kept in memory only.*
- **READ_SLEEP**: *Novila's Sleep tab shows the user's nights, history and sleep coach. It reads when the user went
  to bed, woke up and was awake, so nights a watch recorded fill in Sleep when the user has not logged them. Up to
  two years are read so the yearly history can compare with the year before. Read only while the app is open, kept
  in memory only; the user's own log always takes precedence.*
- **READ_RESTING_HEART_RATE**: *Novila compares today's resting heart rate with the user's own baseline from the last
  two weeks. When it is clearly higher, today's training session is made a little lighter (fewer sets, shorter
  cardio). Read only while the app is open, kept in memory only, never compared with medical thresholds.*
- **READ_HEART_RATE_VARIABILITY**: *Novila compares today's heart rate variability with the user's own baseline from
  the last two weeks. When it is clearly lower, today's training session is made a little lighter. Read only while
  the app is open, kept in memory only, never compared with medical thresholds.*

Health Connect-data ingår **aldrig** i användarens export: den sparas aldrig (bara i minnet), och exporten läser bara
appens egna filer. Det håller exporten utanför Health Connects regler om överföring (Limited Use). Lägg aldrig till
den i exporten utan att läsa om policyn och fråga ägaren.

Övriga frågor i Health Connect-deklarationen:

| Fråga | Svar |
|---|---|
| Is data transferred off the device? | No. The app has no internet permission. |
| Is data stored? | No. Read in the foreground and held in memory only; forgotten when access is revoked or the process ends. |
| Is data shared with third parties? | No. |
| Used for advertising, credit or insurance decisions, or sold? | No. |
| Human access to the data? | No. The developer never receives it. |
| Background reading? | No. |
| Privacy policy / rationale screen | `HealthDataInfoActivity` (Health Connect's privacy link, `ACTION_SHOW_PERMISSIONS_RATIONALE` and `VIEW_PERMISSION_USAGE` on Android 14+), plus the privacy policy URL. |

Var beredd på att Google ber om en kort skärminspelning som visar var varje datatyp används (Inställningar →
*Anslut Health Connect*, Health Connects dialog, och sedan Idag, Träning med Återhämtning, och Sömn).

## Data safety

Play definierar *collected* som data som skickas från enheten. Data som bara behandlas på enheten räknas inte, och
inte heller data som bara skickas via Androids egna system (Health Connect). Novila kan inte skicka något
(ingen `INTERNET`), så:

| Fråga | Svar |
|---|---|
| Does your app collect or share any of the required user data types? | **No** |
| Is all of the user data collected by your app encrypted in transit? | Visas inte när svaret ovan är No |
| Do you provide a way for users to request that their data is deleted? | Visas inte; i appen finns ändå *Radera all data* |

Play visar då *No data collected* och *No data shared with third parties*.

**Exporten ändrar inte svaren.** *Spara en kopia av mina data* (`DataExport`) skriver en fil som användaren själv
skapar i Androids filväljare (Storage Access Framework, `ACTION_CREATE_DOCUMENT`), och *Läs in en sparad kopia*
läser en fil användaren väljer (`ACTION_OPEN_DOCUMENT`). Appen väljer aldrig mål, skickar inget själv och har fortfarande
ingen `INTERNET`; utvecklaren tar aldrig emot filen. Väljer användaren en molntjänst i filväljaren är det tjänstens app
som laddar upp, på användarens initiativ, vilket Play räknar varken som insamling eller delning (användarinitierad
överföring som användaren förväntar sig). Läs ändå Play-formulärets hjälptext när det fylls i, och svara efter den.
Ingen ny behörighet, ingen exporterad komponent (badgingen oförändrad). Kraschrapporter i Android vitals samlas av
Google Play, inte av appen, och deklareras inte.

Detta är bara sant så länge appen saknar `INTERNET` och SDK:er som skickar något. `checkDebugBadging` i CI fallerar
om behörigheten kommer tillbaka; uppdatera då detta avsnitt innan release.

## Behörigheterna i APK:n

Hela listan i `app/badging/debug-badging.txt`. Utöver Health Connect:

| Behörighet | Kommer från | Varför | Kräver deklaration? |
|---|---|---|---|
| `POST_NOTIFICATIONS` | `:core:reminders` | Påminnelser och passets timer. Frågas först när användaren slår på en påminnelse. | Nej |
| `WAKE_LOCK` | WorkManager | Håller enheten vaken de millisekunder en påminnelse körs. | Nej |
| `RECEIVE_BOOT_COMPLETED` | WorkManager | Lägger tillbaka planerade påminnelser efter omstart. | Nej |
| `ACCESS_NETWORK_STATE` | WorkManager | WorkManagers nätverksvillkor; ger ingen nätverksåtkomst och används inte av appen. | Nej |
| `se.novila.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | AndroidX Core | Appens egen signaturbehörighet för icke-exporterade receivers. | Nej |

Inga behörigheter som kräver egna formulär: ingen plats, inga kontakter, inga exakta larm
(`SCHEDULE_EXACT_ALARM`/`USE_EXACT_ALARM`), ingen förgrundstjänst (`FOREGROUND_SERVICE` tas bort i manifestet),
ingen `QUERY_ALL_PACKAGES` (bara `<queries>` för Health Connect), inga tillgänglighetstjänster.

## Butikssidan

- Kategori *Health & Fitness*. E-post till utvecklaren ifylld.
- Appikonen: `docs/play-icon.png` (512 px, 32-bitars PNG). Den ritas av `tools/icon/render_icon.py` ur appens
  adaptiva ikon, så butiken och telefonen visar samma ikon.
- Samma regler som i appen (CLAUDE.md, *Inga medicinska påståenden*): skriv *skonsam träning* och *nivåer*, aldrig
  rehab, rehabilitering, behandling, terapi, eller att appen läker, lindrar, hjälper mot eller förebygger sjukdomar
  eller besvär (sömnlöshet, skada, stress som tillstånd). Inga löften om viktnedgång eller bättre sömn.
- Ta med en rad om att Novila ger allmänna råd och inte är en medicinteknisk produkt.
- Påståenden om integritet ("allt stannar i telefonen", "ingen reklam") är sanna i dag och ska stämma med Data
  safety. Nämn att Health Connect behövs för steg, pass, nätter och återhämtning, men att allt går att logga själv.
- Appen har ingen AI: planen och insikterna görs av regler som förklarar sig själva. Det kan stå på butikssidan.
- Inga emojis i skärmdumpar eller texter (som i appen).
- Nämn aldrig andra appar eller företag, och inga ord som "bäst" eller "nummer ett" (Plays metadata-policy).
  Integriteten är skälet att välja Novila och står därför först efter inledningen.

### Texterna

Positioneringen: den lugna, privata vardagscoachen för hela kroppen, som förklarar varför och aldrig skickar dina
data någonstans. Gränserna är 30 tecken för namnet, 80 för den korta och 4 000 för den långa beskrivningen. Ändras
appen, ändra texterna i samma PR.

#### Svenska (sv-SE)

**Namn:** Novila: träning, sömn och kost

**Kort beskrivning:** Lugn coach för träning, sömn och kost. Förklarar varför. Stannar i telefonen.

**Lång beskrivning:**

```text
Novila är en lugn vardagscoach för hela kroppen. Träning, sömn och kost hänger ihop, så Novila ser dem tillsammans och säger vad som är nästa steg i dag, och varför.

Allt stannar i telefonen
Novila har inte ens behörighet att använda internet. Inget konto, ingen reklam och inga analysverktyg. Dina data finns bara på din telefon. Du kan spara en egen kopia och radera allt när du vill.

Varje råd har ett skäl
Novila har ingen AI. Planen, insikterna och dagsformen görs av tydliga regler som förklarar sig själva: varför dagens pass är lättare, vad som verkar hänga ihop med dina nätter och hur många nätter det bygger på. Ett samband är ett samband, aldrig ett bevis.

Utan press
Vanor räknas i hela veckor, inte i dagar, så en missad dag bryter ingenting. Mer träning är aldrig värt mer, och vilodagar räknas också. Inga röda siffror och inga sviter att förlora.

Träning
- Veckans program efter WHO:s råd om kondition och styrka, efter dina dagar, din tid och redskapen du har, eller bara kroppsvikt
- Passläge med set, vila och nedräkning, också på din Wear OS-klocka
- En ritad figur visar varje övning och musklerna som arbetar
- Svårigheten ökar steg för steg när du klarar dina set
- Skonsam träning i tre nivåer med fokus på knä, ländrygg, axel, nacke eller höft

Sömn
- Logga natten själv eller läs den från din klocka via Health Connect
- Se hur regelbundet du sover och vad som verkar hänga ihop med dina nätter: sent kaffe, träning, mat och vatten
- Korta andningsövningar för kvällen: lugn andning, boxandning och 4-7-8

Kost
- En kostpoäng byggd på Livsmedelsverkets kostråd, utan att räkna kalorier
- Vatten och andra drycker, glas för glas
- Fler än 40 recept efter din kosthållning och dina allergener, och plats för dina egna

Mer
- Dagsformen läser din natt, återhämtning och träningsbelastning och föreslår en lugn dag eller passet som planerat
- Mens och träning, avslaget tills du slår på det: ett lättare pass de dagar du vill, aldrig förutsägelser om ägglossning
- Widget på hemskärmen och en ruta i snabbinställningarna för ett glas vatten
- Påminnelser som bara hörs när något fortfarande saknas
- Svenska och engelska, stor text och TalkBack, surfplattor och vikbara telefoner

Health Connect
Steg, träningspass, nätter, vilopuls och HRV kan läsas från Health Connect, bara medan appen är öppen och bara det du har sagt ja till. Novila skriver inget dit och sparar inget därifrån. Allt går också att logga själv, så du behöver ingen klocka.

Novila ger allmänna råd om träning, sömn och kost och är inte en medicinteknisk produkt. Kontakta vården vid smärta, skada eller besvär som inte går över.
```

#### English (en-US)

**Name:** Novila: Fitness, Sleep, Food

**Short description:** A calm coach for fitness, sleep and food. Explains why. Stays on your phone.

**Full description:**

```text
Novila is a calm everyday coach for your whole body. Training, sleep and food are connected, so Novila looks at them together and tells you what to do next today, and why.

Everything stays on your phone
Novila doesn't even have permission to use the internet. No account, no ads and no analytics. Your data lives only on your phone. You can save your own copy and delete everything whenever you like.

Every suggestion has a reason
Novila has no AI. The plan, the insights and your readiness come from clear rules that explain themselves: why today's session is lighter, what seems to go with your nights and how many nights that is based on. A link is a link, never proof.

No pressure
Habits count in whole weeks, not days, so a missed day breaks nothing. More training is never worth more, and rest days count too. No red numbers and no streaks to lose.

Training
- A weekly programme following WHO guidance on cardio and strength, fitted to your days, your time and the equipment you have, or just bodyweight
- Workout mode with sets, rests and countdowns, also on your Wear OS watch
- A drawn figure shows every exercise and the muscles at work
- Difficulty rises step by step as you complete your sets
- Gentle training in three levels focused on the knee, lower back, shoulder, neck or hip

Sleep
- Log your night yourself or read it from your watch through Health Connect
- See how regular your sleep is and what seems to go with your nights: late coffee, training, food and water
- Short breathing exercises for the evening: calm breathing, box breathing and 4-7-8

Food
- A food score based on the Swedish Food Agency's dietary guidelines, without counting calories
- Water and other drinks, glass by glass
- More than 40 recipes matched to your diet and allergens, with room for your own

More
- Readiness reads your night, recovery and training load and suggests a calm day or the session as planned
- Period and training, off until you turn it on: a lighter session on the days you want, never ovulation predictions
- A home screen widget and a Quick Settings tile for a glass of water
- Reminders that only appear when something is still missing
- English and Swedish, large text and TalkBack, tablets and foldables

Health Connect
Steps, workouts, nights, resting heart rate and HRV can be read from Health Connect, only while the app is open and only what you have allowed. Novila writes nothing there and keeps nothing from it. You can also log everything yourself, so you don't need a watch.

Novila gives general advice about training, sleep and food and is not a medical device. Talk to a healthcare professional about pain, injury or problems that don't go away.
```

## GDPR, kort

- All personlig data (hälsodata är särskild kategori enligt artikel 9) behandlas bara på användarens telefon.
  Utvecklaren tar aldrig emot, lagrar eller kommer åt den, och har därför inga register att lämna ut eller radera,
  inga personuppgiftsbiträden och inga överföringar till tredje land.
- Användarens rättigheter uppfylls i appen: se och ändra (varje skärm), radera (*Radera all data*, avinstallation),
  återkalla Health Connect-åtkomst (Health Connect) och dataportabilitet (artikel 20): *Spara en kopia av mina data*
  ger allt användaren fyllt i som JSON, ett strukturerat och maskinläsbart format, och kan läsas in igen.
- Policyn anger kontaktväg (Play) och rätten att klaga hos IMY.
- Om något någon gång skickas från telefonen (Google Health API, synk, kraschrapportering) blir utvecklaren
  personuppgiftsansvarig för hälsodata. Det kräver laglig grund (uttryckligt samtycke), en konsekvensbedömning
  (DPIA), avtal med leverantörer och ny Data safety, och ägarens ok enligt CLAUDE.md.

## Före första release

- [ ] Release-nyckel i GitHub-hemligheterna och Play App Signing påslaget.
- [x] `:app:bundleRelease` byggs i `Distribute to testers` (artefakten `novila-<nummer>`).
- [ ] Den signerade bundlen laddad upp som AAB.
- [ ] Integritetspolicyn publicerad på en fast adress och inlagd i Play Console.
- [ ] App content ifyllt enligt tabellerna ovan; Health Connect-deklarationen inskickad och godkänd.
- [ ] Skillen `play-policy-insights` körd mot den aktuella koden. En manuell genomgång (oktober 2026) hittade två
      saker, som skillen ska bekräfta:
  - [x] **AI-genererat innehåll:** Play kräver att AI-genererat innehåll går att rapportera inifrån appen. Coachens
        Gemini Nano-kommentar saknade det, så den togs bort; appen har ingen AI.
  - [ ] **16 KB-sidstorlek:** om appen bär native-bibliotek (`native-code` i badgingen) och har targetSdk
        35+ kräver Play att de är 16 KB-justerade. Kontrollera i Play Consoles *App bundle explorer* efter
        första uppladdningen.
- [ ] Intern testning (Internal testing track) med Health Connect-dialogen, påminnelser och widgeten på en riktig
      enhet med Android 14+ och en med Android 13 eller äldre.
