---
project: PlatePlan
version: 2
status: draft
created: 2026-11-03
updated: 2026-11-03
prd_version: "—"
main_goal: quality
top_blocker: none
milestone_id: varied-week-plans
milestone_seq: 2
milestone_status: open
---

# Roadmap: PlatePlan

> Derived from the user description on 2026-11-03 (two separate tasks) + the closed M-1 baseline.
> Edit-in-place; archive when superseded.
> Slices below are listed in dependency order. The "At a glance" table is the index.

## Milestone

**M-2: Plany, które nie powtarzają ostatnich dań** — Status: open

- **Intent:** Kolejne generowanie przestaje wracać do dań z trzech ostatnich planów, a potem uwzględnia dania, które smakowały, i dania, które w ogóle nie smakowały. Pliki PDF dalej są tylko pobierane.
- **Source materials:** user description (anchors below)
- **Done when:** every S-NN below is `done`
- **Scope anchors:**
  - MS-01: Zapisać 3 ostatnich wygenerowanych planów i poprawić prompt tak, aby nie powielał ostatnich planów, tylko wymyślał coś innego.
  - MS-02: Zapisywać całe dania w aplikacji i zrobić opcję like i unlike, aby do promptu podać to, co smakowało, i to, co w ogóle nie smakowało.

## Vision recap

Każdego dnia trzeba wymyślić śniadanie, drugie śniadanie, obiad i kolację. PlatePlan już układa tydzień z potwierdzonych kalorii i preferencji, ale kolejne generowania często wracają do tych samych dań. Ten kamień milowy sprawia, że następny plan pamięta trzy ostatnie plany, a później także oceny dań.

## North star

**S-01: użytkownik może wygenerować plan, który nie powtarza dań z trzech ostatnich planów** — to jest najmniejszy przepływ, który sprawdza, czy pamięć ostatnich planów w ogóle usuwa powtórzenia.

> Gwiazda przewodnia to najmniejszy przekrój od końca do końca, którego dostarczenie pokazuje, że produkt rozwiązuje główny problem — tutaj powtarzanie dań. Stoi tak wcześnie, jak pozwalają warunki wstępne, bo pozostałe przekroje mają sens tylko wtedy, gdy to działa.

## At a glance

| ID   | Change ID           | Outcome (user can …)                                                                 | Prerequisites | PRD refs | Status   |
| ---- | ------------------- | ------------------------------------------------------------------------------------ | ------------- | -------- | -------- |
| S-01 | recent-plan-memory  | użytkownik może wygenerować plan, który nie powtarza dań z trzech ostatnich planów | —             | MS-01    | ready    |
| S-02 | dish-likes          | użytkownik może oznaczyć danie jako lubiane albo nielubiane i dostać plan, który to uwzględnia | S-01          | MS-02    | proposed |

## Baseline

What's already in place in the codebase as of 2026-11-03 (from the closed milestone, `tech-stack.md`, and the current application guide).
Foundations below assume these are present and do not re-scaffold them.

- **Frontend:** present — server-rendered screens for sign-in, the profile, and the plan
- **Backend / API:** present — a signed-in account can save a profile and generate a seven-day plan with a shopping list
- **Data:** present — a database holds the account and the profile; schema changes are migrations
- **Auth:** present — form sign-in, one account, own data only
- **Deploy / infra:** present — a container image, host config, and a workflow that tests then deploys
- **Observability:** partial — health and info are public; there is no error tracker

## Foundations

Ten kamień milowy nie dodaje fundamentu. Pamięć trzech ostatnich planów wchodzi w S-01, bo dopiero kolejne generowanie z niej korzysta. Oceny dań wchodzą w S-02.

## Slices

### S-01: Trzy ostatnie plany bez powtórzeń

- **Outcome:** użytkownik może wygenerować plan, który nie powtarza dań z trzech ostatnich planów
- **Change ID:** recent-plan-memory
- **PRD refs:** MS-01
- **Prerequisites:** —
- **Parallel with:** —
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Pierwszy, bo to jest skarga, od której użytkownik zaczął. Zapis treści jest w tym przekroju, a nie w osobnym fundamencie. Pliki PDF zostają tylko do pobrania.
- **Status:** ready

### S-02: Ocena dań lubię i nie lubię

- **Outcome:** użytkownik może oznaczyć danie jako lubiane albo nielubiane i dostać plan, który to uwzględnia
- **Change ID:** dish-likes
- **PRD refs:** MS-02
- **Prerequisites:** S-01
- **Parallel with:** —
- **Blockers:** —
- **Unknowns:**
  - Czy ocena jest wspólna dla konta, czy osobna dla każdej osoby? — Owner: user. Block: no.
  - Czy „nie lubię” tylko trafia do promptu, czy danie nie wraca już nigdy? — Owner: user. Block: no.
- **Risk:** Po S-01, bo oba przekroje zmieniają to samo generowanie. Ocena dokłada się do pamięci ostatnich planów, zamiast powstawać obok niej.
- **Status:** proposed

## Backlog Handoff

| Roadmap ID | Change ID          | Suggested issue title                                      | Ready for `/10x-plan` | Notes                                      |
| ---------- | ------------------ | ---------------------------------------------------------- | --------------------- | ------------------------------------------ |
| S-01       | recent-plan-memory | Pamiętać trzy ostatnie plany i nie powtarzać ich dań      | yes                   | Run `/10x-plan recent-plan-memory`         |
| S-02       | dish-likes         | Oznaczać dania jako lubiane albo nielubiane                | no                    | After S-01                                 |

## Open Roadmap Questions

1. **Czy zaktualizować PRD, które zabrania przechowywania planów i list zakupów?** — Owner: user. Block: nie blokuje S-01 ani S-02. Ten kamień milowy pamięta treść dań na potrzeby kolejnego generowania. Pliki PDF zostają tylko do pobrania, jak w zamkniętym kamieniu milowym.

## Parked

- **Przechowywanie plików PDF planu i listy zakupów** — Why parked: użytkownik poprosił o pamięć treści na potrzeby kolejnego generowania. Pliki dalej są tylko generowane i pobierane.

## Milestone History

- **M-1: First downloadable next-day plan** — closed 2026-10-07. S-03 delivered in `calorie-formula` (F-03). S-06 delivered by stored profile (S-02, F-03) plus generate on `/plan` (S-04); no separate `confirm-daily-calories` or `return-visit-plan` change.

## Done

Wpisy poniżej należą do zamkniętego M-1. Identyfikatory `S-01` i `S-02` w otwartym kamieniu milowym są nowe.

- **F-01: (foundation) the application has a configured database. Product tables are not created here.** — Archived 2026-10-01 → `context/archive/2026-09-29-database-configured/`. Lesson: —.
- **F-02: (foundation) Spring Security can require a signed-in account before that account's data is shown, and the sign-in tables exist for one account with no roles. Public heap dump and shutdown cannot expose account data.** — Archived 2026-10-02 → `context/archive/2026-10-01-spring-security-sign-in/`. Lesson: —.
- **S-01: user can create an account from the login window and land signed in, without a second login step, and can log in again from that window on a later visit.** — Archived 2026-10-02 → `context/archive/2026-10-02-register-and-sign-in/`. Lesson: —.
- **S-02: user can enter age, height, weight, sex, goal, activity level, preferred products, and excluded products on one screen, in one form and one request, and the profile is stored in the table `user_profile` on the account and shown again on a later visit.** — Archived 2026-10-05 → `context/archive/2026-10-02-save-user-profile/`. Lesson: —.
- **F-03: (foundation) a daily calorie number on the profile is BMR times one activity level, then −250 (`LOSE_WEIGHT`), 0 (`MAINTAIN`), or +250 (`GAIN`), rounded half-up to a whole kilocalorie. Weight is in kilograms, height in centimetres, age in years. Sex selects the BMR line. Male: BMR = (10 × weight) + (6.25 × height) − (5 × age) + 5. Female: BMR = (10 × weight) + (6.25 × height) − (5 × age) − 161. Activity is `SEDENTARY` ×1.2 (most of the day sitting), `LIGHT` ×1.375 (walking or light effort on most days), `MODERATE` ×1.55 (exercise several days a week), or `HIGH` ×1.725 (hard training or physical work on most days). `/profile` shows an editable Cel kaloryczny. The first body save stores the formula as `confirmed_calories`. The client edits that number on `POST /profile/calories` (only the calorie amount). Recalculate is `POST /profile/recalculate`.** — Archived 2026-10-05 → `context/archive/2026-10-05-calorie-formula/`. Lesson: —.
- **S-04: user can open `/plan`, call Ollama Cloud, and receive a next-day diet plus shopping list as two PDF fields (`dietPdf`, `shoppingListPdf`) on `POST /plan/generate`. The text and files are not stored.** — Archived 2026-10-06 → `context/archive/2026-10-05-generate-diet-with-ollama/`. Lesson: —.
- **S-05: user can download the diet and the shopping list as two PDF files that are not stored.** — Archived 2026-10-06 → `context/archive/2026-10-05-generate-diet-with-ollama/`. Lesson: —.
- **S-03: user can see the number from the F-03 formula, computed from the profile saved in S-02, after the goal adjustment, then accept or edit it. The confirmed number stays on the account in `user_profile`. Preferred and excluded products do not change the number.** — Closed 2026-10-07 (delivered in F-03) → `context/archive/2026-10-05-calorie-formula/`. Lesson: —.
- **S-06: user can return later and generate another next-day plan and shopping list without entering age, height, weight, sex, goal, activity, or preferences again.** — Closed 2026-10-07 (delivered by stored profile + S-04; no separate change) → `context/archive/2026-10-05-generate-diet-with-ollama/`. Lesson: —.
