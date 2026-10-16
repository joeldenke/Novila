# Roadmap: från hälsoapp till komplett hälsocoach

En genomgång av vad Novila kan i dag och vad som saknas för att bli en hälsocoach som kan allt. Sparad oktober 2026.
Bocka av här när något blir klart, och flytta upp det som blir viktigare.

## Vad Novila kan i dag

| Del | Funktioner | Bedömning |
|---|---|---|
| Idag | Hälsning med namn, dagens balans (ringar), en ruta per del, insikter, veckans sömn och kost, mående | Snyggt, men allt slutar efter 7 dagar |
| Träning | Veckoprogram (WHO), coach med mål, dagar, fokus och hänsyn, rehab (knä, ländrygg, fas 1–3), 13 illustrerade övningar, passläge med timer, Gemini Nano-kommentar | Appens starkaste del |
| Sömn | Timmar i halvtimmessteg och en veckograf | Tunnast: bara en siffra |
| Kost | Portioner per livsmedelsgrupp, vatten mot eget mål, sött, poäng 0–100, "nästa steg" | Bra och enkelt |
| Rörelse | Dagens steg och aktiva kalorier från Health Connect, i förgrunden | Bara dagens värden |
| Inställningar | Tema, profil, egna mål, ansvarsfriskrivning, radera data | Komplett för sin nivå |

## Det som saknas

Funktioner markerade **(ok)** kräver ägarens ok enligt CLAUDE.md: nya behörigheter eller datatyper, Gemini Nano till
mer än kommentaren, eller data som lämnar appen.

### 1. Grunder för alla användare
- [x] **Introduktion vid första start:** karusell, mål, profil, Health Connect och coachen, så att planen finns från dag ett
- [ ] **Påminnelser** (pass, logga sömn, läggdags) **(ok: `POST_NOTIFICATIONS`)**
- [ ] **Redigera tidigare dagar.** I dag går bara idag att ändra.
- [ ] **Historik längre än 7 dagar:** månad och år, och trender över tid
- [x] **Egna mål** för steg, sömn och vatten, med förslag utifrån ålder och vikt i profilen. Portionerna följer
  Livsmedelsverkets kostråd, som kostpoängen bygger på.
- [ ] **Export och import** som lokal fil, utan moln **(ok)**

### 2. Träning
- [ ] **Logga konditionspass** ("Jag gjorde det"), och läs dem från Health Connect (`ExerciseSessionRecord`) **(ok)**
- [ ] **Ansträngning (RPE) och smärta (0–10) efter passet**, så att coachen själv föreslår dos och rehabfas
- [ ] **Progression per övning:** spara set och reps, och höj stegvis
- [ ] **Fler övningar och redskap:** varianter, hantlar, gummiband, gym
- [ ] **Fler rehabområden:** axel, nacke, höft
- [ ] **Uppvärmning och nedvarvning** i passläget, och vilotimern som notis
- [ ] **Veckoavstämning:** "3 av 4 pass, nästa vecka …"

### 3. Sömn
- [ ] **Läggtid och uppvakning** i stället för bara timmar, så att regelbundenheten syns
- [ ] **Sömnkvalitet (1–5)** och orsaker: sent koffein, skärm, alkohol, stress
- [ ] **Sömn från Health Connect** (`READ_SLEEP`) **(ok)**
- [ ] **Sömncoach:** mål för läggtid, kvällsrutin och sömnhygien (KBT-I-inspirerat)

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
- [ ] **Widget på hemskärmen** (Glance): dagens balans och snabbloggning
- [ ] **Snabbinställningsruta och genvägar:** "Starta passet", "Logga vatten"
- [ ] **Wear OS:** passläget och vilotimern på klockan
- [ ] **Surfplatta och vikbara skärmar** (skillen `adaptive`)
- [ ] **Tillgänglighet:** genomgång med TalkBack och stor text

## Ordning

1. Introduktion vid första start ← klar
2. Egna mål ← klar
3. Logga konditionspass, och RPE och smärta efter passet
4. Redigera tidigare dagar och längre historik
5. Påminnelser **(ok)**
6. Sömn 2.0, sedan sömn från Health Connect **(ok)**
7. Widget på hemskärmen
