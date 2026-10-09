---
runtime: lazy
source: account-adapter/src/main/resources/db/migration/ddl/V1.098__add_household_delegation_to_topics.sql
serves: [flyway-migrations]
kind: worked-example
---
> **Worked example, not project facts.** The structure is the pattern to follow: the layers, the
> classes and their roles, the call order, the tests. Every name is a pseudonymized placeholder: the project, the
> packages, the classes, the methods, the parameters and the fields. Map each one to this repo's own name, and never
> copy a placeholder into code.

<!-- AI_DISCLAIMER v1.0 -->
# Exemplar — `account-adapter/.../db/migration/ddl/V1.098__...sql` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## What this artifact is
One of the versioned migrations under `account-adapter`'s Flyway location, a data-backfill migration
(not a schema change) for a `delegation_types` column.

## Versioned, forward-only backfill migration {#flyway-migrations}
**Serves:** [`flyway-migrations`](../flyway-migrations.md)

Filename shape: `V<major>.<minor>__<snake_case_description>.sql` under
`src/main/resources/db/migration/ddl/` (per module, not a single repo-wide migration tree — each of
the modules with a schema owns its own `ddl/` folder and version sequence). Idempotent by
construction: each `UPDATE` only touches rows that do not already have the value
(`NOT LIKE '%HOUSEHOLD%'`), safe to run against data seeded by an earlier version of the same
migration set. A leading SQL comment explains *why*, not just *what*, for a data migration whose
intent is not obvious from the statements alone.

### Source (pseudonymized)
```sql
-- Add HOUSEHOLD delegation type to all topic_delegation rows that do not already have it
-- This ensures household delegates can access all topics (backward compatible)

UPDATE topic_delegation
SET delegation_types = delegation_types || ',HOUSEHOLD'
WHERE delegation_types IS NOT NULL
  AND delegation_types <> ''
  AND delegation_types NOT LIKE '%HOUSEHOLD%';

UPDATE topic_delegation
SET delegation_types = 'HOUSEHOLD'
WHERE delegation_types IS NULL;

UPDATE topic_delegation
SET delegation_types = 'HOUSEHOLD'
WHERE delegation_types = '';
```

### Edge cases
Row already contains the value:
```sql
delegation_types = 'SELF,HOUSEHOLD'
```
Expected: the first `UPDATE` skips the row, so the migration does not duplicate the token.

Row has multiple unrelated delegation types:
```sql
delegation_types = 'SELF,TEAM'
```
Expected: the migration appends `,HOUSEHOLD` and preserves the existing ordering of earlier values.

Row is blank or null:
```sql
delegation_types = NULL
delegation_types = ''
```
Expected: the later `UPDATE`s normalize both cases to the single value `HOUSEHOLD`.

## Provenance
- Scanned at: `abc1234` · tool/query: `Get-ChildItem -Recurse -Filter "V*__*.sql"` excluding `build/target/bin`
