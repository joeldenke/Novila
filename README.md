# Novila

Svensk hälsoapp för Android som samlar träning, sömn och kost.
Första versionen har fyra flikar (Idag, Träning, Sömn, Kost) och loggar vatten, sömn och mående.

Projektet utvecklas helt från mobilen: Claude Code skriver koden, GitHub Actions bygger
och Firebase App Distribution levererar appen till testarna.

## Så fungerar flödet
1. Claude jobbar i en branch och öppnar en PR.
2. **CI** kör lint, enhetstester och bygger debug + release. Måste vara grön.
3. Du mergar PR:en från GitHub-appen.
4. **Distribute to testers** bygger appen och skickar den till Firebase-testarna.
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
4. Settings → Branches: skydda `main`, kräv PR och kräv att kontrollen **Lint, test and build** är grön.

Inga hemligheter behövs. Jobbet får bara logga in om det körs i repot `joeldenke/Novila`
och i GitHub-environmentet `firebase`.

## Android-skills
Googles officiella [Android-skills](https://github.com/android/skills) aktiveras automatiskt
för Claude Code i det här repot via `.claude/settings.json`. Vilken skill som gäller för vad
står i `CLAUDE.md`.

## Teknik
Kotlin 2.4, AGP 9.4, Gradle 9.6, Jetpack Compose (BOM 2026.09), Navigation 3 och ett eget
designsystem ovanpå Material 3. Appen finns på engelska och svenska.
Versionskatalog, configuration cache, build cache, R8 på release, Dependabot och
SHA-pinnade GitHub Actions.
