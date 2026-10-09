# Wysyłka PDF planu mailem przy generate — Plan Brief

> Full plan: `context/changes/email-plan-on-generate/plan.md`

## What & Why

Po M-1 użytkownik pobiera dwa PDF, które znikają po odświeżeniu. Ta zmiana — **pierwsze** z trzech zaplanowanych rozszerzeń — dodaje opcjonalną wysyłkę **tych samych** plików na adres z profilu, gdy na `/plan` zaznaczy checkbox przy generate. Przyciski pobierania zostają. Rodzina (kilka osób, rozdzielone porcje) i tydzień zamiast jednego dnia to kolejne zmiany, nie ten plan.

## Starting Point

`POST /plan/generate` zwraca `dietPdf` i `shoppingListPdf` albo samo `error`. Nie ma body, maila ani kolumny e-mail. `GET /plan` nie czyta bazy. Fly Machine ma auto-stop; joby w tle są poza MVP.

## Desired End State

Na profilu można zapisać adres. Checkbox na `/plan` widać tylko wtedy; jest odznaczony. Zaznaczenie wysyła oba PDF w tym samym requeście i pokazuje komunikat. Pad SMTP zostawia przyciski i osobny błąd maila.

## Key Decisions Made

| Decision | Choice | Why |
| -------- | ------ | --- |
| Kanał | E-mail obok PDF-ów, nie zamiast | Jedyne otwarte pytanie PRD; Non-Goals zostają |
| Adres | Opcjonalne pole na profilu | Login nie jest e-mailem; `/profile` już trzyma dane między wizytami |
| Treść | Oba PDF jako załączniki | Jeden `PlanPdfWriter`, te same bajty co przyciski |
| Trigger | Checkbox przy generate, ten sam request | Świadomy opt-in; plików nie wolno trzymać na serwerze |
| Scheduler | Odrzucony (była korekta) | Joby w tle i auto-stop Fly; użytkownik wrócił do checkboxa |
| Brak adresu | Ukryj checkbox | Czysty `/plan`; generate nadal działa |
| Pad SMTP | 200 + PDF + `emailError` | Pole `error` chowa przyciski w JS |
| Checkbox | Zawsze odznaczony | Nic nie wychodzi samo |
| Sukces | Komunikat na `/plan` | Nie trzeba otwierać skrzynki, żeby wiedzieć że akcja przeszła |
| Testy | Unit + `@WebMvcTest` | JSON na WebMvc, nie Playwright i nie żywy SMTP |
| Kolejność rozszerzeń | 1. e-mail (ta zmiana) → 2. rodzina → 3. tydzień | Rodzina i tydzień wymagają PRD/modelu; nie mieszają się z SMTP |

## Scope

**In scope:** nullable `email` na `user_profile`; walidacja i zapis na `/profile`; `spring-boot-starter-mail`; `sendEmail` na generate; `emailError`; checkbox i komunikaty; sekrety `MAIL_*`; aktualizacja AGENTS/PRD/tech-stack.

**Out of scope:** scheduler/subskrypcja, HTML w treści maila, weryfikacja adresu, login-jako-e-mail, persistencja PDF, zmiana auto-stop, Playwright skrzynki. **Następne zmiany (nie ten plan):** dodatkowe osoby z własnymi celami i rozdzielonymi porcjami; plan na cały tydzień.

## Architecture / Approach

Profil zapisuje adres. GET `/plan` dostaje tylko boolean. POST generate jak dziś buduje PDF; gdy `sendEmail=true`, `PlanMailSender` wysyła załączniki i ewentualnie ustawia `emailError` bez ruszania `error`. Brak SMTP nie zatrzymuje startu.

## Phases at a Glance

| Phase | What it delivers | Key risk |
| ----- | ---------------- | -------- |
| 1. E-mail na profilu | Kolumna, formularz, walidacja | `replaceBody*` musi pisać e-mail, kcal-only nie |
| 2. Checkbox i wysyłka | Mailer, JSON, UI, testy planu | `emailError` vs `error`; GET `/plan` bez 500 na martwej bazie |
| 3. Dokumenty i sekrety | AGENTS, PRD, tech-stack, Fly secrets | Puste `MAIL_*` muszą startować aplikację |

**Prerequisites:** działający generate na `/plan`; Fly secrets jak przy Ollamie (ludzki SMTP).
**Estimated effort:** ~2–3 sesje w trzech fazach.

## Open Risks & Assumptions

- Fly może uciąć długi generate zanim SMTP skończy — krótki timeout poczty.
- „Wysłano” znaczy przyjęcie przez SMTP, nie doręczenie do skrzynki.
- Operator musi sam ustawić `MAIL_*`; bez nich checkbox działa, wysyłka kończy się `emailError`.

## Success Criteria (Summary)

- Z adresem: odznaczony checkbox, zaznaczenie wysyła dwa PDF i pokazuje komunikat.
- Bez adresu albo z odznaczonym checkboxem: zachowanie M-1 (dwa pobrania, bez maila).
- Pad poczty nie zabiera PDF-ów.
