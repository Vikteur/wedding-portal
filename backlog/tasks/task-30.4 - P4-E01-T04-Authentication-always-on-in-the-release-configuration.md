---
id: TASK-30.4
title: P4-E01-T04 Authentication always on in the release configuration
status: To Do
assignee: []
created_date: '2026-10-07 07:18'
labels:
  - user-story
  - P4
  - stop-auth-access
  - stop-production-config
milestone: m-4
dependencies:
  - TASK-30.3
references:
  - 'rekord-backend/server/couples_api.py:219-227'
  - 'rekord-api/src/test/java/app/rekord/security/RouteGuardTest.java:49-64'
  - 'docs/rewrite/analysis/16-build-test-deploy-ops.md:240'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/analysis/16-build-test-deploy-ops.md
parent_task_id: TASK-30
priority: high
type: feature
ordinal: 40104
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a visitor, I want every wedding route of the release to refuse me without a session so that no couple data or link is open to anyone with the address.

H3 and UX-08: the Python stack runs with AUTH_DISABLED on by default and the proxy Basic auth commented out, so couple data and magic-link tokens are readable by anyone (16 R-02; rekord-backend/server/couples_api.py:219-227). The rewrite drops the switch: every route needs a guard except the seven on the public list (rekord-api/src/test/java/app/rekord/security/RouteGuardTest.java:49-64). P0-E06-T01 and P0-E06-T03 hold the rule in code; this ticket checks it on the release configuration started in CI, and P4-E04-T02 checks it on the new server and on the released commit SHA. Release settings and authentication are STOP items.

- STOP (human approval in the pull request): auth-access, production-config
- Covers: H3, UX-08

Plan item `P4-E01-T04` (user-story,P4,stop-auth-access,stop-production-config) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given the compose stack started in CI from the release configuration behind the nginx container of P4-E01-T02 When a visitor without a session calls GET /api/weddings through nginx Then the answer is 401 with {"detail":{"code":"NOT_SIGNED_IN","message":"Sign in to continue."}}.
- [ ] #2 Given the compose file, the settings keys, the nginx site file and the release workflow When they are searched Then none holds a key or directive that turns authentication or the route guard off, and none contains the text AUTH_DISABLED.
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
