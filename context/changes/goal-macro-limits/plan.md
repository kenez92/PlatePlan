# Pułap tłuszczu dnia — Implementation Plan

## Overview

Udany plan ma, u każdej zaznaczonej osoby i na każdym z siedmiu dni, sumę tłuszczu co najwyżej 30% jej własnych potwierdzonych kilokalorii. Model proponuje dziesięć dni. Do PDF wchodzi siedem dni, które po naprawie mieszczą się w paśmie kalorii i w pułapie. Gdy pierwszej odpowiedzi tyle nie daje, jedna kolejna dopisuje swoje dobre dni do już zachowanych. Wynik ma siedem takich dni albo nie ma plików.

## Current State Analysis

Dzień planu ma jedną regułę liczbową. Suma kilokalorii czterech porcji osoby mieści się w ±10% jej potwierdzonego celu. `WeekPlanValidator.bandsHold` sumuje tylko `Portion::kcal` (`WeekPlanValidator.java:87-110`). Cel osoby jest na `PlanPerson`, ale do tej sumy nie wchodzi.

Prompt opisuje redukcję jako posiłki białkowe wspierające utratę tłuszczu i nie podaje limitu gramów (`DietGenerator.java:38-39`). Jedyne pasmo liczbowe w poleceniu to ±10% kilokalorii (`DietGenerator.java:93-94`). Linia osoby to nazwa, cel i kilokalorie (`DietGenerator.java:37`).

`DietGenerator.generate` najpierw naprawia tydzień, potem woła `isValid` (`DietGenerator.java:143-150`). Naprawa wymaga dokładnie siedmiu dni (`WeekPlanRepair.java:66`) i mnoży `fatG` tym samym stosunkiem co kilokalorie porcji (`WeekPlanRepair.java:165-183`). Przy zerowych kilokaloriach oryginalnej porcji gramy zostają (`WeekPlanRepair.java:175-177`). Generator nie ponawia wywołania. Odrzucony tydzień to `PlanResult.unavailable()`. Prompt prosi o siedem dni (`DietGenerator.java:44-46`).

Testy walidatora składają porcje z `fatG = 0` (`WeekPlanValidatorTest.java:342`). Pasmo kalorii zostaje zielone po dołożeniu pułapu. PRD i `AGENTS.md` opisują dzień wyłącznie pasmem ±10%.

## Desired End State

Każda zaznaczona osoba, przy `LOSE_WEIGHT`, `MAINTAIN` i `GAIN`, ma na każdym dniu sumę `fatG` czterech porcji nie większą niż pułap liczony z jej własnych potwierdzonych kilokalorii. Pułap to zaokrąglenie połowiczne w górę z dzielenia tych kilokalorii przez 30, do pełnego grama. Dzień równy pułapowi przechodzi.

Przy 400 kcal pułap to 13 g, przy 795 kcal 27 g, przy 1999 kcal 67 g, przy 2000 kcal 67 g, przy 3000 kcal 100 g, przy 6000 kcal 200 g. Anna przy 2000 kcal i 60 g oraz Bartek przy 3000 kcal i 90 g przechodzą razem. Dzień ze 150 g tłuszczu przy celu 2000 kcal nie wchodzi do PDF. Sąsiednie dni, które się mieszczą, wchodzą.

Model dostaje prośbę o dziesięć dni. Każdy dzień jest skalowany raz, potem oceniany. Dobre dni zostają w kolejności odpowiedzi. Z tej listy wybierane jest siedem: dzień, którego śniadanie, obiad albo kolacja powtarza nazwę już wybranego dnia, albo którego obiad jest na liście zakazanych obiadów, jest pomijany. Drugie śniadanie może się powtórzyć. Gdy pierwsza odpowiedź daje te siedem, drugiego wywołania nie ma. Gdy daje mniej, ta sama prośba idzie jeszcze raz. Dobre dni z drugiej odpowiedzi dopisują się za dniami z pierwszej. Wynik to pierwsze siedem, które da się tak ułożyć. Ósmy dobry dzień odpada. Lista zakupów jest sumą składników tylko tych siedmiu dni. Gdy po dwóch odpowiedziach siedmiu dni ułożyć się nie da, generate wraca bez PDF. Lista kandydatów żyje do końca tego żądania.

### Key Discoveries:

- Bramka kalorii jest w `WeekPlanValidator.java:87-110` i nie czyta `fatG` ani `Goal`.
- Dziś jest jedno wywołanie, a naprawa odrzuca listę o innej długości niż siedem (`DietGenerator.java:143-150`, `WeekPlanRepair.java:66`). Kandydatów trzeba oceniać dzień po dniu.
- Skalowanie `grams × newKcal / originalKcal` (`WeekPlanRepair.java:175-183`) podnosi 16 g przy 400 kcal do 80 g przy 2000 kcal.
- Istniejące fikstury mają `fatG = 0` (`WeekPlanValidatorTest.java:325`, `:342`), więc sam pułap ich nie wywraca.
- Spisana reguła dnia jest w PRD, w akapicie Business Logic, oraz w `AGENTS.md` w twardych regułach i w sekcji Plan.

## What We're NOT Doing

- Pułap białka albo węglowodanów.
- Osobna matematyka dla `LOSE_WEIGHT`. Masa i utrzymanie dostają ten sam wzór ze swojego celu.
- Jeden wspólny pułap gramów na cały stół, liczony z najniższego celu.
- Średnia tłuszczu z siedmiu dni.
- 35% energii albo stałe 70 g.
- Obcięcie `fatG` w naprawie. Kalorie i gramy dalej skalują się tym samym stosunkiem.
- Trzecie wywołanie modelu. Po dwóch odpowiedziach bez siedmiu dni plików nie ma.
- Zapis listy kandydatów w bazie. Lista żyje do końca jednego `POST /plan/generate`.
- Zwrócenie planu krótszego niż siedem dni.
- Zmiana korekty −250 w `CalorieService`.
- Zmiana sposobu sumowania linii tłuszczu w PDF. Writer dalej dodaje cztery pola `fatG`.
- Zmiana pasma ±10% kilokalorii.

## Implementation Approach

Jedna metoda na `WeekPlanValidator` liczy pułap, a `dayFits` mówi, czy jeden naprawiony dzień mieści się w paśmie i w pułapie. `DietGenerator` prosi o dziesięć dni, skaluje każdy dzień raz i układa z dobrych dni siedem. Przy braku siedmiu robi jedno kolejne wywołanie i dopisuje dobre dni za już zachowanymi. Prompt wstawia liczbę z `fatGramsLimit` i nie liczy jej drugi raz. Na końcu to samo zdanie, razem z zasadą dziesięciu kandydatów i jednej dopiski, wchodzi do PRD i do obu miejsc w `AGENTS.md`.

## Critical Implementation Details

Dzielenie całkowite `kcal / 30` w Javie daje 66 przy 2000 kcal. Pułap jest zaokrągleniem połowicznym w górę dzielenia dziesiętnego, tą samą skalą co pasmo kilokalorii: 2000 kcal to 67 g, 795 kcal to 27 g, 400 kcal to 13 g.

Dzień oceniamy po jednym skalowaniu. Porcja 400 kcal i 16 g tłuszczu, przeskalowana do celu 2000 kcal, ma 80 g i odpada z listy kandydatów. Sąsiedni dzień w pułapie zostaje. Drugie skalowanie tych samych porcji przy składaniu listy zakupów podniosłoby gramy jeszcze raz, więc lista zakupów sumuje składniki już przeskalowanych siedmiu dni. Prompt wstawia liczbę zwróconą przez walidator. `WeekPlanRepair.repair` dziś zwraca null, gdy dni nie ma siedmiu, więc dziesięciu kandydatów nie wolno puścić tą ścieżką.

## Phase 1: Bramka tłuszczu

### Overview

Walidator zna pułap i umie ocenić jeden dzień. Tydzień przekazany w całości odpada, gdy którykolwiek jego dzień jest powyżej pułapu. Faza 2 używa oceny jednego dnia, żeby taki dzień pominąć, a resztę zostawić.

### Changes Required:

#### 1. WeekPlanValidator

**File**: `src/main/java/com/kenez92/plateplan/plan/validator/WeekPlanValidator.java`

**Intent**: Dzień ze 150 g tłuszczu przy celu 2000 kcal przestaje być akceptowalnym tygodniem. Pułap żyje obok pasma kilokalorii, żeby prompt w fazie 2 brał tę samą liczbę.

**Contract**: Publiczna `fatGramsLimit(int confirmedKcal)` zwraca pełne gramy: half-up z `confirmedKcal / 30`. Wyniki metody: 400 → 13, 795 → 27, 1999 → 67, 2000 → 67, 3000 → 100, 6000 → 200. Publiczna `dayFits(DayMeals, List<PlanPerson>)` jest prawdziwa, gdy dzień ma nazwy, po jednej porcji na każdą zaznaczoną osobę, kalorie w paśmie ±10% i sumę `fatG` nie większą niż `fatGramsLimit` tej osoby. Suma równa pułapowi przechodzi. `isValid` wymaga siedmiu dni, na każdym `dayFits`, oraz unikalnych nazw śniadania, obiadu i kolacji. Cel (`LOSE_WEIGHT`, `MAINTAIN`, `GAIN`) nie zmienia wzoru. Suma używa tego samego klucza nazwy co suma kilokalorii. Pasmo ±10% zostaje. Komentarz klasy wymienia pułap obok pasma kalorii.

#### 2. WeekPlanValidatorTest

**File**: `src/test/java/com/kenez92/plateplan/plan/validator/WeekPlanValidatorTest.java`

**Intent**: Zablokować granice gramów i przypadek dwóch osób z różnymi celami, bez ruszania istniejących asercji pasma kalorii.

**Contract**: Testy metod publicznych. Dzień na pułapie przechodzi, dzień o 1 g wyżej odpada, dla 400 kcal (13/14), 2000 kcal (67/68) i 3000 kcal (100/101). 1999 kcal przy 67 g przechodzi. 6000 kcal przy 200 g przechodzi. Anna przy 2000 kcal i 60 g razem z Bartkiem przy 3000 kcal i 90 g przechodzi. `dayFits` przyjmuje jeden dzień na pułapie i odrzuca ten sam dzień o 1 g wyżej. `isValid` odrzuca tydzień z sześcioma dniami w pułapie i jednym dniem powyżej pułapu. Rozłożenie gramów na cztery posiłki liczy się jako suma dnia. Istniejące testy z `fatG = 0` zostają zielone.

### Success Criteria:

#### Automated Verification:

- `.\gradlew.bat test --tests com.kenez92.plateplan.plan.validator.WeekPlanValidatorTest` przechodzi: 400 kcal przyjmuje 13 g i odrzuca 14 g, 2000 kcal przyjmuje 67 g i odrzuca 68 g, 3000 kcal przyjmuje 100 g i odrzuca 101 g
- Ten sam zestaw przyjmuje 1999 kcal przy 67 g i 6000 kcal przy 200 g, przyjmuje Annę przy 2000 kcal i 60 g razem z Bartkiem przy 3000 kcal i 90 g oraz odrzuca tydzień, w którym tylko jeden z siedmiu dni przekracza pułap tej osoby
- Ten sam zestaw: `dayFits` przyjmuje jeden dzień na pułapie tłuszczu i odrzuca ten sam dzień o 1 g powyżej pułapu

**Implementation Note**: After completing this phase and all automated verification passes, pause here for manual confirmation from the human that the manual testing was successful before proceeding to the next phase.

---

## Phase 2: Dziesięć dni, siedem w planie

### Overview

Model dostaje pułap w gramach i prośbę o dziesięć dni. Do wyniku wchodzi siedem dni, które po jednym skalowaniu mieszczą się w planie. Jedna kolejna odpowiedź dopisuje brakujące dobre dni za dniami z pierwszej.

### Changes Required:

#### 1. DietGenerator

**File**: `src/main/java/com/kenez92/plateplan/plan/service/DietGenerator.java`

**Intent**: Polecenie i bramka mają mówić o tej samej liczbie gramów, a jeden dzień powyżej pułapu ma odpaść bez kasowania dni, które się mieszczą.

**Contract**: Prompt prosi o dziesięć dni. Linia osoby zawiera pułap zwrócony przez `WeekPlanValidator.fatGramsLimit`. Zdanie w prompcie mówi, że cztery porcje osoby na dniu zostają na tym pułapie albo poniżej, a tłuszcz sosu, oliwy i masła wlicza się do sumy. Dotychczasowe zdania celu zostają. Każdy zwrócony dzień jest skalowany raz, tak jak naprawa skaluje porcje osoby do jej celu. Dzień, którego nie da się zestawić z zaznaczonymi osobami, odpada. Dzień, dla którego `dayFits` jest fałszem, odpada. Dobre dni zostają w kolejności odpowiedzi, na czas tego żądania. Wybór idzie od początku listy i bierze dzień, gdy nazwy jego śniadania, obiadu i kolacji nie powtarzają nazw już wybranych dni, a obiad nie jest na liście zakazanych. Drugie śniadanie może się powtórzyć. Wybór kończy się na siedmiu. Gdy pierwsza odpowiedź daje te siedem, drugiego wywołania nie ma. Gdy daje mniej, to samo polecenie idzie jeszcze raz, a dobre dni z drugiej odpowiedzi dopisują się za dniami z pierwszej. Wybór idzie wtedy po połączonej liście od początku. Wynik to te siedem przeskalowanych dni. Lista zakupów jest sumą ich składników i nie skaluje porcji drugi raz. Ósmy dobry dzień do wyniku nie wchodzi. Wywołanie, które rzuca wyjątek, kończy się czasem albo nie zwraca tygodnia, dokłada zero dni. Po dwóch odpowiedziach bez siedmiu dni wynik to `PlanResult.unavailable()`. Trzeciego wywołania nie ma. Promptu, pułapu, kilokalorii, celu, nazw dań i gramów się nie loguje. Komentarz klasy mówi o dziesięciu kandydatach, jednej dopisce i pułapie tłuszczu.

#### 2. Skalowanie jednego dnia

**File**: `src/main/java/com/kenez92/plateplan/plan/service/WeekPlanRepair.java`

**Intent**: Dziesięć dni nie może iść przez `repair`, bo ta metoda przy innej długości zwraca null. Dzień trzeba przeskalować osobno, a listę zakupów złożyć dopiero z siedmiu dni, które zostały.

**Contract**: Jest sposób na przeskalowanie jednego dnia do celów zaznaczonych osób, tym samym stosunkiem gramów do kilokalorii co dziś. Jest sposób na listę zakupów z już przeskalowanych dni, bez drugiego skalowania porcji. `repair` dla dokładnie siedmiu dni zostaje dostępne dla dotychczasowych testów naprawy. Dzień 400 kcal i 16 g tłuszczu przy celu 2000 kcal dalej skaluje się do 80 g.

#### 3. DietGeneratorTest

**File**: `src/test/java/com/kenez92/plateplan/plan/service/DietGeneratorTest.java`

**Intent**: Zablokować liczbę w prompcie, kolejność dni z dwóch odpowiedzi i to, że dzień po skalowaniu powyżej pułapu nie wchodzi między dobre.

**Contract**: Prompt przy `Ja` / 2000 kcal i `Anna` / 795 kcal prosi o dziesięć dni oraz zawiera przy pierwszej osobie pułap 67 g, a przy drugiej 27 g, w tej kolejności. Jedna odpowiedź z ośmioma dniami, które `dayFits` przyjmuje, daje pierwsze siedem i jedno wywołanie modelu. Pierwsza odpowiedź z czterema dobrymi dniami i druga z pięcioma daje w wyniku te cztery, potem trzy z drugiej, dwa wywołania, a dzień skalowany z 16 g przy 400 kcal do 80 g przy 2000 kcal w wyniku nie występuje. Dwie odpowiedzi, które razem dają sześć dobrych dni, wracają jako niedostępne po dokładnie dwóch wywołaniach. Gdy wcześniejszy dobry dzień powtarza obiad już wybranego, do siódemki wchodzi późniejszy dzień z inną nazwą obiadu. Składnik, który jest tylko na odrzuconym dniu, nie występuje na liście zakupów. Istniejące asercje o braku danych ciała zostają. Siedem dobrych dni z pierwszej odpowiedzi nadal kończy się sukcesem po jednym wywołaniu.

### Success Criteria:

#### Automated Verification:

- `.\gradlew.bat test --tests com.kenez92.plateplan.plan.service.DietGeneratorTest` przechodzi: prompt prosi o dziesięć dni, linia osoby przy 2000 kcal zawiera pułap 67 g, a linia osoby przy 795 kcal zawiera pułap 27 g
- Ten sam zestaw z jednej odpowiedzi o ośmiu dobrych dniach zwraca pierwsze siedem i woła model raz
- Ten sam zestaw z czterech dobrych dni pierwszej odpowiedzi i pięciu dobrych dni drugiej zwraca te cztery, potem trzy z drugiej, woła model dwa razy, a dnia skalowanego z 16 g przy 400 kcal do 80 g przy 2000 kcal w wyniku nie ma
- Ten sam zestaw przy sześciu dobrych dniach z dwóch odpowiedzi zwraca niedostępność i woła model dokładnie dwa razy
- Ten sam zestaw pomija wcześniejszy dzień, którego obiad powtarza obiad już wybranego dnia, bierze późniejszy dzień z inną nazwą obiadu, a składnika obecnego tylko na odrzuconym dniu nie ma na liście zakupów

#### Manual Verification:

- Wygenerowany PDF diety ma siedem dni, a na każdym dniu tłuszcz każdej zaznaczonej osoby jest nie wyższy niż jej pułap

**Implementation Note**: After completing this phase and all automated verification passes, pause here for manual confirmation from the human that the manual testing was successful before proceeding to the next phase.

---

## Phase 3: Kontrakt w PRD i AGENTS.md

### Overview

Spisana reguła dnia obejmuje pułap tłuszczu w tych miejscach, z których kolejna zmiana czyta kontrakt planu.

### Changes Required:

#### 1. PRD

**File**: `context/foundation/prd.md`

**Intent**: Akapit logiki biznesowej dziś kończy regułę dnia na paśmie ±10%. Bez tego zdania kolejna zmiana uzna pułap za wyjście poza PRD.

**Contract**: W sekcji Business Logic, przy zdaniu o porcjach dnia w paśmie ±10%, dochodzi ta sama reguła: cztery porcje osoby na dniu mają sumę tłuszczu co najwyżej równą zaokrągleniu połowicznemu w górę z jej potwierdzonych kilokalorii podzielonych przez 30. Model dostaje dziesięć dni. Zostaje siedem, które po naprawie mieszczą się w paśmie i w tym pułapie. Gdy pierwsza odpowiedź tylu nie daje, jedna kolejna dopisuje swoje dobre dni za już zachowanymi. Wynik ma siedem dni. Pasmo kalorii zostaje.

#### 2. AGENTS.md

**File**: `AGENTS.md`

**Intent**: Twarde reguły i sekcja Plan powtarzają dziś wyłącznie pasmo kalorii. Obie mają dostać to samo zdanie, żeby skrót na górze pliku nie został przy starej regule.

**Contract**: To samo zdanie co w PRD wchodzi w dwa miejsca: w punkt twardych reguł, przy zdaniu o ±10% potwierdzonej liczby, oraz w sekcji Plan, przy zdaniu o czterech porcjach w ±10%. Brzmienie w obu miejscach jest identyczne i obejmuje pułap, dziesięć dni kandydatów oraz jedną dopiskę.

### Success Criteria:

#### Manual Verification:

- Akapit Business Logic w PRD oraz twarde reguły i sekcja Plan w AGENTS.md mówią, że suma tłuszczu czterech porcji osoby na dniu ma być co najwyżej zaokrągleniem połowicznym w górę z jej potwierdzonych kilokalorii podzielonych przez 30, że model dostaje dziesięć dni, z których zostaje siedem mieszczących się w paśmie kalorii i w tym pułapie, oraz że przy braku siedmiu jedna kolejna odpowiedź dopisuje swoje dobre dni

**Implementation Note**: After completing this phase and all automated verification passes, pause here for manual confirmation from the human that the manual testing was successful before proceeding to the next phase.

---

## Testing Strategy

### Unit Tests:

- `WeekPlanValidatorTest` lockuje `fatGramsLimit` i `isValid` na granicach 13/14, 67/68 i 100/101, na 1999 kcal i 6000 kcal oraz na Annie i Bartku.
- `DietGeneratorTest` lockuje dziesięć dni oraz 67 g i 27 g w promptcie, pierwsze siedem z ośmiu dobrych, cztery z pierwszej odpowiedzi plus trzy z drugiej, brak dnia skalowanego do 80 g, niedostępność przy sześciu dobrych dniach po dwóch wywołaniach, pominięcie powtórzonego obiadu i brak składnika odrzuconego dnia na liście zakupów.
- Istniejące testy z zerowym tłuszczem zostają w tych klasach i przechodzą razem z nowymi.

### Integration Tests:

- Brak. Bramka i prompt nie wymagają kontekstu Springa ani bazy. `ApplicationTest` zostaje przy starcie kontekstu i nie sprawdza tego pułapu.

### Manual Testing Steps:

1. Wygenerować plan dla osoby z potwierdzonym celem 2000 kcal.
2. Na każdej stronie dnia PDF sprawdzić linię tłuszczu tej osoby. Ma być co najwyżej 67 g.
3. Przy dwóch osobach z różnymi celami sprawdzić, że każda linia trzyma pułap policzony z celu tej osoby.

## Performance Considerations

Doliczenie czterech liczb na osobę i dzień nie dodaje zapisu. Jedno generate woła model raz, gdy pierwsza odpowiedź daje siedem dni do ułożenia, i dwa razy, gdy nie daje. Drugie wywołanie wydłuża tylko tę prośbę, która pierwszej siódemki nie uzbierała.

## Migration Notes

Brak migracji. Lista kandydatów nie dostaje tabeli. Plany i PDF-y nie są przechowywane. Poprzednie pobrane pliki zostają takie, jakie były.

## References

- Brief ramowy: `context/changes/goal-macro-limits/frame.md`
- Related research: brak `research.md`
- Bramka kalorii: `src/main/java/com/kenez92/plateplan/plan/validator/WeekPlanValidator.java:87-110`
- Kolejność naprawy i akceptacji: `src/main/java/com/kenez92/plateplan/plan/service/DietGenerator.java:143-150`
- Skalowanie gramów: `src/main/java/com/kenez92/plateplan/plan/service/WeekPlanRepair.java:165-183`

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands. Do not rename step titles. See `references/progress-format.md`.

### Phase 1: Bramka tłuszczu

#### Automated

- [x] 1.1 `.\gradlew.bat test --tests com.kenez92.plateplan.plan.validator.WeekPlanValidatorTest` przechodzi: 400 kcal przyjmuje 13 g i odrzuca 14 g, 2000 kcal przyjmuje 67 g i odrzuca 68 g, 3000 kcal przyjmuje 100 g i odrzuca 101 g
- [x] 1.2 Ten sam zestaw przyjmuje 1999 kcal przy 67 g i 6000 kcal przy 200 g, przyjmuje Annę przy 2000 kcal i 60 g razem z Bartkiem przy 3000 kcal i 90 g oraz odrzuca tydzień, w którym tylko jeden z siedmiu dni przekracza pułap tej osoby
- [x] 1.3 Ten sam zestaw: `dayFits` przyjmuje jeden dzień na pułapie tłuszczu i odrzuca ten sam dzień o 1 g powyżej pułapu

### Phase 2: Dziesięć dni, siedem w planie

#### Automated

- [ ] 2.1 `.\gradlew.bat test --tests com.kenez92.plateplan.plan.service.DietGeneratorTest` przechodzi: prompt prosi o dziesięć dni, linia osoby przy 2000 kcal zawiera pułap 67 g, a linia osoby przy 795 kcal zawiera pułap 27 g
- [ ] 2.2 Ten sam zestaw z jednej odpowiedzi o ośmiu dobrych dniach zwraca pierwsze siedem i woła model raz
- [ ] 2.3 Ten sam zestaw z czterech dobrych dni pierwszej odpowiedzi i pięciu dobrych dni drugiej zwraca te cztery, potem trzy z drugiej, woła model dwa razy, a dnia skalowanego z 16 g przy 400 kcal do 80 g przy 2000 kcal w wyniku nie ma
- [ ] 2.4 Ten sam zestaw przy sześciu dobrych dniach z dwóch odpowiedzi zwraca niedostępność i woła model dokładnie dwa razy
- [ ] 2.5 Ten sam zestaw pomija wcześniejszy dzień, którego obiad powtarza obiad już wybranego dnia, bierze późniejszy dzień z inną nazwą obiadu, a składnika obecnego tylko na odrzuconym dniu nie ma na liście zakupów

#### Manual

- [ ] 2.6 Wygenerowany PDF diety ma siedem dni, a na każdym dniu tłuszcz każdej zaznaczonej osoby jest nie wyższy niż jej pułap

### Phase 3: Kontrakt w PRD i AGENTS.md

#### Manual

- [ ] 3.1 Akapit Business Logic w PRD oraz twarde reguły i sekcja Plan w AGENTS.md mówią, że suma tłuszczu czterech porcji osoby na dniu ma być co najwyżej zaokrągleniem połowicznym w górę z jej potwierdzonych kilokalorii podzielonych przez 30, że model dostaje dziesięć dni, z których zostaje siedem mieszczących się w paśmie kalorii i w tym pułapie, oraz że przy braku siedmiu jedna kolejna odpowiedź dopisuje swoje dobre dni
