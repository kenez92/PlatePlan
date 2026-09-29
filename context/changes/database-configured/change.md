---
change_id: database-configured
title: Configure the Supabase database
status: planned
created: 2026-09-29
updated: 2026-09-29
archived_at: null
---

## Notes

Roadmap item F-01 (`context/foundation/roadmap.md`). The application gets a configured Supabase (PostgreSQL) database, used through Hibernate, with every schema script installed by Liquibase (XML changelogs). No product tables are created here.

The connection settings come from Fly secrets, never from a plain string in a tracked file. The application must start when the database cannot be reached.
