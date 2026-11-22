# Frame Brief: Model pisze przepisy, aplikacja liczy gramy i kcal

> Framing step before /10x-plan. This document captures what is *actually*
> at issue, separated from what was initially assumed.

## Reported Observation

PlatePlan ma zmienić odpowiedzialność. Model układa tylko nazwy posiłków na 10 dni. Osobne wywołanie na danie zwraca składniki w gramach dla wybranych osób. Java zna cele (`targetCalories`, `fatLimit`, `proteinTarget`) i liczy kalorie oraz makro z tabeli `food` (kcal, białko, tłuszcz, węglowodany na 100 g). Osobno: na GTX 1660 Ti warto zmierzyć `qwen2.5:7b` albo `qwen3:4b` — czas, jakość dań, poprawność JSON.

## Initial Framing (preserved)

- **User's stated cause or approach**: Słaby punkt to liczenie kalorii, tłuszczu i makro w promptcie. Przeniesienie tych rachunków do Javy da większy wzrost jakości niż skok z modelu 4B na 12B. Qwen 2.5 7B będzie lepszy dietetycznie od 4B i po kwantyzacji powinien zmieścić się na tym sprzęcie.
- **User's proposed direction**: Cztery kroki (struktura nazw, cele w Javie, wywołanie na danie, gramatury z bazy) plus pomiar większego modelu lokalnego.
- **Pre-dispatch narrowing**: Rozdzielić na osobne zadania i zaplanować je przez /10x-plan. Bieżącej jakości nie mierzono; zmiana odpowiedzialności ma iść niezależnie od obecnych błędów. Tabeli żywności nie ma; jej istnienie jest częścią zmiany. Później: „Obecnie wszystko dostajemy z LLM. Chcemy aby AI teraz wymyślał tylko przepisy. Kcal gramy itp będzie liczyć aplikacja.” Cel białka: użytkownik poprosił o wybór; decyzja framu jest poniżej.

## Dimension Map

The observation could originate at any of these dimensions:

1. **Rachunek w modelu** — prompt każe modelowi pisać `kcal`, `proteinG`, `carbsG`, `fatG` i samemu sumować tłuszcz. ← initial framing
2. **Przepisywanie liczb w Javie** — dzień jest skalowany do potwierdzonych kcal, tłuszcz ścinany do pułapu, węglowodany dobierane tak, by 4/9/4 zeszły się z kcal. Napisy ilości zostają.
3. **Jedno wywołanie na cały tydzień** — jedna odpowiedź niesie dziesięć dni, przepisy, porcje i listę zakupów. Osobnego promptu na danie nie ma.
4. **Zakres produktu** — cel kcal i pułap tłuszczu już należą do aplikacji. Cel białka, tabela żywności, wywołanie na danie i model 7B/4B w kontrakcie nie występują.

## Hypothesis Investigation

| Hypothesis | Evidence | Verdict |
| --- | --- | --- |
| Model jest jedynym właścicielem liczb, które trafiają do planu | Prompt żąda czterech liczb i sumy tłuszczu (`DietGenerator.java:65-68`, `117-119`). `Ingredient.amount` to napis (`Ingredient.java:8`). Brak mnożenia gramów przez wartości na 100 g. Brak tabeli `food` w changelogu (`001`–`008`). Java nadpisuje te cztery liczby przed akceptacją (`WeekPlanRepair.java:241-248`, `193-196`). | WEAK |
| Java już dopina sumę kcal dnia i pułap tłuszczu, a ilości składników nie są źródłem tych liczb | `scaled` kopiuje `ingredients` i podmienia kcal oraz makro (`WeekPlanRepair.java:27-30`, `241-248`). Pułap to zaokrąglenie połową w górę `kcal/30` (`WeekPlanValidator.java:34`, `40-43`). `PlanPerson` to nazwa, cel, kcal (`PlanPerson.java:11`). Po skali pasmo ±10% i pułap tłuszczu są spełnione; odrzucenie dnia zostaje przy pustej nazwie albo dniu, którego nie da się zestawić. | STRONG |
| Kontrakt generowania to jedno wywołanie na tydzień | Jedyny `ChatClient` w `DietGenerator.java:203`. Prompt prosi o 10 dni i listę zakupów (`53-62`, `99-100`). Do pięciu prób, krótszy tydzień zostaje (`42`, `177-186`). `WeekPlanOutput` skleja ucięty JSON i liczby w cudzysłowie. Promptu samych nazw ani promptu na danie nie ma. | STRONG |
| Propozycja dokłada zakres, którego produkt nie ma | PRD: ±10%, pułap kcal/30, model dostaje dziesięć dni (`prd.md` Business Logic). `AGENTS.md` każe modelowi zsumować listę zakupów. Celu białka nie ma w PRD ani w `PlanPerson`. Archiwum `2026-11-07-goal-macro-limits` odłożyło limit białka i węglowodanów (`frame.md` Reframed). Model to `qwen2.5:3b`; wagi nie wchodzą na maszynę Fly (`infrastructure.md`). | STRONG |

## Narrowing Signals

- Użytkownik nie mierzył odpadających dni, słabych dań ani JSON. To zmiana odpowiedzialności, nie naprawa zmierzonej awarii.
- Tabela żywności ma powstać w tej pracy. W schemacie jej nie ma.
- „Wszystko dostajemy z LLM” opisuje prompt, nie plik PDF. Java już przepisuje kcal, tłuszcz i węglowodany. Napis ilości zostaje z modelu, więc gramatura i wydrukowane liczby nie są jednym rachunkiem.
- Użytkownik chce, aby model wymyślał tylko przepisy, a kcal i gramy liczyła aplikacja. To jest stan docelowy. Kod tego stanu nie ma: model nadal pisze liczby, a Java liczy ze stosunku tych liczb, nie z żywności.
- Cel białka: użytkownik poprosił o wybór. Zostaje poza tą pracą. Aplikacja ma go policzyć jako wynik (ile białka jest w porcjach). Trzeci cel obok kcal i pułapu tłuszczu to nowa reguła diety. Poprzedni frame ją odłożył. Przy gotowym przepisie gramy mają za mało swobody, żeby naraz trafić w kcal, tłuszcz i białko.

## Cross-System Convention

W tym projekcie aplikacja ma potwierdzone kcal i pułap tłuszczu (`kcal/30`). Model pisze gramy makro porcji. `WeekPlanRepair` skaluje te gramy do celu i ścina tłuszcz do pułapu, a potem dobiera węglowodany. Archiwum `goal-macro-limits` planowało właśnie ten pułap i wprost odłożyło limit białka. Lista zakupów w `AGENTS.md` jest sumą modelu; naprawa umie też zsumować składniki już przeskalowanych dni. Większy model lokalny nie zmienia właściciela liczb. Wagi zostają poza maszyną Fly, a domyślny model to `qwen2.5:3b`.

## Reframed (or Confirmed) Problem Statement

> **The actual problem to plan around is**: model w jednym wywołaniu wymyśla przepis i liczby, a Java dopina kcal oraz tłuszcz przez przepisanie tych liczb, bez liczenia z gramów żywności.

Początkowe ujęcie („program nie zna liczb, model je liczy”) pokrywa prompt i nie pokrywa naprawy. Cel kcal i pułap tłuszczu już są w Javie. Luka jest taka, że źródłem liczb nie jest składnik i jego masa. Żeby model został przy przepisie, a aplikacja liczyła gramy i kcal, potrzebna jest tabela żywności, której nie ma, i inny kształt wywołania niż jeden tydzień z liczbami. Cel białka do tego nie wchodzi: białko ma wyjść z rachunku. Pomiar `qwen2.5:7b` albo `qwen3:4b` jest osobnym zadaniem i nie jest tą luką.

## Confidence

**HIGH** — trzy hipotezy mają zgodne cytowania w kodzie, PRD i archiwum. Użytkownik potwierdził stan docelowy (przepis z modelu, liczby z aplikacji) i to, że tabeli jeszcze nie ma. Jakości generowania nie mierzono; plan jest zmianą odpowiedzialności, nie poprawką po pomiarze.

## What Changes for /10x-plan

Plan ma rozdzielić zadania wokół cięcia, którego kod nie ma: model zostawia przepis, aplikacja liczy gramy i kcal z nowej tabeli żywności, przy istniejącym celu kcal i pułapie tłuszczu. Osobno zostają cel białka (poza pracą) i pomiar większego modelu lokalnego.

## References

- Source files: `DietGenerator.java:53-139`, `DietGenerator.java:203`, `WeekPlanRepair.java:24-37`, `WeekPlanRepair.java:241-258`, `WeekPlanValidator.java:34-43`, `PlanPerson.java:11`, `Ingredient.java:8`
- Product: `context/foundation/prd.md` (Business Logic), `context/foundation/infrastructure.md`, `AGENTS.md` (Plan)
- Prior decision: `context/archive/2026-11-07-goal-macro-limits/frame.md` (Reframed)
- Investigation tasks: `e0b3ba76-ace6-455c-89ce-372c74b1ca29`, `8b68750a-c58c-48e6-87be-9eec4654390a`, `abb68c96-1668-47ae-ad46-dd59e15e531a`, `8c827884-ca2c-43ff-96fb-80fa4b75beb6`
