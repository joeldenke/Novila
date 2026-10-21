# Säkerhetskopiering och återställning

Novila använder Androids **Auto Backup** (plattformen), inte ett eget backup-agent. Det finns inget
AndroidX-artefakt för att implementera säkerhetskopiering: `androidx.test.backup` är ett
testramverk, inte en backup-implementation. Det används därför bara av testerna.

| Del | Plats |
|---|---|
| Regler för Android 12+ (`cloud-backup` och `device-transfer`) | `app/src/main/res/xml/data_extraction_rules.xml` |
| Regler för Android 11 och äldre | `app/src/main/res/xml/backup_rules.xml` |
| Manifestet | `app/src/main/AndroidManifest.xml` (`allowBackup`, `dataExtractionRules`, `fullBackupContent`) |
| Regler-testet | `app/src/test/java/se/novila/backup/BackupRulesTest.kt` |
| Återställningstestet | `core/data/src/test/java/se/novila/data/EncryptedDataStoreRestoreTest.kt` |

`androidx.test.backup:backup:1.0.0-alpha02` (Google Maven,
<https://developer.android.com/jetpack/androidx/releases/test-backup>) används i enhetstestet för
att återställa och verifiera DataStore-filen på samma sätt som testsviten gör på en enhet.

## Inget lämnar enheten

Hälsodata är personuppgifter och ska stanna på telefonen. Reglerna är därför **uteslutande**: varje
domän (`root`, `file`, `database`, `sharedpref`, `external`) lämnas utanför, i både `cloud-backup`
och `device-transfer`. Varken en molnbackup eller en överföring till en ny telefon bär med sig
något av Novilas data.

Det som annars skulle ha ingått och nu medvetet lämnas utanför:

- **`:core:data`s Preferences DataStore** (`files/datastore/`): loggade dagar, coachernas svar,
  profil, mål, påminnelser, mätningar, matpreferenser (med egna recept), inställningar.
- **De gamla SharedPreferences-filerna** med samma namn.
- **Tink-nyckelringen** (`sharedpref/novila_datastore_keys`). Den är förseglad med Androids
  Keystore-nyckel, som inte går att säkerhetskopiera, så en återställd kopia skulle ändå inte kunna
  läsas här.
- **WorkManager-databasen**, cachar och allt annat ett bibliotek lägger i appens katalog.

`allowBackup` är kvar på `"true"`. Uteslutningen är explicit, och att stänga av backup helt stänger
också av enhetsöverföringen. Se *Dataskydd på enheten* (`docs/data-protection.md`) för krypteringen.

## Tester

- `BackupRulesTest` läser båda XML-filerna och manifestet. Den kontrollerar att backup är på, att
  rätt filer används, och att varje avsnitt **utesluter varje domän och inte inkluderar något**.
- `EncryptedDataStoreRestoreTest` lägger en gammal, okrypterad `settings`-fil med
  `PopulateStorageAction` (som en återställning från en äldre appversion), verifierar den med
  `AssertStorageAction`, och öppnar sedan `SettingsStore`: den läser det gamla värdet och skriver om
  filen krypterad, så nästa backup inte bär någon klartext.

Båda går i CI (Robolectric).

## Kvar att verifiera manuellt

Auto Backups transport (moln eller enhet-till-enhet) kräver en riktig enhet eller emulator med
API 31+ och går inte i CI. Verifiera därför manuellt att en backup nu är tom:

```sh
adb shell bmgr backupnow se.novila.debug
adb shell pm clear se.novila.debug
adb shell bmgr restore <token> se.novila.debug
```

Efter återställningen ska appen vara **som ny**: ingen profil, inga loggade dagar och
introduktionen visas igen. Det gamla beteendet – att allt kom tillbaka – är det som togs bort.
