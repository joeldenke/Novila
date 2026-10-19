# GDPR compliance audit — Novila (Android)

> **This is an engineering audit, not legal advice.** It records what the repository shows and what
> should be checked with a qualified privacy lawyer before a public release. Nothing here is a legal
> opinion, a Data Protection Impact Assessment (DPIA) or a substitute for one.

| | |
|---|---|
| Scope | The Novila Android app in this repository: app code, modules, manifests, Gradle dependencies, backup rules and repository docs. |
| Excluded | The Play Console listing, the Health apps declaration, the hosted privacy policy, Firebase App Distribution settings, the owner's legal entity and any server-side systems (there are none in the app). |
| Method | Static review of the source tree at the commit that adds this document. No runtime traffic capture, no device test, no Play Console access. |
| Status of evidence | Every finding cites a file path. Where something cannot be established from the repository, it says so instead of guessing. |

## 1. Executive summary

Novila is unusually privacy-friendly by architecture: all user data is stored locally in Preferences
DataStore, the app has **no `INTERNET` permission**, no analytics, no crash-reporting and no Firebase
runtime SDK, and the only AI feature (Gemini Nano) runs on the device through AICore. Health Connect
data is read-only, in the foreground, and kept in memory. That part of the design is strong and the
audit found no evidence that personal data leaves the device through the app.

The gaps are mostly governance and user-facing transparency, not exfiltration:

1. **No explicit Art. 9 consent and no documented lawful basis.** Health Connect's permission dialog is
   a platform permission, not necessarily GDPR explicit consent, and there is no consent screen,
   consent record or legal-basis statement (`feature/onboarding/`, `core/healthconnect/`).
2. **No privacy notice.** The Settings "Privacy" row is a static summary; there is no policy text,
   controller identity, contact, retention statement or rights process
   (`feature/settings/.../SettingsScreen.kt:284`). The Health Connect screen covers only Health
   Connect data (`core/healthconnect/.../HealthDataInfoActivity.kt`).
3. **No data export/portability and no full rectification.** The roadmap lists export/import as
   missing (`docs/roadmap.md`), and edits are limited to 30 days back (`EditableDays`).
4. **Android Auto Backup copies all health DataStore files** to the user's Google account and
   device-transfer target, while the UI told the user there is "no cloud" and that deleted data cannot
   be restored (`app/src/main/res/xml/*`, `feature/settings/src/main/res/values/strings.xml:37`). The
   copy was inaccurate; this PR corrects it and adds the backup caveat to the audit.
5. **Erasure was incomplete.** "Delete all data" cleared the health stores but not the reminder store
   and left scheduled reminder work running (`feature/settings/.../SettingsViewModel.kt:83`). Reminders
   are being cleared and cancelled in this PR. Settings (theme, onboarded flag) intentionally remain.
6. **Data at rest relies on the OS sandbox and file-based encryption only.** There is no app-level
   encryption of the DataStore files and no encrypted Room database (Room is not used).
7. **Play Data Safety and the Health apps declaration cannot be verified here.** The repository implies
   "no collection, no sharing", but the Console forms and the backup interpretation must be checked in
   Play Console.

No committed secrets, no third-party processor contract is needed for the current on-device design,
and no EU transfer of app data was found. If the dormant Google Health API source is wired in later,
its scope, `INTERNET` permission, privacy screen, Play declaration and a DPA/transfer assessment are
prerequisites (`core/googlehealth/`; not on `:app`'s classpath).

## 2. Data map

All persisted data lives in Preferences DataStore under the app's private directory
`files/datastore/` (`core/data/src/main/java/se/novila/data/PreferencesStore.kt:41-52`). Each file is a
separate store, created as a singleton in `AppScope`.

| Data category | Personal data | Where it lives | Retention / deletion |
|---|---|---|---|
| Identity / profile | Name, birth year, height, weight | DataStore file `profile` (`core/data/.../ProfileStore.kt`) | Until "Delete all data" clears the file, or the app is uninstalled; included in Auto Backup |
| Body measurements | Weight, waist, neck, hips per day | DataStore file `measurements`, one key per date (`MeasurementsStore.kt`) | Same as above |
| Sleep | Hours, bedtime, wake time, quality 1–5, factors (caffeine, screens, alcohol, stress, late meal, late exercise) | DataStore file `health`, encoded `DayEntry` v6 (`core/model/.../DayEntry.kt:42-67`) | Same as above |
| Nutrition | Portions per food group, treats, water glasses | DataStore file `health` (`DayEntry.food`, `waterGlasses`) | Same as above |
| Training | Strength minutes, cardio logs, RPE effort, pain 0–10, rehab phase | DataStore file `health` (`DayEntry.strengthMinutes`, `cardio`, `effort`, `pain`, `rehabPhase`) | Same as above |
| Wellbeing (legacy) | `mood`, `energy`, `stress`, free-text `diary` | Still encoded in `DayEntry` v6 and therefore still stored in `health`; the UI was removed but the codec keeps the fields (`DayEntry.kt:42-59`, `docs/roadmap.md`) | Until the day is deleted; no UI to correct them |
| Coach answers | Goal, days/week, minutes, focus, considerations, rehab area and phase; sleep-coach answers | DataStore files `coach` and `sleep_coach` (`CoachStore.kt`, `SleepCoachStore.kt`) | Until "Delete all data" |
| Food preferences | Diet, 14 allergens, available time | DataStore file `food` (`FoodPreferencesStore.kt`); allergens are health data | Until "Delete all data" |
| Goals | Steps, sleep hours, water | DataStore file `goals` (`GoalsStore.kt`) | Until "Delete all data" |
| Reminders | On/off and time of day per reminder | DataStore file `reminders` (`RemindersStore.kt`) | Until "Delete all data" (this PR); previously kept |
| App settings | Theme, dynamic colour, onboarded flag | DataStore file `settings` (`SettingsStore.kt`) | Kept after "Delete all data" by design; not health data |
| Health Connect data | Steps, active calories (today), exercise sessions (last 5 weeks), sleep nights (up to 2 years), resting HR and HRV (last 14 days) | **Memory only**: `MovementRepository`/`InMemoryMovementCache`, `ExerciseRepository`, `SleepRepository`, `RecoveryRepository`; never written to DataStore (`docs/backup.md`, `AppGraph.kt:44-78`) | Until process death, permission revocation or app restart |
| Derived data | Balance, insights, sleep links, nutrition score, training plan, coach notes | Computed on the fly in `:core:model`, `:core:coach`, `:core:exercise`; not persisted | Same lifetime as the inputs |
| Gemini Nano comment | Generated answer text | ViewModel state only (`feature/training/.../CoachViewModel.kt:46-83`); not saved | Until the coach screen leaves the back stack |
| Widget data | Balance, sleep, nutrition score, water, cheer | Reads the same DataStore stores in `NovilaWidget`; no separate store | Same as DataStore |
| Notifications | Generic reminder text; workout timer text | System notification channels; no personal values in the reminder text (`core/reminders/.../ReminderNotifications.kt`) | System-managed |

**No location data, contacts, camera, microphone, accelerometer, advertising ID or device identifiers are
collected.** The merged debug manifest declares only the health read permissions, notifications, and
permissions pulled in by WorkManager/Health Connect/AICore (`app/badging/debug-badging.txt:4-15`).

## 3. Legal basis and consent

### What exists

- **Health Connect permission dialog** with a rationale and an in-app explanation screen
  (`core/healthconnect/src/main/AndroidManifest.xml:18-23`; `HealthDataInfoActivity`). Permissions are
  checked on every read and never cached, so revocation in Health Connect takes effect on the next
  refresh (`core/movement/.../MovementRepository.kt`, `core/sleep/.../SleepRepository.kt`,
  `core/recovery/.../RecoveryRepository.kt`).
- **Notification permission** requested only when the user turns on the first reminder, never at start
  (`core/reminders/src/main/AndroidManifest.xml:10-11`, `feature/settings/.../RemindersScreen.kt`).
- **Health Connect can be skipped** in onboarding; the app still works with manual logging
  (`feature/onboarding/.../OnboardingScreen.kt`).
- **No analytics or crash-reporting consent**, because neither exists.

### Gaps

| Area | Finding | Severity |
|---|---|---|
| Art. 9 explicit consent | Health data (sleep, HRV, resting HR, weight, pain, allergens) is processed with no separate explicit-consent statement or affirmative action. A Health Connect permission is a platform access control; it does not by itself satisfy Art. 9(2)(a), and the app does not record consent. | High / Critical |
| Art. 6 lawful basis | No documented basis (contract, consent or legitimate interest) per purpose. Local-only processing still needs a basis; the household exemption does not apply to an app publisher. | High |
| Consent specificity / granularity | Health Connect is all-or-nothing per data type inside its own dialog; the app asks for six types at once and does not let the user opt out of, say, sleep while keeping steps. This may be acceptable as a platform flow but is not granular consent. | Medium |
| Revocation | Health Connect revocation works and stale values are dropped on the next read. There is no in-app "withdraw consent" beyond that, and no record of withdrawal. | Medium |
| Consent records | None. If the owner chooses consent as the basis, records (what, when, version of notice) are required. | High if consent is the basis |

The onboarding and Settings text ("Everything stays on your phone", "no cloud") is an assurance about
storage, not a legal basis. The corrected copy in this PR makes the backup exception explicit but does
not create consent.

## 4. On-device vs off-device

**Finding: the app transmits no personal data.** Evidence:

- `INTERNET` is explicitly removed from the merged manifest, including the copy that ML Kit's
  `datatransport` asks for (`app/src/main/AndroidManifest.xml:12-21`).
- The APK's badging has no `INTERNET`, no `ACCESS_FINE_LOCATION`, no `CAMERA`, no `READ_CONTACTS`
  (`app/badging/debug-badging.txt:4-15`). `ACCESS_NETWORK_STATE`, `WAKE_LOCK` and
  `RECEIVE_BOOT_COMPLETED` come from WorkManager; `BIND_SERVICE` comes from AICore.
- No classes from Firebase, Crashlytics, Google Analytics, Sentry, Datadog or an HTTP client are on
  the app classpath. `app/build.gradle.kts:26-36` lists only AndroidX, Metro, Kotlin, Glance, ML Kit
  GenAI and the app's own modules.
- **Gemini Nano** runs in AICore; the prompt contains the deterministic plan, goal, days, minutes and
  exercise names, not the profile name or free text. The answer is state-only
  (`core/data/.../GeminiNanoCoachModel.kt:19-21`; `core/coach/.../CoachAssistant.kt`).
- The only HTTP code is the **dormant** `core/googlehealth` module, and it is not included in `:app`
  (`settings.gradle.kts:43` includes the module, but `app/build.gradle.kts` has no
  `projects.core.googlehealth`). If wired in, it sends workouts to
  `health.googleapis.com` with an OAuth bearer token (`core/googlehealth/.../GoogleHealthExerciseSource.kt`).
- Firebase exists only in `.github/workflows/distribute.yml` for distributing test APKs to testers,
  not in the app runtime.

Caveats worth documenting rather than "fixing":

- Android **Auto Backup** moves the DataStore files off the device (see §7).
- AICore may download/update the model using Google's infrastructure. That does not send Novila's
  personal data, but it is a Google component on the device; the privacy notice should say so.
- Health Connect itself is a platform data store; Fitbit, Samsung Health and similar apps are separate
  controllers that the user connects. Novila only reads through Health Connect.

## 5. Third parties / processors

| Component | Purpose | What it receives | Processor/DPA? |
|---|---|---|---|
| **Health Connect** (AndroidX client + platform) | Read steps, calories, exercise, sleep, resting HR, HRV | Read requests and the data types the user approved; on-device | Platform component; Health Connect app/BLE providers are separate controllers. No Novila DPA. |
| **ML Kit GenAI Prompt API / AICore** | On-device Gemini Nano comment | The plan prompt, on-device | On-device inference. Google receives no app data for this. The `datatransport` telemetry library's `INTERNET` permission is removed. |
| **AndroidX WorkManager** | Reminders | Reminder name, time, generic notification content | OS library; no off-device processing |
| **Glance** | Home-screen widget | Health summary rendered locally | OS library |
| **Metro, Kotlin, Compose** | Runtime/DI/UI | None | No data processing |
| **Firebase App Distribution** | Delivers test APKs to testers | Tester email/account, APK binary | Not in the app. Google is a processor for distribution; this is a development/testing activity, not app user data |
| **Google Health API** (dormant) | Would read workouts | OAuth token + workout query | Not wired. Would require a DPA/transfer assessment and owner approval before use |

No analytics, advertising, crash-reporting or cloud-AI processor is present.

## 6. User rights

| Right | Status | Evidence | What is missing |
|---|---|---|---|
| Access | Partial | Profile, measurements, goals, coach answers and logged days are visible in the app | No single "what do you hold about me" view; legacy `mood`/`diary` fields are invisible |
| Portability / export | **Gap** | No export code or route; `docs/roadmap.md` lists "Export and import as a local file" as missing | Machine-readable export (JSON/CSV) and import |
| Rectification | Partial | Today and the last 30 days are editable (`EditableDays`, day picker on Sleep/Nutrition); profile, goals, measurements, coaches are editable | Cannot correct days older than 30 days; legacy fields have no UI |
| Erasure | **Partial** (improved in this PR) | `SettingsViewModel.deleteAllData()` cleared health, coach, sleep-coach, profile, goals, food and measurements | Before this PR reminders and their WorkManager jobs survived; settings survive by design; cloud backups are not purged |
| Restriction / objection | Gap | Only Health Connect/notification permission toggles | No way to restrict processing while keeping data, no objection channel |
| Rights contact / response process | **Gap** | No privacy notice, controller identity or contact | Policy + request workflow (even a mailto:) |
| Automated decision-making (Art. 22) | Likely N/A | Coach plans are deterministic and advisory; no legal/significant effect | Document the assessment |

## 7. Backup

Android Auto Backup is **enabled** and deliberately includes all DataStore files:

- `android:allowBackup="true"`, `dataExtractionRules` and `fullBackupContent` in
  `app/src/main/AndroidManifest.xml:22-25`.
- `app/src/main/res/xml/data_extraction_rules.xml` includes `file:datastore/` and `sharedpref:.` for both
  `cloud-backup` and `device-transfer`.
- `app/src/main/res/xml/backup_rules.xml` does the same for Android 11 and older.
- `docs/backup.md` documents the files, the tests and the manual verification gap.
- `BackupRulesTest` asserts the rules stay in this shape; `BackupRestoreTest` verifies a real DataStore
  file can be restored.

Assessment:

| Question | Finding |
|---|---|
| What is backed up | Profile, health days (including sleep, pain, allergens-adjacent data), measurements, coach answers, goals, reminders, food preferences, settings. In practice the complete set of Novila's persisted personal data. |
| Where | The user's own Google account (cloud backup) or a device-to-device transfer target. Novila has no access. |
| Encryption | Google encrypts Auto Backup in transit and at rest as part of the user's account. **The app does not encrypt the DataStore files before backup**, and there is no app-level key material. |
| Data minimisation | Backup is not minimised: everything is included. For a local-only health app that is convenient, but it should be a documented, deliberate choice. |
| Deletion | Deleting data in the app writes empty stores, but it cannot reach an existing Google backup. The backup may outlive the in-app deletion until Google's backup expires or the user clears it. |
| Transparency | The UI previously said "no cloud" and "nothing can be restored once deleted", which was inaccurate. This PR corrects the copy. |
| GDPR defensibility | Defensible only if the privacy notice discloses the backup, the user can opt out (Android backup settings / disabling backup), retention is explained, and the data is minimised or encrypted app-side. Today those conditions are not met. |

Recommendation: decide explicitly between (a) keep Auto Backup and disclose it in the policy and Data
Safety answer, or (b) exclude health stores from `cloud-backup` while keeping `device-transfer`, or
(c) set `allowBackup="false"`. Each is a product/privacy decision, not a mechanical fix; this audit does
not change the rules.

## 8. Transparency

| Item | Status | Evidence |
|---|---|---|
| In-app privacy summary | Partial | `settings_privacy_summary` says "No ads, no analytics and no network calls" (`feature/settings/src/main/res/values/strings.xml:48-49`), now qualified with the backup exception |
| Full privacy policy | **Gap** | No policy text, no URL, no link; the Settings "Privacy" row is a static, non-clickable summary (`SettingsScreen.kt:284-289`) |
| Controller identity / contact | **Gap** | Not present anywhere in the repo |
| Purposes and legal bases | **Gap** | Not stated; only feature-level explanations |
| Retention periods | **Gap** | Not stated; no retention limits in code |
| Recipients | Partial | Health Connect screen says data is not shared; backup recipients are not mentioned before this PR |
| Rights and how to exercise them | **Gap** | Not stated |
| Health Connect-specific notice | Compliant for its scope | `HealthDataInfoActivity` lists exactly the six read types, why, that data is not saved/sent/shared, and how to revoke (`core/healthconnect/src/main/res/values/strings.xml:5-11`) |
| AI notice | Compliant | On-device Gemini Nano disclaimer in `DisclaimerScreen` strings |
| Medical disclaimer | Compliant | `DisclaimerScreen`, "not a medical device" |

A public policy must at minimum name the controller, contact, categories, purposes, legal bases,
recipients (Health Connect, AICore, Google backup), retention, rights, complaint route to the Swedish
IMY, and the fact that health data is Art. 9 data. The policy URL is also required by Play Console.

## 9. Security

| Area | Finding | Evidence |
|---|---|---|
| Encryption at rest | App-private DataStore files, protected by Android's sandbox and file-based encryption. **No app-level encryption, no Tink, no SQLCipher.** Room is not used. | `core/data/.../PreferencesStore.kt`; `gradle/libs.versions.toml` has no security-crypto/Tink/Room |
| Backup encryption | Platform-level, not app-level | `app/src/main/res/xml/*` |
| Secrets | No API keys, tokens or passwords in the repo. `app/debug.keystore` is the standard debug key (password `android`, documented as non-secret); release key comes from CI secrets. | `build-logic/.../AndroidApplicationConventionPlugin.kt:83-106` |
| Permission minimisation | Six read-only Health Connect types, notifications gated behind the first reminder, `INTERNET` removed, `FOREGROUND_SERVICE` removed, Glance's unused service removed | Manifests above; `app/badging/debug-badging.txt:4-15` |
| Exported components | Launcher, Health Connect rationale activity (static text), permission-usage alias (guarded by `START_VIEW_PERMISSION_USAGE`), widget receiver (required), Health Connect SDK service. Debug tooling activities are debug-only. | `app/badging/debug-badging.txt:43-51` |
| Logging | No personal values logged; only generic warnings (`Log.w`) with exceptions. | `core/data/.../GeminiNanoCoachModel.kt`, `core/healthconnect/.../*.kt`, widget/reminder logs |
| Screen exposure | No `FLAG_SECURE`; recent-apps thumbnails and the home-screen widget can show health summaries. Not a data breach, but a shoulder-surfing/lock-screen risk. | `MainActivity.kt`; `feature/widget/.../WidgetContent.kt` |
| Deletion | `clear()` empties the DataStore's logical contents; it is not a secure wipe of flash storage. This is normal for Android apps but should be stated if users expect "secure erase". | `PreferencesStore.kt`, store `clear()` methods |
| Widget/notification leakage | Widget shows sleep/nutrition/water on the home screen; reminder notifications are generic and do not include values. | `WidgetContent.kt`, `ReminderNotifications.kt` |

## 10. Special-category data (Art. 9)

Sleep, resting heart rate, HRV, weight/body measurements, pain, rehab area/phase, allergens and the
legacy mood/stress/diary fields are health data under Art. 9. The app's own medical disclaimer is
appropriate for the MDR position, but it does not address GDPR.

Required before public release:

1. An explicit-consent or other Art. 9(2) condition, documented per purpose.
2. A privacy notice that explicitly identifies health data and the condition relied on.
3. A DPIA screening: processing health data on a large scale is a trigger; a small local app may not
   meet "large scale", but the owner should record the assessment (`docs/` or internal).
4. A record of processing activities (Art. 30). The <250-employee exemption is narrow and does not
   obviously apply to regular health-data processing.

## 11. Children, transfers, DPAs, breach

| Topic | Finding |
|---|---|
| Children / minors | No age gate and no child-directed marketing. Birth year can be entered, so a minor's health data can be stored. The app is not an information-society service offered directly to a child, so Art. 8 is unlikely to be the main issue, but the policy should state that the app is not intended for children and that minors need a guardian. |
| EU/EEA transfers | None found for app data: no network, no cloud AI, no analytics. The dormant Google Health API would transfer workout data to Google; that needs a transfer assessment and owner approval. Auto Backup is stored by Google in the user's account under Google's terms. |
| DPAs | None needed for the current on-device design. If Google Health API or any cloud service is added, a DPA and transfer mechanism are needed. Firebase App Distribution is a development tool, not app processing. |
| Breach notification | No backend, so the main breach vector is device compromise or a stolen backup. The owner still needs a written runbook: assess, contain, notify IMY within 72 hours if risk, inform users when high risk, and record the incident. No logging of PII helps containment. |

## 12. Play Store Data Safety and Health apps declaration

Evidence implies:

- **No data collected or shared by the app** in the Play Data Safety sense, because there is no network
  transmission and no SDK that sends data (`app/src/main/AndroidManifest.xml`, dependencies).
- **Health data is processed on-device** (stored locally, read from Health Connect). Play's forms have a
  separate "Health and fitness" category; whether Android Auto Backup is treated as "collection" must be
  confirmed against the current Play policy and the Console's own guidance. Do not fill the form from
  this document alone.
- **Data deletion**: the app offers in-app deletion, which Play increasingly expects for apps with
  accounts. There is no account, but the app should still link the deletion path in the Data Safety form
  and the policy.
- **Health apps declaration** must list exactly the six Health Connect read types, and the app must have
  the corresponding rationale activity (it does). `HealthConnectManifestTest` keeps the manifest and
  `HEALTH_PERMISSIONS` in sync.
- The privacy policy URL is mandatory for a health app; it does not exist yet.

Cannot verify from the repo: the actual Console answers, the declared data types, the privacy policy
URL, the content rating, and whether the app is enrolled in the Health apps programme.

## 13. Compliance table

Severity: **Critical** = blocks a public release of health data; **High** = must be fixed before or at
release; **Medium** = fix soon, document interim risk; **Low** = improvement.

| # | Area | Status | Evidence | Recommended fix | Severity |
|---|---|---|---|---|---|
| 1 | Lawful basis (Art. 6) | Gap | No basis documented; `feature/onboarding`, `core/data` | Write down a basis per purpose; reflect it in the policy | High |
| 2 | Explicit consent for health data (Art. 9) | Gap | No consent screen or record; Health Connect permission only (`core/healthconnect/src/main/AndroidManifest.xml`) | Add an explicit, granular, recorded consent step; or document another Art. 9(2) condition with counsel | Critical |
| 3 | Consent specificity / revocation | Partial | Health Connect dialog + revocation handled in repositories; no in-app withdrawal or record | Explain revocation in the policy; record consent/withdrawal if consent is the basis | High |
| 4 | No off-device transmission | Compliant | `INTERNET` removed (`app/src/main/AndroidManifest.xml:12-21`); no analytics/Firebase runtime; Google Health not wired (`app/build.gradle.kts`) | Keep it this way; add a regression test or lint rule if possible | — |
| 5 | Third-party processors | Compliant for current design | On-device only; `app/badging/debug-badging.txt`; `.github/workflows/distribute.yml` is not runtime | Re-assess if Google Health or any SDK is added | Low |
| 6 | Access | Partial | Profile/logs visible in app; no consolidated view | Add "my data" overview or rely on export | Medium |
| 7 | Portability / export | Gap | No export code; `docs/roadmap.md` lists it as missing | JSON/CSV export (and import), no cloud | High |
| 8 | Rectification | Partial | 30-day edit window (`EditableDays`); legacy fields have no UI | Allow older corrections or explain the limit; expose/remove legacy fields | Medium |
| 9 | Erasure | Partial | `SettingsViewModel.deleteAllData()` (`SettingsViewModel.kt:83-91`) missed reminders; fixed in this PR; settings remain; backups remain | Decide about settings, document backup retention, offer "delete and clear backup" guidance | High |
| 10 | Retention | Gap | No limits or policy; data kept until deletion/uninstall | State retention in the policy; consider pruning old days | Medium |
| 11 | Backup | Partial | `allowBackup=true`, all DataStore included (`app/src/main/res/xml/*`); UI said "no cloud" (corrected in this PR) | Disclose; choose cloud-exclusion vs keep; document encryption and deletion limits | High |
| 12 | Transparency / privacy policy | Gap | No policy, no link, no controller/contact (`SettingsScreen.kt:284-289`) | Publish policy + link in Settings and Play; add controller identity and contact | High |
| 13 | Health Connect notice | Compliant | `core/healthconnect/.../HealthDataInfoActivity.kt`, strings | Keep in sync with permission changes | — |
| 14 | Encryption at rest | Partial | DataStore plaintext under app sandbox; no Tink/SQLCipher | Add app-level encryption or document risk acceptance | Medium |
| 15 | Secrets | Compliant | No keys; debug keystore documented (`build-logic/.../AndroidApplicationConventionPlugin.kt:83-106`) | None | — |
| 16 | Permission minimisation | Compliant | Six read-only HC permissions, gated notifications, `INTERNET` removed | Keep `HealthConnectManifestTest` and badging review | Low |
| 17 | Special-category data | Gap | Health data throughout; no Art. 9 condition | See #2 | Critical |
| 18 | Children | Partial | No age gate; not child-directed | State intended audience; guardian consent for minors in the policy | Low |
| 19 | EU transfers | Compliant (app) | No network; dormant Google Health source | Assess if wired | Low |
| 20 | DPA | Compliant (none needed) | On-device design | Add when a processor is introduced | Low |
| 21 | Breach readiness | Gap | No runbook | Write a 72-hour runbook and an incident log | Medium |
| 22 | Play Data Safety | Unknown | Repo implies no collection; Console not accessible | Complete and review against the current policy; handle backup | Medium |
| 23 | Health apps declaration | Unknown | Six permission types declared in manifest | Verify the Console lists exactly these six | Medium |
| 24 | Automated decisions | Compliant / N/A | Deterministic coach (`core/coach`) | Document the Art. 22 assessment | Low |
| 25 | On-device AI | Compliant | Gemini Nano via AICore (`GeminiNanoCoachModel`) | Keep the AI disclaimer; never send the prompt off-device | Low |
| 26 | Screen/widget exposure | Partial | No `FLAG_SECURE`; widget shows health summary | Optional `FLAG_SECURE`/widget setting; mention in policy | Low |

## 14. Prioritized remediation

### P0 — before any public release (blocking)

1. **Adopt an Art. 9 condition and record it.** Add an explicit consent screen for health-data
   processing (separate from the Health Connect permission), with a versioned notice and a stored
   consent/withdrawal record. If counsel prefers another Art. 9(2) condition, document it instead.
2. **Publish a privacy policy** with controller identity, contact, categories, purposes, legal bases,
   recipients (Health Connect, AICore, Google backup), retention, rights, IMY complaint route and the
   backup caveat. Link it in Settings > About and in Play Console.
3. **Complete a DPIA screening and an Art. 30 record** (even if the conclusion is "no DPIA required").
4. **Make Play Data Safety and the Health apps declaration match** the six permission types and the
   no-collection design; decide how Auto Backup is represented.

### P1 — next release

5. **Implement export/import** (JSON), covering all stores and legacy fields; test round-trip.
6. **Finish the erasure story**: this PR clears reminders; decide whether settings should also be reset;
   document that Google backups are outside the app's reach and how a user removes them.
7. **Decide on backup policy** (keep and disclose, exclude health from cloud backup, or disable) and
   update the privacy notice accordingly.
8. **Encrypt DataStore at rest** (app-level key in Android Keystore) or record an explicit risk
   acceptance with counsel.
9. **Rectification beyond 30 days** or an explicit explanation of the limit in the policy.
10. **Retention policy** (e.g. keep until deletion; prune days older than N years) and implement it if
    chosen.

### P2 — hardening

11. Breach runbook and incident contacts.
12. `FLAG_SECURE` for sensitive screens and a widget privacy option.
13. Children/intended-audience statement.
14. DPA/transfer assessment if the Google Health API is ever wired in.
15. Remove or restore the legacy `mood`/`energy`/`stress`/`diary` fields so stored data matches what
    the UI and policy describe.

## 15. Code and configuration changes in this PR

Small, safe fixes only; larger items are recommendations above.

1. **Corrected the "no cloud" copy** in Settings and onboarding, in English and Swedish, so the user is
   told that Android's own backup can copy the data to their Google account and that in-app deletion
   does not remove an earlier backup.
   - `feature/settings/src/main/res/values/strings.xml`
   - `feature/settings/src/main/res/values-sv/strings.xml`
   - `feature/onboarding/src/main/res/values/strings.xml`
   - `feature/onboarding/src/main/res/values-sv/strings.xml`
2. **"Delete all data" now also clears the reminder store and cancels scheduled reminder work**, so no
   notification survives an erasure request.
   - `core/data/.../RemindersStore.kt` (`clear()`)
   - `core/reminders/.../ReminderScheduler.kt` (`cancelAll()`)
   - `feature/settings/.../SettingsViewModel.kt` (inject and call both)
   - delete-dialog copy updated to say reminders are turned off
3. **Tests** for the new store and scheduler behavior.
4. This audit document, and a backup disclosure in `README.md`/`README.sv.md`.

Deliberately **not** changed: backup rules, `allowBackup`, DataStore encryption, the 30-day edit window,
and the absent consent/export flows. Each is a product or legal decision, listed above.

## 16. What could not be verified

- The app's behaviour on a physical device (no emulator/JDK in the audit session; CI builds the APK).
- The actual Play Console Data Safety answers, Health apps declaration, privacy-policy URL and content
  rating.
- Whether Google's Auto Backup is treated as "collection" for Data Safety under the current policy.
- Whether Health Connect providers (Fitbit, Samsung Health, etc.) impose their own notices.
- The content of any release build's merged manifest (the badging file is the debug APK; release-only R8
  and signing behaviour are not audited here).
- Any legal conclusion about the owner's role (controller vs joint controller) and the applicable
  establishment; that needs counsel.

## 17. Not legal advice

This document is an engineering assessment of the repository. It is **not legal advice** and does not
create a lawyer–client relationship. GDPR compliance depends on facts outside the repository (the
controller's identity and establishment, the actual data flows on real devices, contracts, the Play
listing and supervisory-authority guidance). Obtain advice from qualified privacy counsel before
relying on this document for a public release.
