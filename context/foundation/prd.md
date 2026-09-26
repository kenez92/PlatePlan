---
project: PlatePlan
version: 1
status: draft
created: 2026-09-23
context_type: greenfield
product_type: web-app
target_scale:
  users: small
timeline_budget:
  mvp_weeks: "# TODO: mvp_weeks — see Open Questions"
  hard_deadline: null
  after_hours_only: true
---

## Vision & Problem Statement

Ty każdego dnia musisz zdecydować, co zjeść na śniadanie, obiad i kolację, gdy chcesz schudnąć, utrzymać wagę albo przytyć. Koszt to czas, energia i często złe wybory żywieniowe. Nie masz czasu na planowanie.

Diety z internetu narzucają konkretne dania. PlatePlan pozwala określić, co lubisz i czego nie lubisz, i generuje plan zgodny z tymi preferencjami, bez narzucania gotowej diety. Aplikacja wylicza dzienne zapotrzebowanie kaloryczne, generuje plan diety na kolejny dzień oraz listę zakupów w formie dwóch plików PDF. Reguła kalorii nie zmienia się przy stukrotnie większej liczbie osób. Zależy od utworzenia kont.

## User & Persona

Primary persona: Ty. Jedna osoba, jedno konto. Kontekst: chcesz schudnąć, utrzymać wagę albo przytyć i trzymać dietę. Nie masz czasu na wymyślanie posiłków. Używasz aplikacji rano lub wieczorem, aby wygenerować plan na kolejny dzień.

### Secondary persona

Po MVP: możliwość dodania więcej osób, na przykład rodziny. Ta wersja ich nie obejmuje.

## Success Criteria

### Primary

- Po zatwierdzeniu kalorii użytkownik pobiera dwa pliki PDF: plan diety na kolejny dzień i listę zakupów.
- Przy kolejnej wizycie nie wpisuje danych ponownie i generuje kolejny plan.

### Secondary

- Więcej osób jest poza tą wersją. Zostaje na później i samo nie wystarcza, żeby uznać produkt za działający.

### Guardrails

- Dane użytkownika nie mogą wyciec.
- Plan diety i lista zakupów nie są zapisywane. Są tylko generowane i pobierane.
- Wiek, wzrost, waga, płeć, cel i preferencje zostają przy koncie między wizytami.

## User Stories

### US-01: Generowanie planu diety i listy zakupów

- **Given** użytkownik otwiera aplikację, widzi okno logowania, wybiera „zarejestruj się”, konto powstaje, logowanie następuje automatycznie.
- **When** użytkownik wpisuje dane, klika „wylicz kalorie”, akceptuje lub edytuje wynik, a następnie wybiera „generuj plan”.
- **Then** aplikacja generuje plan diety na kolejny dzień oraz listę zakupów. Oba pliki są dostępne do pobrania jako PDF i nie są zapisywane.

### US-02: Kolejna wizyta bez ponownego wpisywania danych

- **Given** użytkownik ma konto, a wiek, wzrost, waga, płeć, cel i preferencje są już zapisane
- **When** wraca rano lub wieczorem i nie wpisuje tych danych ponownie
- **Then** generuje plan diety na kolejny dzień oraz listę zakupów. Oba pliki są dostępne do pobrania jako PDF i nie są zapisywane.

## Functional Requirements

- FR-001: Użytkownik can utworzyć konto. Priority: must-have
  > Socrates: Counter-argument considered: none against creating an account. Open point moved to FR-002: whether login is a separate step after registration.
  > Resolution: kept; it stands as written.
- FR-002: Użytkownik can się zalogować. Po rejestracji logowanie następuje automatycznie. Priority: must-have
  > Socrates: Counter-argument considered: "Po rejestracji osobne logowanie jest drugim progiem przed danymi."
  > Resolution: kept for a later visit. After registration, login is automatic. There is a login window with a "zarejestruj się" button at the bottom.
- FR-003: Użytkownik can wpisać wiek, wzrost, wagę, płeć, cel, preferowane produkty i produkty wykluczone. Priority: must-have
  > Socrates: Counter-argument considered: none.
  > Resolution: No counter-argument; it stands as written.
- FR-004: Aplikacja can wyliczyć kalorie na podstawie wieku, wzrostu, wagi, płci i celu. Preferowane i wykluczone produkty nie zmieniają tej liczby. Priority: must-have
  > Socrates: Counter-argument considered: none.
  > Resolution: No counter-argument; it stands as written.
- FR-005: Użytkownik can zaakceptować lub zmienić liczbę kalorii. Priority: must-have
  > Socrates: Counter-argument considered: none.
  > Resolution: No counter-argument; it stands as written.
- FR-006: Użytkownik can pobrać plan diety na kolejny dzień i listę zakupów. Plan i lista zakupów nie są zapisywane. Priority: must-have
  > Socrates: Counter-argument considered: none.
  > Resolution: No counter-argument; it stands as written.

## Non-Functional Requirements

- Plan diety i lista zakupów nie są przechowywane. Są tylko generowane i pobierane.
- Dane użytkownika nie mogą wyciec.
- Wiek, wzrost, waga, płeć, cel i preferencje zostają przy koncie i nie trzeba ich wpisywać ponownie.

## Business Logic

Aplikacja za użytkownika wylicza, ile powinien przyjmować kalorii.

Schudnąć obniża wynik, utrzymać wagę go zostawia, przytyć go podnosi. Wyliczenie używa algorytmu BMR plus cel. Preferowane produkty i produkty wykluczone nie zmieniają tej liczby. W planie diety są uwzględniane albo wykluczane.

Wejście do wyliczenia: wiek, wzrost, waga, płeć, cel (utrzymać wagę, schudnąć, przytyć). Wejście do planu: te produkty oraz zaakceptowana albo zmieniona liczba kalorii. Użytkownik może liczbę edytować. Na wyjściu są dwa pliki PDF: plan diety na kolejny dzień i lista zakupów. Żaden z tych plików nie jest zapisywany. Dane konta zostają. Przy kolejnej wizycie użytkownik nie wpisuje ich ponownie i może od razu generować plan.

## Access Control

Jeden użytkownik to jedno konto. Płaski model dostępu, bez ról. Po wejściu na stronę jest okno logowania. Rejestracja kończy się automatycznym logowaniem. Użytkownik widzi tylko swoje dane. Wiek, wzrost, waga, płeć, cel i preferencje zostają przy koncie.

## Non-Goals

- Obsługa wielu osób. Dodamy ją później. Ta wersja jest dla jednego konta.
- Zapisywanie planów diety i list zakupów. Oba pliki są generowane i pobierane, nie przechowywane.
- Zaawansowane role użytkowników. Model dostępu zostaje płaski.

## Open Questions

1. **O ile schudnąć obniża wynik i o ile przytyć go podnosi?** — Owner: user. Kierunek jest ustalony. Wielkość zmiany nie.
2. **Czy aktywność wchodzi do wyliczenia kalorii?** — Owner: user. Nie została rozstrzygnięta.
3. **Czy wynik zostaje dwoma plikami PDF, czy mailem z pełną treścią na skrzynkę?** — Owner: user. Teraz zapisane są dwa PDF-y. Mail jest rozważany i nie zastępuje PDF, dopóki nie zostanie wybrany.
4. **Ile tygodni ma MVP?** — Owner: user. Użytkownik powiedział „Nie wiem.”
