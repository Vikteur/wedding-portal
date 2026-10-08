---
id: TASK-7.5
title: >-
  P1-E01-T05 Sign-in throttle of 5 failures per address and client within 300
  seconds
status: To Do
assignee: []
created_date: '2026-10-07 07:10'
labels:
  - user-story
  - P1
  - stop-auth-access
  - stop-db-migration
  - stop-crypto-logging
  - stop-production-config
milestone: m-1
dependencies:
  - TASK-7.4
references:
  - 'rekord-api/src/main/java/app/rekord/security/AuthResource.java:60-109'
  - rekord-api/src/main/resources/db/migration/V2__sessions.sql
  - 'rekord-contract/paths/auth.yaml:1-48'
  - 'docs/rewrite/analysis/20-unmerged-and-python-only-work.md:225-236'
  - 'docs/rewrite/STATUS.md:342-344'
  - 'docs/rewrite/business-analysis.md:1420'
  - 'rekord-api/src/main/resources/application.properties:39-41'
  - 'rekord-api/deploy/nginx/rekord.conf:49'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-7
priority: high
type: feature
ordinal: 10105
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a member, I want repeated wrong passwords for my address from one client to be stopped for a while so that nobody guesses my password by trying many of them.

UD-16.UX-13 and RISK-05, a deliberate deviation: rekord-api has no counter (AuthResource.java:62-109) although the contract promises one (auth.yaml:17-20). The rule follows the POC branch feat/auth (S20:225-236): a failure is a login answered 401 BAD_CREDENTIALS; when 5 failures for one address from one client lie within the last 300 seconds, every further login for that address from that client is answered 429 RATE_LIMITED "Too many failed sign-ins — wait a few minutes." before the password is checked, until the oldest of those failures is more than 300 seconds old. A successful sign-in does not clear earlier failures. The failures are stored in the table auth_attempts of the oracle (V2__sessions.sql: kind USER_LOGIN, identifier_hash = the SHA-256 hash of the trimmed, lower-cased address, ip = the client address the server reports for the request, success false), created by a new Flyway migration, so the count survives a restart. The 429 answer and the other refusals write no row. The client is the remote address Vert.x resolves with quarkus.http.proxy.proxy-address-forwarding=true and quarkus.http.proxy.trusted-proxies set to the reverse proxy's address, so X-Forwarded-For is honoured only from that proxy; because Vert.x takes the leftmost X-Forwarded-For entry and the POC proxy appends to a client-sent header (rekord-api/deploy/nginx/rekord.conf:49), the phase-4 proxy sets X-Forwarded-For to $remote_addr instead of appending. The 429 answer carries no Retry-After header, because the contract defines none (auth.yaml:17-20). Retention of old rows is part of RISK-35 (P1-E09-T09).

- STOP (human approval in the pull request): auth-access, db-migration, crypto-logging, production-config
- Covers: RISK-05, UX-13, UD-16.UX-13, PIN-20-0236

Plan item `P1-E01-T05` (user-story,P1,stop-auth-access,stop-db-migration,stop-crypto-logging,stop-production-config) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given 5 logins for planner@example.com from the client 203.0.113.7 answered 401 BAD_CREDENTIALS within 300 seconds When that client calls login for that address a 6th time inside the window, with the right password Then the answer is 429 RATE_LIMITED "Too many failed sign-ins — wait a few minutes." without a Retry-After header, no session is opened and no rm_session cookie is set (deviation UD-16.UX-13; rekord-api answers 200).
- [ ] #2 Given the same 5 failures When the client 198.51.100.4 calls login for planner@example.com with the right password, and 203.0.113.7 calls login for dj@example.com with its right password Then both answers are 200, because the count is per address and per client.
- [ ] #3 Given the same 5 failures, the oldest at second 0 When the client 203.0.113.7 calls login for planner@example.com with the right password at second 301 Then the answer is 200.
- [ ] #4 Given 4 failures for planner@example.com from 203.0.113.7 followed by a successful sign-in When the same client sends one more wrong password inside the window and then the right one Then the wrong one answers 401 BAD_CREDENTIALS and the right one answers 429 RATE_LIMITED, because a success does not clear the failures.
- [ ] #5 Given a login answered 401 BAD_CREDENTIALS When the auth_attempts table is read Then it holds one new row with kind USER_LOGIN, the SHA-256 hash of planner@example.com as identifier_hash, the client address as ip and success false, and no column holds the address or the password in clear.
- [ ] #6 Given logins answered 200, 403 ACCOUNT_DISABLED, 403 FORBIDDEN, 422 VALIDATION_FAILED and 429 RATE_LIMITED When the auth_attempts table is read afterwards Then none of them added a row.
- [ ] #7 Given wedding-portal restarted after 5 failures for an address from one client within 300 seconds When that client calls login for that address inside the window Then the answer is 429 RATE_LIMITED.
- [ ] #8 Given 5 failed sign-ins for planner@example.com within 300 seconds from a peer address that is not in quarkus.http.proxy.trusted-proxies When that peer calls login for that address with the right password, once with X-Forwarded-For 198.51.100.4 and once with X-Forwarded-For 198.51.100.5 Then both answers are 429 RATE_LIMITED, because X-Forwarded-For from an untrusted peer is ignored.
- [ ] #9 Given 5 failed sign-ins for planner@example.com within 300 seconds that the trusted proxy forwarded with X-Forwarded-For 203.0.113.7 When the trusted proxy forwards a login for that address with the right password and X-Forwarded-For 198.51.100.4 Then the answer is 200.
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
