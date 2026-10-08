---
id: TASK-31
title: >-
  P4-E02 Deploy pipeline with a pinned host key, a health gate and a working
  rollback
status: To Do
assignee: []
created_date: '2026-10-07 07:18'
labels:
  - epic
  - P4
milestone: m-4
dependencies:
  - TASK-30
references:
  - 'docs/rewrite/repo-and-contract-decision.md:811-865'
  - rekord-api/deploy/deploy.sh
  - rekord-api/.github/workflows/deploy.yml
  - 'docs/rewrite/analysis/16-build-test-deploy-ops.md:132-167'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/16-build-test-deploy-ops.md
priority: high
ordinal: 40200
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a planner, I want a failed deploy to bring the previous version of wedding-portal back so that the app I work in stays available.

Goal: a deploy pipeline that works the first time (H4) and fails safe. Scope: the deploy workflow reading the pinned SSH host key and stopping on a mismatch (H7, RISK-14); the health gate on GET /api/health with the retry budget rekord-api's deploy uses; a pre-flight step that stops the release before it changes anything while a deployment setting is unset or an origin certificate file is missing (UD-19.n2, UD-19.n3, UD-19.b); a rollback reached on every failure path, including a failed start, a failed pull and a failed bundle swap (H5, RISK-52, RISK-53); the deploy-script PIN items moved here from phase 0; the build of the three unchanged frontend bundles from pinned commits and their swap into the web root of the new server (UD-19.n1, BR-OPS-19). Out of scope: backups (P4-E03).

Plan item `P4-E02` (epic,P4) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a deploy whose new version does not answer GET /api/health within 45 attempts 2 seconds apart When the pipeline runs Then it restores the previous version, which answers GET /api/health with 200 afterwards.
- [ ] #2 Given an SSH host key on the server that differs from the pinned one When the pipeline connects Then it stops before sending any file or secret (H7).
- [ ] #3 Given docker compose up failing after the app container was recreated When the deploy script runs Then it still reaches its rollback step and the previous version answers GET /api/health with 200 (H5).
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
