# Odczyt, czy dania da się ugotować

## Overview

Czytamy prawdziwy plan i dla każdego dania zapisujemy, czy da się je ugotować oraz czy gramy składników schodzą się z wydrukowanymi kcal, białkiem, węglowodanami i tłuszczem. Generator, prompt i wzory z sieci zostają bez zmian.

## Current State Analysis

Java przyjmuje dzień, gdy danie ma niepustą nazwę, po jednej porcji na wybraną osobę, sumę kcal w paśmie ±10% celu i sumę tłuszczu nie wyższą niż pułap (`WeekPlanValidator.java:54-55`). Przygotowania nikt nie czyta (`Meal.java:14-16`). `WeekPlanRepair` kopiuje nazwy składników, napisy ilości i przygotowanie, a podmienia kcal i makro (`WeekPlanRepair.java:136-137`, `WeekPlanRepair.java:241-248`). PDF drukuje te napisy obok przepisanych liczb (`PlanPdfWriter.java:416-423`, `PlanPdfWriter.java:486-492`).

Ostatni udany plan ma 3 dni i poszedł mailem na adres konta Daniel2. Aplikacja plików nie trzyma. W repozytorium nie ma tekstu tego planu. Tabela na 100 g jest w `src/main/resources/food/kcal-per-100g.csv` i żadna klasa jej nie czyta.

## Desired End State

W `context/changes/cookable-dishes/verdict.md` jest jeden wiersz na każde danie przeczytanego planu. Wiersz ma nazwę, werdykt „da się ugotować” (tak albo nie), werdykt liczb (tak albo nie) i jedno zdanie, które wskazuje, co padło. Pełnych kroków nie ma. Pliki PDF nie są w gicie. `src/main/java` jest nietknięte.

Danie jest dobre tylko gdy obie odpowiedzi brzmią „tak”. Sprawdzenie: liczba wierszy równa się liczbie dań w PDF, a zdanie przy „nie” wskazuje składnik, krok albo liczbę, którą widać na talerzu.

### Key Discoveries:

- Pasmo kcal dnia już wymusza generator. Zgodność liczb z ilościami trzeba liczyć ze składników, nie z celu dnia (`DietGenerator.java:225-233`).
- Ilość jest napisem w rodzaju „150 g” albo „2 szt.” (`DietGenerator.java:77-78`).
- Nazwy w CSV są dosłowne. „Pierś z kurczaka” nie jest wierszem „Kurczak (pierś bez skóry)”.
- Kamień M-3 zostawia przepis przy modelu (`context/foundation/roadmap.md`, S-02). Ten odczyt go nie przesuwa.

## What We're NOT Doing

- Nie ściągamy przepisów ze stron i nie dokładamy wzorów.
- Nie zmieniamy promptu, modelu ani generatora.
- Nie liczymy gramów w aplikacji i nie podpinamy CSV do generowania.
- Nie commitujemy PDF-ów ani pełnego sposobu przyrządzenia.
- Nie czytamy drugiego planu, gdy mail jest i da się go otworzyć.

## Implementation Approach

Najpierw są pliki, potem ocena. PDF-y z maila lądują w `tmp/cookable-dishes/`, a `tmp/` wchodzi do `.gitignore`. Gdy maila nie ma, jedno `POST /plan/generate?sendEmail=true` na koncie Daniel2 i te same dwa pliki lądują w tym katalogu. Ocena jest ręczna wobec reguł poniżej i ląduje w `verdict.md`.

Reguła gotowania, raz na danie (wspólny przepis, nie raz na osobę). „Tak” tylko gdy wszystkie punkty zachodzą:

- każdy składnik da się kupić,
- przygotowanie nie jest puste,
- kroki idą po kolei i da się je wykonać bez dopisywania brakującego kroku,
- każdy gotowany składnik jest w krokach,
- kroki nie nazywają jedzenia, którego nie ma w składnikach.

Drugie śniadanie jest towarem ze sklepu: wystarczy jedno albo dwa zdania podania. Piekarnik i temperatura nie są wymagane.

Reguła liczb, osobno dla każdej porcji dania. Danie ma „tak” tylko gdy każda porcja ma „tak”. Porcja ma „nie” od razu, gdy choć jeden składnik nie ma ilości w gramach albo jego nazwa nie występuje w CSV. Nie sumujemy wtedy reszty. Nazwa pasuje po obcięciu spacji, NFC i porównaniu bez wielkości liter, do jednego wiersza CSV. Ilość w gramach to liczba, opcjonalna spacja i jednostka `g`, `gr`, `gram` albo `gramy`. Inny zapis, także „2 szt.” i „1 łyżka”, oblewa porcję.

Gdy wszystkie składniki mają gramy i są w tabeli, dla kcal, białka, węglowodanów i tłuszczu osobno: suma `(gramy / 100) × wartość z CSV` mieści się w ±10% liczby wydrukowanej na tej porcji. Przy wydruku 0 suma musi być 0. Granica 10% włącznie przechodzi: wydruk 200 i suma 220 przechodzą, 221 nie.

Jedno zdanie przy „nie” nazywa składnik albo krok, a przy liczbach wydruk i sumę. Imię osoby pada tylko wtedy, gdy porcje dania różnią się wynikiem. Bez wieku, wagi, wzrostu, płci, celu, maila i bajtów PDF.

## Critical Implementation Details

- **Timing & lifecycle.** Najpierw pliki w `tmp/cookable-dishes/`, potem `verdict.md`. Generowanie startuje tylko po ustaleniu, że maila nie ma. Drugiego generowania nie ma.
- **Debug & observability.** W logu aplikacji nie ma nazw dań, list produktów, kcal, celu ani bajtów PDF. Werdykt jest plikiem w folderze zmiany, nie wpisem w logu.

## Phase 1: Pliki do odczytu

### Overview

Oba PDF-y da się czytać spoza gita. Wiadomo, czy to plan z maila, czy jedyne zastępcze generowanie.

### Changes Required:

#### 1. Ignorowany katalog na pliki

**File**: `.gitignore`

**Intent**: `tmp/` nie wchodzi do gita, więc PDF planu nie zostanie dodany przy najbliższym commicie.

**Contract**: dopisać linię `tmp/`. Pliki czytane to `tmp/cookable-dishes/dieta-na-tydzien.pdf` i `tmp/cookable-dishes/lista-zakupow.pdf`.

#### 2. Źródło plików

**File**: `tmp/cookable-dishes/`

**Intent**: Położyć tu dwa pliki z maila konta Daniel2. Gdy maila nie ma, wygenerować jeden plan z wysyłką i zapisać te same dwa załączniki w tym katalogu.

**Contract**: najwyżej jedno generowanie. Brak trzeciego pliku. Katalog nie jest commitowany.

### Success Criteria:

#### Automated Verification:

- `git check-ignore -q -- tmp/cookable-dishes/dieta-na-tydzien.pdf` kończy się kodem 0
- `git ls-files "context/changes/cookable-dishes/*.pdf"` nie wypisuje żadnego pliku

#### Manual Verification:

- Oba PDF-y są w `tmp/cookable-dishes/` i Daniel potwierdza, że to plan z maila albo to jedyne zastępcze generowanie

**Implementation Note**: Po zielonych sprawdzeniach automatycznych zatrzymaj się i poczekaj, aż Daniel potwierdzi źródło plików, zanim zacznie się ocena.

---

## Phase 2: Werdykt

### Overview

Każde danie z diety ma jeden wiersz. Generator zostaje taki, jaki jest.

### Changes Required:

#### 1. Tabela werdyktu

**File**: `context/changes/cookable-dishes/verdict.md`

**Intent**: Zostawić odczyt, do którego da się wrócić bez maila i bez pełnego przepisu.

**Contract**: nagłówek mówi, czy źródłem jest mail, czy jedno generowanie, oraz ile dań jest w PDF. Potem tabela o kolumnach: nazwa, da się ugotować, liczby, zdanie. Jeden wiersz na danie, w kolejności z PDF. Wartości kolumn to `tak` albo `nie`. Zdanie jest jedno. Nie ma akapitu kroków, listy składników ani cytatu przygotowania.

### Success Criteria:

#### Automated Verification:

- `git diff --exit-code -- src/main/java` kończy się kodem 0

#### Manual Verification:

- W `verdict.md` jest jeden wiersz na każde danie z PDF, z czterema polami i bez pełnego przepisu
- Daniel potwierdza, że zdania przy „nie” wskazują to, co widać na talerzu

**Implementation Note**: Po zielonym `git diff` zatrzymaj się i poczekaj na potwierdzenie zdań. Ta faza nie otwiera poprawki promptu ani wzorów.

---

## Testing Strategy

### Unit Tests:

- Brak. Ta zmiana nie rusza klas produkcyjnych. Reguła ±10% jest zapisana w tym planie i stosowana przy odczycie, nie w nowym teście.

### Integration Tests:

- Brak. Generowanie, jeśli w ogóle nastąpi, jest jednorazowym źródłem plików, nie sprawdzianem endpointu.

### Manual Testing Steps:

1. Otwórz dietę i policz dania. Tyle wierszy ma być w `verdict.md`.
2. Dla dania z „nie” przy gotowaniu wskaż palcem składnik albo krok, o którym mówi zdanie.
3. Dla dania z „nie” przy liczbach sprawdź, że zdanie podaje wydruk i sumę albo nazywa ilość bez gramów albo nazwę spoza CSV.
4. Upewnij się, że w pliku nie ma ciągu kroków przepisu.

## Performance Considerations

Nie dotyczy. Jedno czytanie jednego planu. Ewentualne generowanie jest jednorazowe i używa obecnego limitu czasu modelu.

## Migration Notes

Nie dotyczy. Schemat bazy i pliki planu na koncie zostają nietknięte.

## References

- Frame: `context/changes/cookable-dishes/frame.md`
- Tabela na 100 g: `src/main/resources/food/kcal-per-100g.csv`
- Bramka dnia: `src/main/java/com/kenez92/plateplan/plan/validator/WeekPlanValidator.java:54-55`
- Tekst przepisu zostaje z modelu: `src/main/java/com/kenez92/plateplan/plan/service/WeekPlanRepair.java:136-137`, `src/main/java/com/kenez92/plateplan/plan/service/WeekPlanRepair.java:241-248`
- Kamień, który zostawia przepis przy modelu: `context/foundation/roadmap.md` (S-02)

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands. Do not rename step titles. See `references/progress-format.md`.

### Phase 1: Pliki do odczytu

#### Automated

- [x] 1.1 `git check-ignore -q -- tmp/cookable-dishes/dieta-na-tydzien.pdf` kończy się kodem 0
- [x] 1.2 `git ls-files "context/changes/cookable-dishes/*.pdf"` nie wypisuje żadnego pliku

#### Manual

- [ ] 1.3 Oba PDF-y są w `tmp/cookable-dishes/` i Daniel potwierdza, że to plan z maila albo to jedyne zastępcze generowanie

### Phase 2: Werdykt

#### Automated

- [ ] 2.1 `git diff --exit-code -- src/main/java` kończy się kodem 0

#### Manual

- [ ] 2.2 W `verdict.md` jest jeden wiersz na każde danie z PDF, z czterema polami i bez pełnego przepisu
- [ ] 2.3 Daniel potwierdza, że zdania przy „nie” wskazują to, co widać na talerzu
