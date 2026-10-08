---
id: TASK-6.3
title: P0-E06-T03 No configuration key that turns authentication off
status: To Do
assignee: []
created_date: '2026-10-07 05:46'
labels:
  - technical
  - P0
  - stop-auth-access
milestone: m-0
dependencies:
  - TASK-6.1
references:
  - 'rekord-backend/server/couples_api.py:219-227'
  - 'docs/rewrite/analysis/16-build-test-deploy-ops.md:240'
documentation:
  - docs/rewrite/architecture-conventions.md
parent_task_id: TASK-6
priority: high
type: task
ordinal: 603
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a developer of wedding-portal, I want no setting that bypasses authentication so that a configuration slip never opens wedding data.

RISK-03 (high): the Python stack runs with AUTH_DISABLED on by default and the proxy Basic auth commented out, so tokens, revocation and expiry are not checked (16 R-02; rekord-backend/server/couples_api.py:219-227). rekord-api has no such switch. wedding-portal keeps none; the release check of the same rule is P4-E01-T04.

- STOP (human approval in the pull request): auth-access
- Covers: RISK-03

Plan item `P0-E06-T03` (technical,P0,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given the configuration keys of wedding-portal in every profile When they are listed Then none disables authentication, the route guard or a portal-link check, and no key contains the text AUTH_DISABLED.
- [ ] #2 Given the application started with the environment variable AUTH_DISABLED=1 When a visitor calls a test-only @Authenticated resource method Then the answer is still 401 NOT_SIGNED_IN.
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
