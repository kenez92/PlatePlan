---
project: PlatePlan
version: 2
status: draft
created: 2026-09-23
context_type: greenfield
product_type: web-app
target_scale:
  users: small
timeline_budget:
  mvp_weeks: "# TODO: mvp_weeks — see Open Questions"
  hard_deadline: null
  after_hours_only: true
---

## Vision & Problem Statement

Every day you have to decide what to eat for breakfast, lunch, and dinner when you want to lose weight, maintain it, or gain it. The cost is time, energy, and often poor food choices. You do not have time to plan.

Diets from the internet impose specific dishes. PlatePlan lets you state what you like and what you do not like, and it generates a plan that follows those preferences, without imposing a ready-made diet. The application calculates daily calorie needs, generates a diet plan for seven days starting tomorrow, and a shopping list as two PDF files. The calorie rule does not change at a hundred times the number of people. It depends on creating an account.

## User & Persona

Primary persona: You. One account, no roles. You are the owner, and you may add at most three people. Context: each person wants to lose weight, maintain it, or gain it, and stick to a diet. You do not have time to invent meals. You use the application in the morning or in the evening to generate a plan for seven days starting tomorrow.

### Secondary persona

The added people share the owner's account. They have no login and no role. An empty owner name is Ja.

## Success Criteria

### Primary

- After the calories are confirmed, the user downloads two PDF files: the diet plan and the shopping list for seven days starting tomorrow.
- On the next visit they do not enter the data again, and they generate another plan.

### Secondary

- The owner and at most three added people share one account. The household is part of this version.

### Guardrails

- User data must not leak.
- The diet plan and the shopping list are not stored. They are only generated and downloaded.
- Age, height, weight, sex, goal, activity level, and preferences stay on the account between visits.

## User Stories

### US-01: Generate a diet plan and a shopping list

- **Given** the user opens the application, sees a login window, chooses Register, the account is created, and login happens automatically.
- **When** the user enters their data, clicks Calculate calories, accepts or edits the result, and then chooses Generate plan.
- **Then** the application generates a diet plan and a shopping list for seven days starting tomorrow. Both files are available to download as PDFs and are not stored. If a delivery email is saved, generate may also send those same two PDFs as attachments.

### US-02: A later visit without entering the data again

- **Given** the user has an account, and age, height, weight, sex, goal, activity level, preferences, and the optional delivery email are already stored
- **When** they return in the morning or in the evening and do not enter that data again
- **Then** they generate a diet plan and a shopping list for seven days starting tomorrow. Both files are available to download as PDFs and are not stored.

## Functional Requirements

- FR-001: The user can create an account. Priority: must-have
  > Socrates: Counter-argument considered: none against creating an account. Open point moved to FR-002: whether login is a separate step after registration.
  > Resolution: kept; it stands as written.
- FR-002: The user can log in. After registration, login is automatic. Priority: must-have
  > Socrates: Counter-argument considered: "A separate login after registration is a second hurdle before the data."
  > Resolution: kept for a later visit. After registration, login is automatic. There is a login window with a Register button at the bottom.
- FR-003: The user can enter age, height, weight, sex, goal, activity level, preferred products, excluded products, and an optional delivery email. Priority: must-have
  > Socrates: Counter-argument considered: none.
  > Resolution: No counter-argument; it stands as written. Activity level was added when the calorie formula was set. Delivery email was added as an optional field for sending the two PDFs.
- FR-004: The application can calculate calories from age, height, weight, sex, goal, and activity level. Preferred and excluded products do not change that number. Priority: must-have
  > Socrates: Counter-argument considered: none.
  > Resolution: No counter-argument; it stands as written. Activity level was added when the calorie formula was set.
- FR-005: The user can accept or change the calorie number. Priority: must-have
  > Socrates: Counter-argument considered: none.
  > Resolution: No counter-argument; it stands as written.
- FR-006: The user can download the diet plan and the shopping list for seven days starting tomorrow. The plan and the shopping list are not stored. Priority: must-have
  > Socrates: Counter-argument considered: none.
  > Resolution: No counter-argument; it stands as written.
- FR-007: When a delivery email is stored, the user can opt in on generate to send those same two PDFs as attachments. A mail failure does not remove the downloads. Priority: should-have
  > Socrates: Counter-argument considered: replacing the two PDF buttons with email only.
  > Resolution: Email is extra delivery, not a replacement. The two download buttons stay.

## Non-Functional Requirements

- The diet plan and the shopping list are not stored. They are only generated and downloaded.
- User data must not leak.
- Age, height, weight, sex, goal, activity level, preferences, and the optional delivery email stay on the account, and the user does not enter them again.

## Business Logic

The application calculates, for the owner and for each added person, how many calories that person should take in.

The number before the goal adjustment is BMR times one activity level. Weight is in kilograms, height in centimetres, age in years. Sex selects the BMR line.

- Male: BMR = (10 × weight) + (6.25 × height) − (5 × age) + 5
- Female: BMR = (10 × weight) + (6.25 × height) − (5 × age) − 161

Activity is one choice:

- `SEDENTARY` ×1.2 — most of the day sitting, little walking
- `LIGHT` ×1.375 — walking or light effort on most days
- `MODERATE` ×1.55 — exercise several days a week
- `HIGH` ×1.725 — hard training or physical work on most days

Lose weight subtracts 250 kcal from that result, maintain adds 0, gain adds 250. That is a light change, not a steep cut. The adjustment is applied to BMR times activity, then one round half-up to a whole kilocalorie. Preferred products and excluded products do not change the number. The plan is shared dishes, with a portion for each selected person. Preferences and exclusions are merged only from the selected people. An exclusion of any selected person removes that product from the dishes and the shopping list. An unselected person adds no portion, no preference, and no exclusion.

Input to the calculation, for the owner and for each added person: age (a whole number from 2 to 110), height (70–250 cm), weight (8.0–400.0 kg with one decimal place), sex, goal (maintain, lose weight, or gain), and activity level. A child's development is maintain or gain. The same formula and the ±250 adjustment apply to each person. Confirmed calories, from the formula or from an edit, are a whole number from 400 to 6000. A formula result outside that range rejects the body save or the recalculation and leaves the previous number. A later body save does not recalculate calories. Input to the plan: the merged products of the selected people and each selected person's confirmed calorie number. Each person's portions for a day stay within ±10% of that person's confirmed calories. The user may edit the number. The output is two PDF files: the diet plan and the shopping list for seven days starting tomorrow. Neither file is stored. When a delivery email is on the account, generate may also send those same two files as attachments. Account data stays, including the activity level, the confirmed calorie number, and the optional email. On the next visit the user does not enter it again and can generate a plan immediately.

## Access Control

One account, with no roles and no second login. The owner and at most three added people share that account. An added person has no email and no account. On entering the site there is a login window. Registration ends in an automatic login. The account sees only its own data. Age, height, weight, sex, goal, activity level, preferences, and the optional delivery email stay on the account.

## Non-Goals

- A second login for someone in the household. This version is one account, with no roles, an owner, and at most three added people.
- Storing diet plans and shopping lists. Both files are generated and downloaded, not kept.
- Advanced user roles. The access model stays flat.

## Open Questions

1. **By how much does lose weight lower the result, and by how much does gain raise it?** — Settled: −250 kcal / +250 kcal, applied to BMR × activity, then one round half-up to a whole kilocalorie. Owner: user.
2. **Does the result stay two PDF files, or become an email with the full content?** — Settled: two PDF download buttons stay. An optional profile email plus a checkbox on generate sends those same two PDFs as attachments. Mail failure keeps the downloads. Owner: user.
3. **How many weeks is the MVP?** — Owner: user. The user said "I don't know."
