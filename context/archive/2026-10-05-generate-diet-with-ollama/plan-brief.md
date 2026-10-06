# Generowanie diety z Ollama — Plan Brief

> Full plan: `context/changes/generate-diet-with-ollama/plan.md`

## What & Why

Po zapisanym profilu i celu kalorycznym użytkownik ma dostać jadłospis na jutro i listę zakupów jako dwa PDF, bez zapisu plików (US-01, FR-006). Model (Spring AI → Ollama Cloud) układa cztery posiłki z preferencji; ten wycinek pokazuje też pobieranie, więc osobne S-05 (ZIP/PDF) odpada.

## Starting Point

`/profile` trzyma ciało, produkty i `confirmed_calories`. Nie ma Spring AI, Ollamy, PDF ani akcji „generuj”. Nagłówek ma tylko Profil. Ollama nie może stanąć na Fly 1 GB.

## Desired End State

Zalogowany użytkownik otwiera `/plan`, klika Generuj plan i po sukcesie ma **Pobierz plan** oraz **Pobierz listę zakupów**. Jedno `POST /plan/generate` zwraca JSON z `dietPdf` i `shoppingListPdf`. Bez profilu, celu albo przy martwej chmurze: HTTP 200 i alert, zero plików.

## Key Decisions Made

| Decision | Choice | Why |
| --- | --- | --- |
| Zakres | Dieta + lista + dwa PDF w tym wycinku | Decyzja „od razu PDF”; S-05 wchłonięte |
| Posiłki | Śniadanie, drugie śniadanie, obiad, kolacja | Korekta względem trzech posiłków z PRD |
| Wejście promptu | Preferowane, wykluczone, kcal | Żadnych danych ciała ani loginu do chmury |
| Język | Prompt EN, treść PL | Lepszy prompt, UI po polsku |
| Dostawa plików | JSON dwa pola + dwa przyciski (bloby) | Zamiast ZIP i zamiast sesji |
| Ollama | Cloud `https://ollama.com` + `OLLAMA_API_KEY` | Darmowe API; nic na Machine |
| Awarie | HTTP 200 + alert / JSON `error` | Wzorzec profilu; bez 500/503 |
| Timeout | 120 s (Fly może uciąć ~60 s ciszy) | Synchronicznie; joby poza MVP |
| Testy | Mock collaboratora; CI bez Ollamy | Brak WireMock w stacku |
| Miejsce | Osobna `/plan` + link w nagłówku | Nie mieszać z formularzem profilu |
| Predykat | Wiersz profilu i niepusty `confirmed_calories` | Model bez celu nie ma liczby |

*(ZIP i trzy posiłki były wybrane, potem zastąpione.)*

## Scope

**In scope:** pakiet `plan`, Spring AI + Ollama Cloud, PDFBox, GET `/plan`, POST JSON, JS pobrania, alerty, linki, testy, `AGENTS.md` / roadmapa / tech-stack.

**Out of scope:** ZIP, e-mail, podgląd HTML diety, persistencja, Ollama na Fly, joby, żywa Ollama w CI, Swagger, retry, odrzucanie po sumie kcal.

## Architecture / Approach

`PlanService` składa profil + kcal i woła `DietGenerator` (`ChatClient`). Structured `DietPlan` → dwa PDF w pamięci → `PlanFilesDto`. Kontroler nie rusza `ProfileController`. Generator mockowany w unit/WebMvcTest.

## Phases at a Glance

| Phase | What it delivers | Key risk |
| --- | --- | --- |
| 1. Generowanie | Spring AI, `DietPlan`, mocki | Autoconfig psuje `ApplicationTest` bez klucza |
| 2. Dwa PDF w JSON | PDFBox + DTO Base64 | Polskie znaki / zły kształt JSON |
| 3. Ekran `/plan` | GET, POST, przyciski, CSRF | `fetch` bez CSRF albo wyciek innego loginu |
| 4. Dokumentacja | AGENTS, roadmapa, infra | Kolejny agent planuje ZIP albo localhost |

**Prerequisites:** S-02 i zapis `confirmed_calories` (F-03) są w kodzie; klucz Ollama Cloud do ręcznego testu fazy 3.
**Estimated effort:** ~4 sesje; faza 1 (klient Cloud + start bez klucza) najszersza.

## Open Risks & Assumptions

- Idle timeout Fly ~60 s vs 120 s aplikacji: na produkcji wolny model = ten sam alert.
- Spring AI 2.x może wymagać ręcznego Beera; implementator weryfikuje aktualny starter.
- Nazwa modelu Cloud jest konfiguracją (`OLLAMA_CHAT_MODEL`), nie twardym id z tego briefu.
- Base64 w przeglądarce ginie po odświeżeniu — zgodne z „nie zapisujemy planu”.

## Success Criteria (Summary)

- Z profilem i celem: jedno generowanie, dwa osobne PDF po polsku, cztery posiłki, lista zakupów.
- Bez danych albo bez chmury: 200 i komunikat, nic w bazie.
- `.\gradlew.bat test` zielone bez sieci do ollama.com.
