# Plan na siedem dni — Implementation Plan

## Overview

Jeden `POST /plan/generate` dla istniejącego konta zwraca dietę i listę zakupów na siedem dni, od jutra w Europe/Warsaw. Każdy dzień trzyma się dziennej liczby kcal z profilu. W PDF diety dzień zaczyna własną stronę. Kolejne osoby zostają następną zmianą.

## Current State Analysis

Generate jest jednym następnym dniem. `DietPlan` to cztery posiłki i jedna lista (`DietPlan.java:6-13`). `DietGenerator` woła model raz, `.entity(DietPlan.class)`, a prompt mówi „next-day” i „shoppingList … for the whole day” (`DietGenerator.java:37-47, 78-81`). Suma kcal posiłków nie jest sprawdzana. PDF drukuje cel z profilu, nie sumę posiłków.

PDF diety i lista to dwa dokumenty PDFBox na A4. Łamanie jest tylko po wysokości wiersza w `PageAcc` (`PlanPdfWriter.java:400-420`). Nie ma znacznika nowej strony. Tytuł diety to jutro, `LocalDate.now(Europe/Warsaw).plusDays(1)`, format `d.MM.yyyy` (`PlanPdfWriter.java:39-43, 127-128`).

Ekran, plik `dieta-na-jutro.pdf` i mail mówią „na jutro” (`plan.html:14, 30`, `PlanMailSender.java:26-29`). Request ma tylko `sendEmail`. Sukces to nadal `dietPdf` i `shoppingListPdf`. Plików nie zapisujemy. Timeout czytania modelu to 120 s.

## Desired End State

Zalogowana osoba z profilem i `confirmed_calories` dostaje dwa PDF-y: jadłospis na siedem kolejnych dni od jutra oraz jedną listę zakupów z ilościami zsumowanymi na cały tydzień. Dzień 1 to jutro. Tytuł strony to `Dzień 1 · 14.10.2026` (data w `d.MM.yyyy`). Każdy dzień zaczyna nową stronę; gdy nie wchodzi na A4, ciąg dalszy leci na następną, a kolejny dzień i tak zaczyna od nowej. Na każdej stronie diety „Cel dnia” to ta sama liczba z profilu.

> **Po przeglądzie, 2026-10-12.** Tytuł dnia to `Dzień N`, bez daty. Zwykły dzień z krótkim przygotowaniem (temperatura, czas, naczynie) jest składany na jedną stronę, bez ucinania tekstu. Bardzo długi dzień nadal schodzi na następną stronę. Pusty odstęp nie otwiera pustej strony.

Suma kcal czterech posiłków dnia mieści się w domkniętym paśmie ±10% tej liczby. Nazwa śniadania, obiadu i kolacji, po `strip` i `toLowerCase(Locale.ROOT)`, nie powtarza się wśród tych 21 posiłków. Drugie śniadanie może wrócić, także przez siedem dni i także pod nazwą dania gotowanego. Prompt prosi, żeby nie wracało bez przerwy. Java tego nie odrzuca. Inna liczba dni, pusta nazwa, zejście z pasma albo powtórka śniadania, obiadu lub kolacji dają `UNAVAILABLE`, bez PDF-ów i bez drugiej próby. Plik diety nazywa się `dieta-na-tydzien.pdf`. Lista zostaje `lista-zakupow.pdf`. Eyebrow i mail mówią o 7 dniach.

### Key Discoveries:

- Jedno wywołanie `.entity(...)` i jeden `PlanResult` na request (`DietGenerator.java:78-86`, `PlanController.java:60-63`).
- `PageAcc` otwiera stronę tylko gdy kolejny wiersz się nie mieści (`PlanPdfWriter.java:410-414`). Wymuszony start dnia musi być osobnym znacznikiem, którego się nie rysuje.
- Porównanie bez wielkości liter już jest `toLowerCase(Locale.ROOT)` (`ProductListsValidator.java:85`). Zaokrąglenie kcal w formule jest `RoundingMode.HALF_UP` do pełnej kilokalorii (`CalorieService.java:47`).
- Pusta lista zakupów jest dziś legalna (`DietPlan` zamienia null na pustą listę). Tydzień to zachowuje.

## What We're NOT Doing

- Kolejne osoby na koncie, własne cele i rozdzielone porcje.
- Zapis planu albo listy. Nadal dwa pola PDF, bez ZIP-a.
- Siedem wywołań modelu, pamięć poprzednich dni, druga próba po złej odpowiedzi.
- Parsowanie ilości w Javie (`150 g`, `2 szt.`). Sumę robi model w prompcie.
- Licznik powtórek drugiego śniadania. Ten slot może wrócić dowolnie często. „Bez przesady” jest zdaniem w prompcie.
- Dokładnie siedem stron za cenę ściskania albo ucinania tekstu.
- Tydzień poniedziałek–niedziela albo start od dzisiaj.
- Zmiana formuły kcal, kolumny `confirmed_calories` albo progu 800–6000.
- Nowe pole horyzontu na `POST /plan/generate`.

## Implementation Approach

Nowy kształt odpowiedzi modelu to tydzień: siedem dni po cztery posiłki oraz jedna lista zakupów. `DietPlan` schodzi z generate, bo niesie listę jednego dnia. Po parsowaniu walidator odmawia tygodnia poza pasmem, o złej długości albo z powtórzoną nazwą, i generator zwraca `unavailable()`, tak jak przy pustej odpowiedzi. Daty liczy writer, nie model: dzień o indeksie 0 to jutro w Europe/Warsaw.

PDF diety składa dni po kolei. Przed tytułem dnia od drugiego włącznie jest znacznik końca strony. Lista zakupów zostaje jednym tytułem i jedną serią punktorów. Ekran, nazwy załączników i dokumenty produktu zmieniają „jutro” na siedem dni dopiero, gdy writer już umie ten tydzień narysować. Każda faza zostawia `.\gradlew.bat test` zielone.

## Critical Implementation Details

- **Pasmo kcal.** Delta to 10% `confirmed_calories`, half-up do pełnej kilokalorii. Suma czterech `Meal.kcal` dnia jest przyjęta, gdy leży w `[cel - delta, cel + delta]`. Cel 2200 daje deltę 220 i przedział 1980–2420 włącznie. Cel 1999 daje deltę 200 (199,9 half-up) i przedział 1799–2199 włącznie. 1979 i 2421 przy celu 2200 odpadają. Liczby kcal bierze się z modelu; składników się nie przelicza.
- **Nazwy.** Klucz to `strip` plus `toLowerCase(Locale.ROOT)`. Pusty klucz odrzuca tydzień. Unikalność dotyczy śniadania, obiadu i kolacji: ta sama nazwa drugi raz wśród tych slotów daje `UNAVAILABLE`. Wielkość liter i spacje na brzegach nie tworzą drugiej nazwy. „Jogurt naturalny” jako śniadanie dnia 1 i drugie śniadanie dnia 3 przechodzi. Ten sam skyr jako drugie śniadanie przez siedem dni też przechodzi. Prompt mówi modelowi, żeby drugie śniadanie nie wracało bez przerwy. Walidator nie liczy tych powtórek.
- **Start strony.** `PageAcc` nie ma twardego łamania. Znacznik końca strony zamyka niepustą stronę i sam nie jest rysowany. Na pustej stronie nic nie robi, więc dzień 1 nie zostawia pustej pierwszej strony. Następny dzień zaczyna stronę także wtedy, gdy poprzedni zajął więcej niż jedną.
- **Jedno wywołanie.** Timeout 120 s zostaje. Zła odpowiedź, timeout i odmowa walidatora są tym samym `UNAVAILABLE`. Walidator nie loguje kcal, nazw, promptu ani bajtów PDF.

## Phase 1: Kształt tygodnia i odmowa

### Overview

Dostarcza typy tygodnia i czystą odmowę, bez modelu, PDF i HTTP. Istniejący generate na jeden dzień jeszcze działa.

### Changes Required:

#### 1. Typy dnia i tygodnia

**File**: `src/main/java/com/kenez92/plateplan/plan/model/DayMeals.java`, `src/main/java/com/kenez92/plateplan/plan/model/WeekPlan.java`

**Intent**: Model ma gdzie położyć siedem dni i jedną listę, bez listy zakupów przy każdym dniu.

**Contract**: `DayMeals` ma `breakfast`, `secondBreakfast`, `lunch`, `dinner`, każde `Meal`, żadne null. `WeekPlan` ma `List<DayMeals> days` oraz `List<ShoppingItem> shoppingList`. Null listy dni zostaje null albo pustą listą tak, żeby walidator zobaczył złą długość, a nie ukrył jej. Null listy zakupów staje się pustą listą, jak dziś w `DietPlan`. `DietPlan` w tej fazie zostaje, bo generate jeszcze go używa.

#### 2. Odmowa tygodnia

**File**: `src/main/java/com/kenez92/plateplan/plan/validator/WeekPlanValidator.java`

**Intent**: Jedno miejsce mówi, czy tydzień wolno puścić do PDF. Reguły z wywiadu żyją tutaj, nie w prompcie.

**Contract**: Publiczna metoda przyjmuje `WeekPlan` i dzienny cel w kcal oraz zwraca, czy tydzień jest przyjęty. Przyjęty tydzień ma dokładnie 7 dni, każdy posiłek ma niepustą nazwę po `strip`, suma kcal dnia jest w paśmie z Critical Implementation Details, a klucze nazw śniadania, obiadu i kolacji są unikalne wśród tych 21 posiłków. Powtórka drugiego śniadania, także siedem razy i także z nazwą dania gotowanego, jest przyjęta. Pusta lista zakupów jest przyjęta. Brak drugiego wywołania modelu i brak logowania treści planu.

#### 3. Test walidatora

**File**: `src/test/java/com/kenez92/plateplan/plan/validator/WeekPlanValidatorTest.java`

**Intent**: Przypadki graniczne pasma, powtórki i długości są czerwone, zanim wejdzie model.

**Contract**: Metody `should…`. Przy celu 2200 suma 1980 i 2420 przechodzi, 1979 i 2421 nie. Przy celu 1999 suma 1799 i 2199 przechodzi, 1798 i 2200 nie. Powtórka „Jogurt naturalny” między śniadaniem a obiadem nie przechodzi, także gdy różni się wielkością liter albo spacjami na brzegach. To samo drugie śniadanie przez siedem dni przechodzi. „Jogurt naturalny” jako śniadanie dnia 1 i drugie śniadanie dnia 3 przechodzi. Sześć i osiem dni nie przechodzi. Pusta nazwa nie przechodzi. Pusta lista zakupów przy poprawnych dniach przechodzi.

### Success Criteria:

#### Automated Verification:

- `.\gradlew.bat test --tests com.kenez92.plateplan.plan.validator.WeekPlanValidatorTest` przechodzi na paśmie, powtórce nazwy i długości tygodnia
- `.\gradlew.bat test` przechodzi

**Implementation Note**: Po zielonym suite zatrzymaj się na potwierdzenie, że faza 1 jest domknięta, i dopiero wtedy ruszaj generate.

---

## Phase 2: Jedno wywołanie modelu

### Overview

`DietGenerator` prosi o tydzień i przyjmuje tylko tydzień, który walidator puścił. `PlanResult` niesie `WeekPlan`. Writer zmienia parametr na `WeekPlan`, żeby projekt się budował; twarde łamanie stron jest w fazie 3.

### Changes Required:

#### 1. Prompt i entity

**File**: `src/main/java/com/kenez92/plateplan/plan/service/DietGenerator.java`

**Intent**: Jedno wywołanie zwraca siedem dni i jedną listę. Prompt niesie te same reguły co walidator, po angielsku, a treść diety zostaje po polsku.

**Contract**: `generate` woła `.entity(WeekPlan.class)` raz. Po null albo odmowie walidatora zwraca `PlanResult.unavailable()`. Prompt: siedem kolejnych dni, każdy z czterema posiłkami, drugie śniadanie nadal gotowy produkt sklepowy, każdy dzień blisko tego samego dziennego celu, nazwy śniadania, obiadu i kolacji unikalne, drugie śniadanie bez wracania w kółko, lista zakupów zsumowana na siedem dni (ta sama nazwa raz, ilość zsumowana przez model). Cel, preferencje i wykluczenia wchodzą jak dziś. Do promptu nie wchodzą wiek, wzrost, waga, płeć, aktywność, login ani e-mail. Dat kalendarzowych model nie ustala.

#### 2. Wynik generate

**File**: `src/main/java/com/kenez92/plateplan/plan/model/PlanResult.java`, `src/main/java/com/kenez92/plateplan/plan/service/PlanService.java`

**Intent**: Reszta aplikacji dostaje tydzień albo ten sam brak profilu, brak kcal albo `unavailable`.

**Contract**: `PlanResult.success` bierze `WeekPlan` i dzienny cel z profilu. `PlanService.generate(login)` nadal ładuje jeden wiersz i woła generator raz. Brak wiersza i null `confirmed_calories` zostają `noProfile` i `noCalories`. `DietPlan` znika z tej ścieżki, gdy żaden caller go nie potrzebuje.

#### 3. Sygnatura writera na czas kompilacji

**File**: `src/main/java/com/kenez92/plateplan/plan/service/PlanPdfWriter.java`, `src/main/java/com/kenez92/plateplan/plan/controller/PlanController.java`

**Intent**: Suite zostaje zielony po zmianie typu, zanim faza 3 narzuci strony.

**Contract**: `dietPdf` i `shoppingListPdf` przyjmują `WeekPlan`. Dieta składa dni w kolejności, każdy z czterema posiłkami i sumą makro tego dnia. Twarde „dzień zaczyna stronę” oraz tytuł `Dzień N` wchodzą w fazie 3. Lista rysuje `shoppingList` tygodnia pod jednym tytułem. Controller przekazuje `result` tygodnia i ten sam dzienny cel do obu PDF-ów. Błąd zapisu PDF nadal jest `UNAVAILABLE` i nic nie loguje bajtów.

#### 4. Testy generate

**File**: `src/test/java/com/kenez92/plateplan/plan/service/DietGeneratorTest.java`, `src/test/java/com/kenez92/plateplan/plan/service/PlanServiceTest.java` oraz fixture’y w `PlanControllerTest`, `PlanPdfWriterTest`, `PlanDownloadE2eTest`

**Intent**: Testy łapią jedno wywołanie, kształt promptu i odmowę, a stare konstruktory `DietPlan` nie zostają w połowie suite.

**Contract**: Stub `.entity(WeekPlan.class)`. Prompt zawiera siedem dni, dzienny cel, unikalne nazwy śniadania, obiadu i kolacji, prośbę żeby drugie śniadanie nie wracało bez przerwy, oraz listę zsumowaną na tydzień, i nie zawiera wieku, wzrostu, wagi, płci ani loginu. Tydzień odrzucony przez walidator daje `unavailable` bez drugiej próby. Tydzień z tym samym drugim śniadaniem siedem razy, przyjęty przez walidator, daje sukces. `PlanService` nadal rozróżnia brak profilu, brak kcal i sukces. Aserty obiektów przez porównanie całego obiektu.

### Success Criteria:

#### Automated Verification:

- `.\gradlew.bat test --tests com.kenez92.plateplan.plan.service.DietGeneratorTest` przechodzi: jedno `entity(WeekPlan.class)`, prompt na siedem dni, zły tydzień to `unavailable`
- `.\gradlew.bat test --tests com.kenez92.plateplan.plan.service.PlanServiceTest` przechodzi: brak profilu, brak kcal i sukces z tygodniem
- `.\gradlew.bat test` przechodzi

**Implementation Note**: Po zielonym suite zatrzymaj się, zanim ruszysz łamanie PDF.

---

## Phase 3: PDF diety i listy

### Overview

Dieta dostaje tytuł dnia i start strony. Lista zostaje jedną zsumowaną listą na tydzień.

### Changes Required:

#### 1. Tytuł dnia i nowa strona

**File**: `src/main/java/com/kenez92/plateplan/plan/service/PlanPdfWriter.java`

**Intent**: Czytelnik widzi jeden dzień od początku strony, a długi dzień nie wciąga następnego na tę samą stronę.

**Contract**: Dzień 1 to `LocalDate.now(Europe/Warsaw).plusDays(1)`, dalej kolejne daty, format `d.MM.yyyy`. Tytuł to `Dzień N · data` ze znakiem `·` jak w istniejących makro. `N` liczone od 1. Pod tytułem „Cel dnia” to przekazany dzienny cel, nie suma posiłków, oraz makro z czterech posiłków tego dnia. Przed każdym dniem poza pierwszym jest nierysowany znacznik końca strony z Critical Implementation Details. Nadmiar dnia leci na kolejne strony tym samym `PageAcc`.

#### 2. Lista tygodnia

**File**: `src/main/java/com/kenez92/plateplan/plan/service/PlanPdfWriter.java`

**Intent**: Zakupy na siedem dni są jedną listą, którą model już zsumował.

**Contract**: Tytuł zostaje `Lista zakupów`. Pod nim punktory `shoppingList` tygodnia, bez sekcji dni i bez daty. Writer nie parsuje ilości.

#### 3. Testy PDF

**File**: `src/test/java/com/kenez92/plateplan/plan/service/PlanPdfWriterTest.java`

**Intent**: Tytuły, daty, pasmo stron i lista są sprawdzone na wygenerowanych bajtach.

**Contract**: Krótki tydzień daje siedem stron. Strona dnia 1 zawiera `Dzień 1 ·` i jutrzejszą datę Warsaw; dzień 7 zawiera `Dzień 7 ·` i datę o sześć dni późniejszą. „Cel dnia” równa się przekazanemu celowi, nie sumie posiłków. Dzień z przygotowaniem dłuższym niż wysokość strony zwiększa liczbę stron ponad siedem, a tytuł następnego dnia nie leży na pierwszej stronie poprzedniego. Lista zawiera tytuł i pozycje tygodnia raz.

### Success Criteria:

#### Automated Verification:

- `.\gradlew.bat test --tests com.kenez92.plateplan.plan.service.PlanPdfWriterTest` przechodzi: tytuły `Dzień 1`–`Dzień 7`, daty od jutra, cel z profilu, jedna lista
- Ten sam test przechodzi dla krótkiego tygodnia na siedmiu stronach i dla dnia, który wylewa się tak, że następny dzień zaczyna późniejszą stronę
- `.\gradlew.bat test` przechodzi

#### Manual Verification:

- Otworzyć PDF diety z generate albo z testowego fixture’u i sprawdzić, że dzień zaczyna stronę, a długi dzień nie miesza się z następnym

**Implementation Note**: Po zielonym suite i po ręcznym spojrzeniu na PDF zatrzymaj się, zanim zmienisz napisy na stronie i w mailu.

---

## Phase 4: Napisy, mail i dokumenty

### Overview

To, co widzi użytkownik obok PDF-ów, przestaje mówić „na jutro”. Kontrakt HTTP zostaje: dwa pola, opcjonalny mail tych samych bajtów.

### Changes Required:

#### 1. Ekran i nazwa pliku

**File**: `src/main/resources/templates/plan.html`

**Intent**: Pobrany plik i nagłówek strony nazywają tydzień.

**Contract**: `download` diety to `dieta-na-tydzien.pdf`. Lista zostaje `lista-zakupow.pdf`. Przyciski zostają „Pobierz plan” i „Pobierz listę zakupów”. Eyebrow to „Na 7 dni”. Meta i lead mówią o siedmiu dniach i o czterech posiłkach dnia. `plan-download.js` nie niesie nazwy pliku; nie dodawać jej tam.

#### 2. Mail

**File**: `src/main/java/com/kenez92/plateplan/plan/service/PlanMailSender.java`

**Intent**: Załączniki nazywają się tak jak pobierane pliki, a temat nie obiecuje jutra.

**Contract**: Temat `Dieta na 7 dni i lista zakupów`. Treść mówi o jadłospisie na 7 dni i o liście zakupów. Załączniki: `dieta-na-tydzien.pdf` i `lista-zakupow.pdf`. Nadal te same bajty co przyciski. Porażka maila zostaje `emailError` i nie zabiera PDF-ów. Adresu, hasła i bajtów się nie loguje.

#### 3. Dokumenty produktu

**File**: `AGENTS.md`, `context/foundation/prd.md`

**Intent**: Opis produktu i reguły agenta mówią o siedmiu dniach jednej osoby.

**Contract**: FR-006 i akapit wyniku w Business Logic: dieta i lista na siedem dni od jutra, plików się nie zapisuje. Non-Goals: wiele osób nadal później. Sekcja Plan i twarde reguły w `AGENTS.md`: siedem dni, pasmo ±10%, unikalne nazwy śniadania, obiadu i kolacji, drugie śniadanie może się powtórzyć a Java tego nie odrzuca, `dieta-na-tydzien.pdf`, jedna lista zsumowana przez model, dzień zaczyna stronę. Rodzina zostaje późniejszą zmianą.

#### 4. Testy napisów

**File**: testy `PlanController`, `PlanMailSender` i e2e, które trzymają `dieta-na-jutro.pdf` albo „Na jutro”

**Intent**: Suite nie broni starej nazwy.

**Contract**: Aserty nazw plików, tematu maila i kopii strony oczekują `dieta-na-tydzien.pdf`, `lista-zakupow.pdf` i „7 dni”. JSON sukcesu zostaje `dietPdf` i `shoppingListPdf`.

### Success Criteria:

#### Automated Verification:

- `.\gradlew.bat test --tests com.kenez92.plateplan.plan.controller.PlanControllerTest` i testy maila przechodzą z `dieta-na-tydzien.pdf`, `lista-zakupow.pdf` i kopią o 7 dniach
- `.\gradlew.bat test` przechodzi

#### Manual Verification:

- Na `/plan` po generate przycisk planu pobiera `dieta-na-tydzien.pdf`, a eyebrow nie mówi „Na jutro”
- Przy adresie na koncie wysyłka załącza te dwie nazwy, a temat mówi o 7 dniach
- FR-006 i sekcja Plan w `AGENTS.md` opisują siedem dni jednej osoby i nadal mówią, że plików się nie zapisuje

**Implementation Note**: Po zielonym suite sprawdź ekran i mail ręcznie. Ta faza domyka zmianę.

---

## Testing Strategy

### Unit Tests:

- `WeekPlanValidatorTest`: pasmo 2200 i 1999, powtórka śniadania z obiadem, siedem razy to samo drugie śniadanie, jogurt jako śniadanie i jako drugie śniadanie, pusta nazwa, 6 i 8 dni, pusta lista zakupów.
- `DietGeneratorTest`: jedno `entity(WeekPlan.class)`, treść promptu, odmowa bez drugiej próby, brak danych ciała w prompcie.
- `PlanServiceTest`: brak profilu, brak kcal, sukces.
- `PlanPdfWriterTest`: tytuły, daty Warsaw, cel niebędący sumą posiłków, siedem stron dla krótkiego tygodnia, wylanie długiego dnia, jedna lista.

### Integration Tests:

- `PlanControllerTest`: JSON nadal dwa PDF-e albo `error`; `emailError` nie zabiera plików.
- Test maila: temat, treść i nazwy załączników.
- E2E, jeśli trzyma nazwę pliku, aktualizuje ją razem z kontrolerem. Żywego Ollama nie wołamy.

### Manual Testing Steps:

1. Wejść na `/plan` z profilem i celem kcal. Wygenerować plan. Pobrać `dieta-na-tydzien.pdf` i sprawdzić siedem tytułów od jutra.
2. Sprawdzić, że długi dzień schodzi na drugą stronę, a kolejny dzień zaczyna następną.
3. Otworzyć listę i sprawdzić, że nie ma siedmiu sekcji dni.
4. Z adresem na koncie zaznaczyć wysyłkę i sprawdzić nazwy załączników oraz temat.

## Performance Considerations

Zostaje jedno wywołanie i timeout 120 s. Odpowiedź jest kilka razy większa niż jeden dzień, więc `UNAVAILABLE` po czasie albo po złym JSON będzie częstsze. Drugiej próby nie ma. Walidator jest lokalny i nie dokłada czasu sieci.

## Migration Notes

Brak migracji. `confirmed_calories` zostaje jedną dzienną liczbą. Planu nie zapisujemy.

## References

- Frame: `context/changes/seven-day-plan/frame.md`
- Related research: brak `research.md`
- Similar implementation: `src/main/java/com/kenez92/plateplan/plan/service/DietGenerator.java`, `src/main/java/com/kenez92/plateplan/plan/service/PlanPdfWriter.java`, `context/changes/email-plan-on-generate/plan.md`

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands. Do not rename step titles. See `references/progress-format.md`.

### Phase 1: Kształt tygodnia i odmowa

#### Automated

- [x] 1.1 `.\gradlew.bat test --tests com.kenez92.plateplan.plan.validator.WeekPlanValidatorTest` przechodzi na paśmie, powtórce nazwy i długości tygodnia — b26ac42
- [x] 1.2 `.\gradlew.bat test` przechodzi — b26ac42

### Phase 2: Jedno wywołanie modelu

#### Automated

- [x] 2.1 `.\gradlew.bat test --tests com.kenez92.plateplan.plan.service.DietGeneratorTest` przechodzi: jedno `entity(WeekPlan.class)`, prompt na siedem dni, zły tydzień to `unavailable` — b26ac42
- [x] 2.2 `.\gradlew.bat test --tests com.kenez92.plateplan.plan.service.PlanServiceTest` przechodzi: brak profilu, brak kcal i sukces z tygodniem — b26ac42
- [x] 2.3 `.\gradlew.bat test` przechodzi — b26ac42

### Phase 3: PDF diety i listy

#### Automated

- [x] 3.1 `.\gradlew.bat test --tests com.kenez92.plateplan.plan.service.PlanPdfWriterTest` przechodzi: tytuły `Dzień 1`–`Dzień 7`, daty od jutra, cel z profilu, jedna lista — b26ac42
- [x] 3.2 Ten sam test przechodzi dla krótkiego tygodnia na siedmiu stronach i dla dnia, który wylewa się tak, że następny dzień zaczyna późniejszą stronę — b26ac42
- [x] 3.3 `.\gradlew.bat test` przechodzi — b26ac42

#### Manual

- [x] 3.4 Otworzyć PDF diety z generate albo z testowego fixture’u i sprawdzić, że dzień zaczyna stronę, a długi dzień nie miesza się z następnym — b26ac42

### Phase 4: Napisy, mail i dokumenty

#### Automated

- [x] 4.1 `.\gradlew.bat test --tests com.kenez92.plateplan.plan.controller.PlanControllerTest` i testy maila przechodzą z `dieta-na-tydzien.pdf`, `lista-zakupow.pdf` i kopią o 7 dniach — b26ac42
- [x] 4.2 `.\gradlew.bat test` przechodzi — b26ac42

#### Manual

- [x] 4.3 Na `/plan` po generate przycisk planu pobiera `dieta-na-tydzien.pdf`, a eyebrow nie mówi „Na jutro” — b26ac42
- [x] 4.4 Przy adresie na koncie wysyłka załącza te dwie nazwy, a temat mówi o 7 dniach — b26ac42
- [x] 4.5 FR-006 i sekcja Plan w `AGENTS.md` opisują siedem dni jednej osoby i nadal mówią, że plików się nie zapisuje — b26ac42
