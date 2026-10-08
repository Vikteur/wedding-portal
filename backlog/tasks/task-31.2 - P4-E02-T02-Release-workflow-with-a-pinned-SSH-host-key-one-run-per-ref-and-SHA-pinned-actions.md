---
id: TASK-31.2
title: >-
  P4-E02-T02 Release workflow with a pinned SSH host key, one run per ref and
  SHA-pinned actions
status: To Do
assignee: []
created_date: '2026-10-07 07:18'
labels:
  - technical
  - P4
  - stop-production-config
milestone: m-4
dependencies:
  - TASK-31.1
references:
  - 'rekord-api/.github/workflows/deploy.yml:11-13'
  - 'rekord-api/.github/workflows/deploy.yml:171-233'
  - 'rekord-contract/.github/workflows/contract.yml:42-52'
  - 'docs/rewrite/analysis/16-build-test-deploy-ops.md:163'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/analysis/16-build-test-deploy-ops.md
parent_task_id: TASK-31
priority: high
type: task
ordinal: 40202
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As an operator of wedding-portal, I want the release job to reach only the server whose host key is pinned so that no spoofed host receives the release files or secrets.

H7, RISK-14 and BR-OPS-28: when SSH_KNOWN_HOSTS is empty every POC deploy workflow trusts the host key on first use through ssh-keyscan (rekord-api/.github/workflows/deploy.yml:180-196), and the contract's breaking-change action is pinned to a moving branch (rekord-contract/.github/workflows/contract.yml:42-52). BR-OPS-10: one run per ref, never cancelled, in the production environment (deploy.yml:11-13, :176). BR-OPS-27: the server signs in to the registry over stdin, never in argv (deploy.yml:216-229). A STOP item (production configuration).

- STOP (human approval in the pull request): production-config
- Covers: BR-OPS-10, BR-OPS-27, BR-OPS-28, H7, RISK-14

Plan item `P4-E02-T02` (technical,P4,stop-production-config) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a second push to main while the release run of the first push is still running When the release workflow starts for it Then the second run waits for the first in one concurrency group per ref with cancel-in-progress false, and the release job runs in the production environment.
- [ ] #2 Given the release job When it prepares SSH Then the known_hosts file is written only from the pinned host-key secret, the job fails when that secret is empty, and no step runs ssh-keyscan.
- [ ] #3 Given a server whose host key differs from the pinned one When the release job connects Then the connection is refused and the job fails before any file, image name or secret is sent.
- [ ] #4 Given the release workflow added to wedding-portal When its uses: lines are read under the SHA-pinning rule of P0-E02-T01 Then every third-party action in it is pinned by a 40-character commit SHA, and the breaking-change action of rekord-contract keeps the commit-SHA pin P0-E02-T04 set.
- [ ] #5 Given the release job When the server signs in to the registry Then the credential is passed over --password-stdin and never appears in a command line.
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
