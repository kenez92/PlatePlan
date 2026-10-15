# Domownicy w planie tygodnia — Plan Brief

> Full plan: `context/changes/household-members/plan.md`

## What & Why

Jedno konto ma układać tydzień dla właściciela i dodanych osób, na przykład żony i dziecka, bez drugiego logowania. Każda osoba ma własne ciało, cel i listy produktów. Kuchnia gotuje jedno danie, a porcje rozchodzą się według kcal tej osoby.

## Starting Point

Dziś `user_profile` to jeden wiersz na login. Wiek zaczyna się od 10 lat, waga od 20 kg, a ręczna liczba kcal od 800. Generate wysyła jeden cel i jedną liczbę, a PDF drukuje jeden „Cel dnia”.

## Desired End State

Na `/profile` widać właściciela i najwyżej trzy dodane osoby. Na `/plan` wszyscy startują zaznaczeni. Jedno generowanie zwraca `dieta-na-tydzien.pdf` i `lista-zakupow.pdf`: wspólne dania, porcja każdej zaznaczonej osoby w paśmie ±10% jej kcal, jedna lista zakupów. Pusta nazwa właściciela brzmi w planie „Ja”.

## Key Decisions Made

| Decision | Choice | Why (1 sentence) |
| --- | --- | --- |
| Liczba kcal | Ten sam wzór Mifflin-St Jeor, aktywność i ±250 | Każda osoba liczy się z wieku, wzrostu, wagi, płci, aktywności i jednego z trzech celów. |
| Granice ciała | Wiek 2–110, wzrost 70–250 cm, waga 8,0–400,0 kg | Osoba 2 lata i 13 kg ma się zapisać i dostać liczbę z tego wzoru. |
| Cel | Zostają schudnięcie, utrzymanie i przyrost | Rozwój dziecka to wybór utrzymania albo przyrostu, bez czwartej wartości. |
| Jadłospis | Jedno danie, porcja i kcal osobno, dwa PDF-y | Kuchnia gotuje raz, a dzień każdej osoby trzyma się jej liczby. |
| Listy produktów | Własne listy każdej osoby | Każdy wnosi swoje lubię i nie jadam. |
| Wykluczenie | Wygrywa u wszystkich zaznaczonych w tym generate | Jeden garnek nie zawiera produktu, którego nie je ktoś przy stole. |
| Kto w generate | Checkboxy na `/plan`, domyślnie wszyscy, co najmniej jedna osoba | Tydzień bez kogoś nie kupuje jego jedzenia. |
| Nazwa | Obowiązkowa u domownika; właściciel bez nazwy to „Ja” | Porcja w PDF ma podpis bez dokładania imienia do istniejącego profilu. |
| Unikalność nazw | Bez wielkości liter, razem ze skuteczną nazwą właściciela | Dwie osoby „Ja” albo „Anna” nie rozróżniają się na talerzu. |
| Limit | Najwyżej 4 osoby z właścicielem | Zostaje zapas ponad trójkę, a jeden prompt jeszcze mieści porcje. |
| Zakres kcal | 400–6000 dla wzoru i dla ręcznej edycji | 795 i 416 kcal da się zapisać; 166 i 9837 odrzucają zapis i nic nie zmieniają. |
| Ekran | Sekcja na `/profile`, osobny zapis każdej osoby | Literówka przy dziecku nie nadpisuje właściciela ani żony. |

## Scope

**In scope:**

- Nazwa właściciela, tabela najwyżej trzech domowników, te same pola ciała i kcal.
- Wspólne dania, porcje, pasmo na osobę, scalenie list tylko z zaznaczonych.
- Checkboxy, dwa dotychczasowe PDF-y, mail z tymi samymi plikami.
- Aktualizacja PRD, `AGENTS.md` i punktu Parked o wielu osobach.

**Out of scope:**

- Czwarty cel, osobna matematyka dziecka, osobny plik diety na osobę.
- Drugie konto, rola, e-mail domownika, zapis PDF-ów, druga próba modelu.
- Pamięć odznaczeń między wizytami i wykluczenia osoby odznaczonej.

## Architecture / Approach

Właściciel zostaje w `user_profile`. Domownicy to wiersze `household_member` przy tym samym loginie. Generate składa zaznaczonych w kolejności właściciel, potem kolejność dodania, woła model raz i odrzuca tydzień, który nie domyka pasma którejkolwiek zaznaczonej osoby. Brak parametru `person` oznacza wszystkich. Pusty wybór albo obce id zwraca `SELECTION_REQUIRED` bez PDF-ów.

## Phases at a Glance

| Phase | What it delivers | Key risk |
| --- | --- | --- |
| 1. Zapis osób | Schemat, granice, nazwy, zakres 400–6000, limit trzech | Wzór poza zakresem odrzuca cały zapis ciała, także właściciela |
| 2. Ekran profilu | Sekcja i osobne formularze | Długi `/profile` przy czterech osobach |
| 3. Generowanie | Prompt, porcje, walidator, scalenie list | Jedno wywołanie łatwiej wypada z pasma przy czterech osobach |
| 4. Ekran planu i PDF | Checkboxy, cel każdej osoby, dwa pliki | Zmiana kształtu posiłku psuje writer, jeśli porcje wejdą bez kompilacji |
| 5. Opis produktu | PRD, AGENTS, roadmapa | Stary opis z progiem 800 i jedną osobą zostaje w regułach |

**Prerequisites:** Działający profil, wzór kcal i generate na siedem dni.
**Estimated effort:** Około 4–6 sesji w pięciu fazach, jedna osoba.

## Open Risks & Assumptions

- Przy czterech osobach model częściej nie zmieści porcji w paśmie, a generate zwróci `UNAVAILABLE` bez drugiej próby.
- Wzór dla wieku 2 lat potrafi być daleko od zaleceń dla dziecka; liczba i tak pochodzi z tego wzoru.
- Wynik 166 kcal i 9837 kcal przy skrajnych ciałach nie zapisze się.
- Odznaczona osoba nie wnosi wykluczenia do tego tygodnia.

## Success Criteria (Summary)

- Dziecko o wzorze 795 kcal zapisuje się na `/profile`, a wynik 166 kcal nie nadpisuje poprzedniej liczby.
- Generate dla zaznaczonych osób zwraca dwa PDF-y; dzień każdej osoby jest w jej paśmie ±10%, a wykluczony produkt nie wchodzi do garnka.
- Wejście na `/plan` zaznacza wszystkich, odznaczenie jednej osoby zabiera jej porcję i jej wykluczenia z tego pliku.
