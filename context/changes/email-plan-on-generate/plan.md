# Wysyłka PDF planu mailem przy generate — plan wdrożenia

## Overview

Zalogowany użytkownik może zapisać opcjonalny adres na `/profile`. Gdy adres jest na koncie, `/plan` pokazuje odznaczony checkbox. Zaznaczenie i **Generuj plan** w tym samym `POST /plan/generate` wysyła `dieta-na-jutro.pdf` oraz `lista-zakupow.pdf` jako załączniki — te same bajty co przyciski pobierania. Przyciski zostają. Awaria SMTP nie zabiera PDF-ów.

To jest **pierwsze** rozszerzenie po M-1. Rodzinny plan (kilka osób, rozdzielone porcje) i plan na tydzień są następnymi, osobnymi zmianami — nie fazami tutaj.

## Current State Analysis

- Milestone M-1 jest zamknięty: konto, profil, kalorie i dwa PDF na `/plan`. PRD zostawia otwarte: e-mail zamiast PDF; ta zmiana rozstrzyga to jako **e-mail obok PDF-ów**, nie zamiast nich.
- `POST /plan/generate` nie ma body. Sukces to wyłącznie `dietPdf` i `shoppingListPdf`. Pole `error` w `plan-download.js` chowa przyciski — błąd poczty nie może iść w `error`.
- `GET /plan` nie czyta bazy (`PlanController.java:37-39`). E2e (`PlanDownloadE2eTest`) stoi na martwym Postgresie; odczyt profilu pod checkbox nie może zwracać 500.
- `user_profile` nie ma e-maila. Nullable kolumna ma wzorzec w `004-add-confirmed-calories.xml`. `replaceConfirmedCalories` pisze tylko kcal; `replaceBodyAndProducts` / `replaceBodyProductsAndCalories` muszą dostać e-mail, inaczej zapis ciała go wyzeruje.
- W Gradle nie ma maila. Biblioteka wchodzi tylko przez `tech-stack.md` i `build.gradle.kts`. Start bez `OLLAMA_API_KEY` jest wzorcem dla SMTP.
- `has_background_jobs: false`. Auto-stop Fly (`fly.toml:10-12`) zostaje. Scheduler i subskrypcja były rozważane i **odrzucone**.

## Desired End State

Na profilu jest opcjonalne pole e-mail. Puste jest dozwolone i zapisuje `null`. Niepoprawny adres odmawia całego zapisu ciała. Na `/plan` checkbox widać tylko gdy adres jest zapisany; jest zawsze odznaczony. Zaznaczenie + generate zwraca dwa PDF i komunikat, że mail wyszedł. Odznaczenie zachowuje dzisiejszy JSON. Pad poczty: HTTP 200, oba PDF, `emailError`, przyciski zostają. `.\gradlew.bat test` jest zielone, w tym e2e i `ApplicationTest`, bez żywego SMTP.

### Key Discoveries:

- Błąd generate (`PROFILE_REQUIRED` / `CALORIES_REQUIRED` / `UNAVAILABLE`) i błąd maila to dwa pola. `showError` w `plan-download.js:112-117` woła `hideDownloads` — osobna ścieżka UI dla `emailError`.
- Parametr `sendEmail` z `defaultValue=false` zostawia obecny POST bez body. E2e i odznaczony checkbox nic nie wysyłają.
- Adres należy do właściciela na `/profile`. Na `/plan` tylko boolean (checkbox jest albo go nie ma) — bez adresu w HTML i w JSON generate.
- `UserProfileRepository.java:21-66`: trzy zapytania. Kcal-only nie rusza e-maila; dwa zapisy ciała muszą go pisać.
- Fly ucina bezczynne HTTP ~60 s; Ollama już dominuje 120 s. SMTP na tym samym wątku, synchronicznie, bez joba.

## What We're NOT Doing

- Subskrypcja, scheduler, Spring `@Scheduled`, budzenie Machine, GitHub Actions cron, joby w tle.
- E-mail zamiast PDF, HTML diety w treści maila, trzeci renderer.
- Zapis PDF, Base64 albo treści diety w bazie, na dysku albo w sesji.
- Weryfikacja adresu, reset hasła, login-jako-e-mail, unikalność adresu między kontami.
- Playwright i żywy SMTP w CI. `PlanDownloadE2eTest` zostaje dowodem US-01 (dwa nazwane PDF, CSRF, zniknięcie po odświeżeniu), nie skrzynki.
- Zmiana `auto_stop_machines`, `min_machines_running`, albo 24/7 Machine.
- Logowanie adresu, załączników, promptu, kcal, list produktów albo bajtów PDF.
- **Następna zmiana (nie teraz):** dodatkowe osoby na tym samym koncie (np. dziecko, żona) — własne dane i cele, jeden plan rodziny z rozdzielonym, kto ile ma zjeść. Dziś PRD Non-Goal; odwrócenie należy do tamtej zmiany, nie do fazy 3 tutaj.
- **Zmiana po rodzinie (nie teraz):** plan z jednego dnia na cały tydzień. Generate, PDF i e-mail zostają przy „na jutro”.

## Implementation Approach

Trzy fazy. Faza 1 dodaje nullable e-mail na profilu (schemat, formularz, walidator, zapisy JPQL). Faza 2 dodaje starter mail, collaboratora wysyłki, parametr `sendEmail`, pole `emailError` na sukcesie generate oraz checkbox + komunikaty w JS. Faza 3 wpisuje bibliotekę i sekrety do dokumentów. Testy: `should…`, `final`, `usingRecursiveComparison` dla obiektów; JSON i HTML jako asercje łańcuchów.

## Critical Implementation Details

- **`emailError` vs `error`** — sukces z padłem SMTP to oba PDF plus `emailError` (np. `UNAVAILABLE`). `error` nadal oznacza wyłącznie porażkę generate i czyści PDF-y. JS przy `emailError` zostawia `#plan-downloads`.
- **GET `/plan` i martwa baza** — brak wiersza, pusty e-mail albo `DataAccessException` → ukryj checkbox, HTTP 200, widok `plan`. Inaczej e2e i lokalny start bez bazy padną.
- **Trzy UPDATE-y profilu** — `replaceConfirmedCalories` bez e-maila. Oba `replaceBody*` ustawiają e-mail w tym samym zdaniu co ciało i produkty, inaczej kolejny zapis profilu skasuje adres.

## Phase 1: E-mail na profilu

### Overview

Konto może mieć opcjonalny adres dostawy na `user_profile`. Formularz profilu go pokazuje i zapisuje. Odmowa walidacji nic nie pisze. Zapisy tylko kcal nie ruszają kolumny.

### Changes Required:

#### 1. Migracja

**File**: `src/main/resources/db/changelog/changes/005-add-profile-email.xml`

**Intent**: Istniejące wiersze zostają bez adresu, więc checkbox na `/plan` jest ukryty aż do zapisu.

**Contract**: `changeSet` Liquibase (typy zmian, nie surowy SQL), `addColumn` `email` `varchar(254)` nullable na `user_profile`. Prefiks `005-` — `includeAll` jest alfabetyczny; nie ruszać `001`–`004`.

#### 2. Encja i repozytorium

**File**: `src/main/java/com/kenez92/plateplan/profile/db/UserProfile.java`, `UserProfileRepository.java`

**Intent**: Adres żyje na tym samym wierszu co ciało. Zapis kalorii go nie kasuje; zapis ciała go ustawia albo czyści.

**Contract**: Pole `email` (null = brak). Konstruktor i `replaceValues` przyjmują e-mail. `replaceBodyAndProducts` oraz `replaceBodyProductsAndCalories` ustawiają `profile.email`. `replaceConfirmedCalories` bez zmian sygnatury.

#### 3. Formularz, model, walidator

**File**: `ProfileFormDto.java`, `ProfileDetails.java`, `ProfileService.java`, nowy walidator w `profile.validator`, `ValidationMessages.properties` / `messages.properties`, `profile.html`

**Intent**: Adres jest opcjonalny, należy do właściciela, nie wycieka w `toString` ani logach. Puste pole nie blokuje zapisu ciała.

**Contract**: `ProfileFormDto` ma opcjonalne `email`; `empty()` ma `null`. Puste / same białe znaki po `strip` → `null`. Niepusty: `strip` + NFC, potem reguła w walidatorze (nie sam `@Email`): jeden `@`, długość 3–254, bez sterujących, formatujących i nie-zwykłych spacji. Niepoprawny → błąd pola, `never().save`. `ProfileDetails` niesie e-mail, żeby pierwszy insert i oba replace pisały tę samą wartość. `toString` formularza zostaje zredagowany. Logi serwisu nadal tylko nazwy klas. Etykieta po polsku na `/profile`.

#### 4. Testy profilu

**File**: `src/test/java/com/kenez92/plateplan/profile/` (walidator, `ProfileServiceTest`, `ProfileControllerTest`)

**Intent**: Kolumna i odmowa zapisu są w suite, nie w ręcznym klikaniu.

**Contract**: `should…`: pusty e-mail zapisuje `null`; poprawny wraca przy load; późniejszy zapis ciała trzyma `confirmed_calories` i pisze e-mail; `replaceConfirmedCalories` / recalculate nie woła replace ciała w sposób zerujący e-mail; niepoprawny POST `never().save`; HTML zawiera pole. Porównanie obiektów rekurencyjne.

### Success Criteria:

#### Automated Verification:

- Testy profilu z e-mailem przechodzą: `.\gradlew.bat test --tests com.kenez92.plateplan.profile.*`
- Cały zestaw przechodzi, w tym `ApplicationTest`: `.\gradlew.bat test`

#### Manual Verification:

- Na `/profile` opcjonalny e-mail; puste zapisuje brak adresu; niepoprawna wartość nic nie pisze i pokazuje błąd przy polu

**Implementation Note**: After completing this phase and all automated verification passes, pause here for manual confirmation from the human that the manual testing was successful before proceeding to the next phase. Phase blocks use plain bullets — the corresponding `- [ ]` checkboxes for these items live in the `## Progress` section at the bottom of the plan.

---

## Phase 2: Checkbox i wysyłka przy generate

### Overview

Starter mail i collaborator wysyłają dwa PDF po udanym generate, tylko gdy `sendEmail=true`. JSON sukcesu może dostać `emailError`. UI: ukryty checkbox bez adresu, zawsze odznaczony, komunikat po wysyłce, błąd poczty bez chowania przycisków.

### Changes Required:

#### 1. Biblioteka i konfiguracja SMTP

**File**: `context/foundation/tech-stack.md`, `build.gradle.kts`, `src/main/resources/application.properties`

**Intent**: Gradle nie dostaje biblioteki spoza tech-stacku. Aplikacja startuje bez SMTP, jak bez `OLLAMA_API_KEY`.

**Contract**: `spring-boot-starter-mail` w tech-stacku i w `implementation`. Properties: host, port, username, password, `spring.mail.properties.mail.smtp.auth` / starttls według pustych defaultów z env (`MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`). Pusty host: start OK; próba wysyłki → `emailError`. Timeout SMTP krótki (sekundy, nie 120 s Ollamy). `has_background_jobs` zostaje `false`.

#### 2. Collaborator wysyłki

**File**: nowy `@Service` w `plan.service` (np. `PlanMailSender`)

**Intent**: Jedno miejsce `JavaMailSender`, żeby testy mogły je zastąpić. Generate nie woła SMTP, gdy checkbox jest off.

**Contract**: Metoda w stylu `send(String to, byte[] dietPdf, byte[] shoppingListPdf)` — bez loginu w sygnaturze widocznej w logach. Załączniki: `dieta-na-jutro.pdf`, `lista-zakupow.pdf`. Temat i krótka treść po polsku, bez dań i bez adresu w logach. Brak hosta, brak `MAIL_FROM`, wyjątek transportu: rzut albo wynik niedostępności, który kontroler mapuje na `emailError`. Log przy porażce: tylko nazwy klas, jak `DietGenerator`.

#### 3. Kontrakt JSON

**File**: `PlanFilesDto.java`, `PlanFilesDtoTest.java`

**Intent**: Klient odróżnia porażkę generate od porażki poczty. Sukces nadal wymaga obu PDF.

**Contract**: Opcjonalne `emailError` tylko gdy `error == null` i oba PDF są obecne. `JsonInclude.NON_NULL`. Fabryka sukcesu bez maila jak dziś; fabryka sukcesu z `emailError` (stała `UNAVAILABLE`). Porażka generate czyści PDF i `emailError`. `toString` pomija bajty. Test serializacji: sukces bez checkboxa to nadal dokładnie `dietPdf` i `shoppingListPdf`; sukces z padłem poczty ma te dwa pola plus `emailError`; `error` bez PDF.

#### 4. Kontroler generate i GET `/plan`

**File**: `PlanController.java`

**Intent**: Wysyłka w tym samym requeście co PDF, po zapisie bajtów w pamięci, bez magazynu. Checkbox tylko gdy adres jest na koncie.

**Contract**: `POST /plan/generate` przyjmuje `@RequestParam(defaultValue = "false") boolean sendEmail`. Gdy `sendEmail` jest false albo generate zwraca `error` — mailer `never()`. Gdy true i PDF-y gotowe: wczytaj e-mail właściciela (`Principal.getName()`); brak adresu albo porażka wysyłki → `success` z `emailError`, nie `PlanFilesDto.unavailable()`. GET `/plan` ustawia w modelu wyłącznie boolean (np. `emailOnFile`); wyjątek bazy → `false`. Adres nie trafia do widoku ani do JSON.

#### 5. Widok i JS

**File**: `plan.html`, `plan-download.js`

**Intent**: Opt-in przy każdym generate. Użytkownik widzi, że mail wyszedł albo że PDF-y są, a poczta nie.

**Contract**: Checkbox w DOM tylko przy true boolean (`th:if`). Zawsze odznaczony (bez `checked`). Etykieta po polsku. `fetch` dodaje `sendEmail=true` wyłącznie gdy checkbox istnieje i jest zaznaczony (form-urlencoded albo query); w przeciwnym razie body jak dziś. Po sukcesie bez `emailError`, jeśli wysyłano: status np. istniejący READY plus „Wysłano oba pliki na e-mail.” Po `emailError`: przyciski zostają, osobny alert (nie `showError`). `data-msg-*` na sekcji, bez adresu w atrybutach.

#### 6. Testy planu

**File**: `PlanControllerTest.java`, test mailera, ewentualnie stub w `@TestConfiguration`

**Intent**: Warstwy z test-planu: JSON i CSRF na `@WebMvcTest`, nie Playwright.

**Contract**: `should…`: brak `sendEmail` → mailer nie wołany, JSON jak dziś; `sendEmail=true` → mailer raz, dwa załączniki o tych nazwach, login z Principal nie z `login=bob`; wyjątek mailera → 200, oba PDF, `emailError`, brak `error`; `sendEmail=true` bez adresu → PDF + `emailError`, mailer nie musi wychodzić w sieć; GET bez e-maila / baza down → HTML bez checkboxa; GET z e-mailem → checkbox bez `checked`; CSRF-less nadal 403. E2e bez zmian funkcjonalnych.

### Success Criteria:

#### Automated Verification:

- Testy planu, DTO i mailera przechodzą: `.\gradlew.bat test --tests com.kenez92.plateplan.plan.*`
- Cały zestaw przechodzi, w tym `PlanDownloadE2eTest` i `ApplicationTest`: `.\gradlew.bat test`

#### Manual Verification:

- Adres na profilu: checkbox widać i jest odznaczony; zaznaczenie + generate daje dwa pobrania, komunikat wysyłki i mail z oboma PDF
- Brak adresu: checkboxu nie ma; generate nadal zwraca dwa PDF
- SMTP niedostępny przy zaznaczonym checkboxie: przyciski zostają, widać błąd maila
- Odznaczony checkbox nie wysyła maila

**Implementation Note**: After completing this phase and all automated verification passes, pause here for manual confirmation from the human that the manual testing was successful before proceeding to the next phase. Phase blocks use plain bullets — the corresponding `- [ ]` checkboxes for these items live in the `## Progress` section at the bottom of the plan.

---

## Phase 3: Dokumenty i sekrety

### Overview

AGENTS, PRD, tech-stack i infrastruktura opisują ten sam kontrakt: e-mail obok PDF, opcjonalny adres, SMTP z Fly secrets, start bez sekretów.

### Changes Required:

#### 1. AGENTS i PRD

**File**: `AGENTS.md`, `context/foundation/prd.md`

**Intent**: Agent i produkt nie traktują już e-maila jako otwartej zamiany PDF.

**Contract**: Hard rules / Plan: dwa przyciski zostają; opcjonalna wysyłka tych samych PDF przy `sendEmail`; plików nadal nie wolno persistować; nie logować adresu. Profile: opcjonalny e-mail na `user_profile`, puste = null. Open question PRD o e-mailu: rozstrzygnięte — dwa PDF zostają, e-mail jest dodatkową dostawą, nie zastępuje przycisków. Out of MVP / Non-Goals: joby w tle, wiele osób i „tylko jutro” **bez zmian w tej fazie** — rodzina i tydzień to osobne późniejsze zmiany.

#### 2. Stack i Fly

**File**: `context/foundation/tech-stack.md` (jeśli faza 2 nie domknęła opisu), `context/foundation/infrastructure.md`

**Intent**: Operator wie, które sekrety ustawić i że brak SMTP nie zatrzymuje startu.

**Contract**: Ta sama nazwa startera co w Gradle. Sekrety `MAIL_*` przez `fly secrets`, nigdy `fly.toml` ani committed properties. Akapit: puste wartości → generate bez zaznaczenia działa; zaznaczenie bez SMTP → `emailError`. Auto-stop bez zmian.

### Success Criteria:

#### Automated Verification:

- Cały zestaw przechodzi bez `MAIL_*` w środowisku testowym: `.\gradlew.bat test`

#### Manual Verification:

- `tech-stack.md`, `AGENTS.md`, `prd.md`, `infrastructure.md` i `application.properties` nazywają te same opcjonalne sekrety SMTP; w git nie ma haseł

**Implementation Note**: After completing this phase and all automated verification passes, pause here for manual confirmation from the human that the manual testing was successful before proceeding to the next phase. Phase blocks use plain bullets — the corresponding `- [ ]` checkboxes for these items live in the `## Progress` section at the bottom of the plan.

---

## Testing Strategy

### Unit Tests:

- Walidator e-maila: puste, poprawne, sterujące, brak `@`, za długie.
- `PlanMailSender`: przy mocku `JavaMailSender` dwa załączniki i nazwy plików; brak hosta nie loguje adresu.
- `PlanFilesDto`: rekurencyjne porównanie sukcesu, porażki generate i sukcesu z `emailError`.

### Integration Tests:

- `@WebMvcTest` profilu: odmowa zapisu, pole w HTML, własne konto.
- `@WebMvcTest` planu: macierz `sendEmail` / PDF / `emailError` / CSRF / Principal; GET checkbox.
- E2e istniejący: generate bez checkboxa (brak adresu albo baza down) nadal dwa nazwane PDF.

### Manual Testing Steps:

1. Nowe konto: profil bez e-maila → `/plan` bez checkboxa → generate → dwa PDF, skrzynka pusta.
2. Zapis adresu → `/plan` pokazuje odznaczony checkbox.
3. Zaznaczenie + generate → komunikat + oba załączniki w skrzynce, identyczne z pobranymi plikami.
4. Złe hasło SMTP / pusty `MAIL_HOST` → PDF i alert maila, bez `error` generate.
5. Niepoprawny e-mail na profilu → nic nie zapisane, w tym poprzedni adres jeśli był — zgodnie z „odmowa nic nie pisze”.

## Performance Considerations

Wysyłka jest synchroniczna po PDF, na wątku generate. Ollama (do 120 s) i limit Fly ~60 s bezczynności już istnieją; SMTP musi mieć krótki timeout, żeby pad poczty nie trzymał requestu minutę. Bez kolejki i bez drugiego wywołania modelu.

## Migration Notes

`005-add-profile-email` dodaje nullable kolumnę. Istniejące wiersze mają `email = null`. Checkbox ukryty do pierwszego poprawnego zapisu adresu. Rollback obrazu zostawia kolumnę (Liquibase nie cofa); aplikacja bez pola w encji zignoruje kolumnę tylko jeśli wrócimy do starego jar — trzymaj migrację kompatybilną wstecz (nullable, brak NOT NULL).

## References

- PRD open question (e-mail vs PDF): `context/foundation/prd.md`
- Plan generate: `src/main/java/com/kenez92/plateplan/plan/controller/PlanController.java`
- JSON kontrakt: `src/main/java/com/kenez92/plateplan/plan/controller/dto/PlanFilesDto.java`
- JS chowa PDF przy `error`: `src/main/resources/static/js/plan-download.js`
- Zapisy profilu: `src/main/java/com/kenez92/plateplan/profile/db/UserProfileRepository.java`
- Test-plan (JSON na `@WebMvcTest`): `context/foundation/test-plan.md`
- Auto-stop Fly: `fly.toml`, `context/foundation/infrastructure.md`

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands. Do not rename step titles. See `references/progress-format.md`.

### Phase 1: E-mail na profilu

#### Automated

- [x] 1.1 Testy profilu z e-mailem przechodzą: `.\gradlew.bat test --tests com.kenez92.plateplan.profile.*` — ada0f51
- [x] 1.2 Cały zestaw przechodzi, w tym `ApplicationTest`: `.\gradlew.bat test` — ada0f51

#### Manual

- [x] 1.3 Na `/profile` opcjonalny e-mail; puste zapisuje brak adresu; niepoprawna wartość nic nie pisze i pokazuje błąd przy polu — ada0f51

### Phase 2: Checkbox i wysyłka przy generate

#### Automated

- [x] 2.1 Testy planu, DTO i mailera przechodzą: `.\gradlew.bat test --tests com.kenez92.plateplan.plan.*` — ada0f51
- [x] 2.2 Cały zestaw przechodzi, w tym `PlanDownloadE2eTest` i `ApplicationTest`: `.\gradlew.bat test` — ada0f51

#### Manual

- [x] 2.3 Adres na profilu: checkbox widać i jest odznaczony; zaznaczenie + generate daje dwa pobrania, komunikat wysyłki i mail z oboma PDF — ada0f51
- [x] 2.4 Brak adresu: checkboxu nie ma; generate nadal zwraca dwa PDF — ada0f51
- [x] 2.5 SMTP niedostępny przy zaznaczonym checkboxie: przyciski zostają, widać błąd maila — ada0f51
- [x] 2.6 Odznaczony checkbox nie wysyła maila — ada0f51

### Phase 3: Dokumenty i sekrety

#### Automated

- [x] 3.1 Cały zestaw przechodzi bez `MAIL_*` w środowisku testowym: `.\gradlew.bat test` — ada0f51

#### Manual

- [x] 3.2 `tech-stack.md`, `AGENTS.md`, `prd.md`, `infrastructure.md` i `application.properties` nazywają te same opcjonalne sekrety SMTP; w git nie ma haseł — ada0f51
