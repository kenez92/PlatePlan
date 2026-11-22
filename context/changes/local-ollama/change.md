---
change_id: local-ollama
title: Lokalna Ollama zamiast chmury
status: implementing
created: 2026-11-14
updated: 2026-11-16
archived_at: null
---

## Notes

Ollama Cloud nie ma już darmowego planu. Aplikacja woła lokalną Ollamę na `http://127.0.0.1:11434`, model `qwen2.5:3b`, bez klucza. Maszyna Fly nadal nie trzyma wag. Generator pyta do pięciu razy i odsyła tyle dni, ile przeszło reguły (od jednego do siedmiu). Strona, pole `dayCount` i mail podają tę liczbę.
