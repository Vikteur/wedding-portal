---
id: TASK-7.7
title: P1-E01-T07 Sign out of the current session (logout)
status: To Do
assignee: []
created_date: '2026-10-07 07:10'
labels:
  - user-story
  - P1
  - test-first
  - stop-auth-access
milestone: m-1
dependencies:
  - TASK-7.6
references:
  - 'rekord-api/src/main/java/app/rekord/security/AuthResource.java:111-122'
  - >-
    rekord-api/src/main/java/app/rekord/security/SessionCookieMechanism.java:150-160
  - 'docs/rewrite/analysis/10-identity-access.md:286'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-7
priority: high
type: feature
ordinal: 10107
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a member, I want to sign out on this device so that nobody using it after me acts in my name.

Builds the operation logout, which is public (in the allow-list of P0-E06-T01). Oracle: AuthResource.logout (AuthResource.java:111-122): it revokes only the session of the presented cookie, always clears the cookie, and reports whether a cookie was present. rekord-api has no test for logout, so the ticket is test-first (UD-15.d). The audit_log rows this operation writes are added by P1-E09-T10 (UD-19.k).

- Builds: `logout`
- Test first: yes — pin the rekord-api behaviour with a characterization test before the build (UD-15.d)
- STOP (human approval in the pull request): auth-access
- Covers: BR-ID-16
- Error code `UNKNOWN` without a criterion here: 500 UNKNOWN is the catch-all answer for an unexpected failure (P0-E05); no business rule of this operation raises it, and no criterion provokes it.

Plan item `P1-E01-T07` (user-story,P1,test-first,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given rekord-api as the behaviour oracle and no rekord-api test for logout When a characterization test calls logout against rekord-api with a live rm_session, with no cookie and with an unknown cookie value Then it records each status, error code and body, and the same test passes against wedding-portal before the operation counts as built (UD-15.d).
- [ ] #2 Given a member signed in on two devices When the member calls logout with the first device's rm_session Then the answer is 200 {"signed_out":true} with a Set-Cookie that clears rm_session (empty value, Max-Age=0, Path=/, HttpOnly, SameSite=Lax), getMe with the first cookie answers 401 NOT_SIGNED_IN, and getMe with the second device's cookie still answers 200 (BR-ID-16).
- [ ] #3 Given a request without any cookie When it calls logout Then the answer is 200 {"signed_out":false} with the clearing Set-Cookie and no session row changes.
- [ ] #4 Given a request whose rm_session value matches no session When it calls logout Then the answer is 200 {"signed_out":true} with the clearing Set-Cookie and no session row changes.
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
