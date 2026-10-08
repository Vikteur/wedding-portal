---
id: TASK-20.1
title: P2-E05-T01 Portal access matrix over every portal operation
status: To Do
assignee: []
created_date: '2026-10-07 07:15'
labels:
  - user-story
  - P2
  - stop-auth-access
milestone: m-2
dependencies:
  - TASK-16.2
  - TASK-17.2
  - TASK-17.4
  - TASK-18.2
  - TASK-18.3
  - TASK-19.2
  - TASK-19.3
references:
  - 'rekord-api/src/main/java/app/rekord/portal/PortalGate.java:120-153'
  - 'rekord-api/src/main/java/app/rekord/portal/PortalResource.java:252-270'
  - >-
    rekord-api/src/main/java/app/rekord/security/SessionCookieMechanism.java:65-72
  - 'rekord-api/src/main/java/app/rekord/error/ErrorMappers.java:59-65'
  - 'docs/rewrite/analysis/12-couple-portal.md:74'
  - 'docs/rewrite/analysis/12-couple-portal.md:86-87'
  - 'docs/rewrite/analysis/12-couple-portal.md:109'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-20
priority: high
type: feature
ordinal: 20501
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a partner of the couple, I want every portal route to answer only to our own session so that nobody else reads or changes our wishes.

The access rules every portal operation shares, an access-matrix STOP item: a portal call needs both the path token and a live rm_portal session bound to that same portal (BR-CP-01); friends read only names, date, caps, search_available and the friends' list and write only friends_top20 (BR-CP-13); seven operations are couple only (BR-CP-14), and of those deletePortalEntry, reorderPortalList and deletePortalBlocklistEntry refuse a FRIENDS session with the add-only text of UD-18.a and UD-19.l1, 403 FORBIDDEN "Friends can add songs but cannot change or remove them.", while the other four keep "That part belongs to the couple.". The route decides which cookie counts (UD-19.l3, a STOP item on authentication, settling slice 12 R14, PIN-10-0610 and RISK-19): on /api/portal/* and /api/guest/* only the rm_portal cookie counts, and on every account route only the rm_session cookie counts, so a planner or DJ who opens the couple's link in the browser they work in reaches the portal, and a portal cookie never authenticates an account route. This deliberately deviates from rekord-api, where rm_session wins on every route whenever it is sent, so every portal route answers 401 NOT_SIGNED_IN to such a browser (BR-CP-36, BR-ID-12, SessionCookieMechanism.java:65-72). The per-operation tickets state each answer; this ticket proves the matrix over all of them at once. This ticket settles the cookie rule for every route of wedding-portal, account routes included: a request that carries only rm_portal is not signed in on an account route and answers 401 NOT_SIGNED_IN "Sign in to continue.", not rekord-api's 403 FORBIDDEN "This is not yours to open."; this governs getMe (P1-E01-T08) and the DJ account routes (P3-E01-T01, P3-E01-T02, P3-E09-T01).

- STOP (human approval in the pull request): auth-access
- Covers: BR-CP-01, BR-CP-13, BR-CP-14, BR-CP-36, BR-ID-12, PIN-10-0282, RISK-19, PIN-10-0610, UD-19.l3

Plan item `P2-E05-T01` (user-story,P2,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a wedding with live COUPLE and FRIENDS portals When each of getPortalIdentity, getPortalState, updatePortalCouple, putPortalEntry, deletePortalEntry, reorderPortalList, putPortalBlocklistEntry, deletePortalBlocklistEntry, searchSongs, getPortalTimeline and getPortalTasks is called with no cookie, and once more with only a planner's live rm_session cookie Then each answers 401 NOT_SIGNED_IN "Sign in to continue." (deviation UD-19.l3 for the rm_session calls; rekord-api answers them 401 NOT_SIGNED_IN "Open the full link your DJ sent you, then enter your code.").
- [ ] #2 Given a COUPLE session of wedding A and the COUPLE token of wedding B in the same business When each of getPortalState, updatePortalCouple, putPortalEntry, deletePortalEntry, reorderPortalList, putPortalBlocklistEntry, deletePortalBlocklistEntry, searchSongs, getPortalTimeline and getPortalTasks is called with wedding B's token in the path Then each answers 401 BAD_LINK "That link and code do not match. Check the code your DJ gave you." and nothing of either wedding changes.
- [ ] #3 Given a COUPLE session of wedding A When each of getPortalState, updatePortalCouple, putPortalEntry, deletePortalEntry, reorderPortalList, putPortalBlocklistEntry, deletePortalBlocklistEntry, searchSongs, getPortalTimeline and getPortalTasks is called with a 19-character path token Then each answers 422 VALIDATION_FAILED.
- [ ] #4 Given a FRIENDS session of wedding A When each of updatePortalCouple, deletePortalEntry, reorderPortalList, putPortalBlocklistEntry, deletePortalBlocklistEntry, getPortalTimeline and getPortalTasks is called Then updatePortalCouple, putPortalBlocklistEntry, getPortalTimeline and getPortalTasks answer 403 FORBIDDEN "That part belongs to the couple.", deletePortalEntry, reorderPortalList and deletePortalBlocklistEntry answer 403 FORBIDDEN "Friends can add songs but cannot change or remove them.", and nothing changes (deviation UD-18.a/UD-19.l1 for the three; rekord-api answers them 403 FORBIDDEN "That part belongs to the couple.").
- [ ] #5 Given a FRIENDS session of wedding A When it calls getPortalState, putPortalEntry for a new uid on friends_top20 and searchSongs Then each answers 200, getPortalState carries no briefing_text, blocklist or friends_link, and entries holds only friends_top20.
- [ ] #6 Given a browser holding a planner's live rm_session and a COUPLE rm_portal of wedding A When it calls each of getPortalIdentity, getPortalState, updatePortalCouple, putPortalEntry, deletePortalEntry, reorderPortalList, putPortalBlocklistEntry, deletePortalBlocklistEntry, searchSongs, getPortalTimeline and getPortalTasks Then each answer is the one the same request gets with the COUPLE rm_portal cookie alone, getPortalIdentity answers 200 with scope COUPLE, and no answer sets or clears rm_session (deviation UD-19.l3, RISK-19, PIN-10-0610; rekord-api answers each 401 NOT_SIGNED_IN "Open the full link your DJ sent you, then enter your code.", BR-CP-36).
- [ ] #7 Given a COUPLE session and a FRIENDS session of wedding A When the planner revokes both portals, and in a second fixture soft-deletes wedding A, and each session calls each of getPortalState, updatePortalCouple, putPortalEntry, deletePortalEntry, reorderPortalList, putPortalBlocklistEntry, deletePortalBlocklistEntry, searchSongs, getPortalTimeline and getPortalTasks Then every answer is 401 BAD_LINK (UD-10.a).
- [ ] #8 Given a browser holding an rm_session whose session row has revoked_at set and a live COUPLE rm_portal of wedding A When it calls getPortalState with wedding A's COUPLE token Then the answer is 200 with scope COUPLE (deviation UD-19.l3; rekord-api answers 401 NOT_SIGNED_IN "Sign in to continue." because rm_session wins, BR-ID-12).
- [ ] #9 Given a browser holding only a live COUPLE rm_portal of wedding A, a second browser holding a planner's live rm_session and that rm_portal, and a third holding an rm_session whose session row has revoked_at set and that rm_portal When each calls getMe Then the first answers 401 NOT_SIGNED_IN "Sign in to continue.", the second answers 200 with the planner's account, and the third answers 401 NOT_SIGNED_IN "Sign in to continue.", because on account routes only rm_session counts (deviation UD-19.l3 for the first; rekord-api answers it 403 FORBIDDEN, because the portal session authenticates the request).
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
