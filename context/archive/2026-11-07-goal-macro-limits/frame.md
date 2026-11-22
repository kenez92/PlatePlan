# Frame Brief: Tłuszcz dnia na redukcji

> Framing step before /10x-plan. This document captures what is *actually*
> at issue, separated from what was initially assumed.

## Reported Observation

W obecnej chwili mój plan na redukcje potrafi dostarczyc nawet 150g tłuszczu dziennie. To zdecydowanie za dużo.

Szerszy efekt zapisany w notatce zmiany: plan, który został wygenerowany, nie powinien mieć za dużo tłuszczu, białka albo węglowodanów w zależności od celu.

## Initial Framing (preserved)

- **User's stated cause or approach**: Prompt.
- **User's proposed direction**: Poprawić prompt.
- **Pre-dispatch narrowing**: Plan na redukcję pokazał około 150 g tłuszczu na dzień. Ta liczba była w sumie jednego dnia w PDF.

## Dimension Map

The observation could originate at any of these dimensions:

1. **Tekst celu w prompcie** — redukcja jest opisana jako „protein-forward” i „fat loss”, bez limitu gramów tłuszczu. Prompt prosi też o sos albo tłuszcz jako przyprawę. ← initial framing
2. **Akceptacja planu** — dzień odpada tylko wtedy, gdy suma kilokalorii wychodzi poza ±10% celu. Gramy tłuszczu nie są czytane.
3. **Przeskalowanie porcji** — naprawa mnoży gramy tłuszczu tym samym stosunkiem co kilokalorie i może podnieść sumę dnia.
4. **Suma na PDF** — linia dnia dodaje cztery pola `fatG`. Mogłaby pokazać inną liczbę niż posiłki albo sumę kilku osób.
5. **Znaczenie redukcji** — `LOSE_WEIGHT` odejmuje 250 kcal. Wysoki cel energetycznie mieści 150 g tłuszczu i nadal jest redukcją.

## Hypothesis Investigation

| Hypothesis | Evidence | Verdict |
| --- | --- | --- |
| Prompt nie ogranicza gramów ani udziału tłuszczu | `DietGenerator.java:38-39` (`weight loss … protein-forward meals that support fat loss`), `:50-51` (`fatG` jako pełne gramy), `:93-94` (jedyne pasmo liczbowe to ±10% kcal). Test promptu utrwala to brzmienie i nie wymaga limitu tłuszczu (`DietGeneratorTest`). | STRONG |
| Java przyjmuje dzień z ~150 g tłuszczu, gdy kcal są w paśmie | `WeekPlanValidator.java:87-110` sumuje tylko `Portion::kcal`. `DietGenerator.java:143-150` puszcza tydzień dalej po tym teście. Cel nie wchodzi do walidatora. | STRONG |
| Naprawa sama tworzy 150 g | `WeekPlanRepair.java:165-183` liczy `grams × newKcal / originalKcal`. Udział tłuszczu w kaloriach zostaje udziałem modelu. Test podnosi 16 g przy 400 kcal do 80 g przy 2000 kcal, czyli ten sam stosunek (`WeekPlanRepairTest`). | WEAK |
| PDF pokazuje inną liczbę niż cztery posiłki | `PlanPdfWriter.java:44` i `:212-224`: `Tłuszcz %d g` to suma czterech `fatG` tej osoby. `:43` i `:197` drukują cel z konta, nie z modelu. Osobnej sumy tłuszczu gospodarstwa nie ma. | NONE |
| 150 g wynika z tego, że redukcja to tylko −250 kcal przy wysokim celu | `CalorieService.java:27` i `:67-69`. Potwierdzone kcal to liczba 400–6000. 150 g tłuszczu to około 1350 kcal, więc przy celu 2000 kcal mieści się energetycznie. | PARTIAL |

## Narrowing Signals

- Użytkownik wskazał sumę jednego dnia w PDF planu na redukcję, nie osobną obserwację o białku albo węglowodanach przy innych celach.
- Obok 150 g stał cel 2000 kcal. 150 g to około dwóch trzecich energii tego dnia, nie skutek skrajnie wysokiego celu.
- Cztery posiłki sumowały się do około 150 g, a kalorie dnia wyglądały normalnie. To wyklucza przekłamanie linii dnia.
- PDF w ogóle powstał, więc tydzień przeszedł bramkę akceptacji. Niezależne przejście tej samej ścieżki nie znalazło sprawdzenia, które odrzuciłoby 150 g tłuszczu przy 2000 kcal.

## Cross-System Convention

W tym projekcie dzień planu ma jedną liczbową regułę: cztery porcje osoby mieszczą się w ±10% jej potwierdzonych kilokalorii. Białko, węglowodany i tłuszcz są polami porcji i linią dnia, od pierwszego generowania (`context/archive/2026-10-05-generate-diet-with-ollama`) i po dołożeniu osób (`context/changes/household-members`). Wcześniejsza decyzja nie stawia limitu gramów. Niezależne czytanie ścieżki wskazuje ten sam punkt: model dostaje `fatG` bez limitu, a później kod te gramy kopiuje, skaluje i drukuje.

## Reframed (or Confirmed) Problem Statement

> **The actual problem to plan around is**: dzień redukcji przy około 2000 kcal i około 150 g tłuszczu jest udanym planem, bo dzień jest ograniczony tylko pasmem kalorii.

Prompt rzeczywiście nie stawia limitu tłuszczu, więc podana przyczyna jest prawdziwa. Nie jest całością. Ten sam dzień przechodzi akceptację i trafia do PDF. Naprawa porcji zachowuje udział tłuszczu z odpowiedzi modelu i nie jest osobnym źródłem tych 150 g. Wzór z korektą −250 wyjaśnia, czemu 2000 kcal nadal może być redukcją. Nie wyjaśnia, czemu dwie trzecie tego dnia to tłuszcz.

Szersze zdanie o białku i węglowodanach przy każdym celu zostało odłożone przez zawężenie. Obserwacją do zaplanowania jest tłuszcz dnia na redukcji.

## Confidence

**HIGH** — brak limitu tłuszczu jest w prompcie i w akceptacji, zgodny z dotychczasową regułą „tylko kalorie”, a użytkownik potwierdził cel 2000 kcal i sumę czterech posiłków.

## What Changes for /10x-plan

Plan ma dotyczyć braku ograniczenia tłuszczu na dniu redukcji, który już trafia w swoje kalorie. Luka jest w poleceniu dla modelu i w tym, że dzień z około 150 g tłuszczu zostaje przyjęty. Poza zakresem tej obserwacji są zmiana wzoru −250 oraz zmiana sposobu sumowania linii w PDF.

## References

- Source files: `DietGenerator.java:38-39`, `:50-51`, `:93-94`, `:143-150`; `WeekPlanValidator.java:87-110`; `WeekPlanRepair.java:165-183`; `PlanPdfWriter.java:43-44`, `:197`, `:212-224`; `CalorieService.java:27`, `:67-69`
- Related research: brak `research.md`
- Investigation tasks: [prompt](7e676145-4bb5-4ccb-98f7-45ef59ce29c2), [validator](4b297b10-5d96-4b25-9546-2a9c73fac62e), [repair](c95fef38-8049-464e-9cc3-c5a88be473d7), [pdf and goal](3ebee6b8-48ad-4120-b161-ad685cec06cc), [unbiased path](f18e5ba5-9470-4b80-b3fe-180d628f8b7a)
