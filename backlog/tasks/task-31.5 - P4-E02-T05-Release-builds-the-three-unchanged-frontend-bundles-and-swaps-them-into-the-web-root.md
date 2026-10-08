---
id: TASK-31.5
title: >-
  P4-E02-T05 Release builds the three unchanged frontend bundles and swaps them
  into the web root
status: To Do
assignee: []
created_date: '2026-10-07 07:18'
labels:
  - technical
  - P4
  - stop-production-config
milestone: m-4
dependencies:
  - TASK-31.3
  - TASK-30.6
references:
  - 'planner/.github/workflows/deploy.yml:33-41'
  - 'planner/.github/workflows/deploy.yml:61-63'
  - 'planner/.github/workflows/deploy.yml:99-117'
  - 'rekord-couple/.github/workflows/deploy.yml:106-117'
  - 'rekord-dj/.github/workflows/deploy.yml:109-121'
  - 'docs/rewrite/analysis/16-build-test-deploy-ops.md:154'
  - 'docs/rewrite/analysis/16-build-test-deploy-ops.md:239'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/analysis/16-build-test-deploy-ops.md
parent_task_id: TASK-31
priority: high
type: task
ordinal: 40205
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As an operator of wedding-portal, I want the release to build the planner, couple and DJ bundles from pinned commits of their unchanged repositories and swap them into the web root so that the new server serves the frontends without any frontend change.

UD-19.n1: the release serves the three unchanged frontend bundles as static files through nginx on the new server. The frontend repositories stay unchanged (UD-13), and their own deploy workflows target the POC server and have never succeeded (H4), so the release workflow of wedding-portal builds the bundles itself, each from a commit of its repository pinned by full SHA in that workflow, and that commit is the head of the main branch of the repository when the release runs, so the release serves the unchanged frontends, with the repository's own npm ci and npm run build on Node 24, the version the frontends' deploy workflows set (planner/.github/workflows/deploy.yml:33-41, :61-63). The repositories are private like rekord-contract (16 R-01), so the checkout needs a second fine-grained token, read-only on planner, rekord-couple and rekord-dj; creating and storing it is a STOP item, like the contract token of UD-15.e. BR-OPS-19: bundles are uploaded beside the live directory and swapped in, never written over it; the planner and couple bundles swap with two renames, and the DJ bundle, which owns the web root, is uploaded beside it, then assets/ is removed and the files are copied in (planner/.github/workflows/deploy.yml:99-117; rekord-dj/.github/workflows/deploy.yml:109-121). Neither swap is atomic (16 R-15); the release keeps the frontends' own swap steps unchanged. The web root is /opt/wedding-portal/web (P4-E01-T01). The swap runs on the server after the health gate of P4-E02-T03 passed; before its first rename it copies the live planner/, guest/, index.html and assets/ to /opt/wedding-portal/web.prev, so a failed swap is part of the rollback of every failure path (H5). The release runs no couple or DJ image, so H2 and H6 stay with the frontends. A STOP item (production configuration).

- STOP (human approval in the pull request): production-config
- Covers: BR-OPS-19, UD-19.n1

Plan item `P4-E02-T05` (technical,P4,stop-production-config) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given the release workflow of wedding-portal When it is read Then it checks out planner, rekord-couple and rekord-dj each at a full 40-character commit SHA written in the workflow, with a CI secret holding one fine-grained token that has read-only contents access to those three repositories and no other, and builds each with npm ci and npm run build on Node 24.
- [ ] #2 Given the three pinned SHAs in the release workflow When the release job starts Then it compares each with the head commit of the main branch of its repository, and when one differs the job is red before any build and names that repository (UD-13).
- [ ] #3 Given the checkouts of planner, rekord-couple and rekord-dj made with that token When a step of the release tries to push to one of them Then the push is refused because the token is read-only (UD-13).
- [ ] #4 Given a frontend build that exits non-zero When the release job runs Then the job is red, no bundle is copied to the server, and the web root on the server is unchanged.
- [ ] #5 Given a release whose new version the health gate of P4-E02-T03 rejects When the release job runs Then no bundle is copied to the server and the web root is unchanged.
- [ ] #6 Given the web root /opt/wedding-portal/web holding planner/, guest/ and the DJ files index.html and assets/ of an earlier build When the swap step copies new planner and couple bundles Then each is uploaded into planner.new or guest.new beside its live directory, the live directory is renamed to planner.old or guest.old, the .new directory is renamed to the live name, the .old directory is removed, and no file is written into a live directory (BR-OPS-19).
- [ ] #7 Given the web root /opt/wedding-portal/web holding planner/, guest/ and the DJ files index.html and assets/ of an earlier build When the swap step copies a new DJ bundle Then it is uploaded into .dj.new in the web root, assets/ is removed, the files of .dj.new are copied into the web root, .dj.new is removed, and planner/ and guest/ are untouched (BR-OPS-19; rekord-dj/.github/workflows/deploy.yml:118-121).
- [ ] #8 Given the compose file of wedding-portal at the release commit When it is read Then it names no image of rekord-couple or rekord-dj, and the three bundles reach the server only as static files in the web root (UD-19.n1; H2, H6).
- [ ] #9 Given the bundles built from the pinned commits and swapped into the web root of the CI nginx of P4-E01-T06 When https://portal.example.com/planner/, https://portal.example.com/g/tok-example-123 and https://portal.example.com/ are requested Then each answers 200 with the index.html of the planner, couple and DJ build respectively (UD-19.n1).
- [ ] #10 Given a server on which /opt/wedding-portal/web does not exist When the swap step runs after a passed health gate Then it creates /opt/wedding-portal/web, planner/index.html, guest/index.html, index.html and assets/ of the new build are in place, and no .new, .old or .dj.new directory is left behind.
- [ ] #11 Given a release whose new version passed the health gate, the web root /opt/wedding-portal/web holding planner/, guest/, index.html and assets/ of an earlier build, and the swap step forced to fail after the planner bundle was swapped When the release job runs Then the release script puts back planner/, guest/, index.html and assets/ of the earlier build from /opt/wedding-portal/web.prev, sets APP_IMAGE back to the previous tag, the previous version answers GET /api/health with 200, the script exits non-zero and the job is red (H5).
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
