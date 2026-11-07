<!-- IMPL-REVIEW-REPORT -->
# Implementation Review: Trzy ostatnie plany w prompcie

- **Plan**: context/changes/recent-plan-memory/plan.md
- **Scope**: Full plan
- **Reviewed phases**: 1, 2, 3
- **Date**: 2026-11-07
- **Verdict**: APPROVED
- **Findings**: 0 critical, 1 warning, 0 observations

## Verdicts

| Dimension | Verdict |
|-----------|---------|
| Plan Adherence | PASS |
| Scope Discipline | PASS |
| Safety & Quality | WARNING |
| Architecture | PASS |
| Pattern Consistency | PASS |
| Success Criteria | PASS |

## Findings

### F1 — Dwa równoległe zapisy mogą zostawić więcej niż trzy plany

- **Severity**: ⚠️ WARNING
- **Impact**: 🔎 MEDIUM — real tradeoff; pause to reason through it
- **Dimension**: Safety & Quality
- **Location**: src/main/java/com/kenez92/plateplan/plan/service/RecentPlanMemory.java:56
- **Detail**: `remember` wstawia wiersz, czyta plany tego loginu i kasuje nadmiar w jednej transakcji, bez blokady. Dwa nakładające się wywołania dla tego samego loginu, przy izolacji read committed, nie widzą wzajemnie niezacommitowanego insertu i mogą skasować te same najstarsze identyfikatory. Dwa takie zapisy przy już pełnych trzech planach zostawiają cztery wiersze. `avoidedNames` i tak oddaje tylko trzy najnowsze, a następny zapis wykonany sam przycina nadmiar. Okno jest realne, bo wołanie modelu trwa osobno i może dojść do dwóch równoległych `POST /plan/generate`.
- **Fix A ⭐ Recommended**: Na początku `remember` weź blokadę transakcyjną tego loginu i dopiero potem czytaj oraz kasuj.
  - Strength: Drugi zapis czeka na commit pierwszego, widzi nowy wiersz i zostawia trzy. Blokada obejmuje tylko krótki zapis, nie wołanie modelu.
  - Tradeoff: Nowy mechanizm blokady, którego w projekcie jeszcze nie ma. `SELECT … FOR UPDATE` po istniejących planach nie pokrywa pustej pamięci.
  - Confidence: MEDIUM — wyścig wynika wprost z kolejności insert, odczyt, kasowanie; blokady nie sprawdzono testem współbieżnym.
  - Blind spot: Nie uruchomiono dwóch nakładających się transakcji na PostgreSQL.
- **Fix B**: Zostaw tak. Prompt i tak dostaje trzy najnowsze nazwy, a kolejny samotny zapis przycina nadmiar.
  - Strength: Zero nowego SQL i zero sprzężenia z `user_profile`.
  - Tradeoff: Niezmiennik „najwyżej trzy wiersze” nie jest ścisły, dopóki nie przyjdzie następny udany zapis.
  - Confidence: HIGH — `avoidedNames` już obcina listę do trzech najnowszych, a `trim` kasuje najmniejsze `plan_id` ponad trzy.
  - Blind spot: None significant.
- **Decision**: SKIPPED
