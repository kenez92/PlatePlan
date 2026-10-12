# Plan na siedem dni — Plan Brief

> Full plan: `context/changes/seven-day-plan/plan.md`
> Frame brief: `context/changes/seven-day-plan/frame.md`

## What & Why

Jeden generate dla jednej osoby, która już ma profil, ma dać dietę i listę zakupów na siedem dni, a nie na jeden następny dzień. Każdy dzień trzyma się dziennej liczby kcal. W PDF diety dzień zaczyna własną stronę. Kolejne osoby są następną zmianą.

## Starting Point

Dziś `DietPlan` to cztery posiłki i jedna lista na jutro. Model jest wołany raz. PDF łamie strony tylko gdy wiersz się nie mieści. Ekran, plik `dieta-na-jutro.pdf` i mail mówią „na jutro”. Sukces HTTP to dwa pola PDF. Plików nie zapisujemy.

## Desired End State

Przycisk na `/plan` zwraca `dieta-na-tydzien.pdf` i `lista-zakupow.pdf`. Dieta ma siedem dni od jutra (Europe/Warsaw), tytuł `Dzień 1 · 14.10.2026` i dalej. Każdy dzień zaczyna stronę. Lista jest jedna, z ilościami zsumowanymi na tydzień. Dzień poza ±10% celu, powtórzona nazwa śniadania, obiadu lub kolacji albo inna liczba dni dają `UNAVAILABLE` i zero plików. Drugie śniadanie może wrócić, także siedem razy.

## Key Decisions Made

| Decision | Choice | Why (1 sentence) | Source |
| --- | --- | --- | --- |
| Kto | Jedna osoba z profilem | Rodzina jest osobną, późniejszą zmianą | Frame |
| Które dni | Jutro i sześć kolejnych, Europe/Warsaw | Przedłuża datę, którą PDF już liczy jako jutro | Plan |
| Kalorie | Domknięte ±10%, half-up; poza pasmem `UNAVAILABLE` | „Mniej więcej” ma być sprawdzone na kcal posiłków | Plan |
| Strona PDF | Dzień zaczyna stronę, nadmiar leci dalej | Dni się nie mieszają, a składniki zostają w całości | Plan |
| Lista | Jedna lista, sumuje model | To lista „co kupić”, a ilości są swobodnym tekstem | Plan |
| Model | Jedno wywołanie, bez drugiej próby | Dwa PDF-y powstają albo wcale, w obecnym requeście | Plan |
| Nazwy dań | Unikalne śniadanie, obiad i kolacja. Drugie śniadanie bez limitu w Javie | Prompt prosi, żeby nie wracało bez przerwy. Siedem razy ten sam skyr i tak schodzi | Plan |
| Tytuł dnia | `Dzień N · d.MM.yyyy` | Widać kolejność i datę | Plan |
| Nazwy plików | `dieta-na-tydzien.pdf`, lista bez zmiany nazwy | Pobrany plik nazywa tydzień; mail mówi o 7 dniach | Plan |

Pasmo: cel 2200 → 1980–2420 włącznie. Cel 1999 → 1799–2199 włącznie.

## Scope

**In scope:** `DayMeals` i `WeekPlan`, walidator, prompt, `PlanResult`, PDF, napisy `/plan`, mail, FR-006, `AGENTS.md`, testy tych ścieżek.

**Out of scope:** kolejne osoby, zapis plików, siedem wywołań, retry, parsowanie `150 g` w Javie, ściskanie do dokładnie siedmiu stron, tydzień kalendarzowy, zmiana formuły kcal.

## Architecture / Approach

Model zwraca siedem dni i jedną listę. Walidator puszcza tydzień albo generator zwraca `unavailable()`. Writer sam nakłada daty i łamie strony. Controller i mail nadal wysyłają te same dwa bajty.

## Phases at a Glance

| Phase | What it delivers | Key risk |
| --- | --- | --- |
| 1. Kształt tygodnia i odmowa | Typy i walidator z granicami pasma | Źle zaokrąglona delta 10% |
| 2. Jedno wywołanie modelu | Prompt, `WeekPlan` w `PlanResult`, suite zielone | Większy JSON i timeout 120 s |
| 3. PDF diety i listy | Tytuły dni i start strony | `PageAcc` nie umie dziś twardego łamania |
| 4. Napisy, mail i dokumenty | `dieta-na-tydzien.pdf`, FR-006, AGENTS | Stare „na jutro” zostanie w teście albo w mailu |

**Prerequisites:** profil z `confirmed_calories` już istnieje. Ta zmiana nie dodaje tabeli.
**Estimated effort:** około 2–3 sesji na 4 fazy.

## Open Risks & Assumptions

- Jedno większe wywołanie częściej wpadnie w timeout 120 s. Drugiej próby nie ma.
- Pasmo ufa kcal wpisanym przez model, nie przelicza składników.
- Czy ilości na liście są naprawdę zsumowane, widać dopiero w tekście modelu. Java tego nie liczy.
- Drugie śniadanie może być tym samym produktem przez siedem dni. Java tego nie liczy. Prompt tylko prosi, żeby nie wracało bez przerwy.

## Success Criteria (Summary)

- Generate zwraca dietę na siedem dni od jutra i jedną listę zakupów albo samo `UNAVAILABLE`.
- Każdy dzień jest w ±10% celu. Śniadanie, obiad i kolacja nie powtarzają nazwy. Drugie śniadanie może wrócić. Dzień zaczyna stronę PDF.
- Plik diety nazywa się `dieta-na-tydzien.pdf`. Mail i ekran mówią o 7 dniach. Plików nie zapisujemy.
