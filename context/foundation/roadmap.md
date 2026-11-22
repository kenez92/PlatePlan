---
project: PlatePlan
version: 5
status: draft
created: 2026-11-16
updated: 2026-11-22
prd_version: "—"
main_goal: quality
top_blocker: decisions
milestone_id: recipe-catalog-plans
milestone_seq: 4
milestone_status: open
---

# Roadmap: PlatePlan

> Derived from the user description on 2026-11-22 (own recipe database from the Polish Wikibooks cookbook, custom recipes, per-person likes, portions adapted to the person; optional ready-made second meal).
> Edit-in-place; archive when superseded.
> Slices below are listed in dependency order. The "At a glance" table is the index.

## Milestone

**M-4: Plany z bazy przepisów** — Status: open

- **Intent:** Własna baza trzyma przepisy na śniadanie, drugie śniadanie, obiad i kolację. Zaczyna się od polskiej Książki kucharskiej na Wikibooks i od przepisów dodanych przez użytkownika. Plan dobiera przepis według tego, co która osoba lubi, i dostosowuje porcje do jej planu. Osoba może na drugie danie woleć gotowca.
- **Source materials:** user description (anchors below)
- **Done when:** every S-NN below is `done`
- **Scope anchors:**
  - MS-01: Wszystkie przepisy są w bazie danych.
  - MS-02: Bazę budujemy z polskiej Książki kucharskiej na Wikibooks (https://pl.wikibooks.org/wiki/Książka_kucharska, licencja CC BY-SA 3.0). Przy przepisie zostaje link do strony, data pobrania i licencja.
  - MS-03: Plan jedynie dostosowuje przepis do konkretnej osoby i jej planu.
  - MS-04: Baza obejmuje śniadanie, drugie śniadanie, obiad i kolację.
  - MS-05: Osoba może na drugie danie wybrać gotowca: gotową kupną sałatkę, serek, skyr albo coś w tym stylu.
  - MS-06: Użytkownik może dodać własny przepis.
  - MS-07: Użytkownik może polubić albo odrzucić przepis dla konkretnej osoby, żeby było wiadomo, co kto lubi.

## Vision recap

Dania mają dać się ugotować, a liczby na talerzu mają wyjść z jedzenia. Przepis jest zapisany w naszej bazie: z Książki kucharskiej albo dodany przez użytkownika. Aplikacja dobiera go według polubień i odrzuceń każdej wybranej osoby, a porcję liczy pod jej potwierdzone kalorie. Drugie śniadanie może być gotową kupną rzeczą, gdy osoba tak woli.

## North star

**S-02: użytkownik może wygenerować pierwszy dzień, którego śniadanie, drugie śniadanie, obiad i kolacja pochodzą z bazy przepisów, porcje są dostosowane do planu każdej wybranej osoby, a dobór uwzględnia to, co wybrane osoby lubią i czego nie lubią** — to jest najmniejszy przepływ, na którym widać, że dzień powstaje z zapisanych przepisów i z tego, co kto lubi.

> Gwiazda przewodnia to najmniejszy przekrój od końca do końca, którego dostarczenie pokazuje, że produkt rozwiązuje główny problem — tutaj plan złożony z przepisów z bazy, dobrany według tego, co kto lubi, z porcjami policzonymi pod osobę. Stoi tak wcześnie, jak pozwalają warunki wstępne, bo większa baza i reszta tygodnia mają sens tylko wtedy, gdy ten jeden dzień się zgadza.

## At a glance

| ID   | Change ID               | Outcome (user can …)                                                                 | Prerequisites | PRD refs            | Status   |
| ---- | ----------------------- | ------------------------------------------------------------------------------------ | ------------- | ------------------- | -------- |
| S-01 | first-stored-recipes    | użytkownik może zobaczyć w bazie po jednym przepisie na śniadanie, drugie śniadanie, obiad i kolację, wziętym z Książki kucharskiej na Wikibooks | —             | MS-01, MS-02, MS-04 | ready    |
| S-07 | person-recipe-likes     | użytkownik może przy przepisie oznaczyć dla osoby polubienie albo odrzucenie, tak że widać, co która osoba lubi | S-01          | MS-07               | proposed |
| S-06 | own-recipe              | użytkownik może dodać własny przepis do bazy, na jeden z czterech posiłków          | S-01          | MS-06               | proposed |
| S-02 | first-adapted-day       | użytkownik może wygenerować pierwszy dzień, którego śniadanie, drugie śniadanie, obiad i kolacja pochodzą z bazy przepisów, porcje są dostosowane do planu każdej wybranej osoby, a dobór uwzględnia to, co wybrane osoby lubią i czego nie lubią | S-01, S-07    | MS-03, MS-07        | proposed |
| S-03 | ready-made-second-meal  | użytkownik może dla osoby włączyć gotowca na drugie danie i zobaczyć w jej planie gotową kupną sałatkę, serek, skyr albo coś w tym stylu | S-02          | MS-05               | blocked  |
| S-04 | adapted-week            | użytkownik może dostać kolejne dni w ten sam sposób i listę zakupów zsumowaną z dostosowanych porcji | S-02, S-03    | MS-03               | proposed |
| S-05 | wikibooks-recipe-catalog | użytkownik może układać plany z większej bazy przepisów na te cztery posiłki, zebranej z Książki kucharskiej na Wikibooks | S-02          | MS-01, MS-02, MS-04 | proposed |

## Streams

Navigation aid — groups items that share a Prerequisites chain. Canonical ordering still lives in the dependency graph below; this table is the proposed reading order across parallel tracks.

| Stream | Theme            | Chain                                         | Note                                                                                          |
| ------ | ---------------- | --------------------------------------------- | --------------------------------------------------------------------------------------------- |
| A      | Plan z katalogu  | `S-01` → `S-07` → `S-02` → `S-03` → `S-04`    | Jakość talerza: najpierw przepis i ocena osoby, potem dzień, opcja gotowca i reszta planu. |
| B      | Własny przepis   | `S-06`                                        | Dołącza do strumienia A przy `S-01`. Dodanie przepisu nie blokuje pierwszego dnia.          |
| C      | Większa baza     | `S-05`                                        | Dołącza do strumienia A przy `S-02`. Zbieranie dalszych przepisów nie blokuje tygodnia.     |

## Baseline

What's already in place in the codebase as of 2026-11-22 (carried from the confirmed baseline, food table added 2026-11-22).
Foundations below assume these are present and do not re-scaffold them.

- **Frontend:** present — zgodnie z tech-stack.md: ekrany logowania, profilu i planu są widokami tej samej aplikacji
- **Backend / API:** present — zgodnie z tech-stack.md: konto może zapisać profil i wygenerować plan z listą zakupów; kcal dnia i pułap tłuszczu już liczy aplikacja
- **Data:** partial — baza i migracje są; tabela wartości na 100 g jest zebrana i plan jej jeszcze nie czyta; tabeli przepisów nie ma
- **Auth:** present — zgodnie z tech-stack.md: logowanie formularzem, jedno konto, tylko własne dane
- **Deploy / infra:** present — zgodnie z tech-stack.md: obraz kontenera i wdrożenie po złączeniu do głównej gałęzi; wagi modelu nie jadą na maszynę
- **Observability:** partial — publiczne health i info, zrzut sterty i wyłączenie zamknięte, bez śledzenia błędów
- **Plan memory:** present — trzy ostatnie plany są już w aplikacji. Ocena dania jest dziś jedna na całe konto i dotyczy nazwy dania z wygenerowanego planu. Ten kamień dodaje ocenę przepisu osobno dla każdej osoby.

## Foundations

Ten kamień nie dodaje fundamentu. Pierwsze przepisy wchodzą w S-01, bo użytkownik ma je zobaczyć, zanim powstanie z nich dzień. Ocena osoby wchodzi w S-07, bo dobór dnia ma już wiedzieć, co kto lubi. Dostosowanie porcji wchodzi w S-02, bo dopiero pierwszy dzień używa tabeli na 100 g.

## Slices

### S-01: Pierwsze przepisy czterech posiłków

- **Outcome:** użytkownik może zobaczyć w bazie po jednym przepisie na śniadanie, drugie śniadanie, obiad i kolację, wziętym z Książki kucharskiej na Wikibooks
- **Change ID:** first-stored-recipes
- **PRD refs:** MS-01, MS-02, MS-04
- **Prerequisites:** —
- **Parallel with:** —
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Bez jednego przepisu na każdy z czterech posiłków nie ma z czego ułożyć dnia. Przy przepisie zostaje link do strony, data pobrania i licencja CC BY-SA 3.0. Składniki są w gramach i mają nazwę z tabeli na 100 g.
- **Status:** ready

### S-07: Polubienie i odrzucenie przepisu

- **Outcome:** użytkownik może przy przepisie oznaczyć dla osoby polubienie albo odrzucenie, tak że widać, co która osoba lubi
- **Change ID:** person-recipe-likes
- **PRD refs:** MS-07
- **Prerequisites:** S-01
- **Parallel with:** S-06
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Dziś ocena jest jedna na całe konto i dotyczy nazwy dania z planu. Tu ocena należy do osoby i do przepisu w bazie, i od niej zależy dobór dnia.
- **Status:** proposed

### S-06: Własny przepis

- **Outcome:** użytkownik może dodać własny przepis do bazy, na jeden z czterech posiłków
- **Change ID:** own-recipe
- **PRD refs:** MS-06
- **Prerequisites:** S-01
- **Parallel with:** S-07, S-02, S-03, S-04, S-05
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Po pierwszym zapisie z Książki kucharskiej, bo własny przepis wchodzi do tej samej bazy. Tekst własny jest użytkownika. Przepis z książki niesie link, datę pobrania i licencję.
- **Status:** proposed

### S-02: Pierwszy dzień z bazy

- **Outcome:** użytkownik może wygenerować pierwszy dzień, którego śniadanie, drugie śniadanie, obiad i kolacja pochodzą z bazy przepisów, porcje są dostosowane do planu każdej wybranej osoby, a dobór uwzględnia to, co wybrane osoby lubią i czego nie lubią
- **Change ID:** first-adapted-day
- **PRD refs:** MS-03, MS-07
- **Prerequisites:** S-01, S-07
- **Parallel with:** S-06
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Najwcześniej widać, że dzień składa się z zapisanych dań, a gramy i kcal wychodzą z tabeli na 100 g pod potwierdzone kalorie i istniejący pułap tłuszczu. Danie jest wspólne, porcja osobna. Wykluczenie wybranej osoby usuwa produkt. Dobór bierze polubienia i odrzucenia wybranych osób oraz pamięć ostatnich obiadów.
- **Status:** proposed

### S-03: Gotowiec na drugie danie

- **Outcome:** użytkownik może dla osoby włączyć gotowca na drugie danie i zobaczyć w jej planie gotową kupną sałatkę, serek, skyr albo coś w tym stylu
- **Change ID:** ready-made-second-meal
- **PRD refs:** MS-05
- **Prerequisites:** S-02
- **Parallel with:** S-05, S-06
- **Blockers:** —
- **Unknowns:**
  - Drugie danie to drugie śniadanie. Gdy jedna osoba chce gotowca, a druga przygotowany przepis, czy to danie przestaje być wspólne? — Owner: user. Block: yes.
- **Risk:** Po pierwszym dniu, bo opcja zmienia tylko drugie śniadanie. Sałatka, serek i skyr są kupnym produktem, nie przepisem z krokami.
- **Status:** blocked

### S-04: Reszta planu i lista zakupów

- **Outcome:** użytkownik może dostać kolejne dni w ten sam sposób i listę zakupów zsumowaną z dostosowanych porcji
- **Change ID:** adapted-week
- **PRD refs:** MS-03
- **Prerequisites:** S-02, S-03
- **Parallel with:** S-05, S-06
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Po dniu i po opcji gotowca, bo tydzień powtarza ten sam dobór i ma już uwzględniać drugie danie z przepisu albo z półki. Lista zakupów bierze ilości z dostosowanych porcji.
- **Status:** proposed

### S-05: Większa baza z Książki kucharskiej

- **Outcome:** użytkownik może układać plany z większej bazy przepisów na te cztery posiłki, zebranej z Książki kucharskiej na Wikibooks
- **Change ID:** wikibooks-recipe-catalog
- **PRD refs:** MS-01, MS-02, MS-04
- **Prerequisites:** S-02
- **Parallel with:** S-03, S-04, S-06
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Po pierwszym dniu, żeby zbierać dalsze przepisy w kształcie, który już da się ułożyć na talerz. Źródło jest to samo co w S-01: Książka kucharska, CC BY-SA 3.0, składniki w gramach pod tabelę na 100 g.
- **Status:** proposed

## Backlog Handoff

| Roadmap ID | Change ID                | Suggested issue title                                              | Ready for `/10x-plan` | Notes |
| ---------- | ------------------------ | ------------------------------------------------------------------ | --------------------- | ----- |
| S-01       | first-stored-recipes     | Zapisać po jednym przepisie na cztery posiłki z Książki kucharskiej | yes                   | Run `/10x-plan first-stored-recipes` |
| S-07       | person-recipe-likes      | Oznaczać polubienie i odrzucenie przepisu dla osoby                | no                    | After S-01 |
| S-06       | own-recipe               | Dodać własny przepis do bazy                                       | no                    | After S-01 |
| S-02       | first-adapted-day        | Wygenerować pierwszy dzień z bazy, z uwzględnieniem polubień       | no                    | After S-01 and S-07 |
| S-03       | ready-made-second-meal   | Dodać opcję gotowca na drugie danie                               | no                    | Blocked on whether that meal stays shared |
| S-04       | adapted-week             | Dokończyć plan i zsumować listę zakupów z dostosowanych porcji     | no                    | After S-02 and S-03 |
| S-05       | wikibooks-recipe-catalog | Zebrać większą bazę przepisów z Książki kucharskiej                | no                    | After S-02 |

## Open Roadmap Questions

1. **Skąd biorą się wartości odżywcze na 100 g?** — Answered 2026-11-22. The file `src/main/resources/food/kcal-per-100g.csv` holds the table from https://kalkulatorkalorii.net/tabela-kalorii. Block: none.
2. **Skąd bierzemy przepisy?** — Answered 2026-11-22. The Polish Wikibooks cookbook at https://pl.wikibooks.org/wiki/Książka_kucharska, license CC BY-SA 3.0. A stored recipe keeps the page link, the fetch date, and the license. Ingredients are stored in grams under a name from the per-100 g table. Block: none.

## Parked

- **Ilości makro dnia wysyłane do modelu i przepis pisany przez model** — Why parked: M-3 porzucony 2026-11-22. Dania pochodzą z bazy, a porcje liczy aplikacja.
- **Odczyt tygodnia z modelu pod kątem tego, czy danie da się ugotować** — Why parked: dania planu nie będą już odpowiedzią modelu.
- **Pomiar większego modelu lokalnego (`qwen2.5:7b`, `qwen3:4b`)** — Why parked: ten kamień nie układa dań modelem.
- **Dziesięć dni kandydatów w jednej odpowiedzi, z odrzucaniem dni** — Why parked: dzień powstaje z zapisanych przepisów i policzonych porcji.
- **Cel białka jako warunek, który odrzuca dzień** — Why parked: dostosowanie trzyma się potwierdzonych kalorii i istniejącego pułapu tłuszczu.
- **Przechowywanie plików PDF planu i listy zakupów** — Why parked: pliki dalej są tylko generowane i pobierane.

## Milestone History

- **M-1: First downloadable next-day plan** — closed 2026-10-07. S-03 delivered in `calorie-formula` (F-03). S-06 delivered by stored profile (S-02, F-03) plus generate on `/plan` (S-04); no separate `confirm-daily-calories` or `return-visit-plan` change.
- **M-2: Plany, które nie powtarzają ostatnich dań** (`varied-week-plans`) — abandoned 2026-11-16. Użytkownik otwiera kamień o ilości makro dnia i proporcjach liczonych w aplikacji. Dwa przekroje M-2 nie są done; pamięć trzech planów i oceny dań zostają w kodzie.
- **M-3: Dzień z ilości makro, przepis z modelu, proporcje z aplikacji** (`weighed-day-plan`) — abandoned 2026-11-22. Użytkownik zastępuje przepis z modelu bazą przepisów z internetu. Żaden przekrój M-3 nie jest done. Tabela żywności na 100 g zostaje.

## Done

Wpisy poniżej należą do zamkniętego M-1. Ich identyfikatory nie są identyfikatorami otwartego kamienia.

- **F-01: (foundation) the application has a configured database. Product tables are not created here.** — Archived 2026-10-01 → `context/archive/2026-09-29-database-configured/`. Lesson: —.
- **F-02: (foundation) Spring Security can require a signed-in account before that account's data is shown, and the sign-in tables exist for one account with no roles. Public heap dump and shutdown cannot expose account data.** — Archived 2026-10-02 → `context/archive/2026-10-01-spring-security-sign-in/`. Lesson: —.
- **S-01: user can create an account from the login window and land signed in, without a second login step, and can log in again from that window on a later visit.** — Archived 2026-10-02 → `context/archive/2026-10-02-register-and-sign-in/`. Lesson: —.
- **S-02: user can enter age, height, weight, sex, goal, activity level, preferred products, and excluded products on one screen, in one form and one request, and the profile is stored in the table `user_profile` on the account and shown again on a later visit.** — Archived 2026-10-05 → `context/archive/2026-10-02-save-user-profile/`. Lesson: —.
- **F-03: (foundation) a daily calorie number on the profile is BMR times one activity level, then −250 (`LOSE_WEIGHT`), 0 (`MAINTAIN`), or +250 (`GAIN`), rounded half-up to a whole kilocalorie. Weight is in kilograms, height in centimetres, age in years. Sex selects the BMR line. Male: BMR = (10 × weight) + (6.25 × height) − (5 × age) + 5. Female: BMR = (10 × weight) + (6.25 × height) − (5 × age) − 161. Activity is `SEDENTARY` ×1.2 (most of the day sitting), `LIGHT` ×1.375 (walking or light effort on most days), `MODERATE` ×1.55 (exercise several days a week), or `HIGH` ×1.725 (hard training or physical work on most days). `/profile` shows an editable Cel kaloryczny. The first body save stores the formula as `confirmed_calories`. The client edits that number on `POST /profile/calories` (only the calorie amount). Recalculate is `POST /profile/recalculate`.** — Archived 2026-10-05 → `context/archive/2026-10-05-calorie-formula/`. Lesson: —.
- **S-04: user can open `/plan`, call Ollama Cloud, and receive a next-day diet plus shopping list as two PDF fields (`dietPdf`, `shoppingListPdf`) on `POST /plan/generate`. The text and files are not stored.** — Archived 2026-10-06 → `context/archive/2026-10-05-generate-diet-with-ollama/`. Lesson: —.
- **S-05: user can download the diet and the shopping list as two PDF files that are not stored.** — Archived 2026-10-06 → `context/archive/2026-10-05-generate-diet-with-ollama/`. Lesson: —.
- **S-03: user can see the number from the F-03 formula, computed from the profile saved in S-02, after the goal adjustment, then accept or edit it. The confirmed number stays on the account in `user_profile`. Preferred and excluded products do not change the number.** — Closed 2026-10-07 (delivered in F-03) → `context/archive/2026-10-05-calorie-formula/`. Lesson: —.
- **S-06: user can return later and generate another next-day plan and shopping list without entering age, height, weight, sex, goal, activity, or preferences again.** — Closed 2026-10-07 (delivered by stored profile + S-04; no separate change) → `context/archive/2026-10-05-generate-diet-with-ollama/`. Lesson: —.
