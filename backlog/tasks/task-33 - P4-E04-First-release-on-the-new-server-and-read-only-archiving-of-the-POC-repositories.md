---
id: TASK-33
title: >-
  P4-E04 First release on the new server and read-only archiving of the POC
  repositories
status: To Do
assignee: []
created_date: '2026-10-07 07:18'
labels:
  - epic
  - P4
milestone: m-4
dependencies:
  - TASK-31
  - TASK-32
references:
  - 'docs/rewrite/STATUS.md:279-297'
  - 'docs/rewrite/repo-and-contract-decision.md:269-404'
  - docs/rewrite/backlog-plan/digest-P4.md
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
priority: high
ordinal: 40400
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As an operator of wedding-portal, I want one release of wedding-portal to the new server followed by read-only archiving of rekord-api and rekord-backend so that the POC ends without deleting anything.

Goal: the single deploy of UD-13.d and the end of the POC. Scope: the release of the tagged version after phases 0 to 3 are complete; the check on the new server that health answers, the first admin signs in and the three unchanged frontend bundles are served (UD-19.n1), together with the checks of P4-E01 repeated there (H1, H3, H8, H10) and the Cloudflare origin certificate (UD-19.b); the pre-flight step of P4-E02-T03, proven in CI, stops the release while PUBLIC_HOST or any of the backup settings BACKUP_TARGET, BACKUP_SCHEDULE and BACKUP_RETENTION_DAYS is unset, and the release run log shows that step passed (UD-19.n2, UD-19.n3); the POC Python data volume archived and not imported, its archive and SHA-256 file pushed into archive/ of a new, dedicated, private repository apart from every code repository (UD-13.c, UD-20.d); rekord-api and rekord-backend archived read-only after the release as a separate STOP step, with nothing deleted (UD-13.f). The POC server is the user's to switch off. Out of scope: the QR code (UX-11) and the couple and DJ images (H2, H6), which the release does not run.

Plan item `P4-E04` (epic,P4) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given phases 0 to 3 complete and CI green on main When the release pipeline deploys the tagged version to the new server Then GET /api/health answers 200 there, and login with the first admin's credentials from the secret settings answers 200 with the rm_session cookie set.
- [ ] #2 Given the release accepted When the archiving step runs after a recorded human approval Then rekord-api and rekord-backend are read-only archived repositories and no branch, tag or commit of them is deleted (UD-13.f).
- [ ] #3 Given the POC Python data volume When the archiving step runs after a recorded human approval Then the volume's archive and its SHA-256 file are in archive/ of the dedicated private archive repository and none of its data is in the wedding-portal database (UD-13.c, UD-20.d).
- [ ] #4 Given the run log of the release on the new server When the pre-flight step of P4-E02-T03 is read Then it reports PUBLIC_HOST, BACKUP_TARGET, BACKUP_SCHEDULE and BACKUP_RETENTION_DAYS as set before docker compose pull ran (UD-19.n2, UD-19.n3).
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
