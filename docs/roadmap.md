# Roadmap: från hälsoapp till komplett hälsocoach

En genomgång av vad Novila kan i dag och vad som saknas för att bli en hälsocoach som kan allt. Sparad oktober 2026.
Bocka av här när något blir klart, och flytta upp det som blir viktigare.

## Princip: klockan mäter, Novila förstår och föreslår

Novila tävlar inte med Google Fit, Fitbit eller klockornas egna appar om att mäta. Steg, puls, pass och sömn mäts
bättre av klockan, och då läser Novila dem via Health Connect i stället för att mäta själv. Det Novila tillför är
tolkningen: coachen och rehab, kosten mot Livsmedelsverkets råd, dagens balans och insikter som kopplar ihop delarna,
allt på telefonen. Varje ny funktion ska antingen läsa det som redan mäts eller tillföra tolkning, coachning eller
samband som de andra inte har. Handloggning finns kvar som reserv för den som inte har klocka.

## Vad Novila kan i dag

| Del | Funktioner | Bedömning |
|---|---|---|
| Idag | Hälsning med namn, dagens balans (ringar), en ruta per del, insikter, veckans sömn och kost, mående | Snyggt, men bara veckan |
| Träning | Veckoprogram (WHO), coach med mål, dagar, fokus och hänsyn, rehab (knä, ländrygg, fas 1–3), 13 illustrerade övningar, passläge med timer, konditionspass (loggade och från Health Connect), RPE och smärta efter passet, Gemini Nano-kommentar | Appens starkaste del |
| Sömn | Läggtid och uppvakning (eller klockans natt från Health Connect), kvalitet och orsaker, vad som påverkar sömnen, graf över vecka, fyra veckor eller år, sömncoach med läggtidsmål, kvällsrutin och råd ur egna nätter | Bra |
| Kost | Portioner per livsmedelsgrupp, vatten mot eget mål, sött, poäng 0–100, "nästa steg", poängen över vecka, fyra veckor eller år | Bra och enkelt |
| Rörelse | Dagens steg och aktiva kalorier, fem veckors träningspass och nätterna, från Health Connect, i förgrunden | Steg bara för i dag |
| Inställningar | Tema, profil, egna mål, ansvarsfriskrivning, radera data | Komplett för sin nivå |

## Det som saknas

Funktioner markerade **(ok)** kräver ägarens ok enligt CLAUDE.md: nya behörigheter eller datatyper, Gemini Nano till
mer än kommentaren, eller data som lämnar appen.

### 1. Grunder för alla användare
- [x] **Introduktion vid första start:** karusell, mål, profil, Health Connect och coachen, så att planen finns från dag ett
- [x] **Påminnelser:** logga sömnen, träning (de dagar programmet har ett pass) och logga kosten, var och en på egen
  tid och bara om det inte redan är loggat. WorkManager, ett jobb per påminnelse och dag. Läggdags återstår.
- [x] **Redigera tidigare dagar.** Sömn och kost går att ändra upp till 30 dagar bakåt (dagväljaren på Sömn och Kost).
- [x] **Historik längre än 7 dagar:** Sömn och Kost visar vecka, fyra veckor (en punkt per vecka) eller år (en per
  månad), med snittet jämfört med perioden innan. Träningsminuter och mående över tid återstår.
- [x] **Egna mål** för steg, sömn och vatten, med förslag utifrån ålder och vikt i profilen. Portionerna följer
  Livsmedelsverkets kostråd, som kostpoängen bygger på.
- [ ] **Export och import** som lokal fil, utan moln **(ok)**

### 2. Träning
- [x] **Logga konditionspass** ("Jag gjorde det"), och läs dem från Health Connect (`ExerciseSessionRecord`) **(ok)**
- [x] **Ansträngning (RPE) och smärta (0–10) efter passet**, så att coachen själv föreslår dos och rehabfas
- [ ] **Progression per övning:** spara set och reps, och höj stegvis
- [ ] **Fler övningar och redskap:** varianter, hantlar, gummiband, gym
- [ ] **Fler rehabområden:** axel, nacke, höft
- [ ] **Uppvärmning och nedvarvning** i passläget, och vilotimern som notis
- [ ] **Veckoavstämning:** "3 av 4 pass, nästa vecka …"

### 3. Sömn
- [x] **Läggtid och uppvakning** i stället för bara timmar, så att regelbundenheten syns
- [x] **Sömnkvalitet (1–5)** och orsaker: sent koffein, skärm, alkohol, stress, sen måltid, sen träning
- [x] **Vad påverkar din sömn:** samband mellan dagen före (kost, vatten, träning, sött), orsakerna och nattens
  längd och kvalitet, med statistisk prövning och antalet nätter; veckans förändring med det som ändrades med den
- [x] **Sömn från Health Connect** (`READ_SLEEP`) **(ok)**: klockans natt fyller i Sömn och Idag när användaren inte
  har loggat själv, bara i minnet. Sömnpåminnelsen vet inte om den (ingen bakgrundsläsning).
- [x] **Sömncoach:** mål för läggtid, kvällsrutin och sömnhygien (KBT-I-inspirerat)

### 4. Kost
- [ ] **Måltidsloggning** ("Frukost: gröt, bär") som fyller i grupperna
- [ ] **Snabbval och favoriter**
- [ ] **Kostprofil:** vegetarisk, vegansk, laktos, gluten
- [ ] **Vätska utöver vatten**, och en tidsgräns för koffein kopplad till sömnen

### 5. Kropp och mående
- [ ] **Vikt och mått över tid** (profilen har bara ett värde)
- [ ] **Utökat mående:** energi, stress och en dagboksrad
- [ ] **Återhämtning:** vilopuls och HRV från klockan **(ok)**, som kan sänka passets intensitet
- [ ] **Andning och avslappning:** korta guidade pass, ritade som anatomifiguren

### 6. Coachen som helhet
- [ ] **En coach för alla delar:** "Du sov 5,5 h. Kör ett lättare pass och lägg dig 22.30."
- [ ] **Fler insikter:** träning ↔ sömn, träning ↔ mående, kost ↔ energi, mönster per veckodag
- [ ] **Prestationer och streaks**, återhållsamt och utan att belöna överträning
- [ ] **Gemini Nano i fler roller**, t.ex. en veckosammanfattning **(ok)**

### 7. Plattform
- [x] **Widget på hemskärmen** (Glance): dagens balans, sömn, kost och vatten, senaste loggen, en uppmuntran utifrån
  det du gjort (`cheers`) och ett glas vatten med ett tryck. Mer snabbloggning (portioner, mående) återstår.
- [ ] **Snabbinställningsruta och genvägar:** "Starta passet", "Logga vatten"
- [ ] **Wear OS:** passläget och vilotimern på klockan
- [ ] **Surfplatta och vikbara skärmar** (skillen `adaptive`)
- [ ] **Tillgänglighet:** genomgång med TalkBack och stor text

## Ordning

1. Introduktion vid första start ← klar
2. Egna mål ← klar
3. Logga konditionspass, och RPE och smärta efter passet ← klar
4. Redigera tidigare dagar och längre historik ← klar
5. Påminnelser ← klar
6. Sömn 2.0 och sömn från Health Connect ← klar
7. Widget på hemskärmen ← klar
8. Sömncoach (KBT-I) ← klar
