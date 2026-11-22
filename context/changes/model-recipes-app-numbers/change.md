---
change_id: model-recipes-app-numbers
title: Model pisze przepisy, aplikacja liczy gramy i kcal
status: preparing
created: 2026-11-16
updated: 2026-11-22
archived_at: null
---

## Notes

Obecnie wszystko dostajemy z LLM. Chcemy, aby AI wymyślał tylko przepisy. Kcal, gramy i resztę liczb liczy aplikacja. Tabela żywności jest w `src/main/resources/food/kcal-per-100g.csv`: 13209 nazw z kcal, białkiem, węglowodanami i tłuszczem na 100 g, zebranych 2026-11-22 z https://kalkulatorkalorii.net/tabela-kalorii. Powtórzona nazwa zostawia wcześniejszy wiersz. Bieżącej jakości generowania nie mierzono. Cel białka zostaje poza tą pracą: białko jest wynikiem liczenia, nie trzecim celem obok kcal i pułapu tłuszczu. Pomiar większego modelu lokalnego jest osobnym zadaniem.
