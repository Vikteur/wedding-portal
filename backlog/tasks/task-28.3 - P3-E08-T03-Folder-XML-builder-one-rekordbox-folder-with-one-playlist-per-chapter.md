---
id: TASK-28.3
title: >-
  P3-E08-T03 Folder XML builder: one rekordbox folder with one playlist per
  chapter
status: To Do
assignee: []
created_date: '2026-10-07 07:17'
labels:
  - user-story
  - P3
milestone: m-3
dependencies:
  - TASK-27.1
  - TASK-23
references:
  - 'rekord-backend/server/export/rekordbox_xml.py:102-156'
  - 'rekord-backend/server/export/couple.py:32-34'
  - 'rekord-backend/tests/test_couple_export.py:68-93'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-28
priority: high
type: feature
ordinal: 30803
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want the whole wedding in one rekordbox XML with a folder named after the couple and the date so that one import gives me every chapter in the order of the night.

A pure builder that writes the wedding's playlists into one rekordbox XML, built in Java from the POC (rekord-backend/server/export/rekordbox_xml.py:102-156). ROOT holds one folder node named after the couple and the date, holding one playlist node per chapter; a track used by several chapters is written to COLLECTION once and referenced from each (BR-MX-40). Each COLLECTION TRACK is written by the track rule of P3-E07-T01.

- Covers: BR-MX-40

Plan item `P3-E08-T03` (user-story,P3) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given the names "  Emma & Julian " with the date 2026-09-12, and the names "   " with the same date When the folder label is built Then the labels are "Emma & Julian 2026-09-12" and "Couple 2026-09-12": the names stripped, "Couple" when empty, a space and the ISO date (couple.py:32-34).
- [ ] #2 Given the folder "Emma & Julian 2026-09-12", the track one and the track two, and the playlists ("01 Opening dance", [one]) and ("02 Their top 20", [one, two]) When the builder runs Then the body starts with the XML declaration, DJ_PLAYLISTS and PRODUCT lines of P3-E07-T01, then COLLECTION Entries="2" holding TRACK TrackID="1" for one and TrackID="2" for two, each written by the track rule of P3-E07-T01 (test_couple_export.py:84-93).
- [ ] #3 Given the same input When the builder runs Then PLAYLISTS holds NODE Type="0" Name="ROOT" Count="1", which holds NODE Type="0" Name="Emma &amp; Julian 2026-09-12" Count="2", which holds NODE Name="01 Opening dance" Type="1" KeyType="0" Entries="1" with TRACK Key="1", then NODE Name="02 Their top 20" Type="1" KeyType="0" Entries="2" with TRACK Key="1" and TRACK Key="2" (BR-MX-40; test_couple_export.py:70-81).
- [ ] #4 Given the same input When the builder runs Then the lines are joined by LF and end with LF, with no byte-order mark, indented by 2 spaces for PRODUCT, COLLECTION and PLAYLISTS, 4 for a COLLECTION TRACK and the ROOT node, 6 for the folder node, 8 for a playlist node and 10 for a playlist's TRACK Key (rekordbox_xml.py:121-156).
- [ ] #5 Given a playlist that holds the same track twice When the builder runs Then COLLECTION holds the track once, the playlist node counts Entries 2 and holds its TRACK Key twice (rekordbox_xml.py:116-133).
- [ ] #6 Given the body built for the first input When the rekordbox collection import of P3-E03 reads it Then it returns exactly the two tracks with their paths, artists and titles.
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
