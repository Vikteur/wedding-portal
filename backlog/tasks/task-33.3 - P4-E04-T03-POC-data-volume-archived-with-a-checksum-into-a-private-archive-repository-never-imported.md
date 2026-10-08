---
id: TASK-33.3
title: >-
  P4-E04-T03 POC data volume archived with a checksum into a private archive
  repository, never imported
status: To Do
assignee: []
created_date: '2026-10-07 07:19'
labels:
  - technical
  - P4
  - stop-production-config
  - stop-delete-history
milestone: m-4
dependencies:
  - TASK-33.2
references:
  - 'docs/rewrite/STATUS.md:279-297'
  - 'docs/rewrite/STATUS.md:479-481'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/analysis/16-build-test-deploy-ops.md
parent_task_id: TASK-33
priority: high
type: task
ordinal: 40403
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As an operator of wedding-portal, I want the POC data volume archived with a checksum in a private repository of its own and kept out of the new database so that nothing is lost and nothing stale enters wedding-portal.

UD-13.c: the POC Python data volume is archived, not imported. Nothing is deleted; the POC server is the user's to switch off (UD-13.f). The volume lives on the POC server, so the archive file is made there. UD-20.d settles where it is kept: the archive and its SHA-256 file are pushed into the archive/ folder of a new, dedicated, private repository (for example wedding-poc-archive), apart from every code repository, so no code repository ever holds the POC data. The archive holds personal data, so creating that repository and pushing the archive are each a STOP item needing a recorded human approval; the user names the repository at the approval, and no document records anything from inside the archive.

- STOP (human approval in the pull request): production-config, delete-history
- Covers: UD-13.c, UD-20.d

Plan item `P4-E04-T03` (technical,P4,stop-production-config,stop-delete-history) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a recorded human approval When the archiving step runs on the POC server Then the POC Python data volume is copied into one archive file with a SHA-256 checksum file beside it, and the checksum of a fresh read of the archive matches the stored checksum.
- [ ] #2 Given a recorded human approval for creating the archive repository When it is created Then it is a new private repository that holds no code, separate from rekord-contract, wedding-portal, rekord-api, rekord-backend, planner, rekord-couple and rekord-dj (UD-20.d).
- [ ] #3 Given the archive repository and a separate recorded human approval for the push When the archive file and its SHA-256 file are pushed Then both are in its archive/ folder, the checksum of the archive read back from a fresh clone matches the stored checksum, and the repository is still private (UD-20.d).
- [ ] #4 Given rekord-contract, wedding-portal, rekord-api, rekord-backend, planner, rekord-couple and rekord-dj after the archiving step When their files and history are searched for the archive file name Then none of them holds the archive or its SHA-256 file (UD-20.d).
- [ ] #5 Given the compose file and the release workflow of wedding-portal at the released commit When they are read Then they mount no POC volume and run no import job (UD-13.c).
- [ ] #6 Given the POC server after archiving When its volumes are listed Then the POC data volume still exists.
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
