# Generowanie diety z Ollama — plan wdrożenia

## Overview

Zalogowany użytkownik z zapisanym profilem i `confirmed_calories` otwiera `/plan`, woła Ollama Cloud (Spring AI) i dostaje dwa PDF: jadłospis na jutro (śniadanie, drugie śniadanie, obiad, kolacja) oraz listę zakupów. Jedno `POST /plan/generate` zwraca oba pliki w JSON (`dietPdf`, `shoppingListPdf`); strona pokazuje przyciski **Pobierz plan** i **Pobierz listę zakupów**. Tekst i bajty nie są zapisywane. Ten wycinek pokrywa wynik użytkownika z S-04 i S-05 (dwa PDF do pobrania, bez osobnej zmiany `download-next-day-plan`).

## Current State Analysis

- Aplikacja kończy się na `/profile`: ciało, listy produktów i cel kaloryczny (`ProfileController.java:22-87`). Nie ma przycisku generowania, pakietu planu, Spring AI ani PDF (`build.gradle.kts:20-34`).
- Wejście do modelu już leży w `user_profile`: `preferred_products`, `excluded_products`, `confirmed_calories` (`UserProfile.java:50-58`). Predykat „najpierw profil” istnieje przy zapisie kalorii (`ConfirmedCaloriesResult.noProfile()`, `profile.html:16`).
- Błędy oczekiwane to rekord wyniku i HTML 200 z alertem, nie 500 (`ProfileResult`, `ConfirmedCaloriesResult`, `ProfileController.java:51-61`). Testy slice to `@WebMvcTest` z mockiem serwisu (`ProfileControllerTest.java:49-66`).
- `tech-stack.md` deklaruje Spring AI i lokalną Ollamę, żadna nie jest zależnością. `infrastructure.md:101` zabrania Ollamy na Machine 1 GB. Nagłówek ma tylko link Profil (`chrome.html:19`).
- ZIP był rozważany i **odrzucony**: zastępuje go JSON z dwoma polami PDF. Trzy posiłki były rozważane i **zastąpione** czterema (drugie śniadanie).

## Desired End State

Na `/plan` zalogowany użytkownik z wierszem `user_profile` i niepustym `confirmed_calories` klika generowanie, czeka aż do 120 s i widzi dwa przyciski pobrania. Każdy zapisuje jeden PDF po polsku. Brak profilu, brak celu, timeout, brak klucza albo błąd modelu zostawia stronę z alertem (HTTP 200), bez plików i bez zapisu. `.\gradlew.bat test` jest zielone, w tym `ApplicationTest`, bez żywej Ollamy.

### Key Discoveries:

- Pakiet po tym, czym jest rzecz: `plan`, nie dopinanie do `ProfileController` (`AGENTS.md` Packages; `ProfileController.java:22-24` zostaje przy ciele i kcal).
- Ollama Cloud: `https://ollama.com` + `Authorization: Bearer` z `OLLAMA_API_KEY` ([dokumentacja Ollama](https://docs.ollama.com/api/authentication)). Starter Spring AI Ollama nie dokumentuje klucza — potrzebny `RestClient` z nagłówkiem Bearer albo równoważny hook Spring AI 2.x.
- Fly zamyka **bezczynne** połączenie HTTP po ~60 s. Limit aplikacji to 120 s; po ciszy >60 s na Fly użytkownik dostaje ten sam alert niedostępności. Bez jobów i SSE (poza MVP).
- `byte[]` w JSON to Base64 (Jackson). Dwa przyciski budują bloby w przeglądarce; odświeżenie kasuje pliki (akceptowane: nic nie wolno persistować).
- Do modelu idą **tylko** preferowane, wykluczone i liczba kcal. Wiek, wzrost, waga, płeć, cel, aktywność i login nie wychodzą z procesu do Ollamy (`prd.md` Business Logic: wejście do planu to produkty i przyjęta liczba).
- `ApplicationTest` podnosi cały kontekst (`ApplicationTest.java:12-16`). Autoconfiguracja Spring AI nie może wymagać żywej chmury ani obowiązkowego klucza przy starcie.

## What We're NOT Doing

- ZIP, e-mail zamiast PDF, podgląd jadłospisu jako HTML (treść jest w PDF).
- Zapis diety, listy, PDF ani Base64 w bazie, na dysku albo w sesji serwera.
- Ollama na maszynie Fly; lokalny serwer Ollama jako wymóg.
- Joby w tle, SSE, WebSocket, WireMock, Testcontainers, żywa Ollama w CI.
- Wysyłanie danych ciała (wiek, wzrost, waga, płeć, cel, aktywność) albo loginu do modelu.
- Osobna zmiana `download-next-day-plan` dla tych dwóch plików — wynik S-05 wchodzi tutaj.
- Publiczny `/plan`, nowa ścieżka `permitAll`, throttling, płatności, wiele osób.
- Odrzucanie diety, gdy suma kcal posiłków mija się z celem (prompt ma cel; PDF i tak wraca).
- Swagger / opis API poza tym jednym POST-em pod ekranem.

## Implementation Approach

Cztery fazy. Faza 1 dodaje Spring AI i collaboratora z mockiem: kontrakt czterech posiłków + listy. Faza 2 robi dwa PDF (PDFBox, czcionka z polskimi glifami) i DTO JSON. Faza 3 to GET `/plan`, POST JSON, JS przycisków, alerty, linki. Faza 4 to dokumenty. Nowe typy w `com.kenez92.plateplan.plan.{controller,controller.dto,model,service}`. PDF i klient modelu to `@Service` z wstrzyknięciem; `java.util.zip` odpada. Testy: `should…`, `final`, `usingRecursiveComparison` dla obiektów, HTML i JSON jako asercje łańcuchów/pól odpowiedzi.

## Critical Implementation Details

- **Start bez chmury** — brak `OLLAMA_API_KEY` nie zatrzymuje Boota (w przeciwieństwie do `DATABASE_URL`). `ApplicationTest` musi przejść bez sekretu. Generowanie wtedy zwraca ten sam wynik `unavailable` co timeout.
- **CSRF przy `fetch`** — POST JSON nie ma ukrytego pola formularza. Token z cookie/meta, nagłówek jak w Spring (`X-CSRF-TOKEN`); bez tokenu 403, jak `ProfileControllerTest.java:276-286`.
- **Bearer** — jeśli aktualny starter nie ma `spring.ai.ollama.api-key`, zbuduj `OllamaApi` / `RestClient` z `Authorization: Bearer ${OLLAMA_API_KEY}` i `base-url` `https://ollama.com`. Nie loguj klucza ani treści promptu.
- **Fly 60 s** — `ChatClient`/HTTP read timeout 120 s. Nie dodawaj keep-alive ani streamingu ZIP; po ucięciu proxy ten sam alert co przy awarii modelu.

## Phase 1: Generowanie

### Overview

Spring AI jest w stacku i w Gradle. Collaborator woła model i zwraca `DietPlan` (cztery posiłki + lista zakupów) albo wynik niedostępności. Serwis odmawia bez wiersza profilu albo bez `confirmed_calories`. Testy nie wołają chmury.

### Changes Required:

#### 1. Stack: Spring AI

**File**: `context/foundation/tech-stack.md`, `build.gradle.kts`

**Intent**: Żadna biblioteka nie wchodzi do Gradle, dopóki nie jest w tech-stacku. Spring AI 2.x (BOM zgodny z Boot 4.1.1) i starter modelu Ollama są jedyną drogą do chmury.

**Contract**: W `tech-stack.md` zastąp „local Ollama model; neither is a dependency yet” faktycznym starterem i BOM (wersja PIN-owana, np. linia 2.0.x zgodna z Boot 4.1.1). W `build.gradle.kts` ten sam BOM + `spring-ai-starter-model-ollama` (albo aktualny artifactId z dokumentacji 2.x). PDFBox jeszcze nie. `pull-model-strategy` never — chmura nie ściąga wag na Machine.

#### 2. Konfiguracja klienta

**File**: `src/main/resources/application.properties`, nowy `@Configuration` w `com.kenez92.plateplan.plan` (tylko jeśli starter nie wstawia Beera sam)

**Intent**: Aplikacja mówi z Ollama Cloud, nie z localhost i nie z Fly GPU.

**Contract**: `spring.ai.ollama.base-url` domyślnie `https://ollama.com`. Model z `OLLAMA_CHAT_MODEL` (z fallbackiem zapisanym w properties — aktualna nazwa modelu chat z katalogu Ollama Cloud, nie `gpt-oss:120b` jako twardy wymóg). Timeout odczytu 120 s. Klucz wyłącznie z `OLLAMA_API_KEY` (brak w git). Pusty klucz: start OK, wywołanie → `unavailable`.

#### 3. Model diety

**File**: `src/main/java/com/kenez92/plateplan/plan/model/` (nowe: `Meal`, `DietPlan`, `PlanResult`)

**Intent**: Reszta faz zależy od jednego kształtu dnia, nie od prozy.

**Contract**: `Meal` ma nazwę dania, składniki i kcal (int). `DietPlan` ma dokładnie `breakfast`, `secondBreakfast`, `lunch`, `dinner` oraz `shoppingList` (`List<String>`). `PlanResult` jak kalorie: sukces z `DietPlan`, `unavailable()`, `noProfile()`, `noCalories()`. Oczekiwane ścieżki to wartości, nie wyjątki. Structured output Spring AI (bean/JSON schema) na ten rekord; treść po polsku.

#### 4. Prompt i collaborator

**File**: `src/main/java/com/kenez92/plateplan/plan/service/` (np. `DietGenerator`, `PlanService`)

**Intent**: Jeden serwis orkiestruje profil + kcal; generator jest jedynym miejscem `ChatClient`, żeby testy mogły go zastąpić.

**Contract**: Prompt **po angielsku**. Instrukcja: odpowiedź **po polsku**, cztery posiłki, lista zakupów, trzymaj się celu kcal, **użyj** preferowanych, **nie używaj** wykluczonych. Argumenty generatora: `int dailyCalories`, `List<String> preferred`, `List<String> excluded` — nic więcej. Timeout/parse/HTTP/brak klucza → `PlanResult.unavailable()`. `PlanService` czyta login przez istniejące `ProfileService` / `UserProfileRepository` + `ConfirmedCaloriesService`: brak wiersza → `noProfile()`; `confirmed_calories == null` → `noCalories()`; baza down → `unavailable()`. Nie logować list produktów ani kcal.

#### 5. Testy jednostkowe generatora i serwisu

**File**: `src/test/java/com/kenez92/plateplan/plan/service/`

**Intent**: CI bez Ollamy. Prompt i predykaty są w teście, nie w ręcznym klikaniu.

**Contract**: Mock `ChatClient` albo mock `DietGenerator`. `PlanServiceTest`: `should…` dla sukcesu (rekurencyjne porównanie całego `DietPlan`), braku profilu, braku kcal, bazy down, generatora `unavailable`. Osobny test promptu: zserializowany prompt zawiera kalorie i nazwy produktów, **nie zawiera** wieku, wzrostu, wagi, płci, loginu. Żadnych `private static` stałych w teście.

### Success Criteria:

#### Automated Verification:

- Testy `plan.service` przechodzą: `.\gradlew.bat test --tests com.kenez92.plateplan.plan.service.*`
- Cały zestaw przechodzi, w tym `ApplicationTest`: `.\gradlew.bat test`

#### Manual Verification:

- W `tech-stack.md` i `build.gradle.kts` widać ten sam starter Spring AI; w properties jest `https://ollama.com` i timeout 120 s, bez sekretu w pliku

**Implementation Note**: After completing this phase and all automated verification passes, pause here for manual confirmation from the human that the manual testing was successful before proceeding to the next phase. Phase blocks use plain bullets — the corresponding `- [ ]` checkboxes for these items live in the `## Progress` section at the bottom of the plan.

---

## Phase 2: Dwa PDF w JSON

### Overview

Z `DietPlan` powstają dwa PDF w pamięci i DTO z dwoma polami `byte[]`. Jackson oddaje Base64. Nic nie idzie na dysk.

### Changes Required:

#### 1. PDFBox w stacku

**File**: `context/foundation/tech-stack.md`, `build.gradle.kts`

**Intent**: Apache PDFBox (licencja Apache 2.0) jest jedyną biblioteką PDF; ZIP nie wraca.

**Contract**: Dopisz PDFBox do tech-stacku, potem `org.apache.pdfbox:pdfbox` w Gradle. Czcionka z polskimi glifami w `src/main/resources` (np. Liberation/Noto, licencja pozwalająca na osadzenie). Helvetica odpada.

#### 2. Renderer

**File**: `src/main/java/com/kenez92/plateplan/plan/service/` (np. `PlanPdfWriter`)

**Intent**: S-05 ma dwa pliki; ten serwis jest jedynym miejscem bajtów PDF.

**Contract**: Dwie publiczne metody albo jedna para: jadłospis i lista z tego samego `DietPlan`. Jadłospis: data **jutra** w `Europe/Warsaw`, cztery posiłki z kcal, cel dnia. Lista: pozycje z `shoppingList`. Polskie znaki czytelne po `PDFTextStripper`. Brak loginu w treści (plik może trafić poza konto). Wyjątek IO → warstwa wyżej zamienia na `unavailable`, nie 500.

#### 3. DTO odpowiedzi

**File**: `src/main/java/com/kenez92/plateplan/plan/controller.dto/PlanFilesDto.java` (albo `plan.model` jeśli to wynik, nie formularz — formularza nie ma; DTO wychodzące przy kontrolerze)

**Intent**: Kontrakt, od którego zależy JS w fazie 3.

**Contract**: Sukces:

```json
{"dietPdf":"<base64>","shoppingListPdf":"<base64>"}
```

Porażka (nadal JSON, HTTP 200): jedno pole `error` o wartościach `UNAVAILABLE`, `PROFILE_REQUIRED`, `CALORIES_REQUIRED`. Nigdy jednocześnie `error` i PDF. `toString()` bez bajtów.

#### 4. Testy PDF i JSON

**File**: `src/test/java/com/kenez92/plateplan/plan/`

**Intent**: Polskie glify i kształt JSON są w CI.

**Contract**: Zbuduj `DietPlan` z „Żółć” / „jądro”; po zapisie PDF `PDFTextStripper` widzi te słowa. `ObjectMapper` sukcesu ma dwa pola Base64 dekodowalne do `%PDF`. Porażka serializuje tylko `error`. Porównanie rekordu wyniku: `usingRecursiveComparison`.

### Success Criteria:

#### Automated Verification:

- Testy PDF i DTO przechodzą: `.\gradlew.bat test --tests com.kenez92.plateplan.plan.*`
- `.\gradlew.bat test`

#### Manual Verification:

- Otwórz dwa PDF z testowego `DietPlan` (zrzut z testu albo krótki main deweloperski): polskie znaki, cztery nagłówki posiłków, lista zakupów; plików nie ma w `src/` ani w git

**Implementation Note**: After completing this phase and all automated verification passes, pause here for manual confirmation from the human that the manual testing was successful before proceeding to the next phase. Phase blocks use plain bullets — the corresponding `- [ ]` checkboxes for these items live in the `## Progress` section at the bottom of the plan.

---

## Phase 3: Ekran `/plan`

### Overview

GET pokazuje stronę. POST zwraca JSON. Po sukcesie dwa przyciski pobierają bloby. Alerty jak na profilu. Link w nagłówku i na `/profile`.

### Changes Required:

#### 1. Kontroler

**File**: `src/main/java/com/kenez92/plateplan/plan/controller/PlanController.java`

**Intent**: Jedno API pod widokiem; Security już wymaga logowania (`SecurityConfiguration.java:40-44`) — bez nowej ścieżki publicznej.

**Contract**: `GET /plan` → widok `plan`, 200 HTML. `POST /plan/generate` → `application/json`, 200 zawsze przy oczekiwanych wynikach. Login wyłącznie `Principal.getName()`. CSRF wymagany. Sukces: `PlanFilesDto` z dwoma `byte[]`. `noProfile` → `error: PROFILE_REQUIRED`; `noCalories` → `CALORIES_REQUIRED`; baza/model/timeout → `UNAVAILABLE`. Nie 500, nie 503.

#### 2. Widok i JS

**File**: `src/main/resources/templates/plan.html`, `src/main/resources/static/js/plan-download.js`, `src/main/resources/templates/fragments/chrome.html`, `src/main/resources/templates/profile.html`, `src/main/resources/static/css/site.css`

**Intent**: Użytkownik nie skleja ZIP-a; klika dwa pobrania.

**Contract**: Przycisk generowania (np. „Generuj plan”) woła `POST /plan/generate` z CSRF. Sukces: pokaż **Pobierz plan** (plik `dieta-na-jutro.pdf`) i **Pobierz listę zakupów** (`lista-zakupow.pdf`). Porażka: `role="alert"` — profil: „Najpierw zapisz profil, potem możesz wygenerować plan.”; kcal: „Najpierw zapisz cel kaloryczny, potem możesz wygenerować plan.”; reszta: „Nie udało się wygenerować planu. Spróbuj ponownie za chwilę.”. Nagłówek: link **Plan** obok Profil, tylko gdy `currentLogin != null`. Na `/profile` link do `/plan`. Chrome: `@Import(CurrentAccountAdvice)` w teście, jeśli asercja headera tego wymaga.

#### 3. Testy HTTP

**File**: `src/test/java/com/kenez92/plateplan/plan/controller/PlanControllerTest.java`, `src/test/java/com/kenez92/plateplan/config/SecurityConfigurationTest.java`

**Intent**: Slice bez Ollamy; mock `PlanService` / warstwy zwracającej DTO.

**Contract**: `@WebMvcTest(PlanController.class)` + Security. Zalogowany GET `/plan` 200. Wylogowany GET/POST → redirect `/`. POST bez CSRF → 403. Sukces: JSON ma `dietPdf` i `shoppingListPdf`, nie ma `error`. Każdy kod błędu: 200, pole `error`, treść HTML GET-a z alertem nie jest wymagana przy czystym JSON POST — asercja JSON + osobny GET z komunikatami w szablonie (th-text obecny w znacznikach). Inny login nigdy nie jest argumentem serwisu.

### Success Criteria:

#### Automated Verification:

- `PlanControllerTest` i rozszerzone testy security przechodzą: `.\gradlew.bat test --tests com.kenez92.plateplan.plan.controller.PlanControllerTest --tests com.kenez92.plateplan.config.SecurityConfigurationTest`
- `.\gradlew.bat test`

#### Manual Verification:

- Zalogowany, profil + cel: Generuj plan → dwa przyciski → oba PDF otwierają się i są po polsku, cztery posiłki, lista; ponowne Generuj daje nową parę bez śladu poprzedniej w bazie
- Bez profilu / bez celu / wyłączony klucz albo złe API: 200, alert, brak przycisków plików
- Wylogowany `/plan` wraca na `/`; POST bez CSRF nie zwraca PDF

**Implementation Note**: After completing this phase and all automated verification passes, pause here for manual confirmation from the human that the manual testing was successful before proceeding to the next phase. Phase blocks use plain bullets — the corresponding `- [ ]` checkboxes for these items live in the `## Progress` section at the bottom of the plan.

---

## Phase 4: Dokumentacja

### Overview

Żywe dokumenty zgadzają się z Cloud, JSON-em i wchłoniętym S-05.

### Changes Required:

#### 1. AGENTS.md

**File**: `AGENTS.md`

**Intent**: Kolejny agent nie woła lokalnej Ollamy, nie robi ZIP-a i nie planuje drugiego wycinka PDF.

**Contract**: Hard rules: generowanie przez Spring AI → Ollama Cloud (`OLLAMA_API_KEY`), wynik dwóch PDF w JSON na `POST /plan/generate`, bez zapisu. Packages: `plan.controller`, `controller.dto`, `model`, `service`. Nowa sekcja Plan: GET `/plan`, predykaty profil+cel, cztery posiłki, prompt EN / treść PL, produkty+kcal jedynym wejściem. Open: e-mail zamiast PDF zostaje otwarte i **nie** zastępuje tych przycisków.

#### 2. Roadmapa i infra

**File**: `context/foundation/roadmap.md`, `context/foundation/infrastructure.md`

**Intent**: S-04 to ta zmiana; wynik S-05 (dwa PDF) jest tu, nie w osobnym ZIP-ie na hoście.

**Contract**: Przy archiwizacji (nie w tej fazie kodu) S-04 i S-05 idą na `done` razem — w planie zapisz to w risk/notes. Teraz: opis S-04 = Cloud + `/plan` + dwa pola PDF. S-05: „dostarczone w `generate-diet-with-ollama`; osobna zmiana zbędna”. Infra: Ollama Cloud zamiast „local or external” jako wybrana ścieżka; nadal zero wag na Machine. `OLLAMA_API_KEY` przez `fly secrets`, nigdy `fly.toml`.

### Success Criteria:

#### Automated Verification:

- `.\gradlew.bat test`

#### Manual Verification:

- `AGENTS.md` wymienia `/plan`, dwa pola JSON, Cloud i zakaz zapisu; roadmapa S-05 nie każe zaczynać osobnego ZIP-a

**Implementation Note**: After completing this phase and all automated verification passes, pause here for manual confirmation from the human that the manual testing was successful before proceeding to the next phase. Phase blocks use plain bullets — the corresponding `- [ ]` checkboxes for these items live in the `## Progress` section at the bottom of the plan.

---

## Testing Strategy

### Unit Tests:

- `PlanService`: profil, kcal, baza, sukces całego `DietPlan`, `unavailable` generatora
- Prompt: produkty + kcal obecne; wiek/waga/płeć/login nieobecne
- PDF: polskie znaki, cztery posiłki, lista
- JSON: dwa Base64 albo samo `error`

### Integration Tests:

- `@WebMvcTest(PlanController)`: GET, POST CSRF, JSON sukcesu, trzy kody `error`, redirect wylogowanego
- `SecurityConfigurationTest`: `/plan` niepubliczny
- `ApplicationTest` bez `OLLAMA_API_KEY`

### Manual Testing Steps:

1. Konto z profilem i celem → `/plan` → Generuj → Pobierz plan i Pobierz listę zakupów.
2. To samo bez wiersza profilu i osobno z profilem bez `confirmed_calories`.
3. Bez `OLLAMA_API_KEY` albo z złym kluczem: alert, strona żyje.
4. Wylogowany `/plan`; POST generate bez CSRF.
5. Upewnij się, że w Supabase nie pojawił się blob diety.

## Performance Considerations

Jedno synchroniczne wołanie do 120 s. JSON z dwoma PDF powiększa odpowiedź o Base64 (~33%). Fly może uciąć ciszę po ~60 s — wtedy ten sam alert. Bez cache modelu i bez ponawiania (retry było odrzucone).

## Migration Notes

Brak nowej tabeli i changeSetu. `OLLAMA_API_KEY` (i opcjonalnie `OLLAMA_CHAT_MODEL`) jako sekrety / env. Lokalnie `SESSION_COOKIE_SECURE=false` jak dotychczas; CSRF musi dojść do `fetch`.

## References

- Roadmapa S-04 / S-05: `context/foundation/roadmap.md`
- PRD US-01, FR-006: `context/foundation/prd.md`
- Stack: `context/foundation/tech-stack.md`
- Infra Ollama: `context/foundation/infrastructure.md:48`, `:101`
- Wzorzec wyniku: `profile/model/ConfirmedCaloriesResult.java`
- Wzorzec kontrolera: `profile/controller/ProfileController.java`
- Ollama Cloud auth: https://docs.ollama.com/api/authentication

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands. Do not rename step titles. See `references/progress-format.md`.

### Phase 1: Generowanie

#### Automated

- [x] 1.1 Testy `plan.service` przechodzą: `.\gradlew.bat test --tests com.kenez92.plateplan.plan.service.*` — 09076db
- [x] 1.2 Cały zestaw przechodzi, w tym `ApplicationTest`: `.\gradlew.bat test` — 09076db

#### Manual

- [x] 1.3 W `tech-stack.md` i `build.gradle.kts` widać ten sam starter Spring AI; w properties jest `https://ollama.com` i timeout 120 s, bez sekretu w pliku — 09076db

### Phase 2: Dwa PDF w JSON

#### Automated

- [x] 2.1 Testy PDF i DTO przechodzą: `.\gradlew.bat test --tests com.kenez92.plateplan.plan.*`
- [x] 2.2 `.\gradlew.bat test`

#### Manual

- [ ] 2.3 Otwórz dwa PDF z testowego `DietPlan` (zrzut z testu albo krótki main deweloperski): polskie znaki, cztery nagłówki posiłków, lista zakupów; plików nie ma w `src/` ani w git

### Phase 3: Ekran `/plan`

#### Automated

- [ ] 3.1 `PlanControllerTest` i rozszerzone testy security przechodzą: `.\gradlew.bat test --tests com.kenez92.plateplan.plan.controller.PlanControllerTest --tests com.kenez92.plateplan.config.SecurityConfigurationTest`
- [ ] 3.2 `.\gradlew.bat test`

#### Manual

- [ ] 3.3 Zalogowany, profil + cel: Generuj plan → dwa przyciski → oba PDF otwierają się i są po polsku, cztery posiłki, lista; ponowne Generuj daje nową parę bez śladu poprzedniej w bazie
- [ ] 3.4 Bez profilu / bez celu / wyłączony klucz albo złe API: 200, alert, brak przycisków plików
- [ ] 3.5 Wylogowany `/plan` wraca na `/`; POST bez CSRF nie zwraca PDF

### Phase 4: Dokumentacja

#### Automated

- [ ] 4.1 `.\gradlew.bat test`

#### Manual

- [ ] 4.2 `AGENTS.md` wymienia `/plan`, dwa pola JSON, Cloud i zakaz zapisu; roadmapa S-05 nie każe zaczynać osobnego ZIP-a
