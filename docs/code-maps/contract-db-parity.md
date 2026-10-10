---
runtime: lazy
---

# Code map — `contract-db-parity` (project: `wedding-portal`)

## Inputs

| Input | Where | Note |
|---|---|---|
| Contract | `../rekord-contract/dist/openapi.yaml` (sibling checkout of the hub repo) | Bundled OpenAPI 3.0.3; `v5.0.0` at the last run (run against the committed `dist/`, not a work-in-progress checkout). Only the contract-agent edits it. |
| Designed schema | `../weddingapp/docs/rewrite/analysis/44-target-data-model/schema.sql` | Report 44's DDL, the source the UML diagrams (`*.png` next to it, mermaid in the report) are drawn from. No database runs it yet (UD-24). |
| Migrations | `application/src/main/resources/db/migration` | None on `main` yet: each data-model ticket cuts its `V<n>__*.sql` from report 44, and each is a STOP item. |
| Map file | [map] | Object→table and field→column pairs that names cannot settle, the accepted findings with their reasons, and the bookkeeping columns every table carries. |
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
- The two append-only logs (`song_changes`, `auth_attempts`) use a `bigint` identity; every other id is a `uuid`,
  `audit_log.id` included, because the contract exposes it (`auditEvent.id`).
- A list that grows (`auditAction` since contract v5.0.0) is an `x-extensible-enum`: every contract value must be
  storable (error), but a stored value the contract does not list yet is only a warning, `enum-not-listed`.
- A time of day is `clockTime`, a pattern string with no `format`; the script reads a pattern that accepts `09:30` as a
  time, so it compares cleanly with a `time` column.
- A nested object that is nullable or optional (`documentSeller` is null while a document is a draft) makes its
  required fields optional as columns: `seller.*` may be NULL in `billing_documents`.

## Map file decisions

The contract wins over report 44: when the two disagreed, `schema.sql` changed (`fix/schema-contract-parity` in
`weddingapp`); the map only pairs names and accepts what the schema's own rules justify.

| Map entry | Target | Why |
|---|---|---|
| `userAccount`, `meResponse`, `passwordChange` | `users` | The contract names the account, the schema the person. |
| `userAccount.roles` | `membership_roles.role` | Roles are one row each per membership (report 44 §5.1). |
| `userAccount.vendorId` | `memberships.vendor_id` | The member side holds the link (contract: a vendor carries no account id); `ux_memberships_vendor` is `VENDOR_ALREADY_LINKED`. |
| `meResponse.password`, `passwordChange.newPassword` | `password_hash` | Stored hashed; `currentPassword` is only checked, never stored (`null`). |
| `invite`, `inviteCreated`, `invitePreview` | `invitations` | `invitePreview.invitedBy` is joined from the inviter (`null`). |
| `person` | `wedding_people` | |
| `teamSlot` | `wedding_team` | `contactId` → `vendor_contact_id`, `personName` → `person_name_override`. |
| `wedding*.note`, `wedding*.venueName` | `notes`, `venue_name_override` | The schema's names; the venue name overrides the linked vendor's. |
| `portalIntake` | `weddings` | `names` → `portal_names`. |
| `blocklistResponse`, `portalSongsResponse` | `song_entries` | `portalSongsResponse.kind` → `song_lists.kind_code`. |
| `libraryInfo`, `trackChoice` | `libraries`, `preferences` | `ownerId` → `owner_user_id`. |
| `path:imported-playlists`, `playlistImportResult` (`playlist.*`) | `rekordbox_playlists` | |
| `organization.billing.*` | `billing_settings.*` | The business's billing defaults are their own table (report 44 §5.5). |
| `quote`, `invoice`, `creditNote` | `billing_documents` | One table for the three kinds; their `lines[]` go to `billing_document_lines`. |
| `auditEvent`, `coupleChange` | `audit_log`, `song_changes` | |
| `signedOutResponse` | `null` | A confirmation body, nothing stored. |

Accepted findings (info in the report, each with its reason in the map):

| Field | Finding | Why it holds |
|---|---|---|
| `creditNote.number` | `db-allows-null` | Null only on a draft (`ck_billing_documents_issued`); a credit note is always issued. |
| `quote.weddingId` | `db-allows-null` | Null only on an invoice or credit note kept after the erase (`ck_billing_documents_wedding`). |
| `quote.recipient.name`, `invoice.recipient.name` | `optional-but-not-null` | An empty create body starts a pre-filled draft: the server copies the recipient from the partners (UD-26.f). |
| `portalSongsResponse.position` | `null-not-storable` | Null asks the server to append; it stores the first free slot. |
| `portalIntake.names` | `db-allows-null` | Read as `coalesce(portal_names, couple_display_name)` (UD-18.b). The contract does not say so yet: a gap to raise with the contract-agent. |

## Last run (contract v5.0.0 against report 44, 2026-10-10)

0 errors, 0 warnings, 201 info (6 of them accepted); 22 of 35 tables matched, 23 read models without a table. The
info is the inventory: 87 derived fields, 46 columns and 13 tables the contract does not expose, 26 nested
collections. To get there, `schema.sql` took the contract's side three times:

- `ck_audit_log_action` is the contract's 29 `auditAction` values (`SIGN_IN`/`SIGN_OUT` replace `SIGN_IN_SUCCEEDED`/
  `SIGNED_OUT`; `MEMBER_VENDOR_LINKED`/`UNLINKED` and `WEDDING_DELETED`/`PURGED` are new), and `audit_log.id` is a
  `uuid`.
- `billing_documents.recipient_name` is NOT NULL: `billingRecipient.name` is required, drafts included.
- The member–vendor link moved from `vendor_contacts.user_id` to `memberships.vendor_id`, unique per vendor.

[map]: contract-db-parity.map.json
