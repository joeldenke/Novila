# Säkerhetskopiering och återställning

Novila använder Androids **Auto Backup** (plattformen), inte ett eget backup-agent. Det finns inget
AndroidX-artefakt för att implementera säkerhetskopiering: `androidx.test.backup` är ett
testramverk, inte en backup-implementation. Det används därför bara av testerna.

| Del | Plats |
|---|---|
| Regler för Android 12+ (`cloud-backup` och `device-transfer`) | `app/src/main/res/xml/data_extraction_rules.xml` |
| Regler för Android 11 och äldre | `app/src/main/res/xml/backup_rules.xml` |
| Manifestet | `app/src/main/AndroidManifest.xml` (`allowBackup`, `dataExtractionRules`, `fullBackupContent`) |
| Tester | `app/src/test/java/se/novila/backup/` |

`androidx.test.backup:backup:1.0.0-alpha02` (Google Maven,
<https://developer.android.com/jetpack/androidx/releases/test-backup>) används i enhetstesterna för
att återställa och verifiera DataStore-filen på samma sätt som testsviten gör på en enhet.

## Vad som säkerhetskopieras

`:core:data` sparar all användardata i Preferences DataStore under `files/datastore/`:

| Fil | Innehåll |
|---|---|
| `health` | Loggade dagar: sömn, kost, vatten, träning, mående |
| `coach` | Träningscoachens svar |
| `sleep_coach` | Sömncoachens svar |
| `settings` | Tema, dynamisk färg, introduktionen klar |
| `profile` | Namn, mål och tid för coachen |
| `goals` | Egna mål för steg, sömn och vatten |
| `reminders` | Påminnelser och tider |
| `measurements` | Kroppsmätningar per dag |
| `food` | Kosthållning, allergener och tid för receptförslag |

De gamla SharedPreferences-filerna med samma namn ingår också, så en backup tagen före flytten till
DataStore kan migreras på den nya enheten (`SharedPreferencesMigration`).

## Vad som inte säkerhetskopieras

Reglerna är *inkluderande*: bara det som listas ovan kopieras. Därför lämnas allt annat utanför,
bland annat:

- **WorkManager-databasen** (`databases/androidx.work.workdb`). Påminnelserna läggs om när appen
  startar, så jobben behövs inte i backupen.
- **Cache och `no_backup/`**, som plattformen aldrig säkerhetskopierar.
- **Health Connect-data** (steg, pass, sömn, vilopuls, HRV). Det ägs av Health Connect, ligger bara i
  minnet i Novila och skrivs aldrig till disk.
- **Övriga databaser och filer** som ett bibliotek kan lägga i appens katalog.

Hälsodata är personuppgifter. En molnbackup lagras i användarens eget Google-konto (eller
enhetsöverföringen mellan två enheter) och Novila har ingen åtkomst till den.

## Tester

- `BackupRulesTest` läser båda XML-filerna och manifestet. Den kontrollerar att backup är på, att
  rätt filer används, och att varje avsnitt bara innehåller `file:datastore/` och
  `sharedpref:.` – varken mer eller mindre.
- `BackupRestoreTest` skriver med `SettingsStore`, tar filens byte, raderar den (som en
  fabriksåterställning) och återställer den med `PopulateStorageAction` från
  `androidx.test.backup`. Därefter verifierar `AssertStorageAction` från samma bibliotek att filen
  är identisk. Testet går i CI (Robolectric).

## Kvar att verifiera manuellt

Auto Backups transport (moln eller enhet-till-enhet) kräver en riktig enhet eller emulator med
API 31+ och går inte i CI. Den fullständiga sviten (`BackupRestoreController` /
`BackupRestoreTestRunner`) kräver AGP 9.5 och en ansluten enhet. Verifiera därför manuellt:

```sh
adb shell bmgr backupnow se.novila.debug
adb shell pm clear se.novila.debug
adb shell bmgr restore <token> se.novila.debug
```

Efter återställningen ska loggade dagar, profil, mål, påminnelser och inställningar finnas kvar.
