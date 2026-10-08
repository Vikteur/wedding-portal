---
id: TASK-9.7
title: >-
  P1-E03-T07 Accepting an invitation that names a vendor links the matching
  contact
status: To Do
assignee: []
created_date: '2026-10-07 07:11'
labels:
  - user-story
  - P1
milestone: m-1
dependencies:
  - TASK-8.8
  - TASK-9.5
references:
  - 'rekord-api/src/main/java/app/rekord/account/InviteService.java:136-148'
  - 'rekord-api/src/main/java/app/rekord/account/AccountsResource.java:203-217'
  - 'rekord-contract/components/identity.yaml:57-87'
  - 'docs/rewrite/analysis/11-planner-weddings.md:158'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-9
priority: high
type: feature
ordinal: 10307
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a planner, I want an invited DJ who is already in the directory to become the same person as the directory contact so that I do not wire the account to the vendor by hand.

Oracle: InviteService.accept (InviteService.java:136-148) and AccountsResource.account (AccountsResource.java:203-217): when the accepted invite names a vendor, every contact of that vendor whose user_id is null and whose e-mail equals the invited address case-insensitively gets the new account's id; listMembers then shows that vendor as the member's vendor_id, and getVendor shows the account on the contact (BR-PL-53). The check that the vendor belongs to the inviting business is P1-E09-T04.

- Covers: BR-PL-53

Plan item `P1-E03-T07` (user-story,P1) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a vendor "Nachtwerk" with a contact whose e-mail is New.DJ@example.com and no account, and an open invite for new.dj@example.com naming that vendor When the invitee accepts it Then the contact's user_id is the new account's id and getVendor shows it on the contact (BR-PL-53).
- [ ] #2 Given that accepted invite When a planner calls listMembers Then the new member's entry carries vendor_id of "Nachtwerk".
- [ ] #3 Given the same vendor with a second contact whose e-mail differs, and a third contact with the invited address already linked to another account When the invite is accepted Then neither of those contacts changes.
- [ ] #4 Given an open invite that names no vendor When it is accepted Then no contact changes and the member's vendor_id is null.
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
