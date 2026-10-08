---
id: TASK-31.1
title: >-
  P4-E02-T01 Release image built on main after green tests and tagged by commit
  SHA
status: To Do
assignee: []
created_date: '2026-10-07 07:18'
labels:
  - technical
  - P4
  - stop-production-config
milestone: m-4
dependencies: []
references:
  - 'rekord-api/.github/workflows/deploy.yml:121-168'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/analysis/16-build-test-deploy-ops.md
parent_task_id: TASK-31
priority: high
type: task
ordinal: 40201
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As an operator of wedding-portal, I want an image built only from a tested commit of main and named by its SHA so that every release names exactly the code it runs.

BR-OPS-08: rekord-api builds and pushes an image only on a push to main and only after the tests (and its walkthrough) pass (rekord-api/.github/workflows/deploy.yml:121-130). BR-OPS-09: images are tagged with the commit SHA and latest, with the repository name lowercased (deploy.yml:143-148). The registry push uses the workflow's own token, which never leaves the runner. A STOP item (production configuration).

- STOP (human approval in the pull request): production-config
- Covers: BR-OPS-08, BR-OPS-09

Plan item `P4-E02-T01` (technical,P4,stop-production-config) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a push to main of wedding-portal When the test job is green on that commit Then the image job builds and pushes the image, and on a pull request, a feature branch or a red test job no image is pushed.
- [ ] #2 Given a pushed image When its tags are listed Then it carries the full commit SHA and latest, and the repository part of the image name is lowercase.
- [ ] #3 Given the image job When it signs in to the registry Then it uses the workflow's own token, and no personal token is stored for the push.
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
