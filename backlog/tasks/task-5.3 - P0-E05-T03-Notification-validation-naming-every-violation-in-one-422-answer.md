---
id: TASK-5.3
title: P0-E05-T03 Notification validation naming every violation in one 422 answer
status: To Do
assignee: []
created_date: '2026-10-07 05:45'
labels:
  - user-story
  - P0
milestone: m-0
dependencies:
  - TASK-5.2
references:
  - .claude/skills/validation-notification-result/SKILL.md
  - 'rekord-api/src/main/java/app/rekord/error/ErrorMappers.java:89-103'
  - 'rekord-api/src/main/java/app/rekord/scanner/ScanService.java:166-176'
  - 'docs/rewrite/STATUS.md:270-278'
  - 'docs/rewrite/STATUS.md:394-405'
documentation:
  - docs/rewrite/architecture-conventions.md
parent_task_id: TASK-5
priority: high
type: feature
ordinal: 503
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a member, I want a request with two mistakes refused once with both mistakes named so that I fix everything in one go.

UD-12.a: validation follows the Notification pattern; every rule is evaluated, every violation collected, and the use case decides once (.claude/skills/validation-notification-result/SKILL.md). rekord-api's services throw at the first failing rule, for example ScanService.java:166-176, while its bean-validation mapper already joins every violation into one message (ErrorMappers.java:89-103). A rule declares the field it checks as the request property path in the contract's snake_case with list indexes, or null for a rule that checks no single property (UD-19.d); the top-level message keeps the rendering of rekord-api's bean-validation mapper, which writes the last segment of the Java property path (ErrorMappers.java:95-97), the lower camel case name of the generated request DTO property. The errors list enters the contract in P0-E05-T04 and is filled in P0-E05-T06; the use case runs only after the contract check passes, so a request that also breaks a contract constraint never reaches these rules (UD-19.d2).

- Covers: UD-12.a

Plan item `P0-E05-T03` (user-story,P0) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a test-only command that breaks two use-case rules When the use case validates it Then every rule is evaluated, both violations are collected in one Notification and the use case throws once after the last rule (deviation UD-12.a; rekord-api answers the first failing rule only, ScanService.java:166-176).
- [ ] #2 Given that Notification with two violations, "is reserved" declared on display_name and "is a duplicate" declared on lines[2].name When the error mapper answers Then the status is 422 with the code VALIDATION_FAILED and the message "displayName is reserved; name is a duplicate", each violation written as the last segment of its field path without list index in the lower camel case of the generated request DTO property, which is the segment bean validation reports (ErrorMappers.java:95-97), a space and its message, joined by "; " in rule order.
- [ ] #3 Given a Notification whose first violation "a wedding needs guests or a budget" comes from a rule that checks no single property, so its field is null, and whose second violation is "is reserved" on display_name When the error mapper answers Then the message is "a wedding needs guests or a budget; displayName is reserved", the field-null violation written as its message alone (UD-19.d).
- [ ] #4 Given a command that breaks no rule When the use case runs Then no violation is raised and its write is kept.
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
