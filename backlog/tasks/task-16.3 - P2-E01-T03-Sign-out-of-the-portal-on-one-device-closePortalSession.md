---
id: TASK-16.3
title: P2-E01-T03 Sign out of the portal on one device (closePortalSession)
status: To Do
assignee: []
created_date: '2026-10-07 07:14'
labels:
  - user-story
  - P2
  - test-first
  - stop-auth-access
milestone: m-2
dependencies:
  - TASK-16.2
references:
  - 'rekord-api/src/main/java/app/rekord/security/AuthResource.java:212-225'
  - 'rekord-api/src/main/java/app/rekord/security/SessionService.java:137-145'
  - 'rekord-contract/paths/auth.yaml:228-246'
  - 'docs/rewrite/analysis/12-couple-portal.md:106'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-16
priority: high
type: feature
ordinal: 20103
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a partner of the couple, I want to sign out of the portal on one device so that a shared computer no longer opens our lists.

Closing a portal session, an authentication STOP item. rekord-api has no test for it, so it is built test-first (UD-15.d): the first criterion pins rekord-api before the build. Oracle: AuthResource.java:212-225 revokes only the session of the rm_portal cookie when one is sent, always clears the cookie with Path /api, and reports whether a cookie was present. The contract declares the portalCookie scheme on this route, but rekord-api lets it through without one; it stays public, on the allow-list of P0-E06. The link, the code and the other devices keep working.

- Builds: `closePortalSession`
- Test first: yes — pin the rekord-api behaviour with a characterization test before the build (UD-15.d)
- STOP (human approval in the pull request): auth-access
- Covers: BR-CP-33
- Error code `UNKNOWN` without a criterion here: 500 UNKNOWN is the catch-all answer for an unexpected failure (P0-E05); no business rule of this operation raises it, and no criterion provokes it.

Plan item `P2-E01-T03` (user-story,P2,test-first,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given portal sessions A and B opened on two devices with the COUPLE token of a CONFIRMED wedding of the partners Emma and Julian (display name "Emma & Julian") dated 2027-06-12 in time zone Europe/Amsterdam, with live COUPLE and FRIENDS portals and the access code EJ12062027 When device A calls closePortalSession, first against rekord-api as a characterization test and then against wedding-portal Then the answer is 200 with signed_out true and a Set-Cookie that clears rm_portal with Path=/api, A's next getPortalIdentity answers 401 NOT_SIGNED_IN "Sign in to continue.", B's next getPortalIdentity answers 200, and a new openPortalSession with EJ12062027 answers 200.
- [ ] #2 Given a browser with no rm_portal cookie When it calls closePortalSession Then the answer is 200 with signed_out false and a Set-Cookie that clears rm_portal with Path=/api.
- [ ] #3 Given an rm_portal cookie whose session ended through its 14-day idle limit When the browser calls closePortalSession Then the answer is 200 with signed_out true and the cookie is cleared.
- [ ] #4 Given a browser holding a planner's rm_session and a couple's rm_portal When it calls closePortalSession Then the answer is 200 with signed_out true and a Set-Cookie that clears rm_portal with Path=/api and none for rm_session, a later getPortalIdentity sent with only the old rm_portal cookie answers 401 NOT_SIGNED_IN "Sign in to continue.", and getMe with the rm_session cookie answers 200, because on /api/portal/* only rm_portal counts and on account routes only rm_session counts (UD-19.l3).
- [ ] #5 Given an open portal session of a wedding that the planner set to CANCELLED, and one of a portal the planner revoked When each browser calls closePortalSession Then each answer is 200 with signed_out true and the cookie cleared.
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
