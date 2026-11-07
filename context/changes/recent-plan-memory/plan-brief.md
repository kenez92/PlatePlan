# Trzy ostatnie plany w prompcie — Plan Brief

> Full plan: `context/changes/recent-plan-memory/plan.md`
> Research: `context/changes/recent-plan-memory/research.md`

## What & Why

Kolejne generowanie ma przestać wracać do obiadów z trzech ostatnich planów, które użytkownik naprawdę dostał. Dziś model nie widzi poprzedniego tygodnia, więc ten sam schab może wrócić jako obiad w następnym pliku.

## Starting Point

`POST /plan/generate` woła model raz, składa prompt z osób i list produktów i oddaje dwa PDF-y, których nie zapisuje. W tygodniu 21 nazw śniadania, obiadu i kolacji musi być różnych. Drugie śniadanie może się powtarzać. W bazie nie ma tabeli planu. PRD zabrania przechowywania planu i listy zakupów.

## Desired End State

Po udanym generowaniu konto pamięta siedem nazw obiadów. Zostają trzy takie plany, najstarszy wypada. Następny plan, także dla innego składu tego samego konta, nie może użyć tych nazw jako obiadu. Jeśli model tak zrobi, użytkownik dostaje `UNAVAILABLE` i nie dostaje PDF-ów. Śniadanie, drugie śniadanie i kolacja mogą wrócić. W jednym tygodniu dotychczasowa unikalność 21 nazw zostaje. Pliki i lista zakupów nadal nie są zapisywane. Gdy sam zapis nazw się nie uda, PDF-y i tak wracają.

Przykład: obiady A „Rosół”, B „Schab”, C „Zapiekanka”. Timeout nic nie kasuje. Udany plan D z obiadem „Kurczak” zostawia B, C i D. Obiad „ schab ” nie oddaje plików. Kolacja „Schab” i śniadanie „Schab” nie są z tego powodu odrzucone.

## Key Decisions Made

| Decision | Choice | Why (1 sentence) | Source |
| --- | --- | --- | --- |
| Co zapisujemy | Siedem nazw obiadu z udanego planu | Między planami mają się nie powtarzać obiady. | Plan |
| Czego nie zapisujemy | PDF, lista zakupów, przygotowanie, śniadanie, drugie śniadanie, kolacja | PRD zabrania planu i listy. Reszta posiłków może wrócić w następnym tygodniu. | Plan |
| Powtórka obiadu | Zakaz w prompcie i odmowa bez PDF, jedno wołanie | Samo zdanie w prompcie model już dziś potrafi zignorować. | Plan |
| Klucz nazwy | `strip` i małe litery `Locale.ROOT` | Tak Java już porównuje nazwy w tygodniu. | Research |
| Czyja pamięć | Jedna trójka planów na login | Plan solo ma blokować te obiady także przy rodzinnym generate. | Plan |
| Które próby | Tylko odpowiedź, która ma oba PDF-y | Timeout i odmowa nie wypychają planu, który użytkownik dostał. | Plan |
| Czwarty plan | Wypada najmniejszy `plan_id` | „Ostatnie trzy” to trzy udane zapisy, w kolejności wstawienia. | Plan |
| Błąd insertu | PDF zostają, bez nowego pola JSON | Awaria historii nie zabiera jadłospisu, tak jak awaria maila. | Plan |

## Scope

**In scope:** changelog `007`, odczyt i zapis nazw, dopisek promptu, odmowa powtórki, podłączenie `POST /plan/generate`, wyjątek w `prd.md`, `AGENTS.md` i ryzyku #5 w `test-plan.md`.

**Out of scope:** ekran planu, nowe pole JSON, druga próba modelu, osobna pamięć składu, oceny lubię / nie lubię, zakaz między planami dla śniadania i kolacji, edycja changelogów `001`–`006`.

## Architecture / Approach

`RecentPlanMemory` czyta i zapisuje dwie tabele: nagłówek planu po `login` oraz siedem nazw obiadu z kasowaniem kaskadowym. `PlanService` podaje je do `DietGenerator` przed modelem. Kontroler woła zapis dopiero po obu tablicach bajtów i łapie błąd insertu tak, żeby pliki zostały. Ollama nie siedzi w transakcji tego zapisu.

## Phases at a Glance

| Phase | What it delivers | Key risk |
| --- | --- | --- |
| 1. Zapis trzech planów | Trzy plany na login, same obiady | Zły changelog albo zapis śniadania i kolacji |
| 2. Prompt i odmowa | Model widzi obiady, powtórka obiadu nie daje PDF | Odmowa kolacji albo drugie wołanie modelu |
| 3. Generate i dokumenty | Zapis po PDF, wyjątek w PRD | Insert w tym samym `try` co PDF zamieni błąd historii na `UNAVAILABLE` |

**Prerequisites:** badania w `research.md`. Testy nie wołają Ollamy. Migracja `007` wejdzie przy następnym udanym starcie Liquibase.
**Estimated effort:** około dwóch sesji na trzy fazy.

## Open Risks & Assumptions

- Użytkownik, któremu model powtórzy obiad, klika generuj jeszcze raz. Drugiej próby w tym żądaniu nie ma.
- Nieudany start Liquibase pomija `007`. Odczyt pamięci kończy się wtedy `UNAVAILABLE`, a błąd insertu po gotowych PDF-ach zostawia pliki.
- Konta sprzed tej zmiany zaczynają z pustą pamięcią. Starych planów nie da się odtworzyć, bo nie były zapisane.

## Success Criteria (Summary)

- Po trzech udanych planach i czwartym najstarsze nazwy wypadają, a nazwy innego loginu zostają.
- Tydzień, który powtarza zapamiętany obiad, nie zwraca PDF-ów. Śniadanie i kolacja z poprzedniego planu mogą wrócić.
- Błąd zapisu nazw nie zabiera PDF-ów, a PRD nadal zabrania zapisu plików i listy zakupów.
