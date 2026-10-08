---
id: TASK-7.6
title: >-
  P1-E01-T06 Account sessions and the rm_session cookie with the session limits
  of rekord-api
status: To Do
assignee: []
created_date: '2026-10-07 07:10'
labels:
  - technical
  - P1
  - stop-auth-access
  - stop-crypto-logging
milestone: m-1
dependencies:
  - TASK-7.4
references:
  - 'rekord-api/src/main/java/app/rekord/security/SessionService.java:25-165'
  - 'rekord-api/src/main/java/app/rekord/security/Tokens.java:20-51'
  - >-
    rekord-api/src/main/java/app/rekord/security/SessionCookieMechanism.java:60-160
  - 'rekord-contract/paths/auth.yaml:28-40'
  - 'docs/rewrite/analysis/10-identity-access.md:85-95'
  - 'docs/rewrite/analysis/10-identity-access.md:279-284'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-7
priority: high
type: task
ordinal: 10106
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a developer of wedding-portal, I want account sessions stored and resolved with the limits and cookie attributes of rekord-api so that a signed-in member stays signed in exactly as long as the oracle allows.

The session store and the cookie mechanism, an authentication STOP item. Oracle: SessionService (SessionService.java:31-165), Tokens (Tokens.java:20-51) and SessionCookieMechanism (SessionCookieMechanism.java:60-160). getMe (P1-E01-T08) and the wedding operations come later, so this ticket adds a test endpoint guarded by the session filter that answers 200 with the session's account id and roles, and its criteria call that endpoint. Kept: 32 random bytes in base64url as the token, only its SHA-256 hash stored; idle 7 days sliding, written at most once an hour; absolute 30 days; roles snapshotted into the session at sign-in; session resolution does not re-check the account status. Which cookie wins when a request carries both rm_session and rm_portal is decided with the portal sessions in the portal access matrix of P2-E05-T01, where only rm_session counts on account routes (BR-ID-12, RISK-19, UD-19.l3). Portal sessions use the same store with 14 days idle and 30 days absolute; they are opened by P2-E01. CSRF defence is SameSite=Lax with no Origin check (RISK-12, kept by the user's decision UD-19.h); the test that no GET operation changes data is P1-E09-T03.

- STOP (human approval in the pull request): auth-access, crypto-logging
- Covers: BR-ID-09, BR-ID-10, BR-ID-11, BR-ID-13, BR-ID-14, PIN-10-0280, PIN-10-0092, PIN-10-0459, RISK-12, BR-ID-05, UD-19.h

Plan item `P1-E01-T06` (technical,P1,stop-auth-access,stop-crypto-logging) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a successful login When the answer is read Then it carries Set-Cookie rm_session with a 43-character base64url value, HttpOnly, SameSite=Lax, Path=/ and Max-Age=2592000, and the sessions row holds the SHA-256 hash of that value and no column holds the value itself (BR-ID-09, BR-ID-11).
- [ ] #2 Given a login request with the header X-Forwarded-Proto: https and one without it When each answer is read Then the first rm_session cookie carries Secure and the second does not (PIN-10-0092).
- [ ] #3 Given a session last used 6 days and 23 hours ago, and one last used 7 days and 1 minute ago When each calls the test endpoint Then the first answers 200 and moves its idle end to 7 days after this call, and the second answers 401 NOT_SIGNED_IN "Sign in to continue." (BR-ID-10, PIN-10-0280).
- [ ] #4 Given a session used every day since it was opened When it calls the test endpoint 30 days and 1 minute after the sign-in Then the answer is 401 NOT_SIGNED_IN (BR-ID-10).
- [ ] #5 Given a session whose last_seen_at is 50 minutes old, and one whose last_seen_at is 61 minutes old When each makes a request Then the first leaves last_seen_at and idle_expires_at unchanged and the second sets both (BR-ID-10).
- [ ] #6 Given a session opened while the account held PLANNER When the account's PLANNER membership row is deleted directly in the database and the account's status is set to DISABLED directly in the database Then the session still reaches the test endpoint with 200 and the roles [PLANNER], because resolution reads only the session row and its stored roles; the operations of P1-E02 revoke the sessions instead (BR-ID-13, BR-ID-14).
- [ ] #7 Given any Set-Cookie of rm_session or a cleared rm_session, and a request to the test endpoint with a live rm_session and the header Origin: https://other.example.com When the attributes and the answer are read Then SameSite=Lax is set and the request answers 200, because wedding-portal has no Origin check (RISK-12, UD-19.h).
- [ ] #8 Given an account with ACTIVE memberships in one business When it signs in with login Then the answer carries Set-Cookie rm_session, and the session holds that business and every ACTIVE role of the account in it; there is no business switch (BR-ID-05).
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
