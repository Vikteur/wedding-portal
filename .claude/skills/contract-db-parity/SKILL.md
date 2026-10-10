---
name: contract-db-parity
description: Extract an API contract's CRUD surface and a database schema as JSON and report where they disagree. Use before a migration or a contract change, or to check a designed schema against the contract.
---
# Contract–database parity

## When to use
A contract says what clients may read, create, update and delete; a schema says what can be stored. Use this when
either side changes, or before one is cut from the other (a designed schema turning into migrations, a contract
growing new fields), to find what one side promises and the other cannot hold. Use it before writing a migration or
a contract change, not after.

## How
- **Run the script, never compare by reading** — `scripts/contract-db-parity.sh` does all three steps deterministically
  and writes three JSON files: the contract surface, the schema, and the parity report. A model reads the report's
  summary and findings, not the spec or the DDL.
- **Pick the schema source** — `--schema-sql <file>` for a designed schema that no database runs yet, `--migrations
  <dir>` for versioned migrations, `--db-schema <json>` for one extracted earlier. The first two apply the SQL to a
  throwaway Postgres container (docker), which is removed on exit; `--image` must be new enough for the DDL.
- **Read the contract surface** — every operation gets a CRUD class (GET read, POST create when it answers 201 or a
  `/{id}` sibling exists, otherwise action, PUT/PATCH update, DELETE delete) and an object; every object gets its
  fields with `read`/`create`/`update` flags, required-on-create/update, nullability, enum, format and maxLength.
- **Settle names in the map file, not in code** — objects, fields and tables that naming conventions cannot pair
  (an object named after a role, a table holding several document kinds) go in the project's map file: `objects`,
  `fields`, `accept`, `ignoreTables`, `ignoreColumns`. An object or field mapped to `null` is not stored. A field may
  name another table (`<table>.<column>`: an array kept one row per item compares its items), and `<prefix>.*` maps a
  nested object onto a table by name.
- **Accept a finding only with its reason** — `accept` drops one finding to info and carries the reason into the
  report. When the reason is a CHECK constraint, name it: the script fails the acceptance if the constraint is gone.
- **Triage by severity** — `error` is a value one side accepts and the other cannot hold (length, nullability, enum,
  type width, a mapped name that is missing); `warning` is likely drift (a writable field with no column, a writable
  object with no table, a promised value the column lets be NULL); `info` is the inventory (derived fields, nested
  collections, columns and tables the contract does not expose).
- **Fix the right side** — a finding names both ends; decide which side is wrong before touching either. The fix
  lands as a contract change or a migration through its own workflow.

## Pattern signals (discovery cues — how the scanner recognizes this in any codebase)
- An OpenAPI document next to SQL DDL or versioned migrations, with no check that the two agree.
- Enum value lists, string lengths or nullability repeated in the spec and in the schema.

## Project specifics → see docs
- Code map (spec location, schema sources, image, map file, how to run, known findings) →
  `docs/code-maps/contract-db-parity.md`

## Guardrails (what NOT to do)
- Don't apply the DDL or migrations to any database but the throwaway container the script starts.
- Don't edit the contract or write a migration because a finding says so: each is its own change, and both need a
  human decision first.
- Don't silence a finding with the map file to make the report clean; map only real name differences, and accept only
  what the schema's own rules justify, with the reason written down.
- Don't hand the full report to another agent; hand its summary and the findings that concern that agent.

## Definition of done
- [ ] The script ran against the current contract and the chosen schema source; the three JSON files exist.
- [ ] Every `error` and `warning` is fixed on the side that is wrong, or the map file shows a name difference or an
  acceptance with its reason.
- [ ] New name pairings live in the map file, not in a script.
