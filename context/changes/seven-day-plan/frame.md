# Frame Brief: Plan na siedem dni

> Framing step before /10x-plan. This document captures what is *actually*
> at issue, separated from what was initially assumed.

## Reported Observation

Zacznijmy pracę nad kolejnym change. Czyli teraz aby plan generował się na 7 dni, a nie tylko na jeden jak poprzednio.

## Initial Framing (preserved)

- **User's stated cause or approach**: Kolejna zmiana po mailu to wydłużenie generate z jednego dnia do siedmiu.
- **User's proposed direction**: Zacząć change, w którym plan generuje się na 7 dni zamiast na jeden.
- **Pre-dispatch narrowing**: Jeden generate daje dietę na jutro i listę zakupów na jutro. Oba pliki mają trwać siedem dni. W tej obserwacji są też kolejne osoby na koncie (na przykład dziecko i żona).

## Dimension Map

The observation could originate at any of these dimensions:

1. **Horyzont jednego generate** — dieta, lista, PDF, strona i mail są na jeden dzień („jutro”). Wydłużenie tego samego generate do siedmiu dni jest całą zmianą. ← initial framing
2. **Dla kogo jest plan** — dopisane osoby (dziecko, żona) są inną jednostką niż długość planu. Jeden wiersz profilu i wcześniejsza kolejność trzymają rodzinę osobno.
3. **Jednostka kalorii i celu** — jedna dzienna liczba i jeden cel jednej osoby. Tydzień i druga osoba to dwie różne luki w tej jednostce.
4. **Co jeden generate oddaje** — para PDF-ów (`dietPdf`, `shoppingListPdf`) podpisana na jutro, dla jednej osoby. Tydzień i rodzina wchodzą w tę samą parę plików.

## Hypothesis Investigation

| Hypothesis | Evidence | Verdict |
| --- | --- | --- |
| Horyzont: generate jest jednym następnym dniem | `DietPlan.java:6-13` (cztery posiłki i jedna lista). `DietGenerator.java:37-45` („next-day”, lista „for the whole day”). `plan.html:3,14,30` („Na jutro”, `dieta-na-jutro.pdf`). `PlanMailSender.java:26-28`. Brak kolekcji dni w `plan`. | STRONG |
| Dla kogo: rodzina to inna zmiana niż tydzień | `003-create-user-profile.xml:10-18` — PK `login`, jeden profil. `prd.md` Non-Goals: wiele osób później, ta wersja to jedno konto. `email-plan-on-generate/change.md` Notes: (2) rodzina, (3) tydzień, osobno. `AGENTS.md` Hard rules: rodzina i tydzień to późniejsze zmiany. | STRONG |
| Kalorie: jedna dzienna liczba jednej osoby | `UserProfile.java:51-52` jeden `Integer`. `DietGenerator.java:47,69-72` jeden cel i jeden „daily calorie target”. `PlanService.java:55-59` jeden wiersz. Brak liczby na dzień tygodnia i na drugą osobę. | STRONG |
| Dostawa: dwa PDF-y na jutro, jeden jedzący | `PlanFilesDto` — dokładnie `dietPdf` i `shoppingListPdf`. `PlanMailSender.java:40,52-53` te same dwa załączniki. Brak trzeciego pliku i pliku na osobę. | STRONG |

## Narrowing Signals

- Po mapie: ta zmiana to siedem dni dla jednej osoby, która już ma profil. Kolejne osoby zostają poza tą zmianą.
- Po mapie, później zastąpione: liczba kcal jest przy osobie, a dni i osoby nazwano jedną obserwacją.
- Wcześniejsze zawężenie („również dziecko i żona”) zostało później wyłączone z tej zmiany. Zostaje w Initial Framing, nie wchodzi do problemu do zaplanowania.
- Wcześniejsza kolejność (mail → rodzina → tydzień) w `email-plan-on-generate/plan-brief.md` zostaje jako split na osobne zmiany. Kolejność się odwraca: tydzień teraz, rodzina później.
- Po dyskusji, słowa użytkownika: w tej zmianie oczekuje planu na 7 dni dla jednej osoby. Osoby dołożymy w kolejnej zmianie. Kalorie wyliczone są dzienne, więc przy generate na 7 dni każdy dzień powinien mieć mniej więcej tyle samo kalorii. Na jednej stronie PDF oczekuje jednego dnia z diety.

## Cross-System Convention

W tym repozytorium rodzina i tydzień są dwiema późniejszymi zmianami, rodzina pierwsza (`email-plan-on-generate/plan-brief.md`, sekcja Key Decisions, „Kolejność rozszerzeń”; `plan.md` „Zmiana po rodzinie”). Generate, który istnieje, to jeden następny dzień, dwa PDF-y, jeden jedzący, jedna dzienna liczba kcal. Prowadząca hipoteza zgadza się ze splitem (osobne zmiany) i z kształtem generate. Nie zgadza się z kolejnością „najpierw rodzina”. Użytkownik tę kolejność świadomie odwrócił.

## Reframed (or Confirmed) Problem Statement

> **The actual problem to plan around is**: jeden generate dla jednej osoby, która już ma profil, ma dać dietę i listę zakupów na siedem dni, a nie na jeden następny dzień.

Początkowe ujęcie się trzyma. Horyzont „jutro” jest w modelu, prompcie, PDF, stronie i mailu. Użytkownik potwierdził w dyskusji: ta zmiana to plan na siedem dni dla jednej osoby, a kolejne osoby wchodzą w następnej zmianie. Dzienna liczba kcal zostaje ta sama w przybliżeniu na każdy z siedmiu dni. W PDF diety jedna strona to jeden dzień.

## Confidence

**HIGH** — horyzont jednego dnia ma mocne ślady w kodzie, a dyskusja rozstrzyga zakres, kalorie i stronę PDF. Split rodzina/tydzień zostaje; kolejność „najpierw rodzina” użytkownik odwraca świadomie. Każdy dzień ma trzymać się tej samej dziennej liczby kcal. Jedna strona PDF diety to jeden dzień.

## What Changes for /10x-plan

Plan dotyczy diety i listy zakupów na siedem dni dla jednego istniejącego konta. Kolejne osoby są następną zmianą. Każdy dzień trzyma się wyliczonej dziennej liczby kcal, w przybliżeniu tej samej. W PDF diety jedna strona pokazuje jeden dzień.

## References

- Source files: `src/main/java/com/kenez92/plateplan/plan/model/DietPlan.java:6-13`, `src/main/java/com/kenez92/plateplan/plan/service/DietGenerator.java:37-47`, `src/main/java/com/kenez92/plateplan/plan/service/PlanMailSender.java:26-28`, `src/main/java/com/kenez92/plateplan/profile/db/UserProfile.java:51-52`, `src/main/resources/db/changelog/changes/003-create-user-profile.xml:10-18`, `context/foundation/prd.md` Non-Goals, `context/changes/email-plan-on-generate/plan-brief.md` (kolejność rozszerzeń)
- Related research: brak `research.md`
- Investigation tasks: [horyzont](99961616-f990-4b30-aacf-1a9642bb219c), [dla kogo](448eb8c1-416f-4b2b-93d6-bfde16da68ef), [kalorie](fb5e83e9-da9b-4f72-9819-4e1c20e62b26), [dostawa](d3d636d3-bb00-4b41-8337-1fe013b7dd1f)
