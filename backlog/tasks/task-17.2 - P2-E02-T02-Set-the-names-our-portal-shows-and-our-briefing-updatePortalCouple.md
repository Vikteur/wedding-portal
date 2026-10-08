---
id: TASK-17.2
title: >-
  P2-E02-T02 Set the names our portal shows and our briefing
  (updatePortalCouple)
status: To Do
assignee: []
created_date: '2026-10-07 07:14'
labels:
  - user-story
  - P2
  - test-first
  - stop-contract-push
  - stop-auth-access
  - stop-db-migration
milestone: m-2
dependencies:
  - TASK-17.1
references:
  - 'rekord-api/src/main/java/app/rekord/portal/PortalResource.java:74-92'
  - 'rekord-contract/components/music.yaml:513-530'
  - 'docs/rewrite/analysis/12-couple-portal.md:110'
  - 'docs/rewrite/analysis/12-couple-portal.md:463'
  - 'docs/rewrite/analysis/15-data-model-persistence.md:85'
  - 'rekord-couple/src/screens.tsx:43'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-17
priority: high
type: feature
ordinal: 20202
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a partner of the couple, I want to set the names our portal shows and the briefing for our DJ so that our portal greets us the way we write our names and our DJ reads our briefing.

The couple's details edit. rekord-api has no test for it, so it is built test-first (UD-15.d). Oracle: PortalResource.java:74-92 sets non-blank names untrimmed and ignores blank ones, sets the date and the briefing when present, writes a details change row and sets no stale flag; couple_display_name is never recomputed from the partner people (BR-DM-24). UD-18.b (RISK-30, slice 12 R8) deviates from it: the couple cannot change the wedding date, and the names the couple enters never replace couple_display_name, so the planner and the DJ always see the names the planner entered; the access code never changes from this call. The couple's names are kept in the nullable text column weddings.portal_names, null until the couple sends non-blank names; adding it needs a Flyway migration of wedding-portal. Blank names are ignored, so nothing sets portal_names back to null. UD-19.a: every portal view of the wedding, for the COUPLE and the FRIENDS scope alike, shows portal_names when it is set and couple_display_name otherwise, in openPortalSession, getPortalIdentity and getPortalState. UD-19.l4: once portal_names is set, a later change of couple_display_name by the planner through updateWedding leaves it unchanged; while it is null, the portal shows the planner's latest couple_display_name. The DJ's reads take couple_display_name and never portal_names; P3-E01-T02 proves it for getDjWedding and listDjWeddings (UD-18.b). UD-20.c replaces the 422 that UD-18.b first set for a date sent by the couple: updatePortalCouple ignores a wedding_date in the body without refusing it, as UD-18.d does for start_pref, saves the rest of the body, and the date stays the planner's. wedding_date leaves the CoupleUpdate schema of rekord-contract (rekord-contract/components/music.yaml:513-524), a contract change and so a STOP item (contract push), and wedding-portal ignores the unknown wedding_date property the unchanged couple app still sends from its date field (rekord-couple/src/screens.tsx:43), whatever its value. A call whose only property is wedding_date changes nothing but still answers 200 and adds the one details change row every updatePortalCouple call adds (P2-E05-T02). The couple app keeps re-sending the date with every later details save, because its pending details are merged and never reset (rekord-couple/src/store.tsx:315-318); those saves now succeed, so its retrying saver (rekord-couple/src/saver.ts:94-107, RISK-38, deferred with the frontend work) is not blocked by them.

- Builds: `updatePortalCouple`
- Test first: yes — pin the rekord-api behaviour with a characterization test before the build (UD-15.d)
- STOP (human approval in the pull request): contract-push, auth-access, db-migration
- Covers: BR-CP-37, RISK-30, BR-DM-24, PIN-12-0463, UD-18.b, UD-19.a, UD-19.l4, UD-20.c

Plan item `P2-E02-T02` (user-story,P2,test-first,stop-contract-push,stop-auth-access,stop-db-migration) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a COUPLE session of a CONFIRMED wedding of the partners Emma and Julian (display name "Emma & Julian") dated 2027-06-12 in time zone Europe/Amsterdam, with live COUPLE and FRIENDS portals and the access code EJ12062027 When the partner calls updatePortalCouple on rekord-api with names "Emma and Julian" and briefing_text "First dance at 21:00" as a characterization test Then the answer is 200 with PortalState names "Emma and Julian" and that briefing, and rekord-api's getWedding shows couple_display_name "Emma and Julian" (BR-CP-37).
- [ ] #2 Given the same fixture on wedding-portal When the partner makes the same call Then the answer is 200 with PortalState names "Emma and Julian" and that briefing, and getWedding still shows couple_display_name "Emma & Julian" and wedding_date 2027-06-12 (deviation UD-18.b; rekord-api overwrites couple_display_name).
- [ ] #3 Given the same session with briefing_text "Old" When the partner calls updatePortalCouple with names "   ", next with names " Emma & Jules ", and next with briefing_text "" Then the blank names leave the names in getPortalState unchanged, getPortalState shows " Emma & Jules " untrimmed, the calls without briefing_text keep "Old", and "" stores an empty briefing.
- [ ] #4 Given the same session When the partner changes the names through updatePortalCouple Then getWeddingPortal shows code_stale false and the access code EJ12062027, which still opens the portal (UD-18.b).
- [ ] #5 Given the same session and getWeddingPortal showing expires_at 00:00 on 2027-06-20 When the partner calls updatePortalCouple with wedding_date 2027-08-21 alone, then with wedding_date 2027-08-21 and names "X & Y", then with wedding_date "2027-13-40" and briefing_text "New" Then all 3 answers are 200 with wedding_date 2027-06-12, the second with names "X & Y" and the third with briefing_text "New", getWedding still shows wedding_date 2027-06-12, getWeddingPortal still shows that expires_at, and each call adds one song_changes row "Updated their details" (deviation UD-20.c; rekord-api stores a valid date).
- [ ] #6 Given the same wedding When the planner changes a PARTNER's first name through updatePerson Then couple_display_name stays "Emma & Julian" in getPortalState and getWedding.
- [ ] #7 Given a FRIENDS session of the same wedding When the friend calls updatePortalCouple with names "X & Y" Then the answer is 403 FORBIDDEN "That part belongs to the couple." and nothing changes.
- [ ] #8 Given the same wedding When updatePortalCouple is called without a cookie, with another wedding's token and this wedding's cookie, and with a 10-character token Then the answers are 401 NOT_SIGNED_IN, 401 BAD_LINK and 422 VALIDATION_FAILED, and nothing changes.
- [ ] #9 Given rekord-contract with wedding_date in CoupleUpdate (rekord-contract/components/music.yaml:513-524) When the contract pull request of this ticket is merged after its contract-push approval Then CoupleUpdate holds names and briefing_text only, the pull request carries the label semver:major because it removes a property and raises info.version by the major part (UD-19.e, UD-20.b), and the CoupleUpdate class wedding-portal generates from it has no wedding_date field (deviation UD-20.c).
- [ ] #10 Given a COUPLE session and a FRIENDS session of the same wedding after the partner set names "Emma and Julian" through updatePortalCouple When both sessions call getPortalIdentity and getPortalState, and a visitor calls openPortalSession with EJ12062027 once with the COUPLE token and once with the FRIENDS token Then all 6 answers carry names "Emma and Julian", while getWedding and listWeddings show couple_display_name "Emma & Julian" and no string value of either answer contains "Emma and Julian" (deviation UD-18.b and UD-19.a; rekord-api overwrites couple_display_name, so the planner sees "Emma and Julian" too).
- [ ] #11 Given the same wedding after the partner set names "Emma and Julian" through updatePortalCouple When the planner changes couple_display_name to "Emma & Jules" through updateWedding Then getWedding shows couple_display_name "Emma & Jules", and getPortalState and getPortalIdentity of a COUPLE session and of a FRIENDS session still carry names "Emma and Julian" (deviation UD-19.l4; rekord-api keeps one name field, so the planner's change replaces the couple's names in the portal).
- [ ] #12 Given the same wedding whose couple has sent no non-blank names through updatePortalCouple When the planner changes couple_display_name to "Emma & Jules" through updateWedding Then getPortalState and getPortalIdentity of a COUPLE session and of a FRIENDS session carry names "Emma & Jules" (UD-19.a).
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
- [ ] #12 No response of an account route carries portal_names; the DJ side of UD-18.b is proven by P3-E01-T02's criterion that getDjWedding and listDjWeddings carry the planner-entered couple_display_name after the couple set portal names.
<!-- DOD:END -->
