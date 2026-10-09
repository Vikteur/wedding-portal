---
runtime: lazy
---

# Code map — `contract-db-parity` (project: `wedding-portal`)

## Inputs

| Input | Where | Note |
|---|---|---|
| Contract | `../rekord-contract/dist/openapi.yaml` (sibling checkout of the hub repo) | Bundled OpenAPI 3.0.3; `v4.0.0` when this leaf was written. Only the contract-agent edits it. |
| Designed schema | `../weddingapp/docs/rewrite/analysis/44-target-data-model/schema.sql` | Report 44's DDL, the source the UML diagrams (`*.png` next to it, mermaid in the report) are drawn from. No database runs it yet (UD-24). |
| Migrations | `application/src/main/resources/db/migration` | None on `main` yet: each data-model ticket cuts its `V<n>__*.sql` from report 44, and each is a STOP item. |
| Map file | [map] | Object→table pairs that names cannot settle, and the bookkeeping columns every table carries. |
| Image | `postgres:18` (the script's default) | Report 44 targets PostgreSQL 18; `uuidv7()` fails on 17. |

## Run it

```bash
# The contract against the designed schema (what to use until migrations exist):
scripts/contract-db-parity.sh --spec ../rekord-contract/dist/openapi.yaml \
  --schema-sql ../weddingapp/docs/rewrite/analysis/44-target-data-model/schema.sql \
  --map docs/code-maps/contract-db-parity.map.json

# Once migrations exist:
scripts/contract-db-parity.sh --spec ../rekord-contract/dist/openapi.yaml \
  --migrations application/src/main/resources/db/migration --map docs/code-maps/contract-db-parity.map.json
```

Output lands in `build/contract-db-parity/` (git-ignored). The tests are `scripts/test/contract-db-parity.test.sh`.

## Conventions the comparison relies on

- Enums are `text` plus a named `CHECK (x IN (...))`, never a PostgreSQL `ENUM` (report 44 §3); the script reads
  those values as the column's `allowedValues`.
- Every business table carries `org_id`, `created_at`, `updated_at` and some `deleted_at`; the map file ignores them,
  because the contract never exposes the tenant key or the soft-delete stamp.
- The three append-only logs use a `bigint` identity, every other id is a `uuid`.

## Map file decisions

| Object | Table | Why |
|---|---|---|
| `userAccount` | `users` | The contract names the account, the schema the person. |
| `invite`, `inviteCreated`, `invitePreview` | `invitations` | |
| `person` | `wedding_people` | |
| `teamSlot` | `wedding_team` | |
| `quote`, `invoice`, `creditNote` | `billing_documents` | One table for the three kinds (report 44 §5.5); their `lines[]` go to `billing_document_lines`. |
| `auditEvent` | `audit_log` | |
| `coupleChange` | `song_changes` | |

## First run (contract v4.0.0 against report 44, 2026-10-10)

2 errors, 65 warnings, 178 info; 16 of 35 tables matched. The errors:

- `auditEvent.action`: the contract's actions (`SIGN_IN`, `SIGN_OUT`, `MEMBER_VENDOR_LINKED`, …) and the schema's
  `ck_audit_log_action` (`SIGN_IN_SUCCEEDED`, `SIGNED_OUT`, `INVITE_ACCEPTED`, …) disagree.
- `auditEvent.id`: a `uuid` in the contract, a `bigint` identity in the schema.

The warnings worth a look: `organization.billing.*` has no column on `organizations` (the schema keeps it in
`billing_settings`), `atTime`/`ceremonyTime`-style fields are plain strings over `time` columns, and `wedding.note`,
`wedding.venueName`, `userAccount.roles`, `teamSlot.contactId` have no matching column.

[map]: contract-db-parity.map.json
