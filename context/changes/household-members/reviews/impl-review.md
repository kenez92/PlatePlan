<!-- IMPL-REVIEW-REPORT -->
# Implementation Review: Domownicy w planie tygodnia

- **Plan**: context/changes/household-members/plan.md
- **Scope**: Full plan
- **Reviewed phases**: 1, 2, 3, 4, 5
- **Date**: 2026-10-15
- **Verdict**: NEEDS ATTENTION
- **Findings**: 0 critical, 3 warnings, 4 observations
- **Resolution**: F1–F5 are applied in the working tree. F6 stays open until the manual checks. F7 stays as Fix A.

## Verdicts

| Dimension | Verdict |
|-----------|---------|
| Plan Adherence | WARNING |
| Scope Discipline | WARNING |
| Safety & Quality | WARNING |
| Architecture | PASS |
| Pattern Consistency | PASS |
| Success Criteria | WARNING |

## Findings

### F1 — Baza niedostępna kończy się HTTP 500 na planie i kcal właściciela

- **Severity**: ⚠️ WARNING
- **Impact**: 🏃 LOW — szybka decyzja; poprawka jest oczywista i wąsko ograniczona
- **Dimension**: Safety & Quality
- **Location**: src/main/java/com/kenez92/plateplan/plan/service/PlanService.java:78
- **Detail**: `options` (linia 78) i `generate` (linia 98) łapią tylko `DataAccessException`. To samo robi `ConfirmedCaloriesService.load`, `update` i `recalculate` (linie 50, 65, 86). Gdy baza nie wstanie, Spring rzuca `CannotCreateTransactionException`, które jest `TransactionException`, a nie `DataAccessException`. `ProfileService` i `HouseholdMemberService` łapią oba typy i zostają przy HTTP 200. `GET /plan` oraz `POST /plan/generate` nie mają własnego try/catch, więc odpowiedź schodzi do 500 zamiast pustej listy albo `UNAVAILABLE`. `POST /profile/calories` i `POST /profile/recalculate` też schodzą do 500. Ścieżki domownika zostają przy 200, bo łapie je `HouseholdMemberService`.
- **Fix**: W obu serwisach łap `DataAccessException | TransactionException` i zwracaj to, co już zwracają przy awarii bazy: pustą listę, `PlanResult.unavailable()` albo `ConfirmedCaloriesResult.failed()`.
- **Decision**: applied — `PlanService` i `ConfirmedCaloriesService` łapią oba typy.

### F2 — PDF gubi porcję, którą walidator przyjął

- **Severity**: ⚠️ WARNING
- **Impact**: 🔎 MEDIUM — jest rzeczywisty kompromis; zatrzymaj się i przemyśl
- **Dimension**: Safety & Quality
- **Location**: src/main/java/com/kenez92/plateplan/plan/service/PlanPdfWriter.java:143
- **Detail**: `WeekPlanValidator.nameKey` (linia 109) i `portionKcal` (linia 87) porównują nazwę po `strip` i `toLowerCase(Locale.ROOT)`. Writer sumuje makro (`personSum`, linia 143) i drukuje porcję (`portionLines`, linia 159) przez `String.equals`. Tydzień, który walidator przyjmie, bo model oddał `" ja "` albo `"JA"` przy skutecznej nazwie `Ja`, schodzi jako udany PDF: cel dnia jest z konta, linia makro ma 0 g, a blok tej porcji znika.
- **Fix A ⭐ Recommended**: Dopasuj porcje w writerze tym samym kluczem co walidator, a na stronie drukuj nazwę z konta.
  - Strength: Cel i makro zostają przy osobie z konta, a różnica wielkości liter albo spacji na brzegach nie wyzerowuje dnia.
  - Tradeoff: Nazwa z modelu, jeśli różni się spacją albo wielkością liter, nie trafia do PDF. To jest ta sama skuteczna nazwa.
  - Confidence: HIGH — walidator już tak porównuje w `portionKcal`.
  - Blind spot: Nie sprawdziłem, czy model kiedykolwiek zwraca inną pisownię niż skuteczną nazwę.
- **Fix B**: Odrzucaj tydzień, gdy `personName` porcji nie jest równe skutecznej nazwie znak w znak.
  - Strength: PDF i walidator wymagają tej samej pisowni, więc `equals` w writerze zostaje prawdziwe.
  - Tradeoff: Więcej odmów `UNAVAILABLE` za spację albo wielkość liter, bez drugiej próby modelu.
  - Confidence: MEDIUM — plan każe przyjąć nazwę po strip i bez wielkości liter przy unikalności osób, nie przy druku.
  - Blind spot: Nie zliczyłem, jak często model zmienia pisownię.
- **Decision**: applied — `PlanPdfWriter` porównuje osobę po `strip` i `toLowerCase`, tak jak walidator.

### F3 — Przeliczenie właściciela poza 400–6000 pokazuje zły komunikat

- **Severity**: ⚠️ WARNING
- **Impact**: 🏃 LOW — szybka decyzja; poprawka jest oczywista i wąsko ograniczona
- **Dimension**: Plan Adherence
- **Location**: src/main/java/com/kenez92/plateplan/profile/controller/ProfileController.java:242
- **Detail**: `ConfirmedCaloriesService.recalculate` przy wzorze poza 400–6000 zwraca `refused` i nie rusza poprzedniej liczby (linia 82–83). `calorieWrite` traktuje każde nieudane, istniejące przeliczenie jako `calorieSaveFailed`. Ekran mówi wtedy „Nie udało się zapisać celu kalorycznego. Spróbuj ponownie za chwilę.” Zapis ciała właściciela i przeliczenie domownika używają już `profile.calories.refused`. Liczba zostaje, tekst obiecuje ponowienie.
- **Fix**: Gdy `result.refused()` jest prawdziwe, ustaw `ownerCaloriesRefused` i nie ustawiaj `calorieSaveFailed`.
- **Decision**: applied — `calorieWrite` ustawia `ownerCaloriesRefused`.

### F4 — `generate(String)` gotuje tylko dla właściciela

- **Severity**: 💡 OBSERVATION
- **Impact**: 🏃 LOW — szybka decyzja; poprawka jest oczywista i wąsko ograniczona
- **Dimension**: Scope Discipline
- **Location**: src/main/java/com/kenez92/plateplan/plan/service/PlanService.java:64
- **Detail**: Jednoargumentowe `generate` zawsze woła `generate(login, List.of("owner"))`. Kontroler go nie używa: brak parametru `person` idzie jako `null` i oznacza wszystkich. Starsze testy `PlanServiceTest` nadal wołają wersję jednoargumentową, więc domownik istniejący na koncie jest w tym wywołaniu pomijany. Plan takiej trzeciej ścieżki nie opisał.
- **Fix**: Usuń `generate(String)`. Testy, które mają znaczyć „tylko właściciel”, niech wołają `generate(login, List.of("owner"))`. Testy całego domu niech wołają `generate(login, null)`.
- **Decision**: applied — jednoargumentowe `generate` jest usunięte.

### F5 — Czwarta osoba nie zostaje w formularzu

- **Severity**: 💡 OBSERVATION
- **Impact**: 🏃 LOW — szybka decyzja; poprawka jest oczywista i wąsko ograniczona
- **Dimension**: Plan Adherence
- **Location**: src/main/resources/templates/profile.html:154
- **Detail**: Plan każe zostawić czwartego domownika na stronie z komunikatem. Przy trzech zapisanych osobach formularz nowej osoby w ogóle się nie rysuje (`members.size() < 3`). Komunikat limitu jest banerem nad listą (linia 140). Wpisane imię czwartej osoby znika. Przypadek nazwy `ja` zostaje w formularzu, bo ten formularz jest na stronie.
- **Fix**: Przy odmowie zapisu nowej osoby pokazuj formularz tworzenia razem z komunikatem, także gdy trzy osoby już są.
- **Decision**: applied — formularz zostaje, gdy odmowa dotyczy nowej osoby.

### F6 — Sześć ręcznych sprawdzeń jest nadal otwartych

- **Severity**: 💡 OBSERVATION
- **Impact**: 🏃 LOW — szybka decyzja; poprawka jest oczywista i wąsko ograniczona
- **Dimension**: Success Criteria
- **Location**: context/changes/household-members/plan.md:340
- **Detail**: Automatyczne kryteria 1.1–5.1 są `[x]` i mają SHA. Żadne ręczne pole nie jest odhaczone na zapas. Otwarte zostają 2.3, 2.4, 4.4, 4.5, 5.2 i 5.3. Suite `.\gradlew.bat test` i testy zakresowe z planu przeszły 2026-10-15.
- **Fix**: Zostaw te wiersze otwarte, aż przejdziesz sprawdzenie na `/profile`, w PDF i w dokumentach.
- **Decision**: PENDING

### F7 — Dwa równoległe zapisy mogą przekroczyć limit trzech osób

- **Severity**: 💡 OBSERVATION
- **Impact**: 🔎 MEDIUM — jest rzeczywisty kompromis; zatrzymaj się i przemyśl
- **Dimension**: Safety & Quality
- **Location**: src/main/java/com/kenez92/plateplan/profile/service/HouseholdMemberService.java:96
- **Detail**: `create` czyta domowników, odrzuca czwartego, a potem zapisuje w osobnej transakcji. Nie ma blokady wiersza właściciela ani ograniczenia w `006-household-members.xml`. Dwa nakładające się zapisy mogą oba zobaczyć mniej niż trzy wiersze i oba wstawić. To samo dotyczy unikalności nazwy. Jedno żądanie naraz nadal zatrzymuje się na trzech. Plan nie wymagał blokady.
- **Fix A ⭐ Recommended**: Zostaw tak. Konto obsługuje jedna osoba, a podwójne kliknięcie jest rzadkie.
  - Strength: Nie dokłada blokady, której plan nie żądał, a zwykły zapis dalej trzyma limit.
  - Tradeoff: Dwa równoległe żądania mogą zapisać czwartą osobę albo tę samą nazwę dwa razy.
  - Confidence: HIGH — w kodzie nie ma transakcji obejmującej odczyt i zapis.
  - Blind spot: Nie sprawdziłem, czy przeglądarka wysyła dwa submit jednocześnie.
- **Fix B**: W jednej transakcji zablokuj wiersz `user_profile` tego loginu, policz osoby i nazwy jeszcze raz, potem wstaw.
  - Strength: Limit trzech i unikalność nazwy trzymają się także przy dwóch żądaniach naraz.
  - Tradeoff: Nowa blokada w zapisie właściciela i domownika, poza tym, co plan opisał.
  - Confidence: MEDIUM — wzorzec pesymistycznej blokady nie występuje dziś przy profilu.
  - Blind spot: Nie sprawdziłem, jak długo PostgreSQL trzyma taki lock przy tym schemacie.
- **Decision**: accepted Fix A — bez pesymistycznej blokady. Jedno konto, jedno żądanie naraz.
