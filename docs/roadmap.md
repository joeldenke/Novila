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
| Training | Weekly program (WHO), coach with goal, days, focus and considerations, rehab (knee, lower back, shoulder, neck, hip, phases 1–3), 15 illustrated exercises, workout mode with timer, cardio sessions (logged and from Health Connect), RPE and pain after the session, recovery from the watch that can lighten today's session, Gemini Nano comment, weekly review | The app's strongest part |
| Sleep | Bedtime and wake-up (or the watch's night from Health Connect), quality and causes, what affects your sleep, chart over a week, four weeks or a year, sleep coach with bedtime goal, evening routine and advice from your own nights | Good |
| Nutrition | Servings per food group, water against your own goal, sweets, score 0–100, "next step", the score over a week, four weeks or a year, recipe suggestions for what is missing (diet, allergens, time) | Good and simple |
| Movement | Today's steps and active calories, five weeks of workouts, the nights and recovery (resting heart rate, HRV), from Health Connect, in the foreground | Steps only for today |
| Settings | Profile, *My goals* (daily goals, both coaches and food preferences in one place), theme, reminders, Health Connect, disclaimer, delete data | Complete for its level |

## What is missing

Features marked **(ok)** require the owner's approval according to CLAUDE.md: new permissions or data types, Gemini
Nano for more than the comment, or data that leaves the app.

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
- [ ] **Progression per exercise:** save sets and reps, and increase step by step
- [ ] **Count reps automatically:** `RepetitionCounter` counts reps, depth and tempo from joint angles (done);
  the camera and an on-device pose model in workout mode remain **(ok)**
- [ ] **More exercises and equipment:** variations, dumbbells, resistance bands, gym
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
- [ ] **Meal logging** ("Breakfast: porridge, berries") that fills in the groups
- [ ] **Quick picks and favorites**
- [x] **Diet profile:** diet (everything, pescetarian, vegetarian, vegan), the 14 labelled allergens and time in the
  kitchen, in *My goals*
- [x] **Recipe suggestions:** Novila's own recipes that fill what today is missing, chosen deterministically and never
  with an allergen the user avoids; "I ate this" logs a serving. More recipes remain.
- [ ] **Fluids beyond water**, and a caffeine cut-off time linked to sleep

### 5. Body and mood
- [x] **Weight and measurements over time** (the profile only has one value)
- [x] ~~**Extended mood:** energy, stress and a journal line~~ Removed from Today in October 2026, together with mood:
  their purpose was unclear to the owner. The saved fields remain in `DayEntry`.
- [x] **Recovery:** resting heart rate and HRV from the watch **(ok)**, which can lower the session's intensity. A card on
  Training with today's values and a line about the status; today's session gets a little lighter (fewer sets, shorter
  cardio) when the resting heart rate is clearly higher or HRV clearly lower than the user's own baseline. In memory
  only, read in the foreground.
- [ ] **Breathing and relaxation:** short guided sessions, drawn like the anatomy figure

### 6. The coach as a whole
- [x] **One coach for all parts:** "You slept 5.5 h. Do a lighter session and go to bed at 22:30." `dailyCoach`
  weighs sleep, training and nutrition into one connected suggestion, shown on Today.
- [x] **More insights:** training ↔ sleep, training ↔ mood, nutrition ↔ energy, patterns per weekday. `connections`
  in `:core:model` joins the parts with the same honest test as the sleep links, shown in a *Connections* card on
  Today. Mood and energy are read from the stored fields, so those two only appear when they were logged (they left
  Today in October 2026).
- [ ] **Achievements and streaks**, restrained and without rewarding overtraining
- [ ] **Gemini Nano in more roles**, e.g. a weekly summary **(ok)**

### 7. Platform
- [x] **Home screen widget** (Glance): the day's balance, sleep, nutrition and water, the latest log, encouragement
  based on what you have done (`cheers`) and a glass of water with one tap. More quick logging (servings) remains.
- [ ] **Quick Settings tile and shortcuts:** "Start the session", "Log water"
- [ ] **Wear OS:** workout mode and the rest timer on the watch
- [ ] **Tablets and foldables** (the `adaptive` skill)
- [ ] **Accessibility:** a pass with TalkBack and large text
- [ ] **iOS via Kotlin and Compose Multiplatform:** the pure Kotlin modules (`:core:model`, `:core:coach`,
  `:core:exercise`, `:core:sleep`, `:core:anatomy`, `:core:movement`) already have no Android dependencies and can
  become shared code first. Then the design system and the screens in Compose Multiplatform, with Apple Health
  (HealthKit) as `MovementSource`, `ExerciseSource` and `SleepSource`, and an on-device alternative to Gemini Nano.
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
- [ ] **Explain a serving on Nutrition:** "Vegetables 3 of 5" is abstract. A short line per food group, e.g. a
  serving is about a handful or 100 g, and potatoes don't count.
- [x] **A shorter Training tab:** the next session and the week's program show the same sessions; fold the next
  session into the program, opened by tapping a day.
- [ ] **A steady CI:** the stores' "survives a restart" tests sometimes time out after 5 s, on `main` as well, and turn
  CI red. Find what the reopened store waits on and fix it.

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
12. Structure: no duplicates on Today, explain a serving, a shorter Training tab, a steady CI
