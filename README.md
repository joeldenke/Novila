# Novila

Svensk hälsoapp för Android som samlar träning, sömn och kost.
Första versionen har fyra flikar (Idag, Träning, Sömn, Kost) och loggar vatten, sömn och mående.

Projektet utvecklas helt från mobilen: Claude Code skriver koden, GitHub Actions bygger
och Firebase App Distribution levererar appen till testarna.

## Så fungerar flödet
1. Claude jobbar i en branch och öppnar en PR.
2. **CI** kör lint, enhetstester och ett debug-bygge. Måste vara grön.
3. Du mergar PR:en från GitHub-appen.
4. **Distribute to testers** skickar debug-appen till Firebase-testarna. Release-bygget (R8) byggs också som kontroll.
   Testarna får ett mejl, eller en notis i Firebase App Tester-appen.

## Engångssetup för Google Cloud och Firebase (går att göra från mobilen)
Skriptet `scripts/bootstrap-firebase.sh` gör allt och kan köras om utan risk:
GCP-projekt, Firebase, Android-appen `se.novila.debug`, testgruppen `testare`,
ett servicekonto som bara får distribuera, och Workload Identity Federation så att
GitHub Actions loggar in utan JSON-nyckel.

1. Öppna [Google Cloud Shell](https://shell.cloud.google.com/?cloudshell_git_repo=https://github.com/joeldenke/Novila)
   i mobilens webbläsare. Repot klonas automatiskt och du är redan inloggad.
2. Kör, med ett globalt unikt projekt-id:
   ```sh
   ./scripts/bootstrap-firebase.sh novila-joel
   ```
   Du läggs till som testare. Fler testare: `./scripts/bootstrap-firebase.sh novila-joel du@x.se,kompis@y.se`.
3. Skriptet skriver ut fyra variabler. Lägg in dem i GitHub-repot under
   Settings → Secrets and variables → Actions → Variables
   (eller kör `gh auth login` i Cloud Shell först, då sätter skriptet dem själv).

4. Valfritt, behövs inte för testarna: skapa release-nyckeln i [Cloud Shell](https://shell.cloud.google.com) (fungerar i mobilens webbläsare):
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
5. Settings → Branches: skydda `main`, kräv PR och kräv att kontrollen **Lint, test and build** är grön.

Ingen JSON-nyckel behövs för Firebase. Jobbet får bara logga in om det körs i repot `joeldenke/Novila`
och i GitHub-environmentet `firebase`.

## Android-skills
Googles officiella [Android-skills](https://github.com/android/skills) aktiveras automatiskt
för Claude Code i det här repot via `.claude/settings.json`. Vilken skill som gäller för vad
står i `CLAUDE.md`.

## Teknik
Kotlin 2.4, AGP 9.4, Gradle 9.8, Jetpack Compose (BOM 2026.09), Navigation 3 och ett eget
designsystem ovanpå Material 3. Appen finns på engelska och svenska.
Moduler per lager: `:app` (bara aktivitet, navigation och ihopkoppling), en `:feature:*` per flik,
och `:core:*` för designsystem, delad UI, data, Health Connect och ren Kotlin-logik (modell, anatomi, träning,
coach). Convention plugins i `build-logic` gör att modulernas byggfiler bara innehåller plugins och beroenden;
kompatibelt med Isolated Projects. Versionskatalog, configuration cache, build cache, R8 på release, Dependabot och
SHA-pinnade GitHub Actions.
