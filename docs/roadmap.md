# Roadmap: from health app to complete health coach

A review of what Novila can do today and what is missing to become a health coach that does it all. Saved October 2026.
Tick items off here when they are done, and move up what becomes more important.

## Principle: the watch measures, Novila understands and suggests

Novila does not compete with Google Fit, Fitbit or the watches' own apps at measuring. Steps, heart rate, workouts and
sleep are measured better by the watch, so Novila reads them via Health Connect instead of measuring them itself. What
Novila adds is the interpretation: the coach and rehab, nutrition against the Swedish Food Agency's dietary guidelines,
the day's balance and insights that connect the parts, all on the phone. Every new feature should either read what is
already measured or add interpretation, coaching or connections the others do not have. Manual logging remains as a
fallback for people without a watch.

## What Novila can do today

| Part | Features | Assessment |
|---|---|---|
| Today | Greeting by name, the day's balance (rings), *Your day*: a checklist of sleep, today's training, food and water with the one button that does each, a tile per part and insights | Says what to do now |
| Training | Weekly program (WHO), coach with goal, days, focus, considerations and equipment, rehab (knee, lower back, shoulder, neck, hip, phases 1–3), 30 illustrated exercises (bodyweight, dumbbells, resistance band and barbell), workout mode with timer, cardio sessions (logged and from Health Connect), RPE and pain after the session, recovery from the watch that can lighten today's session, weekly review | The app's strongest part |
| Sleep | Bedtime and wake-up (or the watch's night from Health Connect), quality and causes, what affects your sleep, chart over a week, four weeks or a year, sleep coach with bedtime goal, evening routine and advice from your own nights, guided breathing (calm, box, 4-7-8) | Good |
| Nutrition | Servings per food group, water against your own goal, sweets, score 0–100, "next step", the score over a week, four weeks or a year, recipe suggestions for what is missing (diet, allergens, time), own recipes with servings and allergens counted from the ingredients | Good and simple |
| Movement | Today's steps and active calories, five weeks of workouts, the nights and recovery (resting heart rate, HRV), from Health Connect, in the foreground | Steps only for today |
| Settings | Profile, *My goals* (daily goals, both coaches and food preferences in one place), theme, reminders, Health Connect, disclaimer, delete data | Complete for its level |

## What is missing

Features marked **(ok)** require the owner's approval according to CLAUDE.md: new permissions or data types, a
language model in the app, or data that leaves the app.

### 1. Basics for every user
- [x] **Introduction on first launch:** carousel, goals, profile, Health Connect and the coach, so the plan exists from day one
- [x] **Reminders:** log sleep, training (on the days the program has a session) and log nutrition, each at its own
  time and only if not already logged. WorkManager, one job per reminder and day. Bedtime remains.
- [x] **Edit earlier days.** Sleep and nutrition can be changed up to 30 days back (the day picker on Sleep and Nutrition).
- [x] **History longer than 7 days:** Sleep and Nutrition show a week, four weeks (one point per week) or a year (one
  per month), with the average compared to the period before. Training minutes over time remain.
- [x] **Own goals** for steps, sleep and water, with suggestions based on age and weight in the profile. Servings follow
  the Swedish Food Agency's dietary guidelines, which the nutrition score is built on.
- [ ] **Export and import** as a local file, without the cloud **(ok)**

### 2. Training
- [x] **Log cardio sessions** ("I did it"), and read them from Health Connect (`ExerciseSessionRecord`) **(ok)**
- [x] **Effort (RPE) and pain (0–10) after the session**, so the coach itself suggests dose and rehab phase
- [x] **Progression per exercise:** save sets and reps, and increase step by step. Session mode asks how many
  repetitions each set was (holds count the seconds held); when every set reached the target, the next session has
  one repetition (or five seconds) more, and a set clearly short takes a step back (`Progression`, double
  progression). Rehab stays dosed by pain.
- [ ] **Count reps automatically:** `RepetitionCounter` counts reps, depth and tempo from joint angles (done);
  the camera and an on-device pose model in workout mode remain **(ok)**
- [x] **More exercises and equipment:** bodyweight variations (knee push-up, single-leg bridge, dead bug, high
  knees), dumbbells (goblet squat, row, shoulder press, biceps curl, Romanian deadlift), resistance band (row,
  pulldown, chest press) and barbell for the gym (front squat, Romanian deadlift, row). The figure draws the
  equipment in the hands; the user says in the coach what they have, and the coach only picks what they can do,
  preferring equipment for the same muscles. Barbell exercises wait until the user already trains strength.
- [x] **More rehab areas:** shoulder, neck, hip
- [x] **Warm-up and cool-down** in workout mode, and the rest timer as a notification
- [x] **Weekly review:** "3 of 4 sessions, next week …"

### 3. Sleep
- [x] **Bedtime and wake-up** instead of just hours, so regularity shows
- [x] **Sleep quality (1–5)** and causes: late caffeine, screens, alcohol, stress, late meal, late training
- [x] **What affects your sleep:** connections between the day before (nutrition, water, training, sweets), the causes
  and the night's length and quality, with statistical testing and the number of nights; the week's change with what
  changed along with it
- [x] **Sleep from Health Connect** (`READ_SLEEP`) **(ok)**: the watch's night fills in Sleep and Today when the user
  has not logged it themselves, only in memory. The sleep reminder does not know about it (no background reading).
- [x] **Sleep coach:** bedtime goal, evening routine and sleep hygiene (CBT-I-inspired)

### 4. Nutrition
- [x] **Meal logging** ("Breakfast: porridge, berries") that fills in the groups
- [x] **Quick picks and favorites:** the food sheet offers the meals written in the user's own words, the starred ones
  first and then the ones logged most often in the last 30 days, each logged again with one tap; the drinks sheet
  offers the kinds and amounts drunk most often. Remembered in the `food` store and deleted with the rest
- [x] **Diet profile:** diet (everything, pescetarian, vegetarian, vegan), the 14 labelled allergens and time in the
  kitchen, in *My goals*
- [x] **Recipe suggestions:** Novila's own recipes that fill what today is missing, chosen deterministically and never
  with an allergen the user avoids; "I ate this" logs a serving. The library covers every ingredient family and diet
  across quick, medium and longer dishes; see `docs/recipes.md`.
- [x] **Fluids beyond water**, and a caffeine cut-off time linked to sleep: drinks by kind and amount, water, coffee,
  tea and milk count towards the water goal, and caffeine from 14:00 counts as late caffeine in the sleep links
- [x] **Own recipes with autocomplete:** the user writes a recipe (name, ingredients, steps) and saves it. As they
  type, known foods are suggested from the same dictionary as meal logging. The app counts servings per food group
  and looks up allergens from the ingredients, never guesses them, and the user confirms them. Saved recipes are
  suggested alongside Novila's own, and what the user cooks and logs steers the suggestions, like the quick picks.
  Never generated by a model (see *Recipes* in `CLAUDE.md`). Done without a model: the user writes the recipe on the Recipes screen, see
  `docs/recipes.md`.

### 5. Body and mood
- [x] **Weight and measurements over time** (the profile only has one value)
- [x] ~~**Extended mood:** energy, stress and a journal line~~ Removed from Today in October 2026, together with mood:
  their purpose was unclear to the owner. The saved fields remain in `DayEntry`.
- [x] **Recovery:** resting heart rate and HRV from the watch **(ok)**, which can lower the session's intensity. A card on
  Training with today's values and a line about the status; today's session gets a little lighter (fewer sets, shorter
  cardio) when the resting heart rate is clearly higher or HRV clearly lower than the user's own baseline. In memory
  only, read in the foreground.
- [x] **Breathing and relaxation:** short guided sessions (calm breathing, box breathing, 4-7-8) with a visual pacer,
  opened from Sleep and the sleep coach's evening routine. Nothing is logged (#76)
- [ ] **The menstrual cycle, for those who want it.** Never a gender question in the introduction: gender does not say
  who has a period (menopause, hormonal IUD, trans people), and the introduction only asks what the app uses. The
  evidence: the phase's effect on performance is on average small and very individual (McNulty et al. 2020), training
  planned by phase has weak support, and symptoms (cramps, fatigue, poorer sleep before the period) are what matter.
  Never predict ovulation or fertile days (contraception and fertility apps are medical devices under MDR, see *No
  medical claims* in `CLAUDE.md`), and no promises of training "optimized" by phase. Cycle data is special category
  health data (GDPR article 9); say plainly next to the choice that it never leaves the phone. In three steps:
  - [x] **Period as a sleep cause** (`SleepFactor.Period`), so the sleep links test it like the other causes. No new
    permission and no gender. The sleep coach never turns it into a tip, since it is not a habit. The factors are
    stored by name, so it needed no new `DayEntryCodec` version: an older app skips names it doesn't know.
  - [x] **"Follow your cycle"** in *My goals*, off by default: log period days and symptoms in a sheet. On a symptom
    day the coach offers a lighter session but never switches by itself. In the luteal phase the resting heart rate
    normally rises a few beats and HRV falls, so `analyzeRecovery` could read a normal cycle as poor recovery and
    lighten sessions every month without anyone knowing why. Done: the period days and symptoms are logged in a
    sheet on Training (`CycleStore`, its own file, deleted when the cycle is turned off), a symptom day offers a
    lighter session (`CoachNote.Lighter`), and recovery compares a day only with days in the same half of the cycle
    (`cyclePhase`). Health Connect gives only two weeks of resting heart rate and HRV, so "the same phase of the last
    cycle" would need a longer read; the same half within the two weeks fixes the false alarm without it.
  - [ ] **Period days from Health Connect** (`READ_MENSTRUATION`) **(ok)**: a new badging line, privacy policy and
    Play declaration.

### 6. The coach as a whole
- [x] **One coach for all parts:** "You slept 5.5 h. Do a lighter session and go to bed at 22:30." `dailyCoach`
  weighs sleep, training and nutrition into one connected suggestion, shown on Today.
- [x] **More insights:** training ↔ sleep, training ↔ mood, nutrition ↔ energy, patterns per weekday. `connections`
  in `:core:model` joins the parts with the same honest test as the sleep links, shown in a *Connections* card on
  Today. Mood and energy are read from the stored fields, so those two only appear when they were logged (they left
  Today in October 2026).
- [x] **Training load (session RPE):** RPE × minutes per session gives the week's load, and its monotony and strain
  (Foster) show when a week gets too heavy or too uniform. The ratio of the last week to the last four (ACWR) is
  debated as an injury predictor, so it is only calm guidance ("this week is heavier than usual"), never a warning.
  Built on data the app already has, deterministic and tested. Done: `trainingLoad` in `:core:exercise`, a card on
  the week's program. The last seven days are compared with the three weeks before (uncoupled), and sessions
  without the user's own effort (cardio, recorded workouts) count with a moderate effort for their kind.
- [x] **Sleep regularity:** a regularity index (Sleep Regularity Index) and social jetlag (the sleep midpoint on
  weekdays against weekends) from the bedtimes and wake-ups already logged. Regularity is linked to health in studies
  at least as strongly as length. Shown on Sleep and weighed by the sleep coach. Done: `sleepRegularity` in
  `:core:model` over four weeks, a card in Sleep's trends, and the sleep coach suggests a steady bedtime when the
  nights are irregular and a steady weekend wake-up (`WeekendShift`) when the weekend moves the sleep an hour.
- [x] **Readiness that sums it up:** an explained readiness from last night's sleep, recovery, the training load and
  any cycle symptoms ("A calm day is suggested, because …"). `dailyCoach` does part of it; make the weighting clear
  and tested, never a score without reasons. Done: `readiness` in `:core:coach` reads four signals that each pull
  down, up or neither, and counts them. Two down is a calm day, one down a day as planned, none down and two up a
  ready body. Today's coach says a calm day or a ready body with its reasons, and nothing unknown counts as bad.
- [ ] **Step goals by age:** benefits level off around 6,000–8,000 steps a day from 60 and 8,000–10,000 below
  (Paluch et al., Lancet 2022), a better basis for the suggested step goal.
- [ ] **Cardio fitness (VO2max) from the watch** via Health Connect **(ok)**: a good long-term measure, a new data type
  in the Play declaration.
- [ ] **Achievements and streaks**, restrained and without rewarding overtraining
- [x] ~~**Gemini Nano in more roles**, e.g. a weekly summary~~ Gemini Nano was removed in October 2026: the
  coach's comment added little to its own reasons, and Play requires AI content to be reportable in the app, which
  needs the internet. The weekly review and the insights explain the numbers without a model.

### 7. Platform
- [x] **Home screen widget** (Glance): the day's balance, sleep, nutrition and water, and encouragement
  based on what you have done (`cheers`), on a frosted card. More quick logging (servings) remains.
- [x] **Quick Settings tile and shortcuts:** "Start the session", "Log water". Two app shortcuts (`AppShortcut`,
  dynamic so they follow the build and the language) and a tile that logs a glass of water and shows today's glasses
  (`WaterTileService`, not on the lock screen). Logging servings from the widget remains: the widget has no buttons
  by design (see *Widget* in `CLAUDE.md`), so that needs the owner's decision.
- [x] **Wear OS:** workout mode and the rest timer on the watch. A standalone app in `:wear` runs a strength
  session and its rests; it does not share the session with the phone yet (see *Wear OS* in `CLAUDE.md`).
- [x] **Tablets and foldables** (the `adaptive` skill): a navigation rail from 600 dp, content centred at most
  720 dp wide, and two to four entry tiles per row (#87)
- [x] **Accessibility:** a pass with TalkBack and large text (#77)
- [ ] **iOS via Kotlin and Compose Multiplatform:** the pure Kotlin modules (`:core:model`, `:core:coach`,
  `:core:exercise`, `:core:sleep`, `:core:anatomy`, `:core:movement`) already have no Android dependencies and can
  become shared code first. Then the design system and the screens in Compose Multiplatform, with Apple Health
  (HealthKit) as `MovementSource`, `ExerciseSource` and `SleepSource`.
  Storage, reminders and the widget need their own iOS implementations.

### 8. Structure and clarity
The app grew one feature at a time and felt scattered. Everything now follows one daily loop: Today says what to do
now, and every tab has the same parts in the same order.
- [x] **Today as a checklist:** *Your day* with sleep, training, food and water, each with the button that does it; the
  week's charts moved to their own tabs (#59)
- [x] **The same parts on every tab:** Overview, Log, Your plan and Trends on Sleep, Nutrition and Training (#60)
- [x] **My goals in one place:** daily goals, both coaches and food preferences on one screen; Settings keeps what is
  about the app (#60)
- [x] **No duplicates on Today:** the text under the balance rings said the same as the food task, and the rings and
  the tiles below showed the same three values. The rings are the summary and each part opens its own tab.
- [x] **Explain a serving on Nutrition:** "Vegetables 3 of 5" is abstract. A short line per food group, e.g. a
  serving is about a handful or 100 g, and potatoes don't count.
- [x] **A shorter Training tab:** the next session and the week's program show the same sessions; fold the next
  session into the program, opened by tapping a day.
- [x] **A steady CI:** the stores' "survives a restart" tests sometimes time out after 5 s, on `main` as well, and turn
  CI red. Fixed by setting the snapshot from each write's result instead of a new emission from `dataStore.data` (#68).

## Order

1. Introduction on first launch ← done
2. Own goals ← done
3. Log cardio sessions, and RPE and pain after the session ← done
4. Edit earlier days and longer history ← done
5. Reminders ← done
6. Sleep 2.0 and sleep from Health Connect ← done
7. Home screen widget ← done
8. Sleep coach (CBT-I) ← done
9. Recovery (resting heart rate and HRV from the watch) ← done
10. Recipe suggestions and food preferences ← done
11. Structure: Today as a checklist, the same parts on every tab, My goals ← done
12. Structure: no duplicates on Today, explain a serving, a shorter Training tab, a steady CI ← done

Next, first what needs no approval and is used every day, then what needs the owner's approval:

13. Quick picks and favorites on Nutrition: the most common meals and drinks with one tap, from what the user has
    already logged. The fastest way to make daily logging easier. ← done
14. Own recipes with autocomplete: saved recipes written by the user, with foods suggested from the dictionary and
    servings and allergens counted by the app, never by a model. Builds on the quick picks. ← done
15. Progression per exercise: save sets and reps per exercise, and let the coach increase step by step. It
    strengthens the app's strongest part, and `CoachFeedback` already reads effort and pain. ← done
16. Shortcuts and a Quick Settings tile: "Start the session" and "Log water", plus logging servings from the widget.
    App shortcuts first; the tile is an exported service, so it needs a new badging line and the owner's approval.
    ← done (shortcuts and the tile, approved by the owner; servings from the widget remain)
17. More exercises and equipment: variations, dumbbells, resistance bands and gym, via the `add-exercise` skill.
    Makes progression worth more and gives the coach more to choose from. ← done
18. Period as a sleep cause, and "Follow your cycle" with recovery that knows the cycle: it corrects something the
    app gets wrong today for many users. Period days from Health Connect follow **(ok)**.
    ← period as a sleep cause and "Follow your cycle" done
19. Training load and sleep regularity: both build on data the app already logs, and make the readiness that sums it
    up possible. ← done
20. Achievements and streaks: restrained, built on what the user already logs, and never rewarding overtraining
    (recovery and rest days count).
21. Export and import as a local file **(ok)**: a backup without the cloud before users have much data to lose.
22. Count reps with the camera **(ok)**: needs `CAMERA` and an on-device pose model, which changes the privacy policy
    and the Play declaration.
23. iOS via Kotlin and Compose Multiplatform: last, when the Android app is complete. Moving the pure Kotlin modules to
    shared code can start earlier without changing the app.
