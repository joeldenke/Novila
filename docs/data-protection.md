# Dataskydd på enheten

Novilas hälsodata stannar på telefonen. Den läses i förgrunden, ligger i minnet så kort som möjligt
och lämnar aldrig enheten: appen har ingen `INTERNET`-behörighet, inga analysverktyg och ingen
Firebase-SDK, och backup-reglerna kopierar ingenting (se `docs/backup.md`). Den här filen beskriver
de två andra delarna: kryptering av det som ändå sparas, och vad *Radera data* gör.

## Kryptering i vila

Det som sparas på disk är `:core:data`s Preferences DataStore-filer. De är **förseglade med Tink**
(AES-256-GCM) genom `datastore-tink`-artefaktet, se `EncryptingPreferencesSerializer`:

- Varje fil skrivs med en Tink-nyckel (`AeadSerializer`) bakom en egen rubrik (`NVLDST` + en
  versionsbyte), så en fil kan skiljas från den okrypterade fil en äldre version skrev.
- Filens namn är *associated data*, så en förseglad fil kan inte bytas mot en annan stores fil.
- Nyckelringen ligger i appens privata `SharedPreferences`
  (`sharedpref/novila_datastore_keys`) och skyddas av en huvudnyckel i **Android Keystore**
  (`KeystoreDatastoreCipher`). Nyckelmaterialet lämnar aldrig telefonen och kan inte läsas ur en
  backup; nyckelringen är dessutom utesluten från backup.
- Nyckeln öppnas första gången en fil läses eller skrivs, på DataStores IO-tråd – inte när en store
  skapas.

### Migrering och fel

- En fil från en äldre version är okrypterad. DataStore behåller samma filnamn, så den läses som
  okrypterad (rubriken saknas) och skrivs krypterad vid nästa sparning. Testat i
  `EncryptedDataStoreTest` och `EncryptedDataStoreRestoreTest`.
- En fil som inte går att dekryptera – fel nyckel, en trunkerad skrivning, en gammal kopia –
  ersätts med tom data (`ReplaceFileCorruptionHandler`), precis som en oläsbar SharedPreferences-fil
  förr lästes som tom, i stället för att skärmarna hänger. Testat i `EncryptedDataStoreTest`.

## Health Connect

Health Connect-data (steg, pass, sömn, vilopuls, HRV) ägs av Health Connect. Novila läser den bara i
förgrunden och håller den bara i minnet (`MovementRepository`, `ExerciseRepository`,
`SleepRepository`, `RecoveryRepository`); inget skrivs till disk. När behörigheten återkallas (eller
processen tar slut) glöms den senaste läsningen, så gammal data aldrig visas kvar:

- `NeedsPermission` och `Unavailable` **släpper** den visade läsningen.
- Ett tillfälligt fel behåller den, så skärmen inte flimrar.

Beteendet är enhetstestat (`revoked access forgets …` i varje repositorys test). Novila raderar
aldrig Health Connects egna data – det är inte appens att radera.

## Radera data

*Radera data* i Inställningar (`SettingsViewModel.deleteAllData`) tömmer varje DataStore-fil för
hälsodata: loggade dagar, coachernas svar (med resultaten per övning), profil, mål, matpreferenser (med egna recept),
mätningar och cykeln.
`deleteLegacyPreferences` tar dessutom bort de gamla SharedPreferences-filerna, även för en store
användaren aldrig öppnat. Påminnelserna töms och deras planerade jobb avbryts
(`RemindersStore.clear`, `ReminderScheduler.cancelAll`), så ingen notis kommer efter en radering.
Inställningar (tema, introduktionen klar) behålls, eftersom de inte är hälsodata.

Cykeln (*Mens och träning*, `CycleStore`) ligger i en egen fil, `cycle`, eftersom mensdagar och symtom är
särskild kategori enligt GDPR artikel 9. Den är avslagen från början, och att stänga av den raderar allt som
loggats, inte bara vid *Radera data*. Cykeln läses aldrig från Health Connect (`READ_MENSTRUATION` kräver ägarens
ok, se roadmapen).

Två saker görs **inte**, medvetet:

- **Backupen** rörs inte, för det finns ingen: ingenting säkerhetskopieras (se `docs/backup.md`), så
  det finns ingen molnkopia som kan återställa det raderade.
- **Tink-nyckelringen** roteras inte. Den innehåller ingen data, den lämnar aldrig enheten, och den
  behövs för att de inställningar som överlever en radering fortfarande ska kunna läsas. Att rotera
  den skulle göra dem oläsbara utan att skydda något.

## Export och import

*Spara en kopia av mina data* och *Läs in en sparad kopia* i Inställningar (`DataExport` i `:core:data`) ger
användaren en egen kopia, till exempel inför en ny telefon, eftersom ingen backup följer med (se `docs/backup.md`).

- **Vad:** samma filer som *Radera data* tömmer (dagarna, båda coacherna med resultaten per övning, profil, mål,
  matpreferenser med egna recept, mätningar och cykeln), utom påminnelserna, som hör till telefonen de är inställda på.
  Appens inställningar (tema, introduktionen) ingår inte. Health Connect-data ingår aldrig: den sparas inte.
- **Var:** användaren väljer fil i Androids filväljare (Storage Access Framework). Appen behöver ingen behörighet,
  väljer aldrig mål och skickar inget.
- **Format:** JSON (`ExportCodec`), läsbart för människor och andra appar (GDPR artikel 20): appens namn, filformatets
  version (`format`), appens byggnummer (`version`), datumet och filerna med varje värde och dess typ, exakt som
  appen sparar det.
- **Okrypterad:** filen är hälsodata i klartext, eftersom användaren ska kunna läsa och flytta den. Appen säger det
  bredvid knappen och i integritetspolicyn.
- **Versioner:** varje värde har sin egen version (`DayEntryCodec`, `CoachPreferencesCodec` …), så en export från en
  äldre app läses och migreras av samma kod som läser äldre data i telefonen. En export från ett senare bygge (högre
  `version` eller `format`) vägras, eftersom den kan ha en version den här appen läser som tom.
- **Import:** ersätter allt i de exporterade filerna med exakt det filen har; en fil som saknas i exporten töms. Filen
  är okänd indata: högst 20 MB, bara kända filnamn, varje värde med rätt typ, och värdena läses sedan av storarnas
  egna avkodare, som tål allt. Kan inte hela filen läsas ändras ingenting. Frågar först, eftersom det inte går att ångra.

Testat i `DataExportTest` (export och import tillbaka, en äldre export som migreras, trasiga och för nya filer).
