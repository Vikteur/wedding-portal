---
runtime: lazy
kind: worked-example
---
> **Worked example, not project facts.** The structure is the pattern to follow: the layers, the
> classes and their roles, the call order, the tests. Every name is a pseudonymized placeholder: the project, the
> packages, the classes, the methods, the parameters and the fields. Map each one to this repo's own name, and never
> copy a placeholder into code.

<!-- AI_DISCLAIMER v1.0 -->
# Code map — `flyway-migrations` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## Where this pattern lives (exemplars)
| Exemplar | What it shows | Layer / module |
|----------|---------------|----------------|
| [`V1.098__add_household_delegation_to_topics.sql`](exemplars/topics-delegation-migration.md#flyway-migrations) | Idempotent data-backfill migration with an explanatory leading comment | adapter (`account-adapter`) |

### Excerpts
Idempotent append for existing rows (full source in the [exemplar leaf](exemplars/topics-delegation-migration.md#flyway-migrations)):
```sql
UPDATE topic_delegation
SET delegation_types = delegation_types || ',HOUSEHOLD'
WHERE delegation_types IS NOT NULL
  AND delegation_types <> ''
  AND delegation_types NOT LIKE '%HOUSEHOLD%';
```

Separate handling for null and empty-string rows:
```sql
UPDATE topic_delegation
SET delegation_types = 'HOUSEHOLD'
WHERE delegation_types IS NULL;

UPDATE topic_delegation
SET delegation_types = 'HOUSEHOLD'
WHERE delegation_types = '';
```

### Edge cases
Existing multi-value row:
```sql
delegation_types = 'SELF,TEAM'
```
Expected: the migration appends `,HOUSEHOLD` exactly once.

Null column value:
```sql
delegation_types = NULL
```
Expected: the migration sets the column to `HOUSEHOLD` instead of leaving it blank.

Already migrated row:
```sql
delegation_types = 'SELF,HOUSEHOLD'
```
Expected: the first update skips the row because the value already contains `HOUSEHOLD`.

## Local conventions (the project facts the skill omits)
- Package root: `<module>/src/main/resources/db/migration/ddl/` — one `ddl/` tree **per module**
  that owns a schema, each with its own version sequence, not one repo-wide migration tree.
- Naming shape: `V<major>.<minor>__<snake_case_description>.sql` (two-part version, e.g. `V1.098`,
  not `V1__` / timestamp-based).
- Required collaborators / base types: none — plain SQL, no Java migration classes observed.
- Config / wiring: applied automatically on startup (Spring Boot Flyway auto-config).

## Frequency & coverage (why this earned a skill)
- Occurrences: 99 migration files across 5 modules — `application`, `authentication-adapter`,
  `storefront-adapter`, `account-adapter`, `attribute-mapping-adapter` (as of `abc1234`).

## Drift / exceptions
- None observed — version numbering and idempotent-update style are consistent across the sampled
  migrations.

## Provenance
- Scanned at: `abc1234` · tool/query: `Get-ChildItem -Recurse -Filter "V*__*.sql"` excluding `build/target/bin`
