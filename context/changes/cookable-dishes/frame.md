# Frame Brief: Czy dania z modelu da się ugotować

> Framing step before /10x-plan. This document captures what is *actually*
> at issue, separated from what was initially assumed.

## Reported Observation

Dania nie da się ugotować albo składniki nie trzymają się kupy. Najpierw ma paść odpowiedź, czy dania, które układa model, w ogóle nadają się do gotowania.

## Initial Framing (preserved)

- **User's stated cause or approach**: Sam model, który wymyśla danie, może nie wystarczyć — przydałby się gotowy wzór.
- **User's proposed direction**: Powyciągać przeróżne przepisy z różnych stron i mieć je w projekcie jako wzory.
- **Pre-dispatch narrowing**: Dania nie da się ugotować albo składniki nie trzymają się kupy. Najpierw rozstrzygnąć, czy dania, które układa model, w ogóle nadają się do gotowania.

## Dimension Map

The observation could originate at any of these dimensions:

1. **Bramka akceptacji** — dzień przechodzi, gdy zgadzają się kcal, pułap tłuszczu, porcje i unikalne nazwy. Tekst przepisu nikt nie czyta.
2. **Jedno wywołanie na dziesięć dni** — lokalny model sam układa przepis, bez wzoru i bez osobnego wywołania na danie.
3. **Naprawa liczb zostawia tekst** — Java przepisuje kcal i makro, a nazwy składników, napisy ilości i przygotowanie zostają z modelu. ← initial framing sits next to this: a template would replace that text
4. **Brak odczytu** — żaden wygenerowany tydzień nie został zapisany ani oceniony pod kątem gotowania. Kamień M-3 i tak zostawia przepis przy modelu.

## Hypothesis Investigation

| Hypothesis | Evidence | Verdict |
| --- | --- | --- |
| Java przyjmuje dzień bez czytania przepisu | `dayFits` to nazwa, porcje, pasmo kcal i pułap tłuszczu (`WeekPlanValidator.java:54-55`). `goodDays` tylko to i `scaleDay` (`DietGenerator.java:225-233`). `select` dokłada unikalność śniadania, obiadu i kolacji (`DietGenerator.java:236-266`). `Meal` zamienia puste przygotowanie na `""` (`Meal.java:14-16`). PDF i tak drukuje danie (`PlanPdfWriter.java:486-492`). | STRONG |
| Model wymyśla dziesięć dni naraz, bez korpusu przepisów | Jedno do pięciu wywołań tego samego promptu o dziesięć dni (`DietGenerator.java:42`, `:53-138`, `:203`). Model `qwen2.5:3b` (`application.properties:33`). Przygotowanie ma być co najwyżej trzema zdaniami (`DietGenerator.java:69-76`). Żadna klasa nie czyta pliku przepisu. `kcal-per-100g.csv` to liczby na 100 g, nie sposób przyrządzenia. | STRONG |
| Wydrukowane gramy i sposób nie pochodzą z tabeli żywności | `scaled` kopiuje `ingredients()` i podmienia kcal oraz makro (`WeekPlanRepair.java:241-248`). `meal` kopiuje nazwę i przygotowanie (`WeekPlanRepair.java:136-137`). PDF drukuje te napisy (`PlanPdfWriter.java:416-423`, `:486-492`). | STRONG |
| Wzorów z sieci nie ma w kamieniu, a gotowalności nikt nie zmierzył | M-3: model pisze nazwy, składniki i przygotowanie, Java dobiera proporcje (`roadmap.md` Milestone, S-02). Parked odrzuca wywołanie na danie i dziesięć dni kandydatów. `change.md` modelu: „Bieżącej jakości generowania nie mierzono.” W repozytorium nie ma tekstu prawdziwego planu. | STRONG |

## Narrowing Signals

- Użytkownik wybrał najpierw pytanie, czy dania z modelu da się ugotować, a nie pulę przepisów zamiast modelu.
- To pytanie nie ma w repozytorium odpowiedzi: pliki PDF i JSON odpowiedzi nie są przechowywane, a testy mają ręcznie wpisane zdania w rodzaju „Usmaż schab.”
- Ściągnięcie wzorów odpowiedziałoby na inne pytanie: skąd bierze się tekst dania. Nie mówi, czy dania z ostatniego planu da się ugotować.

## Cross-System Convention

Kamień M-3 już rozstrzyga właściciela tekstu: model pisze przepis jednego dnia, aplikacja liczy gramy z tabeli na 100 g. Wzory z cudzych stron nie są w tym kamieniu. Poprzedni frame (`model-recipes-app-numbers`) mówi wprost, że jakości dań nie mierzono i że praca jest zmianą odpowiedzialności, nie poprawką po odczycie talerza.

## Reframed (or Confirmed) Problem Statement

> **The actual problem to plan around is**: nikt nie odczytał wygenerowanego tygodnia pod tym, czy danie da się ugotować, a Java i tak odda dzień, którego składniki się nie trzymają, byle przeszły liczby i nazwy.

Wzory z sieci nie rozstrzygają tego odczytu. Mogą później zastąpić tekst, który pisze model, ale to jest inna decyzja niż ta, którą użytkownik postawił na pierwszym miejscu. Dopóki talerz z prawdziwej odpowiedzi nie jest przeczytany, nie wiadomo, czy psuje się sam przepis, czy dopiero zestawienie przepisu z liczbami, które Java dopisała obok.

## Confidence

**MEDIUM** — ścieżka w kodzie jest jednoznaczna: gotowalność nie jest warunkiem, a żaden plan nie został zachowany do oceny. Kształt „nie da się ugotować” (składniki bez związku, ucięte kroki, ilości nie z tego dania) nie ma cytatu z prawdziwej odpowiedzi.

## What Changes for /10x-plan

Plan, jeśli powstanie, dotyczy odczytu prawdziwego tygodnia pod kątem gotowania, nie zaciągnięcia przepisów ze stron. Korpus wzorów zostaje poza planem, dopóki ten odczyt nie pokaże, że psuje się sam tekst dania.

## References

- Source files: `DietGenerator.java:42`, `DietGenerator.java:53-138`, `DietGenerator.java:203`, `DietGenerator.java:225-266`, `WeekPlanValidator.java:54-55`, `WeekPlanRepair.java:136-137`, `WeekPlanRepair.java:241-248`, `Meal.java:14-16`, `PlanPdfWriter.java:416-423`, `PlanPdfWriter.java:486-492`
- Related: `context/foundation/roadmap.md` (M-3, S-02, Parked), `context/changes/model-recipes-app-numbers/frame.md`, `src/main/resources/food/kcal-per-100g.csv`
- Investigation tasks: `8d817453-0573-4743-b3f6-195e8bae160b`, `8009d7e1-0e34-4d59-980c-977b82057fed`, `a79c7ea7-a7cc-4ecd-89d7-bd9af12f90d5`, `e8ad50b8-2e33-45e4-b6ac-f7187abef84d`
