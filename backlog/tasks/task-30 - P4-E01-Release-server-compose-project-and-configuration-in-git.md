---
id: TASK-30
title: 'P4-E01 Release server, compose project and configuration in git'
status: To Do
assignee: []
created_date: '2026-10-07 07:17'
labels:
  - epic
  - P4
milestone: m-4
dependencies:
  - TASK-29
  - TASK-6
references:
  - 'docs/rewrite/STATUS.md:279-297'
  - 'docs/rewrite/repo-and-contract-decision.md:811-865'
  - 'docs/rewrite/analysis/16-build-test-deploy-ops.md:235-259'
  - rekord-api/deploy/docker-compose.yml
  - docs/rewrite/backlog-plan/digest-P4.md
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/16-build-test-deploy-ops.md
priority: high
ordinal: 40100
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As an operator of wedding-portal, I want the new server's compose project, proxy and settings kept in git with secrets only in the secret store so that every deploy is reproducible and no token reaches a log.

Goal: the release environment, a STOP item (production configuration). Scope: a new server with its own compose project, app directory /opt/wedding-portal and settings file /opt/wedding-portal/.env shared with no other stack (H1, RISK-55), test values only in the tracked file ci/test.env that the release never reads; the proxy configuration in git and applied from git (H8, UX-16 with LF line endings), naming the host only through the deployment setting PUBLIC_HOST, whose value is set on the server and named in no document, and from which the app's public base address for couple links and invite links is derived (UD-19.n2); the real client address restored from CF-Connecting-IP for requests from the published Cloudflare ranges only, so the sign-in throttle counts per client (UD-16.UX-13); TLS with a Cloudflare origin certificate kept as a server secret and placed on the new server by the operator with a recorded approval, the one step of this epic done on the server (UD-19.b, P4-E01-T07); the three unchanged frontend bundles served as static files by the same nginx (UD-19.n1; BR-OPS-15, BR-OPS-16, BR-OPS-17); authentication always on, with no switch that turns it off (H3, UX-08); the secret settings of the first admin (UD-14.g) and the CI token kept out of git; access logs without guest, invite or magic-link tokens, the invite page /invite/<token> included (H10). The criteria of this epic run against the compose stack started in CI, because wedding-portal is deployed only once (UD-13.d); the same checks on the new server belong to that single release, P4-E04-T02. Out of scope: the nginx images of the couple and DJ apps (H2, H6), which the release does not run, and any change to a frontend repository (UD-13).

Plan item `P4-E01` (epic,P4) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given the compose file of wedding-portal in git When CI starts it next to a probe container of another compose project Then wedding-portal runs in its own compose project wedding-portal with its own settings file, and the probe container keeps running (H1).
- [ ] #2 Given the compose stack started in CI behind the nginx container of P4-E01-T02, after the smoke run of P4-E01-T05 with one test portal token, one test invite token and one test access code and the requests of P4-E01-T06 to /g/<token> and /invite/<token> When its nginx and container logs are searched for those values Then none of them appears (H10).
- [ ] #3 Given the compose stack started in CI from the release configuration behind the nginx container of P4-E01-T02 When a visitor calls a route outside the public allow-list through nginx without a session Then the answer is 401, and no setting of the release configuration turns authentication off (H3).
- [ ] #4 Given the compose stack started in CI behind the nginx container of P4-E01-T02, with PUBLIC_HOST set to portal.example.com by ci/test.env and the web root /opt/wedding-portal/web holding test builds of the three frontend bundles When https://portal.example.com/planner/, https://portal.example.com/g/tok-example-123 and https://portal.example.com/ are requested Then each answers 200 with the index.html of the planner, couple and DJ bundle respectively (UD-19.n1).
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
