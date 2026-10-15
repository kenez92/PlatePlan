# Domownicy w planie tygodnia — Implementation Plan

## Overview

Jedno konto, bez ról i bez drugiego logowania, trzyma właściciela i najwyżej trzy dodane osoby. Każda ma własne ciało, jeden z trzech celów i własne listy produktów. Jedno `POST /plan/generate` układa wspólne dania na siedem dni od jutra, z osobną porcją dla każdej zaznaczonej osoby, i jedną listę zakupów. Zostają dwa PDF-y.

## Current State Analysis

Profil to jeden wiersz `user_profile` z kluczem `login` (`003-create-user-profile.xml:15-39`, `UserProfile.java:26-61`). Nie ma nazwy wyświetlanej. Wiek to 10–110, wzrost 80–250 cm, waga 20,0–400,0 kg (`ProfileFormDto.java:31-42`). Cele to `LOSE_WEIGHT`, `MAINTAIN`, `GAIN` (`Goal.java:6-10`). Liczbę liczy Mifflin-St Jeor razy aktywność, potem −250 / 0 / +250, half-up do pełnej kilokalorii (`CalorieService.java:35-48`). Ręczna edycja przyjmuje 800–6000 (`DailyCaloriesForm.java:13-14`). Pierwszy zapis ciała wstawia wynik wzoru bez tego zakresu. Późniejszy zapis ciała nie przelicza kcal. Osobny przelicznik nadpisuje tylko `confirmed_calories`.

Generate czyta ten jeden wiersz (`PlanService.java:39-59`). Prompt dostaje jeden cel, jedną liczbę kcal i dwie listy (`DietGenerator.java:39-68, 82-92`). Model nie dostaje wieku, wzrostu, wagi, płci, aktywności, loginu ani e-maila. `WeekPlan` to siedem `DayMeals` i jedna lista zakupów (`WeekPlan.java:9-10`). Posiłek ma jedną liczbę kcal (`Meal.java:10-16`). Walidator pilnuje pasma ±10% tej jednej liczby i unikalnych nazw śniadania, obiadu i kolacji; drugie śniadanie może się powtórzyć (`WeekPlanValidator.java:29-78`). PDF zaczyna dzień stroną i drukuje jeden `Cel dnia: N kcal` (`PlanPdfWriter.java:37-38, 101-120`). Ekran `/plan` nie wybiera osób. Sukces JSON to `dietPdf` i `shoppingListPdf`. Plików się nie zapisuje.

## Desired End State

Właściciel z profilem dodaje na `/profile` najwyżej trzy osoby. Każda ma obowiązkową nazwę, te same pola ciała co właściciel i własne kcal. Właściciel może zapisać nazwę; pusta zostaje w planie jako `Ja`. Nazwy na koncie nie powtarzają się bez względu na wielkość liter. Granice ciała, także właściciela: wiek 2–110, wzrost 70–250 cm, waga 8,0–400,0 kg. Wzór i ręczna edycja przyjmują 400–6000 kcal. Wynik wzoru poza tym zakresem odrzuca cały zapis ciała albo przeliczenie i zostawia poprzednią liczbę.

Na `/plan` każda osoba startuje zaznaczona. Można odznaczyć, zostaje co najmniej jedna. Generate układa jedno danie na posiłek i porcję każdej zaznaczonej osoby. Suma czterech porcji osoby mieści się w ±10% jej własnych kcal. Wykluczenie którejkolwiek zaznaczonej osoby usuwa produkt z dań i z listy zakupów. Odznaczona osoba nie wnosi porcji, preferencji ani wykluczeń. Dwa pliki zostają: `dieta-na-tydzien.pdf` i `lista-zakupow.pdf`. Dzień nadal zaczyna stronę. Na stronie widać cel i makro każdej zaznaczonej osoby.

### Key Discoveries:

- Jeden login to jeden profil i jeden e-mail (`UserProfile.java:26-61`). Domownik nie dostaje konta ani adresu.
- Skrypt list produktów wiąże każdy `[data-product-list]`, nie jeden formularz (`profile-products.js:7, 27`).
- `PlanResult` niesie jedną liczbę kcal (`PlanResult.java:8-16`). PDF dostaje ją obok tygodnia (`PlanPdfWriter.java:101-106`).
- Błąd generate to `PROFILE_REQUIRED`, `CALORIES_REQUIRED` albo `UNAVAILABLE` (`PlanFilesDto.java:15-17`). Pusta selekcja nie mieści się w żadnym z nich.
- `PageAcc` łamie stronę tylko znacznikiem albo brakiem miejsca (`PlanPdfWriter.java:116-120, 410-416`).

## What We're NOT Doing

- Czwarty cel i osobna matematyka „prawidłowego rozwoju”. Rozwój dziecka to wybór `MAINTAIN` albo `GAIN`.
- Osobny jadłospis albo osobny plik diety na osobę. ZIP zostaje poza zakresem.
- Drugie konto, rola, hasło albo e-mail domownika.
- Zapamiętanie odznaczonych osób między wizytami. Wejście na `/plan` zaznacza wszystkich.
- Wykluczenia i preferencje osoby odznaczonej w tym generate.
- Piąta osoba na koncie.
- Zapis PDF-ów, druga próba modelu, job w tle.
- Liczenie sumy składników w Javie. Sumę listy zakupów robi model.
- Zmiana wzoru Mifflin-St Jeor, mnożników aktywności albo korekty ±250.

## Implementation Approach

Właściciel zostaje wierszem `user_profile` z dopisaną opcjonalną nazwą. Dodane osoby są wierszami nowej tabeli przy tym samym loginie, najwyżej trzy. Zapis każdej osoby jest osobnym żądaniem: błąd dziecka nie zapisuje właściciela ani żony.

Generate ładuje właściciela i domowników, zostawia zaznaczonych i scala ich listy. Prompt dostaje skuteczną nazwę, instrukcję celu i kcal każdej zaznaczonej osoby oraz jedną listę preferencji i jedną wykluczeń. Ciała, loginu i e-maila nie wysyła. Model zwraca tydzień, w którym posiłek ma wspólną nazwę i przygotowanie oraz porcję na osobę. Walidator odrzuca tydzień, gdy którejkolwiek zaznaczonej osoby brakuje, porcji jest za dużo albo jej dzień wypada z jej pasma. Odmowa zostaje `UNAVAILABLE`, bez drugiej próby.

Jedna osoba bez domowników ma jedną porcję i to samo pasmo co dziś. Brak parametru `person` oznacza wszystkich zapisanych. Pliki i mail się nie mnożą.

## Critical Implementation Details

- **Pasmo osoby.** Delta to 10% jej `confirmed_calories`, half-up do pełnej kilokalorii, jak dziś. Suma kcal jej czterech porcji mieści się w `[cel - delta, cel + delta]`. Cel 795 daje deltę 80 i przedział 715–875 włącznie. Cel 400 daje 360–440. Cel 6000 daje 5400–6600. Składników Java nie przelicza.
- **Nazwa skuteczna.** Pusty zapis nazwy właściciela jest w planie i w unikalności tekstem `Ja`. Porównanie to `strip` i `toLowerCase(Locale.ROOT)`. Dopóki właściciel nie ma nazwy, domownik `ja` się nie zapisuje. Gdy właściciel zapisze `Daniel`, `Ja` wolno nadać domownikowi. `anna` i `Anna` to jedna nazwa.
- **Zakres 400–6000 przy wzorze.** Dziś pierwszy zapis ciała wstawia wynik wzoru także poza 800–6000. Po tej zmianie wynik 399, 166 i 9837 odrzuca cały zapis ciała albo przeliczenie i nie rusza wiersza. 400, 795 i 6000 przechodzą. Późniejszy zapis ciała nadal nie przelicza kcal.
- **Selekcja.** Brak parametru `person` bierze wszystkich. Parametr obecny i pusty albo z id spoza tego konta daje `SELECTION_REQUIRED`, bez PDF-ów i bez czytania cudzego wiersza. Zaznaczona osoba bez kcal daje `CALORIES_REQUIRED`. Odznaczona osoba bez kcal nie blokuje. Wykluczenie odznaczonej osoby nie wchodzi do scalenia.
- **Kompilacja porcji.** `Meal` traci jedną liczbę kcal na rzecz listy porcji. Writer musi w tej samej fazie czytać porcje, inaczej build stoi. Pełna linia celu każdej osoby jest w fazie PDF; do tego czasu writer ma się kompilować na porcjach.

## Phase 1: Zapis osób

### Overview

Powstaje tabela domowników, opcjonalna nazwa właściciela oraz reguły granic, nazw, limitu i zakresu kcal. Ekran jeszcze ich nie pokazuje.

### Changes Required:

#### 1. Schemat

**File**: `src/main/resources/db/changelog/changes/006-household-members.xml`

**Intent**: Właściciel dostaje miejsce na nazwę, a dodane osoby własny wiersz, bez ruszania zastosowanych changeSetów.

**Contract**: Jeden nowy plik i jeden changeSet. Na `user_profile` dopisać nullable `display_name` `varchar(40)`. Tabela `household_member`: `member_id` jako identity i klucz główny, `login` `varchar(50)` bez klucza obcego, `display_name` `varchar(40)` not null, oraz te same kolumny ciała, celu, aktywności, `confirmed_calories` i dwóch list produktów co `user_profile`, bez `email`. Indeks po `login`. Puste listy produktów zostają null, jak na profilu.

#### 2. Nazwa i zakres kcal właściciela

**File**: `src/main/java/com/kenez92/plateplan/profile/controller/dto/ProfileFormDto.java`, `src/main/java/com/kenez92/plateplan/profile/controller/dto/DailyCaloriesForm.java`, `src/main/java/com/kenez92/plateplan/profile/db/UserProfile.java`, `src/main/java/com/kenez92/plateplan/profile/service/ProfileService.java`, `src/main/java/com/kenez92/plateplan/profile/service/ConfirmedCaloriesService.java`

**Intent**: Właściciel korzysta z szerszych granic ciała i z jednego zakresu kcal dla wzoru oraz ręcznej edycji. Pusta nazwa nie jest błędem.

**Contract**: Wiek 2–110, wzrost 70–250, waga 8,0–400,0 z jednym miejscem po przecinku. `DailyCaloriesForm` przyjmuje 400–6000. Opcjonalna nazwa: pusta staje się null; niepusta ma 2–40 znaków, jest już obcięta i w NFC, bez ciągu zwykłych spacji, bez znaków sterujących, formatujących i spacji innej niż zwykła. Średnik w nazwie wolno. Pierwszy zapis ciała odrzuca wynik wzoru poza 400–6000 i nic nie zapisuje. Przeliczenie poza zakresem nic nie zapisuje i zostawia poprzednią liczbę. Późniejszy zapis ciała nadal nie bierze kcal z klienta. Nazwa jedzie w tym samym zapisie ciała.

#### 3. Domownik

**File**: `src/main/java/com/kenez92/plateplan/profile/db/HouseholdMember.java`, `src/main/java/com/kenez92/plateplan/profile/db/HouseholdMemberRepository.java`, `src/main/java/com/kenez92/plateplan/profile/service/HouseholdMemberService.java`

**Intent**: Dodana osoba ma te same reguły ciała, celu, list i kcal co właściciel, własną nazwę i limit miejsc.

**Contract**: Repozytorium ma `@Repository`. Zapis domownika wymaga istniejącego wiersza właściciela. Brak wiersza właściciela nic nie tworzy. Nazwa domownika jest obowiązkowa i spełnia tę samą regułę znaków co nazwa właściciela. Czwarty domownik (piąta osoba na koncie) się nie zapisuje, wcześniejsze wiersze zostają. Unikalność obejmuje skuteczną nazwę właściciela i nazwy domowników tego loginu. Konflikt odrzuca tylko ten zapis. Pierwszy zapis wstawia wzór, gdy wynik jest w 400–6000. Późniejszy zapis ciała zostawia `confirmed_calories`. Usunięcie kasuje tylko wiersz tego loginu. Id innego konta nie kasuje nic i nie zwraca jego danych. Kolejność odczytu to `member_id` rosnąco.

#### 4. Testy zapisu

**File**: `src/test/java/com/kenez92/plateplan/profile/service/HouseholdMemberServiceTest.java`, testy `ProfileService` i `ConfirmedCaloriesService`

**Intent**: Reguły z wywiadu są w serwisie, zanim powstanie ekran.

**Contract**: Testy bez kontekstu Springa, metody `should…`, lokalne `final`, porównanie całych obiektów. Przypadki: wiek 2 i waga 8,0 przechodzą; wiek 1 i waga 7,9 odpadają; wzór 795 zapisuje się; wzór 166 i 9837 nie tworzą wiersza; przeliczenie na 166 zostawia poprzednią liczbę; ręczna 400 i 6000 przechodzą, 399 odpada; `ja` przy pustej nazwie właściciela odpada; po nazwie właściciela `Daniel` domownik `Ja` przechodzi; czwarty domownik odpada i trzy istniejące zostają; zapis dziecka nie zmienia wiersza właściciela.

### Success Criteria:

#### Automated Verification:

- `.\gradlew.bat test --tests com.kenez92.plateplan.profile.service.HouseholdMemberServiceTest --tests com.kenez92.plateplan.profile.service.ProfileServiceTest --tests com.kenez92.plateplan.profile.service.ConfirmedCaloriesServiceTest` przechodzi dla granic ciała, zakresu 400–6000, unikalnej nazwy i limitu trzech domowników
- `.\gradlew.bat test` przechodzi

**Implementation Note**: Po zielonym suite zatrzymaj się, zanim ruszysz ekran.

---

## Phase 2: Ekran profilu

### Overview

`/profile` pokazuje właściciela i sekcję dodanych osób. Każdy formularz zapisuje się sam.

### Changes Required:

#### 1. Żądania domownika

**File**: `src/main/java/com/kenez92/plateplan/profile/controller/ProfileController.java`

**Intent**: Dziecko z błędem nie nadpisuje właściciela ani innej osoby, a id spoza konta nic nie ujawnia.

**Contract**: Zostają `POST /profile`, `POST /profile/calories` i `POST /profile/recalculate`. Dochodzą zapis, korekta kcal, przeliczenie i usunięcie domownika pod ścieżką z jego `member_id`, tylko dla loginu z `Principal`. Błędny zapis wraca na `/profile` z komunikatem przy tej osobie i nie woła zapisu pozostałych. Brak wiersza i id innego konta dają ten sam komunikat braku osoby, bez 500 i bez cudzych pól. Baza niedostępna zostaje przy dotychczasowym komunikacie ekranu i HTTP 200.

#### 2. Formularze

**File**: `src/main/resources/templates/profile.html`, `src/main/resources/messages.properties`

**Intent**: Właściciel i najwyżej trzy osoby edytują się na jednym ekranie, każda ze swoimi kcal i listami.

**Contract**: Opcjonalne pole nazwy właściciela. Puste oznacza podpis `Ja`. Sekcja dodanych osób jest pod formularzem właściciela. Każda osoba ma własne ciało, cel, aktywność, listy i kcal: pierwszy zapis liczy wzór, osobna korekta i osobne przeliczenie. Listy używają `[data-product-list]`, żeby istniejący skrypt obsłużył więcej niż jedną parę. Komunikaty po polsku: nazwa zajęta, wynik poza 400–6000, najwyżej trzy dodane osoby, brak osoby. Usunięcie jest osobnym przyciskiem tej osoby.

#### 3. Test kontrolera

**File**: `src/test/java/com/kenez92/plateplan/profile/controller/ProfileControllerTest.java`

**Intent**: Ekran nie scala osób w jedno żądanie.

**Contract**: `@WebMvcTest(ProfileController.class)`. Odmowa domownika nie wywołuje zapisu właściciela. Czwarty domownik i nazwa `ja` przy pustej nazwie właściciela zostają na stronie z komunikatem. Id innego loginu nie jest argumentem zapisu ani usunięcia.

### Success Criteria:

#### Automated Verification:

- `.\gradlew.bat test --tests com.kenez92.plateplan.profile.controller.ProfileControllerTest` przechodzi: osobny zapis domownika, odmowa nazwy i kcal, błąd dziecka nie zmienia właściciela
- `.\gradlew.bat test` przechodzi

#### Manual Verification:

- Na `/profile` pusta nazwa właściciela zostawia go jako „Ja”, a zapis dziecka z nazwą „ja” pokazuje konflikt i nie zmienia właściciela
- Zapis ciała dziecka, które wychodzi na 795 kcal, pokazuje tę liczbę; zapis wychodzący na 166 kcal nic nie zapisuje

**Implementation Note**: Po zielonym suite i tym sprawdzeniu ekranu zatrzymaj się, zanim ruszysz generate.

---

## Phase 3: Generowanie dla zaznaczonych

### Overview

Model dostaje zaznaczone osoby i zwraca wspólne dania z porcjami. Zły tydzień nie staje się PDF-em. Ekran wyboru jest w następnej fazie.

### Changes Required:

#### 1. Porcja

**File**: `src/main/java/com/kenez92/plateplan/plan/model/Meal.java`, `src/main/java/com/kenez92/plateplan/plan/model/Portion.java`, `src/main/java/com/kenez92/plateplan/plan/service/PlanPdfWriter.java`

**Intent**: Jedno danie ma wspólną nazwę i przygotowanie, a kcal, makro i składniki należą do porcji osoby.

**Contract**: `Portion` ma skuteczną nazwę osoby, składniki, kcal oraz białko, węglowodany i tłuszcz w pełnych gramach. `Meal` ma nazwę, przygotowanie i listę porcji. `DayMeals` zostaje przy czterech posiłkach. `WeekPlan` zostaje przy siedmiu dniach i jednej liście. Writer w tej fazie czyta kcal z porcji, żeby moduł się budował. Linia celu każdej osoby dochodzi w fazie 4.

#### 2. Walidator

**File**: `src/main/java/com/kenez92/plateplan/plan/validator/WeekPlanValidator.java`

**Intent**: Każda zaznaczona osoba ma własne pasmo, a danie jest jedno.

**Contract**: Metoda przyjmuje tydzień i zaznaczone osoby (skuteczna nazwa oraz jej kcal). Przyjęty tydzień ma 7 dni. Każdy posiłek ma niepustą nazwę i dokładnie po jednej porcji na każdą zaznaczoną nazwę, bez dodatkowych porcji. Suma czterech porcji osoby jest w jej paśmie. Klucze nazw śniadania, obiadu i kolacji są unikalne w tygodniu. Powtórka drugiego śniadania jest przyjęta. Pusta lista zakupów jest przyjęta. Treści planu się nie loguje.

#### 3. Prompt i scalenie

**File**: `src/main/java/com/kenez92/plateplan/plan/service/DietGenerator.java`, `src/main/java/com/kenez92/plateplan/plan/service/PlanService.java`

**Intent**: Jedno wywołanie modelu układa garnek dla zaznaczonych osób i nikogo więcej.

**Contract**: `PlanService` ładuje właściciela i domowników w kolejności: właściciel, potem `member_id` rosnąco. Brak wiersza właściciela to `noProfile()`. Pusta lista osób przekazana do serwisu to osobny wynik braku wyboru. Brak parametru HTTP rozwija się do wszystkich dopiero w fazie 4. Zaznaczona osoba bez kcal to `noCalories()`, także gdy inne osoby kcal mają. Odznaczona bez kcal nie blokuje. Do modelu idą, po kolei, skuteczna nazwa, dotychczasowa angielska instrukcja celu i kcal. Potem jedna lista preferencji i jedna wykluczeń: suma zaznaczonych, pierwsze zapisane brzmienie zostaje, porównanie bez wielkości liter, nazwa wykluczona przez kogokolwiek zaznaczonego wypada także z preferencji. Wieku, wzrostu, wagi, płci, aktywności, loginu, e-maila i list osób odznaczonych prompt nie zawiera. Jedno `.entity(WeekPlan.class)`. Odmowa walidatora, pusty wynik, brak klucza, timeout i błąd bazy to `unavailable()`, bez drugiej próby. Log bez promptu, list, kcal, celu, nazw i bajtów.

### Success Criteria:

#### Automated Verification:

- `.\gradlew.bat test --tests com.kenez92.plateplan.plan.validator.WeekPlanValidatorTest` przechodzi: pasmo każdej zaznaczonej osoby, brak porcji i powtórka nazwy dania
- `.\gradlew.bat test --tests com.kenez92.plateplan.plan.service.DietGeneratorTest --tests com.kenez92.plateplan.plan.service.PlanServiceTest` przechodzi: prompt bez ciała i e-maila, pusta selekcja to brak wyboru, odznaczona osoba nie wnosi wykluczenia
- `.\gradlew.bat test` przechodzi

**Implementation Note**: Po zielonym suite zatrzymaj się, zanim ruszysz checkboxy i linie celu w PDF.

---

## Phase 4: Ekran planu i PDF

### Overview

`/plan` wybiera osoby, PDF rozpisuje ich cele, a mail nadal niesie te same dwa pliki.

### Changes Required:

#### 1. Wybór osób

**File**: `src/main/java/com/kenez92/plateplan/plan/controller/PlanController.java`, `src/main/java/com/kenez92/plateplan/plan/controller/dto/PlanFilesDto.java`, `src/main/resources/templates/plan.html`, `src/main/resources/static/js/plan-download.js`

**Intent**: Generate obejmuje tylko zaznaczonych, a żądanie bez listy osób obejmuje cały zapisany dom.

**Contract**: GET `/plan` dodaje osoby w kolejności właściciel, potem `member_id`, bez adresu e-mail. Checkboxy są zaznaczone, bez pamięci poprzedniej wizyty. Wartość właściciela to stały token `owner`, domownika to jego id. POST przyjmuje powtarzany parametr `person` obok dotychczasowego `sendEmail`. Brak parametru oznacza wszystkich. Pusty parametr albo id spoza konta daje HTTP 200 i `error` = `SELECTION_REQUIRED`, bez PDF-ów. Zaznaczenie nikogo w przeglądarce nie woła generate i pokazuje prośbę o co najmniej jedną osobę. `data-msg-selection` niesie tę prośbę. Dotychczasowe trzy kody błędów zostają.

#### 2. PDF

**File**: `src/main/java/com/kenez92/plateplan/plan/service/PlanPdfWriter.java`, `src/main/java/com/kenez92/plateplan/plan/model/PlanResult.java`

**Intent**: Strona dnia mówi, ile ma zjeść każda zaznaczona osoba, a nie jedną sumę domu.

**Contract**: Tytuł zostaje `Dzień N`. Dzień od drugiego włącznie zaczyna stronę tym samym znacznikiem co dziś. Pod tytułem, w kolejności zaznaczenia, jest linia `Cel dnia — <nazwa>: <kcal> kcal` z liczbą z konta, nie z modelu, oraz linia makro tej osoby zsumowana z jej czterech porcji, w dotychczasowym brzmieniu białka, węglowodanów i tłuszczu. Przy daniu: wspólna nazwa, potem każda porcja (nazwa, kcal, makro, składniki), potem jedno przygotowanie. Wylanie dnia na kolejną stronę nie przesuwa startu następnego dnia. Nazwy plików zostają `dieta-na-tydzien.pdf` i `lista-zakupow.pdf`.

#### 3. Mail

**File**: `src/main/java/com/kenez92/plateplan/plan/service/PlanMailSender.java`

**Intent**: Wysyłka nadal dokłada te same dwa pliki do pobrania, na adres właściciela.

**Contract**: Załączniki i temat bez zmian względem siedmiu dni. Porażka maila zostaje `emailError` = `UNAVAILABLE` obok PDF-ów. Adresu, hasła i bajtów się nie loguje. Domownik nie ma adresu.

### Success Criteria:

#### Automated Verification:

- `.\gradlew.bat test --tests com.kenez92.plateplan.plan.service.PlanPdfWriterTest` przechodzi: dzień zaczyna stronę, a cel i makro są wypisane osobno dla każdej zaznaczonej osoby
- `.\gradlew.bat test --tests com.kenez92.plateplan.plan.controller.PlanControllerTest` przechodzi: brak parametru `person` bierze wszystkich, nieznane id zwraca `SELECTION_REQUIRED` bez PDF-ów
- `.\gradlew.bat test` przechodzi

#### Manual Verification:

- Na `/plan` checkboxy startują zaznaczone, odznaczenie jednej osoby nie dokłada jej porcji do pobranego PDF
- Przy adresie na koncie wysyłka załącza `dieta-na-tydzien.pdf` i `lista-zakupow.pdf`

**Implementation Note**: Po zielonym suite i sprawdzeniu pobrania zatrzymaj się, zanim ruszysz dokumenty produktu.

---

## Phase 5: Opis produktu

### Overview

PRD, reguły agenta i roadmapa opisują dom na jednym koncie zamiast odkładać wiele osób.

### Changes Required:

#### 1. Wymagania

**File**: `context/foundation/prd.md`

**Intent**: Wiele osób na jednym koncie wchodzi do tej wersji i przestaje być non-goalem.

**Contract**: Persona i Non-Goals: jedno konto, bez ról, z właścicielem i najwyżej trzema dodanymi osobami. Business Logic: ten sam wzór i korekta ±250, granice wieku 2–110, wzrostu 70–250 cm i wagi 8,0–400,0 kg, potwierdzone kcal 400–6000, wspólne dania i porcje, pasmo ±10% na osobę, wykluczenie zaznaczonej osoby obowiązuje garnek. Plików się nie zapisuje. FR-006 zostaje przy dwóch PDF-ach na siedem dni od jutra.

#### 2. Reguły agenta i roadmapa

**File**: `AGENTS.md`, `context/foundation/roadmap.md`

**Intent**: Kolejna implementacja nie wraca do jednego jedzącego ani do progu 800.

**Contract**: Twarde reguły i sekcja Plan: najwyżej cztery osoby, nazwa, selekcja, porcje, scalenie list tylko z zaznaczonych, zakres 400–6000, dwa PDF-y, brak zapisu plików, model bez ciała i e-maila. Sekcja Profile: szersze granice i nazwa. Z Parked schodzi „Support for multiple people”. Zostaje jedno konto i brak ról. Pozostałe punkty Parked bez zmian.

### Success Criteria:

#### Automated Verification:

- `.\gradlew.bat test` przechodzi

#### Manual Verification:

- PRD opisuje domowników na jednym koncie i nie trzyma wielu osób w Non-Goals
- `AGENTS.md` opisuje porcje, zakres 400–6000 i najwyżej cztery osoby, a Parked w roadmapie nie zostawia wielu osób jako późniejszego non-goala

**Implementation Note**: Ta faza zamyka opis. Nie dodawaj piątej osoby ani czwartego celu przy poprawkach dokumentów.

---

## Testing Strategy

### Unit Tests:

- Serwis domownika: granice, wzór 795 przeciw 166, unikalność `Ja`, limit trzech, izolacja zapisu właściciela.
- Walidator tygodnia: pasmo 795 → 715–875, brak porcji osoby, dodatkowa porcja, powtórka obiadu, powtórka drugiego śniadania.
- Generator: prompt zawiera nazwy, cele i kcal zaznaczonych, nie zawiera wieku, wzrostu, wagi, płci, aktywności, loginu ani e-maila. Wykluczenie odznaczonej osoby nie wchodzi do tekstu.

### Integration Tests:

- `@WebMvcTest(ProfileController.class)`: błąd dziecka nie woła zapisu właściciela; cudze id nie jest argumentem.
- `@WebMvcTest(PlanController.class)`: brak `person` bierze wszystkich; obce id daje `SELECTION_REQUIRED` bez PDF-ów; mail nadal dokłada dwa pliki albo `emailError`.

### Manual Testing Steps:

1. Zapisać właściciela bez nazwy i dodać osobę `Ja`. Zapis ma się nie udać, a właściciel zostaje.
2. Zapisać dziecko o wzorze 795 kcal i zobaczyć tę liczbę. Zmienić ciało tak, by wzór dał 166, i sprawdzić, że przeliczenie zostawia 795.
3. Dodać drugą osobę z wykluczeniem produktu, który właściciel ma w preferencjach. Wygenerować plan dla obu i sprawdzić, że produktu nie ma w PDF. Odznaczyć tę osobę i wygenerować ponownie.

## Performance Considerations

Jedno wywołanie modelu na najwyżej cztery osoby. Timeout czytania zostaje 120 s. Częstsza odmowa walidatora przy czterech osobach kończy się `UNAVAILABLE`, bez drugiej próby i bez joba. Listy produktów pozostają przy dotychczasowym limicie 50 nazw na osobę.

## Migration Notes

Istniejące wiersze `user_profile` dostają null w `display_name` i dalej działają jako `Ja`. Nikt nie jest przenoszony do `household_member`. Stare PDF-y nie istnieją w bazie. Po wdrożeniu generate jednej osoby oczekuje porcji, nie pojedynczego kcal przy posiłku; zła odpowiedź modelu to `UNAVAILABLE`. Próg ręcznych kcal schodzi z 800 na 400. Zapisany wynik wzoru poniżej 400 albo powyżej 6000, jeśli taki jest, zostaje do przeliczenia; przeliczenie poza zakresem go nie nadpisuje.

## References

- Podobny zapis profilu: `src/main/java/com/kenez92/plateplan/profile/service/ProfileService.java`
- Tydzień i pasmo: `src/main/java/com/kenez92/plateplan/plan/validator/WeekPlanValidator.java:29-78`
- Prompt jednej osoby: `src/main/java/com/kenez92/plateplan/plan/service/DietGenerator.java:39-68`
- Odłożona rodzina: `context/archive/2026-10-12-seven-day-plan/plan.md`

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands. Do not rename step titles. See `references/progress-format.md`.

### Phase 1: Zapis osób

#### Automated

- [x] 1.1 `.\gradlew.bat test --tests com.kenez92.plateplan.profile.service.HouseholdMemberServiceTest --tests com.kenez92.plateplan.profile.service.ProfileServiceTest --tests com.kenez92.plateplan.profile.service.ConfirmedCaloriesServiceTest` przechodzi dla granic ciała, zakresu 400–6000, unikalnej nazwy i limitu trzech domowników — d919872
- [x] 1.2 `.\gradlew.bat test` przechodzi — d919872

### Phase 2: Ekran profilu

#### Automated

- [x] 2.1 `.\gradlew.bat test --tests com.kenez92.plateplan.profile.controller.ProfileControllerTest` przechodzi: osobny zapis domownika, odmowa nazwy i kcal, błąd dziecka nie zmienia właściciela — 8e958ca
- [x] 2.2 `.\gradlew.bat test` przechodzi — 8e958ca

#### Manual

- [ ] 2.3 Na `/profile` pusta nazwa właściciela zostawia go jako „Ja”, a zapis dziecka z nazwą „ja” pokazuje konflikt i nie zmienia właściciela
- [ ] 2.4 Zapis ciała dziecka, które wychodzi na 795 kcal, pokazuje tę liczbę; zapis wychodzący na 166 kcal nic nie zapisuje

### Phase 3: Generowanie dla zaznaczonych

#### Automated

- [x] 3.1 `.\gradlew.bat test --tests com.kenez92.plateplan.plan.validator.WeekPlanValidatorTest` przechodzi: pasmo każdej zaznaczonej osoby, brak porcji i powtórka nazwy dania — 2432441
- [x] 3.2 `.\gradlew.bat test --tests com.kenez92.plateplan.plan.service.DietGeneratorTest --tests com.kenez92.plateplan.plan.service.PlanServiceTest` przechodzi: prompt bez ciała i e-maila, pusta selekcja to brak wyboru, odznaczona osoba nie wnosi wykluczenia — 2432441
- [x] 3.3 `.\gradlew.bat test` przechodzi — 2432441

### Phase 4: Ekran planu i PDF

#### Automated

- [x] 4.1 `.\gradlew.bat test --tests com.kenez92.plateplan.plan.service.PlanPdfWriterTest` przechodzi: dzień zaczyna stronę, a cel i makro są wypisane osobno dla każdej zaznaczonej osoby — ec66f7a
- [x] 4.2 `.\gradlew.bat test --tests com.kenez92.plateplan.plan.controller.PlanControllerTest` przechodzi: brak parametru `person` bierze wszystkich, nieznane id zwraca `SELECTION_REQUIRED` bez PDF-ów — ec66f7a
- [x] 4.3 `.\gradlew.bat test` przechodzi — ec66f7a

#### Manual

- [ ] 4.4 Na `/plan` checkboxy startują zaznaczone, odznaczenie jednej osoby nie dokłada jej porcji do pobranego PDF
- [ ] 4.5 Przy adresie na koncie wysyłka załącza `dieta-na-tydzien.pdf` i `lista-zakupow.pdf`

### Phase 5: Opis produktu

#### Automated

- [x] 5.1 `.\gradlew.bat test` przechodzi

#### Manual

- [ ] 5.2 PRD opisuje domowników na jednym koncie i nie trzyma wielu osób w Non-Goals
- [ ] 5.3 `AGENTS.md` opisuje porcje, zakres 400–6000 i najwyżej cztery osoby, a Parked w roadmapie nie zostawia wielu osób jako późniejszego non-goala
