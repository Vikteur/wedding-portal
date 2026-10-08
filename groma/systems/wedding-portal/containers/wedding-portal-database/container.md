---
type: C4 Container
title: Wedding portal database
description: The backend's own PostgreSQL database, migrated by Flyway at start-up.
status: draft
groma:
  id: wedding-portal-database
  parent: wedding-portal
  technology: PostgreSQL 17
---

The backend's own PostgreSQL database, `wedding_portal`; it is not shared with `rekord-api`. The API reaches it through the Quarkus datasource: Flyway migrates it at start-up from `db/migration` and refuses a database that holds tables but no migration history, and Hibernate only validates the schema. V1 is the comment-only baseline; V2 holds the identity tables: organisations, users, memberships (roles ADMIN, PLANNER, DJ) and sessions, which store only the SHA-256 hash of their token.

Development and tests get `postgres:17-alpine` from Quarkus Dev Services; production reads `DB_URL`, `DB_USER` and `DB_PASSWORD` from the environment. The box is a draft because no configured scanner reads SQL or configuration files, so a scan cannot confirm it.
