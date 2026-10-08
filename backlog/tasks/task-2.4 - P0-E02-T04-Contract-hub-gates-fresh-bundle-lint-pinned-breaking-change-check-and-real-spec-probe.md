---
id: TASK-2.4
title: >-
  P0-E02-T04 Contract hub gates: fresh bundle, lint, pinned breaking-change
  check and real-spec probe
status: To Do
assignee: []
created_date: '2026-10-07 05:45'
labels:
  - technical
  - P0
  - stop-contract-push
milestone: m-0
dependencies:
  - TASK-2.2
  - TASK-2.3
references:
  - 'rekord-contract/.github/workflows/contract.yml:1-52'
  - 'rekord-contract/redocly.yaml:15-37'
  - 'rekord-contract/smoke/pom.xml:60-95'
  - 'rekord-contract/package.json:12'
  - 'rekord-contract/README.md:64-73'
  - 'docs/rewrite/analysis/16-build-test-deploy-ops.md:136-166'
  - 'docs/rewrite/STATUS.md:406-408'
documentation:
  - docs/rewrite/architecture-conventions.md
parent_task_id: TASK-2
priority: high
type: task
ordinal: 204
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a maintainer of rekord-contract, I want the hub's CI to refuse a stale bundle, a lint break, a breaking change and a spec that stops the Java generation so that no consumer is the first to find a broken contract.

The hub already fails on a stale dist/ bundle and on its redocly policy (rekord-contract/.github/workflows/contract.yml:21-29; rekord-contract/redocly.yaml:15-37). Its breaking-change job runs only on pull requests, through an oasdiff action pinned to a moving branch, against the pull request base (contract.yml:42-52), so a direct push to main bypasses it (BR-OPS-06). Its Java smoke job compiles a synthetic probe without dateLibrary, so a spec that breaks Java generation first fails a consumer (BR-OPS-04, PIN-16-0139, 16 R-10; rekord-contract/smoke/pom.xml:60-95). npm run diff names dist/openapi.baseline.yaml, which nothing creates (rekord-contract/package.json:12). The POC's walkthrough started the app without the contract (16 R-06). UD-19.e names semver:major as the label of a breaking change, so the breaking-change gate lets an ERR-level change pass only under that label; the human approval every contract pull request needs as a STOP item (contract push, Definition of Done) is the check on that label. Changing the hub's CI is a STOP item (contract push).

- STOP (human approval in the pull request): contract-push
- Covers: CONV-6, BR-OPS-03, BR-OPS-04, BR-OPS-05, BR-OPS-06, PIN-16-0139, RISK-57, UD-19.e

Plan item `P0-E02-T04` (technical,P0,stop-contract-push) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a commit to rekord-contract whose dist/openapi.yaml differs from the bundle its sources produce When the contract CI runs Then the validate job fails and names dist/ as stale.
- [ ] #2 Given a spec with an operation that lacks an operationId, a duplicate operationId, an operation without a summary, an undefined tag, an unused component or an operation without defined security When the contract CI runs Then redocly fails the job.
- [ ] #3 Given a contract pull request that removes an operation and carries the label semver:minor, the label semver:patch or no semver label When the contract CI runs, also after a label is added or removed Then the breaking-change job, running oasdiff pinned by a 40-character commit SHA, compares the change with the base of the pull request on main, as rekord-contract does (rekord-contract/.github/workflows/contract.yml:42-52), and fails on the ERR-level change (UD-19.e).
- [ ] #4 Given a contract pull request that removes an operation and carries the label semver:major When the contract CI runs Then the breaking-change job lists the ERR-level change in its output and passes, so the pull request is allowed to merge after the human approval its STOP item contract-push needs, and the merge commit gets the tag that raises the major part (UD-19.e).
- [ ] #5 Given the main branch of rekord-contract When anyone pushes to it directly Then branch protection refuses the push, so every change to main passes the breaking-change job in a pull request.
- [ ] #6 Given the package.json of rekord-contract When npm run diff runs Then it compares dist/openapi.yaml with the bundle of the highest v<major>.<minor>.<patch> tag reachable from main by semantic-version order, which is v0.1.0 from the first merge after P0-E02-T02 is in place (UD-19.e, UD-20.a), and no script names dist/openapi.baseline.yaml.
- [ ] #7 Given the hub's smoke job When the contract CI runs Then it generates Java from the real dist/openapi.yaml with openapi-generator 7.25.0, jaxrs-spec and the seven options of architecture-conventions §4, dateLibrary=java8 included, and a spec change that stops the generated Java compiling fails the contract job.
- [ ] #8 Given the hub's smoke job and the openApiGenerate task of wedding-portal When their generator versions and option sets are compared Then they are identical, so wedding-portal never meets a generation failure the hub passed.
- [ ] #9 Given any CI job of wedding-portal that starts the application When it runs Then it passes contract.spec pointing at the checkout of the rekord-contract tag or the shared feature branch the build file pins (P0-E02-T02), so no job depends on a sibling path CI lacks (16 R-06).
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
