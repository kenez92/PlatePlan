# Przejrzysty front zachęcający do założenia konta — Plan Brief

> Full plan: `context/changes/front-views/plan.md`
> Research: `context/changes/front-views/research.md`

## What & Why

Start, rejestracja i stopka opisują plan na jeden dzień i jedną osobę, a produkt układa siedem dni od jutra dla właściciela i najwyżej trzech bliskich. Prośba o konto jest drugim przyciskiem obok logowania. Front ma brzmieć jak konkretna reklama i wyglądać jak jedna aplikacja.

## Starting Point

Sześć plików Thymeleaf i jedenaście zmiennych w `site.css`. Logowanie zostaje na `/`. Profil i plan już mówią o siedmiu dniach i trzech dodanych osobach. `PersonBoundsValidator` nie porównuje celu z wiekiem, a odmowa `BODY` u właściciela pokazuje komunikat awarii zapisu.

## Desired End State

Niezalogowany czyta „Cały tydzień posiłków. Zero zgadywania.” i klika „Załóż konto”. Zalogowany wchodzący na `/` ląduje na `/plan`. Cel „Schudnąć” zapisuje się od 2. roku życia. Profil i plan zostają przy tych samych polach, w tym samym kolorze błędu i przycisku.

## Key Decisions Made

| Decision | Choice | Why (1 sentence) | Source |
| --- | --- | --- | --- |
| Tekst publiczny | Reklama: tydzień od jutra, do czterech osób, aktywność, dwa pliki, przycisk „Załóż konto” | Ma być konkretna i zgodna z produktem, nie z dawnym „kolejnym dniem”. | Plan |
| Logowanie | Zostaje w pasku | Start jest oknem logowania, a test wymaga formularza przed treścią. | Research |
| Zalogowany start | `GET /` przekierowuje na `/plan` | Osoba z kontem nie czyta reklamy konta. | Plan |
| Profil i plan | Nowy układ, te same pola i komunikaty | Ekrany mają być schludne, a testy trzymają etykiety i akcje. | Plan |
| Wygląd | Te same tokeny w `site.css`, bez nowej biblioteki | Stos nie wymienia frameworka CSS. | Research |
| Formularz właściciela | Osobny od fragmentu osoby | Właściciel ma e-mail i pustą nazwę „Ja”. | Plan |
| Cel dziecka | „Schudnąć” zapisuje się od 2. roku życia, razem z utrzymaniem i przytyciem | 16-latek z nadwagą ma móc wybrać schudnięcie. Osobnego progu wieku nie ma. | Korekta |
| Istniejące wiersze | Bez migracji | Reguła działa przy następnym zapisie ciała. | Plan |

## Scope

**In scope:** tokeny dla hexów spoza `:root`; tekst startu, rejestracji i stopki; przekierowanie zalogowanego `/` oraz zalogowanej rejestracji na `/plan`; układ profilu i planu. Cel „Schudnąć” jest dozwolony od 2. roku życia.

**Out of scope:** nowa biblioteka UI; zmiana nazw `--paper` i `--green`; ukrycie „Schudnąć”; migracja starych wierszy; inny wzór kalorii; kolory PDF; strona błędu; zdjęcie logowania z paska.

## Architecture / Approach

Thymeleaf i jeden arkusz. Faza 1 dokłada cztery zmienne. Faza 2 zmienia tekst i dwa przekierowania. Faza 3 dokłada osobny wynik odmowy, nie `BODY`. Faza 4 układa profil i plan na tych tokenach, zostawiając `data-product-list` i teksty przycisków.

## Phases at a Glance

| Phase | What it delivers | Key risk |
| --- | --- | --- |
| 1. Tokeny kolorów | Błąd, biel i hover z `:root` | Testy Javy tego nie widzą |
| 2. Tekst i wejście | Reklama, „Załóż konto”, zalogowany start na `/plan` | Test kolejności logowania i e2e po `POST /login` |
| 3. Cel poniżej 18 lat | Odmowa zapisu i zdanie przy celu | Podpięcie pod `BODY` pokazałoby komunikat awarii bazy |
| 4. Układ profilu i planu | Czytelniejsze ekrany przy tych samych polach | Ruszenie `data-product-list` psuje dodawanie produktów |

**Prerequisites:** badanie `front-views` i zatwierdzone cztery fazy. Baza i klucz Ollama nie są potrzebne do testów kontrolera.
**Estimated effort:** około dwóch sesji na cztery fazy.

## Open Risks & Assumptions

- `PlanDownloadE2eTest` czeka na `.signed-in-name` po zalogowaniu. Przekierowanie z `/` na `/plan` musi zostawić ten pasek na stronie, na której czekanie się kończy.
- Wiersz już zapisany z wiekiem poniżej 18 lat i „Schudnąć” zostaje do następnej edycji ciała.
- Granica 18 lat nie jest w PRD. Jest decyzją tej sesji.

## Success Criteria (Summary)

- Niezalogowany widzi reklamę tygodnia i zakłada konto z pierwszego ekranu.
- Zalogowany `GET /` otwiera plan.
- Wiek 16 lat i „Schudnąć” zapisuje ciało. Zdania o granicy 18 lat nie ma.
- Profil i plan wyglądają jak reszta aplikacji, a dotychczasowe pola i pobranie PDF działają.
