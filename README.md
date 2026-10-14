# Novila

Svensk hälsoapp för Android som samlar träning, sömn och kost.
Första versionen har fyra flikar (Idag, Träning, Sömn, Kost) och loggar vatten, sömn och mående.

Projektet utvecklas helt från mobilen: Claude Code skriver koden, GitHub Actions bygger
och Firebase App Distribution levererar appen till testarna.

## Så fungerar flödet
1. Claude jobbar i en branch och öppnar en PR.
2. **CI** kör lint, enhetstester och ett debug-bygge. Måste vara grön.
3. Du mergar PR:en från GitHub-appen.
4. **Distribute to testers** bygger en signerad release (R8) och skickar den till Firebase-testarna.
   Testarna får ett mejl, eller en notis i Firebase App Tester-appen.

## Engångssetup för Firebase (går att göra från mobilens webbläsare)
1. Skapa ett projekt på [console.firebase.google.com](https://console.firebase.google.com).
2. Lägg till en Android-app med paketnamnet **`se.novila`**. Hoppa över `google-services.json`, appen behöver den inte.
3. Gå till **App Distribution**, kom igång och skapa en testgrupp med aliaset `testare`. Lägg till dig själv.
4. Kopiera **App ID** från Projektinställningar (ser ut som `1:123…:android:abc…`).
5. I [Google Cloud Console](https://console.cloud.google.com/iam-admin/serviceaccounts) för samma projekt:
   skapa ett servicekonto med rollen **Firebase App Distribution Admin** och ladda ner en JSON-nyckel.
6. I GitHub-repot, Settings → Secrets and variables → Actions:
   - Secret `FIREBASE_SERVICE_ACCOUNT` = hela JSON-filens innehåll
   - Variable `FIREBASE_APP_ID` = App ID från steg 4
   - Variable `FIREBASE_TESTER_GROUPS` = `testare`
7. Skapa release-nyckeln i [Cloud Shell](https://shell.cloud.google.com) (fungerar i mobilens webbläsare):
   ```sh
   keytool -genkeypair -keystore novila-release.jks -alias novila \
     -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=Novila"
   base64 -w0 novila-release.jks
   ```
   Lägg in i GitHub:
   - Secret `NOVILA_RELEASE_KEYSTORE_BASE64` = utskriften från `base64`
   - Secret `NOVILA_RELEASE_STORE_PASSWORD` och `NOVILA_RELEASE_KEY_PASSWORD` = lösenorden du valde
   - Variable `NOVILA_RELEASE_KEY_ALIAS` = `novila`

   Spara `.jks`-filen och lösenorden i en lösenordshanterare. Tappar du nyckeln kan befintliga
   installationer inte uppdateras. Radera sedan filen i Cloud Shell.
8. Settings → Branches: skydda `main`, kräv PR och kräv att kontrollen **Lint, test and build** är grön.

## Android-skills
Googles officiella [Android-skills](https://github.com/android/skills) aktiveras automatiskt
för Claude Code i det här repot via `.claude/settings.json`. Vilken skill som gäller för vad
står i `CLAUDE.md`.

## Teknik
Kotlin 2.4, AGP 9.4, Gradle 9.6, Jetpack Compose (BOM 2026.09), Navigation 3 och ett eget
designsystem ovanpå Material 3. Appen finns på engelska och svenska.
Versionskatalog, configuration cache, build cache, R8 på release, Dependabot och
SHA-pinnade GitHub Actions.
