# Trzy ostatnie plany w prompcie — Implementation Plan

## Overview

Udane generowanie zapisuje na koncie siedem nazw obiadów. Następne wołanie modelu dostaje te nazwy z trzech ostatnich takich planów i nie oddaje PDF-ów, gdy któraś wróci jako obiad. Śniadanie, drugie śniadanie i kolacja mogą wrócić w następnym planie. Pliki PDF i lista zakupów nadal nie są zapisywane. W jednym tygodniu dotychczasowa unikalność śniadania, obiadu i kolacji zostaje.

## Current State Analysis

`PlanService.generate` ładuje zaznaczone osoby i woła `DietGenerator` raz (`PlanService.java:117`). Prompt podstawia tylko linie osób i dwie listy produktów (`DietGenerator.java:132-133`). Szablon każe, żeby nazwy śniadania, obiadu i kolacji były unikalne w tygodniu (`DietGenerator.java:95`). `WeekPlanValidator` trzyma te 21 nazw w jednym zbiorze. Klucz to `strip` i `toLowerCase(Locale.ROOT)` (`WeekPlanValidator.java:99-111`). Drugie śniadanie jest poza zbiorem (`WeekPlanValidator.java:101-104`). Odmowa, timeout i pusty tydzień dają `PlanResult.unavailable()` bez drugiej próby (`DietGenerator.java:124-149`).

`PlanController.toFiles` buduje dwa PDF-y w pamięci i zwraca je jako JSON (`PlanController.java:96-101`). Wyjątek zapisu PDF zwraca `UNAVAILABLE` i czyści pliki (`PlanController.java:104-107`). Awaria maila zostawia oba pliki i dopisuje `emailError` (`PlanController.java:111-116`).

Changelog w `src/main/resources/db/changelog/changes/` ma pliki `001`–`006`. Tabele aplikacji to `account`, `user_profile` i `household_member`. Login nie jest kluczem obcym (`006-household-members.xml:8-12`). `db.changelog-master.xml:7-11` dokleja każdy plik XML alfabetycznie i zabrania edycji zastosowanego changeSetu.

`prd.md:44` i `prd.md:118` zabraniają przechowywania planu i listy zakupów. `AGENTS.md:7` zabrania zapisu obu plików PDF. Badanie nie znalazło przyjętego zdania, które pozwala trzymać nazwy dań (`context/changes/recent-plan-memory/research.md`).

## Desired End State

Na jednym loginie leżą najwyżej trzy udane plany. Udany plan to ten, dla którego kontroler ma już oba `byte[]` PDF. W każdym jest siedem nazw obiadu, po jednej na dzień. Śniadanie, drugie śniadanie, kolacja, przygotowanie, lista zakupów i bajty PDF nie są zapisywane.

Następny prompt, dla dowolnego składu tego konta, dostaje te nazwy obiadów raz, pierwsza pisownia wygrywa, klucz bez wielkości liter. Model jest proszony, żeby nie użyć ich jako obiadu. Jeśli obiad ma taki klucz, Java odrzuca tydzień: HTTP 200, `UNAVAILABLE`, bez PDF, bez drugiej próby, i ten tydzień nie wchodzi do pamięci. Ten sam klucz jako śniadanie, drugie śniadanie albo kolacja nowego tygodnia nie jest powodem tej odmowy.

Przykład. Zapamiętane obiady to A „Rosół”, B „Schab”, C „Zapiekanka”. Timeout nic nie zapisuje i nie wypycha A. Potem udaje się D z obiadem „Kurczak”. Pamięć to B, C i D. „Rosół” może wrócić jako obiad. Obiad „ schab ” daje `UNAVAILABLE`, D zostaje, „Schab” zostaje zakazany jako obiad. Kolacja „Schab” albo śniadanie „Schab” w nowym tygodniu nie jest powodem tej odmowy.

Gdy oba PDF-y są gotowe, a insert się nie uda, użytkownik dostaje te same dwa pliki, bez nowego pola JSON. W logu jest klasa wyjątku i klasa przyczyny, bez nazw dań.

### Key Discoveries:

- Zapis musi nastąpić po obu tablicach bajtów i poza `try`, które przy błędzie PDF zwraca `UNAVAILABLE` (`PlanController.java:96-107`). Inaczej decyzja „PDF zostają” nie wejdzie.
- Odczyt nazw jest zwykłym błędem bazy na ścieżce `PlanService.generate` (`PlanService.java:91-93`): model nie jest wołany, wynik to `UNAVAILABLE`.
- Wołanie modelu nie może siedzieć w transakcji zapisu. Ollama ma limit 120 s (`DietGenerator` i konfiguracja klienta).
- Nazwa dania z modelu nie przechodzi walidatora produktów, więc może zawierać `;`. Kolumna nazw nie może być sklejką średnikami jak `preferred_products`.
- Testy planu są jednostkowe z mockami. `ApplicationTest` nie dowodzi poprawności XML, bo nieudana migracja przy starcie jest logowana i pomijana.

## What We're NOT Doing

- Zapis bajtów PDF, listy zakupów, przygotowania, śniadania, drugiego śniadania albo kolacji.
- Zakaz między planami dla śniadania, drugiego śniadania i kolacji. W jednym tygodniu zostaje dotychczasowy zbiór 21 nazw.
- Nowe pole w JSON obok `dietPdf` i `shoppingListPdf`. Ekran `/plan` się nie zmienia.
- Druga próba modelu, gdy tydzień powtórzy zapamiętaną nazwę.
- Osobna pamięć dla zaznaczonego składu. Jeden login, jedna trójka planów.
- Oceny lubię i nie lubię. To zostaje w `dish-likes`.
- Pamiętanie nieudanego generowania: timeout, odmowa walidatora, błąd PDF, brak profilu, brak kcal, zły wybór osób.
- Edycja changeSetów `001`–`006`.

## Implementation Approach

Nowa usługa w `plan.service` trzyma i oddaje nazwy. Dwie tabele Liquibase, w stylu `household_member`: `login varchar(50)` bez klucza obcego, `plan_id` jako identity, nazwy w osobnej tabeli z `ON DELETE CASCADE`. Kolejność „ostatni” to rosnące `plan_id`, nie zegar. Po czwartym udanym zapisie usługa usuwa wiersze tego loginu o najmniejszym `plan_id`, aż zostaną trzy. Inny login nie jest ruszany.

`DietGenerator.generate` dostaje dodatkową listę nazw obiadów. Dopisuje ją do promptu. Po dotychczasowym `isValid` odrzuca tydzień, którego obiad ma klucz z tej listy. Śniadanie, drugie śniadanie i kolacja tej listy nie sprawdzają. Pusta lista niczego nie odrzuca.

`PlanService.generate` czyta listę przed modelem. `PlanController` woła zapis dopiero, gdy obie tablice bajtów istnieją, i łapie błąd zapisu osobno od błędu PDF.

## Critical Implementation Details

- **Kolejność w kontrolerze.** `remember` jest wołane tylko wtedy, gdy `dietPdf` i `shoppingListPdf` już powstały, i zanim kod rozgałęzia się na mail. Własny `try` loguje klasę wyjątku i klasę przyczyny, a potem i tak zwraca pliki. Nie wolno włożyć tego wywołania do `try` z `PlanController.java:96-107`, bo ten blok zamienia każdy wyjątek na `UNAVAILABLE`.
- **Dwie transakcje.** Odczyt nazw i późniejszy zapis są krótkie. Samo `chatClient.prompt()` zostaje poza `@Transactional`. Zapis planu i skasowanie najstarszego ponad trzy są jedną transakcją: przy wyjątku nie zostaje czwarty plan ani nazwy bez nagłówka.

## Phase 1: Zapis trzech planów

### Overview

Konto może dostać siedem nazw obiadu z udanego tygodnia i oddać obiady z najwyżej trzech najnowszych planów. Śniadanie, drugie śniadanie i kolacja nie wchodzą do pamięci. Inny login się nie miesza.

### Changes Required:

#### 1. Changelog

**File**: `src/main/resources/db/changelog/changes/007-recent-lunch-names.xml`

**Intent**: Nowa migracja dokleja się alfabetycznie za `006` i nie rusza wcześniejszych plików.

**Contract**: Jeden changeSet `007-recent-lunch-names`, author `plateplan`, typy Liquibase, bez surowego SQL. Tabela `recent_plan`: `plan_id` bigint identity, klucz główny, oraz `login varchar(50)` not null. Indeks nieunikalny `recent_plan_login_idx` na `login`. Tabela `recent_lunch_name`: `lunch_name_id` bigint identity, klucz główny, `plan_id` bigint not null, `name` typu `text` not null. Klucz obcy `plan_id` → `recent_plan.plan_id` z `ON DELETE CASCADE`. Brak klucza obcego z `login` do `account`.

#### 2. Encje i repozytoria

**File**: `src/main/java/com/kenez92/plateplan/plan/db/RecentPlan.java`

**File**: `src/main/java/com/kenez92/plateplan/plan/db/RecentLunchName.java`

**File**: `src/main/java/com/kenez92/plateplan/plan/db/RecentPlanRepository.java`

**File**: `src/main/java/com/kenez92/plateplan/plan/db/RecentLunchNameRepository.java`

**Intent**: Wiersze da się czytać i kasować po loginie, w kolejności `plan_id`.

**Contract**: Encje publiczne, pola zgodne z kolumnami, repozytoria z `@Repository`. Repozytorium planu umie znaleźć plany loginu rosnąco po `plan_id` i skasować plan po `plan_id` i `login`. Repozytorium nazw umie oddać nazwy wskazanych planów i wstawić nazwy nowego planu. Żadna metoda nie przyjmuje loginu z innego konta jako luznego filtra nazwy: kasowanie i odczyt planu idą z `login`.

#### 3. Usługa pamięci

**File**: `src/main/java/com/kenez92/plateplan/plan/service/RecentPlanMemory.java`

**Intent**: Jedno miejsce, które z tygodnia bierze siedem obiadów i pilnuje limitu trzech planów.

**Contract**: Bean `@Service`. `List<String> avoidedNames(String login)` zwraca nazwy obiadów z najwyżej trzech najnowszych planów tego loginu, od najstarszego z tej trójki. Ta sama nazwa po kluczu `strip` + `toLowerCase(Locale.ROOT)` występuje raz, zostaje pierwsza pisownia. Pusta pamięć zwraca pustą listę. `void remember(String login, WeekPlan week)` w jednej transakcji wstawia jeden `recent_plan` i siedem wierszy `recent_lunch_name` z `lunch` każdego dnia, bez `breakfast`, `secondBreakfast` i `dinner`, a potem kasuje plany tego loginu ponad trzy, najpierw najmniejszy `plan_id`. Metoda nie loguje nazw, loginu ani treści tygodnia.

#### 4. Test usługi

**File**: `src/test/java/com/kenez92/plateplan/plan/service/RecentPlanMemoryTest.java`

**Intent**: Limit trzech, izolacja loginu i brak śniadania, drugiego śniadania oraz kolacji są zablokowane bez bazy.

**Contract**: Test jednostkowy, bez kontekstu Springa. Współpracownicy podani w konstruktorze. Asercja całego wyniku `avoidedNames`, nie pola po polu. Cztery `remember` na jednym loginie: zostają obiady drugiego, trzeciego i czwartego tygodnia. Obiady drugiego loginu zostają u niego. „Jajecznica” jako `breakfast`, „Jogurt” jako `secondBreakfast` i „Kanapki” jako `dinner` nie wracają z `avoidedNames`.

### Success Criteria:

#### Automated Verification:

- `.\gradlew.bat test --tests com.kenez92.plateplan.plan.service.RecentPlanMemoryTest`

#### Manual Verification:

- W difie jest nowy `src/main/resources/db/changelog/changes/007-recent-lunch-names.xml`, a pliki `001`–`006` nie są zmienione.

**Implementation Note**: Po zielonym teście zatrzymaj się na ręczne sprawdzenie changelogu, zanim pójdzie faza 2.

---

## Phase 2: Prompt i odmowa

### Overview

Generator dostaje listę obiadów z zewnątrz. Wpisuje ją do promptu i odrzuca tydzień, który użyje takiej nazwy jako obiadu. Śniadanie, drugie śniadanie i kolacja tej listy nie odrzucają. Nadal jedno wołanie modelu.

### Changes Required:

#### 1. Walidator

**File**: `src/main/java/com/kenez92/plateplan/plan/validator/WeekPlanValidator.java`

**Intent**: Powtórzony obiad jest odmową tego samego rodzaju co druga jajecznica w jednym tygodniu.

**Contract**: Nowa metoda obok `isValid`. Przyjmuje tydzień i listę zakazanych nazw obiadów. Zwraca fałsz, gdy klucz `lunch` któregoś dnia jest w zbiorze kluczy tej listy. Klucz jest ten sam co dziś: `strip` i `toLowerCase(Locale.ROOT)`. `breakfast`, `secondBreakfast` i `dinner` nie wchodzą do porównania. Pusta lista i tydzień bez trafienia w obiedzie zwracają prawdę. `isValid` nie zmienia swojego kontraktu i nie dostaje tej listy. Metoda nie loguje nazw.

#### 2. Generator

**File**: `src/main/java/com/kenez92/plateplan/plan/service/DietGenerator.java`

**Intent**: Model widzi zakaz, a Java nie puszcza tygodnia, który go złamie.

**Contract**: `generate` dostaje dodatkowo `List<String> avoided`. Prompt dokleja jedno angielskie zdanie, że tych nazw nie wolno użyć jako obiadu (`lunch`), po przecinku. Pusta lista wstawia słowo `none`, tak jak puste produkty. Po repair i prawdziwym `isValid` fałsz z nowej metody daje `PlanResult.unavailable()` bez drugiego `prompt()`. Istniejące powody `UNAVAILABLE` zostają. Log nadal nie zawiera promptu, nazw, kcal ani list produktów.

#### 3. Testy

**File**: `src/test/java/com/kenez92/plateplan/plan/validator/WeekPlanValidatorTest.java`

**File**: `src/test/java/com/kenez92/plateplan/plan/service/DietGeneratorTest.java`

**Intent**: Odmowa jest zablokowana na kluczu obiadu. Śniadanie, drugie śniadanie, kolacja i pusta lista nie odrzucają z powodu pamięci.

**Contract**: „Schab” w pamięci i obiad „ schab ” są odrzucone. Ten sam „Schab” jako `breakfast`, `secondBreakfast` albo `dinner`, przy obiedzie spoza listy, nie jest odrzucony przez nową metodę. Pusta lista nie odrzuca. Test generatora z powtórzonym obiadem sprawdza jedno `prompt()` i `UNAVAILABLE`. Test udanego tygodnia sprawdza, że złapany prompt zawiera podane nazwy obiadów, a przy pustej liście zawiera `none` w nowym zdaniu. Istniejące asercje, że prompt nie zawiera wieku, wzrostu, wagi, płci, aktywności, loginu ani maila, zostają zielone.

### Success Criteria:

#### Automated Verification:

- `.\gradlew.bat test --tests com.kenez92.plateplan.plan.validator.WeekPlanValidatorTest --tests com.kenez92.plateplan.plan.service.DietGeneratorTest`

**Implementation Note**: Po zielonych testach tej fazy nie ma kroku ręcznego. Faza 3 podłącza listę do prawdziwego generowania.

---

## Phase 3: Podłączenie generate i dokumenty

### Overview

Prawdziwe `POST /plan/generate` czyta pamięć obiadów konta, przekazuje ją do modelu i zapisuje siedem nazw obiadu dopiero po zbudowaniu obu PDF-ów. Dokumenty produktu mówią o tym wyjątku i nadal zabraniają zapisu plików oraz listy zakupów.

### Changes Required:

#### 1. PlanService

**File**: `src/main/java/com/kenez92/plateplan/plan/service/PlanService.java`

**Intent**: Skład zaznaczonych osób nie wybiera innej pamięci, a błąd odczytu nie woła modelu.

**Contract**: `generate(login, selected)` przed `DietGenerator` bierze `avoidedNames(login)` i podaje tę listę do `generate`. Brak wiersza profilu, brak kcal i zły wybór osób nadal kończą się swoimi wynikami i nie czytają pamięci, tak jak dziś nie wołają modelu tam, gdzie kod wraca wcześniej. `DataAccessException` i `TransactionException` na odczycie pamięci dają `PlanResult.unavailable()` i nie wołają modelu. Nowa metoda `remember(login, week)` deleguje do `RecentPlanMemory` i nie łapie wyjątku bazy. Nie loguje nazw.

#### 2. Kontroler

**File**: `src/main/java/com/kenez92/plateplan/plan/controller/PlanController.java`

**Intent**: Użytkownik dostaje pliki także wtedy, gdy historii nie udało się dopisać.

**Contract**: `remember` jest wołane tylko po udanym `dietPdf` i `shoppingListPdf`, przed rozgałęzieniem `sendEmail`. Wyjątek z `remember` jest łapany osobno, loguje klasę i klasę przyczyny, i nie zmienia DTO: przy wyłączonym mailu zostaje sukces z dwoma PDF-ami, przy włączonym zostaje dotychczasowa ścieżka maila, łącznie z `emailError`. Błąd zapisu PDF nadal zwraca `UNAVAILABLE` i nie woła `remember`. Brak profilu, brak kcal, zły wybór i `UNAVAILABLE` z generatora nie wołają `remember`. JSON nie dostaje nowego pola.

#### 3. Testy ścieżki

**File**: `src/test/java/com/kenez92/plateplan/plan/service/PlanServiceTest.java`

**File**: `src/test/java/com/kenez92/plateplan/plan/controller/PlanControllerTest.java`

**Intent**: Pamięć konta, odmowa zapisu po błędzie PDF i zostawienie plików po błędzie insertu są zablokowane obok dotychczasowych przypadków.

**Contract**: Generate dla jednego loginu podaje do generatora nazwy z pamięci tego loginu, także gdy zaznaczona jest tylko jedna osoba. Błąd odczytu pamięci daje `UNAVAILABLE` i generator nie jest wołany. Kontroler przy dwóch gotowych PDF-ach woła `remember` z tym loginem i tym tygodniem. Gdy `remember` rzuci, odpowiedź nadal ma oba PDF-y i nie ma `error`. Gdy zapis PDF rzuci, `remember` nie jest wołane i odpowiedź to `UNAVAILABLE`. Istniejące testy maila i `emailError` zostają zielone.

#### 4. Dokumenty

**File**: `context/foundation/prd.md`

**File**: `AGENTS.md`

**File**: `context/foundation/test-plan.md`

**Intent**: Zakaz zapisu planu nie kłóci się już z pamięcią nazw, a pliki i lista zakupów zostają zakazane.

**Contract**: W `prd.md` dopisać ten sam wyjątek w guardrail (`prd.md:44`), w FR-006, w wymaganiu niefunkcjonalnym o braku zapisu, w zdaniu „Neither file is stored” w logice biznesowej i w non-goalu (`prd.md:118`). Wyjątek brzmi: na koncie zostają nazwy obiadów z najwyżej trzech ostatnich udanych planów, żeby następny plan nie powtórzył ich jako obiadu. Śniadanie, drugie śniadanie i kolacja nie wchodzą do tej pamięci. Plik diety, lista zakupów i bajty PDF nie są zapisywane. W `AGENTS.md` to samo w hard rule i w sekcji Plan, oraz zakaz logowania tych nazw obok istniejącego zakazu logowania promptu i bajtów PDF. W `test-plan.md` ryzyko #5 nadal jest porażką, gdy zapisany zostanie PDF albo lista zakupów albo gdy log lub prompt wycieka. Tabela nazw obiadów z trzech ostatnich planów nie jest tą porażką. Reszta tych plików zostaje.

### Success Criteria:

#### Automated Verification:

- `.\gradlew.bat test`

#### Manual Verification:

- W `prd.md` i w `AGENTS.md` widać wyjątek na nazwy obiadów z trzech ostatnich udanych planów, a PDF i lista zakupów nadal nie są zapisywane.

**Implementation Note**: Po zielonym `test` zatrzymaj się na ręczne przeczytanie tych zdań, zanim zmiana zostanie uznana za wdrożoną.

---

## Testing Strategy

### Unit Tests:

- `RecentPlanMemoryTest`: czwarty plan wypycha najstarszy, drugi login zostaje, śniadanie, drugie śniadanie i kolacja nie wchodzą do listy.
- `WeekPlanValidatorTest`: klucz obiadu bez wielkości liter i spacji na brzegach, śniadanie, drugie śniadanie i kolacja nie odrzucają z pamięci, pusta lista nie odrzuca.
- `DietGeneratorTest`: jedno wołanie, `UNAVAILABLE` przy powtórzonym obiedzie, prompt zawiera nazwy obiadów albo `none`.
- `PlanServiceTest` i `PlanControllerTest`: odczyt przed modelem, zapis tylko po PDF, błąd insertu zostawia pliki.

### Integration Tests:

- Brak nowego testu z żywą bazą. XML changelogu sprawdza człowiek w fazie 1. `ApplicationTest` nadal tylko podnosi kontekst.

### Manual Testing Steps:

1. W fazie 1 sprawdzić, że diff dodaje `007-recent-lunch-names.xml` i nie zmienia `001`–`006`.
2. W fazie 3 przeczytać guardrail, FR-006 i non-goal w `prd.md` oraz hard rule i sekcję Plan w `AGENTS.md`.

## Performance Considerations

Pamięć to najwyżej trzy plany po siedem obiadów na login. Prompt dostaje każdą nazwę raz. Nie ma drugiego wołania modelu ani paginacji.

## Migration Notes

Konta, które już generowały plany, startują z pustą pamięcią. Nie ma uzupełniania wstecz. `007` stosuje się przy następnym udanym starcie Liquibase. Nieudany start loguje błąd i pomija migrację, tak jak dziś. Wtedy odczyt pamięci daje `UNAVAILABLE`, a zapis po gotowych PDF-ach zostawia pliki i nic nie dopisuje.

## References

- Related research: `context/changes/recent-plan-memory/research.md`
- Similar implementation: `src/main/resources/db/changelog/changes/006-household-members.xml`
- Uniqueness key: `src/main/java/com/kenez92/plateplan/plan/validator/WeekPlanValidator.java:99-111`
- PDF success path: `src/main/java/com/kenez92/plateplan/plan/controller/PlanController.java:96-116`

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands. Do not rename step titles. See `references/progress-format.md`.

### Phase 1: Zapis trzech planów

#### Automated

- [x] 1.1 `.\gradlew.bat test --tests com.kenez92.plateplan.plan.service.RecentPlanMemoryTest` — 0bdfbd1

#### Manual

- [x] 1.2 W difie jest nowy `src/main/resources/db/changelog/changes/007-recent-lunch-names.xml`, a pliki `001`–`006` nie są zmienione. — 0bdfbd1

### Phase 2: Prompt i odmowa

#### Automated

- [x] 2.1 `.\gradlew.bat test --tests com.kenez92.plateplan.plan.validator.WeekPlanValidatorTest --tests com.kenez92.plateplan.plan.service.DietGeneratorTest` — f56cf30

### Phase 3: Podłączenie generate i dokumenty

#### Automated

- [x] 3.1 `.\gradlew.bat test`

#### Manual

- [x] 3.2 W `prd.md` i w `AGENTS.md` widać wyjątek na nazwy obiadów z trzech ostatnich udanych planów, a PDF i lista zakupów nadal nie są zapisywane.
