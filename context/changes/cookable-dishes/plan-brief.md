# Odczyt, czy dania da się ugotować — Plan Brief

> Full plan: `context/changes/cookable-dishes/plan.md`
> Frame brief: `context/changes/cookable-dishes/frame.md`

## What & Why

Czytamy prawdziwy plan danie po daniu. Nikt nie odczytał wygenerowanego tygodnia pod tym, czy danie da się ugotować, a Java i tak odda dzień, którego składniki się nie trzymają, byle przeszły liczby i nazwy.

## Starting Point

Dzień przechodzi na nazwie, porcjach, paśmie kcal i pułapie tłuszczu. Przygotowanie i napisy ilości zostają z modelu, a kcal i makro Java dopisuje obok. Ostatni plan ma 3 dni i poszedł mailem. Tabeli na 100 g żadna klasa nie czyta. Wzorów przepisów w repozytorium nie ma.

## Desired End State

`verdict.md` ma jeden wiersz na danie: nazwa, czy da się ugotować, czy liczby się zgadzają, jedno zdanie. Danie jest dobre tylko gdy obie odpowiedzi brzmią „tak”. PDF-y leżą poza gitem. Generator jest ten sam co przed odczytem.

## Key Decisions Made

| Decision | Choice | Why (1 sentence) | Source |
| --- | --- | --- | --- |
| Co jest „do ugotowania” | Przepis i liczby naraz | Samo pasmo dnia nie mówi, czy składniki trzymają się kroków ani czy gramy dają wydrukowane makro | Plan |
| Skąd bierzemy plan | Mail, a gdy go nie ma — jedno generowanie | Plan na 3 dni już powstał, a plików aplikacja nie trzyma | Plan |
| Co robimy z werdyktem | Zapis w tej zmianie, generator bez zmian | Wzory i prompt wchodzą dopiero, gdy wiadomo, co pada | Frame |
| Kiedy liczby się zgadzają | Gramy razy CSV, ±10% wydruku | Pasmo celu dnia generator już wymusza, więc nie odróżnia złych gramów | Plan |
| Kto czyta | Daniel kładzie PDF-y, odczyt jest z pliku | Do skrzynki nie ma dostępu | Plan |
| Jak wygląda zapis | Wiersz na danie, bez pełnego przepisu | Widać, które danie padło, a jadłospis nie wraca w całości do repozytorium | Plan |

## Scope

**In scope:**

- Katalog `tmp/cookable-dishes/` ignorowany przez gita
- Jeden plan: mail albo jedyne zastępcze generowanie
- `verdict.md` z wierszem na danie

**Out of scope:**

- Wzory przepisów ze stron
- Zmiana promptu, modelu i generatora
- Liczenie gramów w aplikacji
- Commit PDF-ów i pełnych kroków

## Architecture / Approach

PDF zostaje lokalnie i poza gitem. Dla każdego dania ocena gotowania jest jedna, bo przepis jest wspólny. Ocena liczb jest na każdą porcję i danie przechodzi tylko wtedy, gdy przejdzie każda. Brak gramów albo nazwy spoza CSV oblewa porcję bez sumowania reszty. Wynik ląduje w folderze zmiany.

## Phases at a Glance

| Phase | What it delivers | Key risk |
| --- | --- | --- |
| 1. Pliki do odczytu | Dwa PDF-y w `tmp/cookable-dishes/` i wpis `tmp/` w `.gitignore` | Maila może nie być i wtedy dochodzi jedno generowanie |
| 2. Werdykt | `verdict.md` z wierszem na danie | Nazwa z modelu często nie pokryje się dosłownie z wierszem CSV i porcja obleje |

**Prerequisites:** mail z planem na 3 dni albo działająca lokalna Ollama, gdy maila nie ma. Tabela `src/main/resources/food/kcal-per-100g.csv` już jest.

**Estimated effort:** jedna sesja odczytu. Generowanie, jeśli potrzebne, dokłada czas jednego wywołania modelu.

## Open Risks & Assumptions

- Pewność ramy jest średnia: kształt „nie da się ugotować” nie ma cytatu z prawdziwej odpowiedzi, dopóki nie powstanie `verdict.md`.
- Dokładne porównanie nazw obleje składnik, który w CSV jest pod inną nazwą. To jest wynik reguły, nie powód do poprawiania tabeli w tej zmianie.
- Plan z maila ma 3 dni i wspólne dania dla osób na koncie. Wiersz jest na danie, nie na osobę.

## Success Criteria (Summary)

- Wiadomo, z którego planu jest odczyt, a PDF-y nie są w gicie.
- Każde danie ma werdykt gotowania i werdykt liczb oraz jedno zdanie przy porażce.
- Kod generatora jest nietknięty.
