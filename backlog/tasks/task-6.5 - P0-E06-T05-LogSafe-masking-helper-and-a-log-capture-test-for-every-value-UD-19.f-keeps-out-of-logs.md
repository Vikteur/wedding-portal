---
id: TASK-6.5
title: >-
  P0-E06-T05 LogSafe masking helper and a log-capture test for every value
  UD-19.f keeps out of logs
status: To Do
assignee: []
created_date: '2026-10-07 05:46'
labels:
  - technical
  - P0
  - stop-crypto-logging
milestone: m-0
dependencies:
  - TASK-5.2
  - TASK-6.1
references:
  - 'docs/rewrite/architecture-conventions.md:799-861'
  - .claude/skills/identifier-pseudonymization/SKILL.md
  - 'rekord-api/src/main/resources/application.properties:1-81'
  - 'docs/rewrite/STATUS.md:409-411'
documentation:
  - docs/rewrite/architecture-conventions.md
parent_task_id: TASK-6
priority: high
type: task
ordinal: 605
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a developer of wedding-portal, I want one masking helper and a test that searches the captured log so that no token, access code, password, e-mail address, name or phone number is written by the application.

UD-19.f (analysis 18 C-19) settles what is never logged: no log line of wedding-portal holds a token of any kind (invite, portal, guest, session), an access code, a password, an e-mail address, a person's name or a phone number, and log lines identify rows by internal ids only. architecture-conventions §12.1 and §12.4 put one helper, app.rekord.logging.LogSafe, in the logging module, with LogSafe.email and LogSafe.token; UD-19.f has LogSafe.token write the fixed text [redacted], and since no part of an e-mail address is allowed in a log line, LogSafe.email writes the same fixed text. The application never logs a full portal URL. The catch-all of P0-E05-T02 writes the cause of every 500 to the server log, so that line is searched too. The log-capture run is a test of the integrationTest task (the slow set of architecture-conventions §13.2) with the log category app.rekord at TRACE and every other category at DEBUG, so every application log call is written while framework categories stay at the level that writes no raw request bytes, and with quarkus.http.access-log.enabled left unset so the HTTP access log is off, as in rekord-api, whose application.properties sets no quarkus.http.access-log key. The test-only resources live under /api/test/log/, a prefix no contract operation uses, so they never collide with previewInvite or getPortalState. The test-only resource POST /api/test/log/values writes one log line at TRACE per value it receives, received <name>: <masked value>, naming the rm_session cookie session_cookie and each body field by its JSON name, with the e-mail address passed through LogSafe.email and every other value through LogSafe.token. Any change to what is logged where personal data could be involved is a STOP item.

- STOP (human approval in the pull request): crypto-logging
- Covers: UD-19.f

Plan item `P0-E06-T05` (technical,P0,stop-crypto-logging) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given LogSafe.token applied to the test portal token tok-example-123 and to the test invite token inv-example-456 When each result is read Then each result is exactly [redacted] (UD-19.f).
- [ ] #2 Given LogSafe.email applied to member-7f3a@example.com When the result is read Then it is exactly [redacted], so a log line that names an address through it holds no part of the address (UD-19.f).
- [ ] #3 Given the log-capture run and test-only resources at GET /api/test/log/guest/{token} and GET /api/test/log/invites/{token} that each write one log line at INFO naming the path token through LogSafe.token When GET /api/test/log/guest/gst-example-789 and GET /api/test/log/invites/inv-example-456 are served Then no captured line holds gst-example-789, inv-example-456 or either request path with its token, and both application lines are captured holding [redacted] (UD-19.f).
- [ ] #4 Given the log-capture run and the test-only resource POST /api/test/log/values of the description When a request with the cookie rm_session=sess-example-0001 and the JSON body {"invite_token":"inv-example-456","portal_token":"tok-example-123","guest_token":"gst-example-789","access_code":"4821-7735","password":"pw-test-0001","email":"member@example.com","name":"Testa Persona","phone":"+12025550100"} is served Then no captured line holds any of the nine values, and the capture holds received <name>: [redacted] for all nine names (UD-19.f).
- [ ] #5 Given a test-only resource method that throws an IllegalStateException whose message holds member@example.com and tok-example-123 When the catch-all of P0-E05-T02 answers 500 UNKNOWN and writes the cause at ERROR Then neither value appears in the captured log line or its stack trace (UD-19.f).
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
