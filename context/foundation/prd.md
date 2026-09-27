---
project: PlatePlan
version: 1
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

Diets from the internet impose specific dishes. PlatePlan lets you state what you like and what you do not like, and it generates a plan that follows those preferences, without imposing a ready-made diet. The application calculates daily calorie needs, generates a diet plan for the next day, and a shopping list as two PDF files. The calorie rule does not change at a hundred times the number of people. It depends on creating an account.

## User & Persona

Primary persona: You. One person, one account. Context: you want to lose weight, maintain it, or gain it, and stick to a diet. You do not have time to invent meals. You use the application in the morning or in the evening to generate a plan for the next day.

### Secondary persona

After the MVP: the ability to add more people, for example a family. This version does not include them.

## Success Criteria

### Primary

- After the calories are confirmed, the user downloads two PDF files: the diet plan for the next day and the shopping list.
- On the next visit they do not enter the data again, and they generate another plan.

### Secondary

- More people are outside this version. That stays for later, and on its own it is not enough to call the product working.

### Guardrails

- User data must not leak.
- The diet plan and the shopping list are not stored. They are only generated and downloaded.
- Age, height, weight, sex, goal, and preferences stay on the account between visits.

## User Stories

### US-01: Generate a diet plan and a shopping list

- **Given** the user opens the application, sees a login window, chooses Register, the account is created, and login happens automatically.
- **When** the user enters their data, clicks Calculate calories, accepts or edits the result, and then chooses Generate plan.
- **Then** the application generates a diet plan for the next day and a shopping list. Both files are available to download as PDFs and are not stored.

### US-02: A later visit without entering the data again

- **Given** the user has an account, and age, height, weight, sex, goal, and preferences are already stored
- **When** they return in the morning or in the evening and do not enter that data again
- **Then** they generate a diet plan for the next day and a shopping list. Both files are available to download as PDFs and are not stored.

## Functional Requirements

- FR-001: The user can create an account. Priority: must-have
  > Socrates: Counter-argument considered: none against creating an account. Open point moved to FR-002: whether login is a separate step after registration.
  > Resolution: kept; it stands as written.
- FR-002: The user can log in. After registration, login is automatic. Priority: must-have
  > Socrates: Counter-argument considered: "A separate login after registration is a second hurdle before the data."
  > Resolution: kept for a later visit. After registration, login is automatic. There is a login window with a Register button at the bottom.
- FR-003: The user can enter age, height, weight, sex, goal, preferred products, and excluded products. Priority: must-have
  > Socrates: Counter-argument considered: none.
  > Resolution: No counter-argument; it stands as written.
- FR-004: The application can calculate calories from age, height, weight, sex, and goal. Preferred and excluded products do not change that number. Priority: must-have
  > Socrates: Counter-argument considered: none.
  > Resolution: No counter-argument; it stands as written.
- FR-005: The user can accept or change the calorie number. Priority: must-have
  > Socrates: Counter-argument considered: none.
  > Resolution: No counter-argument; it stands as written.
- FR-006: The user can download the diet plan for the next day and the shopping list. The plan and the shopping list are not stored. Priority: must-have
  > Socrates: Counter-argument considered: none.
  > Resolution: No counter-argument; it stands as written.

## Non-Functional Requirements

- The diet plan and the shopping list are not stored. They are only generated and downloaded.
- User data must not leak.
- Age, height, weight, sex, goal, and preferences stay on the account, and the user does not enter them again.

## Business Logic

The application calculates, for the user, how many calories they should take in.

Lose weight lowers the result, maintain leaves it, gain raises it. The calculation uses BMR plus goal. Preferred products and excluded products do not change that number. In the diet plan they are included or excluded.

Input to the calculation: age, height, weight, sex, goal (maintain, lose weight, gain). Input to the plan: those products and the accepted or edited calorie number. The user may edit the number. The output is two PDF files: the diet plan for the next day and the shopping list. Neither file is stored. Account data stays. On the next visit the user does not enter it again and can generate a plan immediately.

## Access Control

One user is one account. The access model is flat, with no roles. On entering the site there is a login window. Registration ends in an automatic login. The user sees only their own data. Age, height, weight, sex, goal, and preferences stay on the account.

## Non-Goals

- Support for multiple people. That comes later. This version is one account.
- Storing diet plans and shopping lists. Both files are generated and downloaded, not kept.
- Advanced user roles. The access model stays flat.

## Open Questions

1. **By how much does lose weight lower the result, and by how much does gain raise it?** — Owner: user. The direction is settled. The size of the change is not.
2. **Does activity enter the calorie calculation?** — Owner: user. It has not been decided.
3. **Does the result stay two PDF files, or become an email with the full content?** — Owner: user. Two PDFs are what is written down now. Email is under consideration and does not replace the PDFs until it is chosen.
4. **How many weeks is the MVP?** — Owner: user. The user said "I don't know."
