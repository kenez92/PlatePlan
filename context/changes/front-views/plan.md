# Przejrzysty front zachęcający do założenia konta — Implementation Plan

## Overview

Cztery ekrany PlatePlan mają wyglądać jak jedna aplikacja i mówić prawdę o produkcie: siedem dni od jutra, do czterech osób, dwa pliki. Start niezalogowany prosi o konto. Zalogowany wchodzący na `/` ląduje na `/plan`. Osoba poniżej 18 lat nie zapisze celu „Schudnąć”.

## Current State Analysis

Widoki to Thymeleaf i jeden arkusz `src/main/resources/static/css/site.css`. W `:root` (`site.css:3-13`) jest jedenaście zmiennych: kolory `--paper`, `--ink`, `--muted`, `--line`, `--card`, `--header`, `--field`, `--green`, `--green-press` oraz fonty `--serif` i `--sans`. Poza tym zbiorem zostały hexy: błąd `#b00020` (`site.css:127`), biel na przycisku, polu, selekcie i chipie oraz hover drugiego przycisku `#f3eee6` (`site.css:206`).

`chrome.html` daje head, header i footer. Logowanie jest w pasku. `home.html` w liniach 7–84 nie ma linku do `/register`. Jedyny „Zarejestruj się” to drugorzędny przycisk w pasku (`chrome.html:43`). Zdania na starcie, w rejestracji i w stopce (`chrome.html:49`) mówią o planie na kolejny dzień albo o jednej osobie. `prd.md:20` i `prd.md:24` mówią o siedmiu dniach od jutra oraz o właścicielu i najwyżej trzech dodanych osobach. Profil i plan już to opisują (`plan.html:17`, `profile.html:139`).

`GET /` zawsze zwraca widok `home` (`HomeController.java:13-15`). Zalogowany widzi tę samą stronę. `HomeControllerTest` wymaga, żeby formularz logowania stał w HTML przed `#tresc` (`HomeControllerTest.java:44`) i żeby anonimowa strona zawierała nagłówek „Plan na kolejny dzień, bez gotowej diety.” oraz `href="/register"` (`HomeControllerTest.java:37-43`).

`PersonBoundsValidator.acceptsBody` sprawdza wiek 2–110 i niepusty cel. Nie porównuje celu z wiekiem (`PersonBoundsValidator.java:35-48`). Odmowa `BODY` u właściciela trafia na atrybut nieudanego zapisu, a ekran pokazuje wtedy „Nie udało się zapisać profilu. Spróbuj ponownie za chwilę.” (`ProfileController.java:264`, `profile.html:16`). U dodanej osoby `BODY` pokazuje „Sprawdź wiek, wzrost, wagę, płeć, cel i aktywność.” (`messages.properties:17`).

## Desired End State

Niezalogowany start czyta się jak reklama: nadtytuł „Dla Ciebie i trzech bliskich”, nagłówek „Cały tydzień posiłków. Zero zgadywania.”, lead o siedmiu dniach od jutra, aktywności i dwóch plikach, przycisk „Załóż konto”. Logowanie zostaje w pasku. Zalogowany `GET /` odpowiada przekierowaniem na `/plan`. Stopka na każdym ekranie mówi o siedmiu dniach dań dla domu i liście zakupów do pobrania.

Profil i plan zostają przy tych samych polach, akcjach i komunikatach, w układzie z jednym głównym przyciskiem na zadanie i tym samym kolorem błędu. Wybór „Schudnąć” przy wieku mniejszym niż 18 nie zapisuje ciała. Przy polu celu widać „Dla osoby poniżej 18 lat wybierz utrzymanie wagi albo przytycie.” Wiek 18 może ten cel zapisać.

### Key Discoveries:

- Stos widoków to Thymeleaf. `tech-stack.md:24` nie nazywa frameworka CSS. `AGENTS.md:11` każe dodawać bibliotekę wtedy, gdy ten plik ją wymienia.
- Test strony głównej trzyma kolejność: formularz logowania przed `#tresc` (`HomeControllerTest.java:44`).
- Logowanie nie ma własnego adresu sukcesu (`SecurityConfiguration.java:49-54`). Po udanym `POST /login` przeglądarka wraca na `/`.
- Skrypt list produktów szuka `[data-product-list]` (`profile-products.js` jest już podpięty pod więcej niż jedną parę list).
- `PlanDownloadE2eTest` po zalogowaniu czeka na `.signed-in-name`, a potem otwiera `/plan`.

## What We're NOT Doing

- Nowa biblioteka komponentów albo drugi arkusz stylów.
- Zmiana nazw istniejących zmiennych `--paper` i `--green`.
- Scalenie formularza właściciela z `profile-person.html`. Właściciel ma e-mail i pustą nazwę „Ja”.
- Ukrycie opcji „Schudnąć” na liście celów.
- Migracja wierszy, które już mają wiek poniżej 18 i cel `LOSE_WEIGHT`. Zostają do następnego zapisu ciała.
- Osobna matematyka kalorii dla dziecka. Wzór i ±250 zostają.
- Kolory w `PlanPdfWriter`, strona `/error`, nazwy plików PDF.
- Zdjęcie formularza logowania z paska.

## Implementation Approach

Najpierw brakujące kolory wchodzą do `:root`, żeby kolejne fazy nie dokładały hexów. Potem tekst publiczny i przekierowanie zalogowanego startu. Potem odmowa celu, osobnym wynikiem, nie przez `BODY`, bo `BODY` u właściciela pokazuje komunikat awarii zapisu. Na końcu układ profilu i planu, bez ruszania identyfikatorów pól, akcji formularzy i zdań, które trzymają testy.

## Critical Implementation Details

- Odmowa celu dziecka nie może użyć `ProfileRefusal.BODY` ani `HouseholdMemberError.BODY`. U właściciela `BODY` włącza ten sam komunikat co niedostępna baza (`profile.html:16`).
- Anonimowy HTML startu musi nadal mieć formularz logowania przed `#tresc`. Przekierowanie dotyczy tylko żądania z principalem.
- Wiek 18 z `LOSE_WEIGHT` przechodzi. Odmowa jest przy wieku mniejszym niż 18. Pusty cel zostaje przy dotychczasowym „Uzupełnij to pole.”, nie przy nowym zdaniu.
- Układ profilu zostawia `data-product-list`, identyfikatory pól i teksty przycisków, których szukają `ProfileControllerTest` i `profile-products.js`.

## Phase 1: Tokeny kolorów

### Overview

Błąd, biel i hover drugiego przycisku biorą wartość z `:root`. Wygląd ekranów się nie zmienia.

### Changes Required:

#### 1. Zmienne w arkuszu

**File**: `src/main/resources/static/css/site.css`

**Intent**: Jedno miejsce na kolory, które dziś są wpisane w regułach, żeby późniejszy układ profilu i planu użył tego samego błędu.

**Contract**: Obok istniejącego bloku `:root` dochodzą `--danger: #b00020`, `--on-emphasis: #fff`, `--field-fill: #fff` i `--secondary-hover: #f3eee6`. `.form-error` używa `--danger`. Tekst `.button` i `.skip` używa `--on-emphasis`. Tło `input`, `select` i `.product-chip` używa `--field-fill`. Hover `.button-secondary` używa `--secondary-hover`. Dotychczasowe jedenaście zmiennych zostaje pod tymi samymi nazwami. Promień i odstępy zostają literalami.

### Success Criteria:

#### Automated Verification:

- `.\gradlew.bat test` kończy się sukcesem

#### Manual Verification:

- Na stronie z błędem formularza kolor błędu, tekst głównego przycisku i tło pola pochodzą ze zmiennych, nie z wpisanego hexu

**Implementation Note**: Po zielonych testach zatrzymaj się na ręczne potwierdzenie, zanim ruszy faza 2.

---

## Phase 2: Tekst publiczny i wejście zalogowanego

### Overview

Start, rejestracja i stopka mówią o siedmiu dniach i o domu. Niezalogowany dostaje „Załóż konto” w pierwszym ekranie. Zalogowany `GET /` idzie na `/plan`.

### Changes Required:

#### 1. Start i stopka

**File**: `src/main/resources/templates/home.html`, `src/main/resources/templates/fragments/chrome.html`

**Intent**: Reklama ma być konkretna i zgodna z produktem, a prośba o konto ma stać w pierwszym ekranie, nie tylko w pasku.

**Contract**: Nadtytuł: „Dla Ciebie i trzech bliskich”. Nagłówek: „Cały tydzień posiłków. Zero zgadywania.” Lead: „Od jutra, na jednym koncie. Wpisujesz wiek, wzrost, wagę, płeć, aktywność i cel każdej osoby, a PlatePlan liczy jej kalorie i układa wspólne dania na siedem dni: śniadanie, drugie śniadanie, obiad i kolację. Pobierasz dwa pliki — dietę i listę zakupów. Tego, czego nie jecie, nie ma w daniach ani na liście.” Przycisk „Załóż konto” prowadzi na `/register`. Pod nim: „Masz konto? Zaloguj się u góry.” Nagłówki „Co to jest PlatePlan”, „Jak powstaje plan” i „Kalorie i preferencje” zostają. Zdania pod nimi wymieniają siedem dni od jutra, do czterech osób i aktywność. Skrót obok bohatera: wejście to Ty i do trzech bliskich, wynik to dieta na siedem dni i lista zakupów. Stopka: „PlatePlan. Siedem dni dań dla domu i lista zakupów do pobrania.” Opis w `<head>` startu nie mówi o kolejnym dniu. Pasek logowania zostaje.

#### 2. Rejestracja

**File**: `src/main/resources/templates/register.html`

**Intent**: Formularz konta powtarza tę samą obietnicę co start.

**Contract**: Nagłówek: „Załóż konto i ułóż tydzień”. Lead: „Jedno konto dla Ciebie i do trzech bliskich. Po rejestracji wchodzisz od razu.” Punkty pod spodem wymieniają aktywność i do trzech bliskich oraz to, że dieta na siedem dni i lista zakupów są do pobrania. Przycisk zostaje „Załóż konto”. Komunikaty błędów loginu i hasła zostają.

#### 3. Przekierowanie zalogowanego

**File**: `src/main/java/com/kenez92/plateplan/home/controller/HomeController.java`, `src/main/java/com/kenez92/plateplan/account/controller/RegisterController.java`

**Intent**: Osoba z kontem nie wraca do reklamy. Ląduje na planie.

**Contract**: `GET /` z principalem odpowiada przekierowaniem na `/plan`. Bez principala zostaje widok `home`. Zalogowany `GET` i `POST /register` przekierowują na `/plan`. Adres sukcesu logowania w `SecurityConfiguration` się nie zmienia: po `POST /login` przeglądarka wchodzi na `/`, a stamtąd na `/plan`.

#### 4. Testy tych ekranów

**File**: `src/test/java/com/kenez92/plateplan/home/controller/HomeControllerTest.java`, `src/test/java/com/kenez92/plateplan/account/controller/RegisterControllerTest.java`

**Intent**: Testy trzymają nowy nagłówek i przekierowanie, a nie stary tekst o kolejnym dniu.

**Contract**: Anonimowy `GET /` zawiera nowy nagłówek, trzy dotychczasowe nagłówki sekcji, „listę zakupów”, `href="/register"` i formularz logowania przed `#tresc`. Zalogowany `GET /` to przekierowanie na `/plan`. Test rejestracji zalogowanego oczekuje `/plan`.

### Success Criteria:

#### Automated Verification:

- `.\gradlew.bat test --tests com.kenez92.plateplan.home.controller.HomeControllerTest --tests com.kenez92.plateplan.account.controller.RegisterControllerTest` kończy się sukcesem
- Anonimowy HTML startu zawiera „Cały tydzień posiłków. Zero zgadywania.”, `href="/register"` i formularz logowania przed `#tresc`
- Zalogowany `GET /` przekierowuje na `/plan`

#### Manual Verification:

- Niezalogowany start pokazuje reklamę i „Załóż konto”; po zalogowaniu przeglądarka jest na `/plan` i widać pasek zalogowanego

**Implementation Note**: Po zielonych testach zatrzymaj się na ręczne potwierdzenie, zanim ruszy faza 3.

---

## Phase 3: Cel poniżej 18 lat

### Overview

Zapis ciała z celem „Schudnąć” przy wieku mniejszym niż 18 nie zapisuje nic. Komunikat stoi przy polu celu, u właściciela i u dodanej osoby.

### Changes Required:

#### 1. Reguła

**File**: `src/main/java/com/kenez92/plateplan/profile/validator/PersonBoundsValidator.java`

**Intent**: Jedna reguła dla obu osób, obok dotychczasowych granic ciała.

**Contract**: Nowa metoda przyjmuje wiek i cel. Odmowa, gdy wiek jest mniejszy niż 18 i cel to `LOSE_WEIGHT`. Wiek 18 oraz cele `MAINTAIN` i `GAIN` przy wieku od 2 lat tę metodę przechodzą. `acceptsBody` zostaje przy dotychczasowych granicach.

#### 2. Wynik zapisu

**File**: `src/main/java/com/kenez92/plateplan/profile/model/ProfileRefusal.java`, `src/main/java/com/kenez92/plateplan/profile/model/HouseholdMemberError.java`, `src/main/java/com/kenez92/plateplan/profile/service/ProfileService.java`, `src/main/java/com/kenez92/plateplan/profile/service/HouseholdMemberService.java`, `src/main/java/com/kenez92/plateplan/profile/controller/ProfileController.java`, `src/main/resources/messages.properties`

**Intent**: Odmowa ma własne zdanie przy celu, a nie komunikat awarii bazy ani ogólne „sprawdź ciało”.

**Contract**: Nowa wartość w obu wyliczeniach. `ProfileService.save` i sprawdzenie dodanej osoby wołają nową metodę po udanym `acceptsBody`. Odmowa nie woła zapisu. Każdy `switch` po tych wyliczeniach przyjmuje nową wartość. Klucz komunikatu: „Dla osoby poniżej 18 lat wybierz utrzymanie wagi albo przytycie.” Przeliczenie kalorii i zapis samej liczby kalorii tej reguły nie dotykają.

#### 3. Miejsce komunikatu

**File**: `src/main/resources/templates/profile.html`, `src/main/resources/templates/fragments/profile-person.html`

**Intent**: Osoba widzi zdanie przy celu, który właśnie wybrała.

**Contract**: Komunikat jest w `p.form-error` z `role="alert"` pod selectem celu właściciela i pod selectem celu edytowanej osoby. Lista nadal ma „Schudnąć”, „Utrzymać wagę” i „Przytyć”.

#### 4. Testy reguły

**File**: `src/test/java/com/kenez92/plateplan/profile/validator/PersonBoundsValidatorTest.java`, `src/test/java/com/kenez92/plateplan/profile/controller/ProfileControllerTest.java`

**Intent**: Granica 18 lat i brak zapisu są sprawdzone, zanim układ ekranu się zmieni.

**Contract**: Test metody: wiek 17 i `LOSE_WEIGHT` odmawia, wiek 18 i `LOSE_WEIGHT` przechodzi, wiek 17 i `MAINTAIN` przechodzi. Test kontrolera: taki zapis właściciela i dodanej osoby zwraca ekran z tym zdaniem i nie wywołuje zapisu. Nazwy metod zaczynają się od `should`.

### Success Criteria:

#### Automated Verification:

- `.\gradlew.bat test --tests com.kenez92.plateplan.profile.validator.PersonBoundsValidatorTest --tests com.kenez92.plateplan.profile.controller.ProfileControllerTest` kończy się sukcesem
- Wiek 17 z `LOSE_WEIGHT` jest odmową, wiek 18 z `LOSE_WEIGHT` przechodzi
- Zapis właściciela i dodanej osoby przy tym zestawie pokazuje „Dla osoby poniżej 18 lat wybierz utrzymanie wagi albo przytycie.” i nic nie zapisuje

#### Manual Verification:

- Na profilu wybór „Schudnąć” dla osoby poniżej 18 lat pokazuje to zdanie przy celu, a zapisany wcześniej wiersz zostaje

**Implementation Note**: Po zielonych testach zatrzymaj się na ręczne potwierdzenie, zanim ruszy faza 4.

---

## Phase 4: Układ profilu i planu

### Overview

Profil i plan dostają czytelniejszy układ na tokenach z fazy 1. Pola, akcje i zablokowane zdania zostają.

### Changes Required:

#### 1. Profil

**File**: `src/main/resources/templates/profile.html`, `src/main/resources/templates/fragments/profile-person.html`, `src/main/resources/static/css/site.css`

**Intent**: Jeden ekran ma jedno zadanie na raz: ciało, potem kalorie, potem dodane osoby. Ma wyglądać jak start, nie jak drugi system.

**Contract**: Zostają identyfikatory pól, `data-product-list`, akcje `POST /profile`, `POST /profile/calories`, `POST /profile/recalculate` i przyciski „Zapisz profil”, „Zapisz cel”, „Wylicz ponownie”, „Dodaj”, „Usuń osobę”. Sekcja dodanych osób zostaje pod właścicielem. Poniżej 800 px kolumny schodzą do jednej, tak jak reszta strony przy tym progu (`site.css:650`).

#### 2. Plan

**File**: `src/main/resources/templates/plan.html`, `src/main/resources/static/css/site.css`

**Intent**: Generowanie, osoby i pobranie są w jednej kolejności i z tym samym przyciskiem co reszta aplikacji.

**Contract**: Zostają „Na 7 dni”, „siedem dni”, „cztery posiłki”, „Generuj plan”, „Pobierz plan”, „Pobierz listę zakupów”, „Dla kogo” oraz `data-msg-*`. Główny przycisk ekranu to „Generuj plan”. Pobrania zostają drugorzędne. Checkbox wysyłki pojawia się na tych samych warunkach co dziś.

### Success Criteria:

#### Automated Verification:

- `.\gradlew.bat test --tests com.kenez92.plateplan.profile.controller.ProfileControllerTest --tests com.kenez92.plateplan.plan.controller.PlanControllerTest` kończy się sukcesem

#### Manual Verification:

- Profil i plan mają ten sam przycisk i ten sam kolor błędu co start, a dodanie produktu oraz generowanie nadal działają
- Poniżej szerokości 800 px profil i plan są jedną kolumną

**Implementation Note**: Po zielonych testach zatrzymaj się na ręczne potwierdzenie układu.

---

## Testing Strategy

### Unit Tests:

- Nowa metoda walidatora: wiek 17 odmawia `LOSE_WEIGHT`, wiek 18 je przyjmuje, wiek 17 przyjmuje `MAINTAIN`.
- Nazwy `should…`, pola i lokale `final`, bez `private static` w teście.

### Integration Tests:

- `HomeControllerTest` i `RegisterControllerTest` na nowy tekst i przekierowanie.
- `ProfileControllerTest` na odmowę celu przy właścicielu i dodanej osobie oraz na to, że etykiety profilu przetrwały układ.
- `PlanControllerTest` na to, że etykiety planu przetrwały układ.
- `.\gradlew.bat test` na końcu fazy 1, bo tokeny nie mają własnego testu Javy.

### Manual Testing Steps:

1. Wejść na `/` bez sesji i przeczytać nagłówek, lead i „Załóż konto”.
2. Założyć konto albo zalogować się i sprawdzić, że lądowanie jest na `/plan`.
3. Zapisać osobę w wieku 17 lat z celem „Schudnąć” i zobaczyć zdanie przy celu.
4. Zapisać tę samą osobę z „Utrzymać wagę”.
5. Dodać produkt na profilu i wygenerować plan.

## Performance Considerations

Przekierowanie zalogowanego `GET /` to jedna odpowiedź 302, bez dodatkowego zapytania poza tym, co `/plan` i tak wykonuje.

## Migration Notes

Brak changeset Liquibase. Wiersz z wiekiem poniżej 18 lat i celem `LOSE_WEIGHT` zostaje do następnego zapisu ciała. Ten zapis wtedy odmawia i nie nadpisuje kalorii.

## References

- Related research: `context/changes/front-views/research.md`
- Similar implementation: `src/main/java/com/kenez92/plateplan/profile/validator/PersonBoundsValidator.java:29-48`
- Locked home order: `src/test/java/com/kenez92/plateplan/home/controller/HomeControllerTest.java:37-44`
- Body refusal today: `src/main/java/com/kenez92/plateplan/profile/controller/ProfileController.java:259-265`

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands. Do not rename step titles. See `references/progress-format.md`.

### Phase 1: Tokeny kolorów

#### Automated

- [x] 1.1 `.\gradlew.bat test` kończy się sukcesem — 81eab5d

#### Manual

- [ ] 1.2 Na stronie z błędem formularza kolor błędu, tekst głównego przycisku i tło pola pochodzą ze zmiennych, nie z wpisanego hexu

### Phase 2: Tekst publiczny i wejście zalogowanego

#### Automated

- [x] 2.1 `.\gradlew.bat test --tests com.kenez92.plateplan.home.controller.HomeControllerTest --tests com.kenez92.plateplan.account.controller.RegisterControllerTest` kończy się sukcesem — e5dcd86
- [x] 2.2 Anonimowy HTML startu zawiera „Cały tydzień posiłków. Zero zgadywania.”, `href="/register"` i formularz logowania przed `#tresc` — e5dcd86
- [x] 2.3 Zalogowany `GET /` przekierowuje na `/plan` — e5dcd86

#### Manual

- [ ] 2.4 Niezalogowany start pokazuje reklamę i „Załóż konto”; po zalogowaniu przeglądarka jest na `/plan` i widać pasek zalogowanego

### Phase 3: Cel poniżej 18 lat

#### Automated

- [x] 3.1 `.\gradlew.bat test --tests com.kenez92.plateplan.profile.validator.PersonBoundsValidatorTest --tests com.kenez92.plateplan.profile.controller.ProfileControllerTest` kończy się sukcesem — 543a8fb
- [x] 3.2 Wiek 17 z `LOSE_WEIGHT` jest odmową, wiek 18 z `LOSE_WEIGHT` przechodzi — 543a8fb
- [x] 3.3 Zapis właściciela i dodanej osoby przy tym zestawie pokazuje „Dla osoby poniżej 18 lat wybierz utrzymanie wagi albo przytycie.” i nic nie zapisuje — 543a8fb

#### Manual

- [ ] 3.4 Na profilu wybór „Schudnąć” dla osoby poniżej 18 lat pokazuje to zdanie przy celu, a zapisany wcześniej wiersz zostaje

### Phase 4: Układ profilu i planu

#### Automated

- [x] 4.1 `.\gradlew.bat test --tests com.kenez92.plateplan.profile.controller.ProfileControllerTest --tests com.kenez92.plateplan.plan.controller.PlanControllerTest` kończy się sukcesem

#### Manual

- [ ] 4.2 Profil i plan mają ten sam przycisk i ten sam kolor błędu co start, a dodanie produktu oraz generowanie nadal działają
- [ ] 4.3 Poniżej szerokości 800 px profil i plan są jedną kolumną
