---
id: TASK-15.10
title: >-
  P1-E09-T10 Record sign-in, member, invitation and portal events in an
  append-only audit_log
status: To Do
assignee: []
created_date: '2026-10-07 07:13'
labels:
  - technical
  - P1
  - stop-db-migration
  - stop-crypto-logging
  - stop-auth-access
milestone: m-1
dependencies:
  - TASK-7.4
  - TASK-7.5
  - TASK-7.7
  - TASK-8.4
  - TASK-8.5
  - TASK-8.6
  - TASK-8.7
  - TASK-8.8
  - TASK-12.3
  - TASK-12.4
  - TASK-12.5
  - TASK-15.9
references:
  - 'rekord-api/src/main/resources/db/migration/V2__sessions.sql:48-63'
  - 'docs/rewrite/architecture-conventions.md:832-841'
  - 'docs/rewrite/analysis/10-identity-access.md:618'
  - 'docs/rewrite/STATUS.md:438-441'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-15
priority: high
type: task
ordinal: 10910
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a developer of wedding-portal, I want sign-in, member, invitation and portal events written to an append-only audit_log so that an incident can be traced to an account and a row without storing personal data.

Deviation UD-19.k (architecture-conventions §12.2, 15 R-13): rekord-api creates an audit_log table (V2__sessions.sql:48-63) and writes no row. wedding-portal creates its own audit_log in a new Flyway migration with exactly the columns id bigserial primary key, at timestamptz not null, actor_id uuid null, action text not null checked against the 11 actions below, and target_id uuid null, and an index over target_id and at. No foreign key starts at audit_log, so a row outlives the account or row it names, and the table holds no e-mail address, name, phone number, token, access code, password or client address (UD-19.f). The use cases write through an AuditLog port with record(AuditEntry) in app.rekord.usecase.identity.port; its persistence adapter inserts and reads, and never updates or deletes. at comes from the Clock of architecture-conventions §5.3. A row is written in the transaction of its operation, so a refused or rolled-back call writes none, except SIGN_IN_FAILED, which login stores the way P1-E01-T05 stores a failed attempt, so it survives the refusal. Actions, with actor_id and target_id: SIGN_IN_SUCCEEDED for a login answered 200 (actor and target the account); SIGN_IN_FAILED for a login answered 401 BAD_CREDENTIALS, 403 ACCOUNT_DISABLED or 403 FORBIDDEN (actor null, target the live account the address names, else null), while a login answered 422 or 429 writes none; SIGNED_OUT for a logout that revokes a session (actor and target the account), while a logout without a live session writes none; ROLES_CHANGED for a setMemberRoles that changes the member's role set (actor the admin, target the member), while a call that sets the same set writes none; INVITE_CREATED for createInvite, INVITE_REVOKED for every 204 of revokeInvite (actor the caller, target the invite), and INVITE_ACCEPTED for acceptInvite (actor the new account, target the invite); MEMBER_REMOVED for deleteMember (actor the caller, target the removed account); PORTAL_ROTATED for each portal row rotateWeddingPortal inserts (target the new portal) and PORTAL_REVOKED for each live portal revokeWeddingPortal revokes (target the revoked portal), with actor the caller; ACCESS_CODE_REISSUED for reissueAccessCode (actor the caller, target the wedding). The cleanup job of P1-E09-T09 deletes no audit_log row, and a planner operation that reads audit_log is not part of phase 1.

- STOP (human approval in the pull request): db-migration, crypto-logging, auth-access
- Covers: UD-19.k

Plan item `P1-E09-T10` (technical,P1,stop-db-migration,stop-crypto-logging,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given the phase-1 schema on an empty database When the test reads audit_log Then its columns are exactly id, at, actor_id, action and target_id, no foreign key starts at it, and an insert with action "EXPORTED" fails on the action check (deviation UD-19.k; rekord-api writes no audit row).
- [ ] #2 Given an ACTIVE account planner@example.com holding PLANNER and a fixed clock at 2027-06-01T10:00:00Z When a visitor calls login with its right password Then one audit_log row exists with at 2027-06-01T10:00:00Z, action SIGN_IN_SUCCEEDED and actor_id and target_id the account's id (UD-19.k).
- [ ] #3 Given that account and an address no account has When a visitor calls login with the account's address and a wrong password, then with the unknown address Then both answer 401 BAD_CREDENTIALS and two SIGN_IN_FAILED rows exist with actor_id null, the first with target_id the account's id and the second with target_id null, and no column of either row holds an address (UD-19.k, UD-19.f).
- [ ] #4 Given a DISABLED account and an ACTIVE account without any ACTIVE membership When each calls login with its right password Then the answers are 403 ACCOUNT_DISABLED and 403 FORBIDDEN, and each adds one SIGN_IN_FAILED row with target_id that account's id (UD-19.k).
- [ ] #5 Given 5 failed sign-ins for planner@example.com from one client within the last 300 seconds When a visitor calls login with a body without email, and that client calls login for planner@example.com a sixth time Then the answers are 422 VALIDATION_FAILED and 429 RATE_LIMITED and no audit_log row is added by either call (UD-19.k).
- [ ] #6 Given a signed-in planner When the planner calls logout, and a request without a cookie calls logout Then the first call adds one SIGNED_OUT row with actor_id and target_id the planner's id and the second adds no row (UD-19.k).
- [ ] #7 Given an admin and a member holding only DJ When the admin calls setMemberRoles for the member with [DJ, PLANNER], then again with [DJ, PLANNER] Then the first call adds one ROLES_CHANGED row with actor_id the admin's id and target_id the member's id and the second adds no row (UD-19.k).
- [ ] #8 Given a planner When the planner calls createInvite for new.dj@example.com with role DJ, revokeInvite on that invite, and createInvite for next.dj@example.com, and that second invite is accepted through acceptInvite Then the rows INVITE_CREATED, INVITE_REVOKED, INVITE_CREATED and INVITE_ACCEPTED exist in that order, each with target_id its invite's id, the first three with actor_id the planner's id and the last with actor_id the new account's id (UD-19.k).
- [ ] #9 Given a CONFIRMED wedding of the partners Emma and Julian (display name "Emma & Julian") dated 2027-06-12 in time zone Europe/Amsterdam, with live COUPLE and FRIENDS portals and the access code EJ12062027 When a planner calls reissueAccessCode, then rotateWeddingPortal with scope FRIENDS, then revokeWeddingPortal with scope COUPLE Then the rows ACCESS_CODE_REISSUED with target_id the wedding's id, PORTAL_ROTATED with target_id the new friends portal's id and PORTAL_REVOKED with target_id the couple portal's id exist in that order, each with actor_id the planner's id (UD-19.k).
- [ ] #10 Given an admin, a planner without ADMIN and a member holding only DJ When the planner calls deleteMember for the DJ and setMemberRoles for the DJ with [PLANNER], and then the admin calls deleteMember for the DJ Then the planner's calls answer 403 FORBIDDEN "This is not yours to open." and add no audit_log row, and the admin's call adds one MEMBER_REMOVED row with actor_id the admin's id and target_id the DJ's account id (UD-19.k).
- [ ] #11 Given the AuditLog port and its persistence adapter When the architecture test runs Then the port declares only record and a read method, and no class issues an update or a delete on audit_log (architecture-conventions §12.2).
- [ ] #12 Given an audit_log row with at 2020-01-01T00:00:00Z and the clock at 2027-07-01T03:00:00Z When the cleanup job of P1-E09-T09 runs Then the row remains (UD-19.j, UD-19.k).
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 Every acceptance criterion of the ticket is checked by at least one automated test in wedding-portal that fails when the criterion is broken.
- [ ] #2 The CI run of the ticket's pull request is green, read by commit SHA; no test, gate or check is skipped, disabled or quarantined to get there.
- [ ] #3 The ArchUnit rules pass with no new entry in the frozen baseline.
- [ ] #4 Every contract change the ticket needs is merged and tagged in rekord-contract before the code that uses it; wedding-portal pins that tag; no generated file is edited by hand.
- [ ] #5 Every STOP item the ticket touches (contract push, database migration, authentication or the access matrix, encryption or logging of personal data, deleting history, production configuration or secrets) has a human approval recorded in the pull request before the step is taken.
- [ ] #6 Every refusal is answered with the error envelope {"detail":{"code","message"}} and a code from the contract's ErrorCode enum; validation follows the Notification pattern (UD-12), and a 422 lists its violations in the `errors` list (UD-19.d).
- [ ] #7 Every operation the ticket builds answers with the status, error code and body shape rekord-api gives for the same request, except for the deviations recorded as user decisions (UD-9 to UD-20) in docs/rewrite/STATUS.md; each deviation the ticket implements is named in the pull request.
- [ ] #8 No secret, token, password, access code or personal data appears in logs, test data, commits or documents.
- [ ] #9 Every schema change is a new Flyway migration (an applied migration is never edited) and is exercised by a Testcontainers test.
- [ ] #10 Every document or code-map leaf that the change makes outdated is updated in the same pull request.
- [ ] #11 The pull request is reviewed and merged into main.
<!-- DOD:END -->
