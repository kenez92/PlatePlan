# Pułap tłuszczu dnia — Plan Brief

> Full plan: `context/changes/goal-macro-limits/plan.md`
> Frame brief: `context/changes/goal-macro-limits/frame.md`

## What & Why

Dzień redukcji przy około 2000 kcal i około 150 g tłuszczu jest udanym planem, bo dzień jest ograniczony tylko pasmem kalorii.

Plan dokłada górny limit tłuszczu i zapas dni. Każda zaznaczona osoba, niezależnie od celu, ma na każdym z siedmiu dni wyniku sumę tłuszczu co najwyżej 30% własnych potwierdzonych kilokalorii. Model proponuje dziesięć dni. Do PDF wchodzi siedem, które się mieszczą. Gdy pierwszej odpowiedzi tyle nie daje, jedna kolejna dopisuje swoje dobre dni za już zachowanymi.

## Starting Point

Prompt opisuje redukcję jako posiłki białkowe i prosi o sos albo tłuszcz, bez limitu gramów. Walidator sumuje tylko kilokalorie czterech porcji. Naprawa mnoży gramy tłuszczu tym samym stosunkiem co kilokalorie, więc udział z odpowiedzi modelu zostaje. PDF wypisuje sumę czterech pól `fatG` tej osoby. Przy celu 2000 kcal około 150 g tłuszczu mieści się energetycznie i przechodzi bramkę.

## Desired End State

Udany plan ma siedem stron dnia. Linia tłuszczu na każdej jest nie wyższa niż pułap tej osoby: przy 2000 kcal to 67 g, przy 3000 kcal to 100 g. Dzień równy pułapowi może wejść. Dzień ze 150 g przy celu 2000 kcal do PDF nie wchodzi. Cztery dobre dni z pierwszej odpowiedzi zostają, a druga odpowiedź dopisuje brakujące. Po dwóch odpowiedziach bez siedmiu dni plików nie ma.

## Key Decisions Made

| Decision | Choice | Why (1 sentence) | Source |
| --- | --- | --- | --- |
| Gdzie jest luka | Polecenie dla modelu i akceptacja tygodnia | Dzień 150 g przechodzi, bo prompt nie zna limitu, a walidator czyta tylko kilokalorie | Frame |
| Pułap | 30% własnych kcal, 9 kcal na gram, half-up do pełnego grama; dzień równy pułapowi przechodzi | Przy 2000 kcal to 67 g, więc 150 g odpada, a pułap skaluje się od 400 do 6000 kcal | Plan |
| Dni wyniku | Siedem dni, każdy w pułapie; dzień powyżej pułapu wypada z kandydatów | Obserwacją była linia tłuszczu jednego dnia w PDF | Plan |
| Kto | Każda zaznaczona osoba, przy każdym celu | Pułap ma obowiązywać także utrzymanie i masę, nie tylko redukcję | Plan |
| Różne cele przy stole | 30% własnego celu: Anna przy 2000 kcal ma 67 g, Bartek przy 3000 kcal ma 100 g; dzień 60 g i 90 g przechodzi | Wzór zostaje przy osobie, a nie przy najniższym celu stołu | Plan |
| Kandydaci | Prośba o dziesięć dni; do wyniku pierwsze siedem, które da się ułożyć | Trzy dodatkowe dni dają zapas, gdy część odpowiedzi jest poza planem | Plan |
| Ponowienie | Jedna kolejna próba; dobre dni z pierwszej zostają, dobre z drugiej dopisują się za nimi | Sześć dobrych dni z dwóch odpowiedzi nadal nie jest planem | Plan |
| Kolejność | Oceniać dzień po jednym skalowaniu | Naprawa potrafi podnieść 16 g przy 400 kcal do 80 g przy 2000 kcal, i taki dzień odpada sam | Frame |
| Białko i węglowodany | Poza tą zmianą | Obserwacją do zaplanowania został tłuszcz dnia | Frame |
| Wzór −250 i suma PDF | Poza tą zmianą | Ani korekta kalorii, ani dodawanie czterech pól `fatG` nie jest źródłem tych 150 g | Frame |

## Scope

**In scope:**

- Pułap gramów tłuszczu na każdym dniu każdej zaznaczonej osoby
- Prośba o dziesięć dni i wybór siedmiu, z jedną dopiską dobrych dni z drugiej odpowiedzi
- Ta sama liczba w prompcie i w `WeekPlanValidator`
- Zdanie z tym wzorem w PRD oraz w twardych regułach i sekcji Plan w `AGENTS.md`

**Out of scope:**

- Pułapy białka i węglowodanów
- Obcięcie gramów w naprawie, trzecie wywołanie modelu, zapis kandydatów w bazie, plan krótszy niż siedem dni, zmiana −250, zmiana sumy na PDF

## Architecture / Approach

`WeekPlanValidator.fatGramsLimit` zwraca half-up z potwierdzonych kilokalorii dzielonych przez 30. `dayFits` ocenia jeden dzień po skalowaniu. `DietGenerator` prosi o dziesięć dni, zostawia dobre w kolejności odpowiedzi i układa z nich siedem. Przy braku siedmiu to samo polecenie idzie jeszcze raz, a nowe dobre dni dopisują się na końcu listy. Lista zakupów sumuje składniki tylko tych siedmiu dni.

## Phases at a Glance

| Phase | What it delivers | Key risk |
| --- | --- | --- |
| 1. Bramka tłuszczu | `dayFits` i pułap 67 g przy 2000 kcal | Dzielenie całkowite dałoby 66 g zamiast 67 g przy 2000 kcal |
| 2. Dziesięć dni, siedem w planie | Wybór siedmiu i jedna dopiska | Drugie wywołanie wydłuża prośbę, która z pierwszej odpowiedzi nie uzbierała siedmiu |
| 3. Kontrakt w PRD i AGENTS.md | Jedno zdanie w trzech miejscach | Twarde reguły zostałyby przy samym paśmie kalorii, gdyby zmienić tylko sekcję Plan |

**Prerequisites:** Generowanie tygodnia, naprawa porcji i pasmo ±10% są już w kodzie. Nie trzeba schematu ani klucza, żeby przejść fazę 1.
**Estimated effort:** Około jednej sesji na fazę, trzy fazy.

## Open Risks & Assumptions

- Po dwóch odpowiedziach bez siedmiu dobrych dni prośba wraca bez PDF. Zdanie o gęstych kalorycznie posiłkach przy masie nadal zwiększa liczbę dni poza pułapem.
- Naprawa zachowuje udział tłuszczu. Dzień, który przed skalowaniem wygląda skromnie, po dojściu do celu kalorii może przekroczyć pułap i wypaść z kandydatów.
- Pułap 30% jest decyzją tej sesji. Przy 2000 kcal odcina 68 g i zostawia 67 g. Lista kandydatów nie jest zapisywana w bazie.

## Success Criteria (Summary)

- PDF ma siedem dni. Dzień przy 2000 kcal i około 150 g tłuszczu na żadnej stronie się nie pojawia.
- Cztery dobre dni z pierwszej odpowiedzi zostają na początku, a druga odpowiedź dopisuje brakujące aż do siedmiu.
- Prompt, walidator, PRD i `AGENTS.md` opisują ten sam wzór: half-up z potwierdzonych kilokalorii dzielonych przez 30.
