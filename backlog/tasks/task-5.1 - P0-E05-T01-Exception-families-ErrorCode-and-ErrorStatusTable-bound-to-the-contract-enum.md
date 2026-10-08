---
id: TASK-5.1
title: >-
  P0-E05-T01 Exception families, ErrorCode and ErrorStatusTable bound to the
  contract enum
status: Done
assignee: []
created_date: '2026-10-07 05:45'
updated_date: '2026-10-07 23:03'
labels:
  - technical
  - P0
milestone: m-0
dependencies:
  - TASK-2.3
references:
  - 'docs/rewrite/architecture-conventions.md:745-798'
  - 'rekord-contract/dist/openapi.yaml:2039-2075'
  - 'rekord-api/src/main/java/app/rekord/portal/PortalGate.java:83-89'
  - 'rekord-api/src/main/java/app/rekord/portal/PortalGate.java:151-154'
  - 'rekord-api/src/main/java/app/rekord/scanner/ScanService.java:166-176'
  - 'rekord-api/src/main/java/app/rekord/library/LibraryRepository.java:218'
  - 'rekord-api/src/main/java/app/rekord/spotify/SpotifyPlaylistFetch.java:60-72'
documentation:
  - docs/rewrite/architecture-conventions.md
parent_task_id: TASK-5
priority: high
type: task
ordinal: 501
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a developer of wedding-portal, I want one sealed exception family and one code table checked against the contract so that no refusal carries a status or code the apps do not know.

architecture-conventions §11.1: RekordException is sealed with the families NotFoundException, RejectedException, NotPermittedException and UpstreamUnavailableException; ErrorCode lives in app.rekord.domain.shared.error and ErrorStatusTable in app.rekord.application.error. Five codes are shared with the Python stack at different statuses; rekord-api's statuses are the target (UX-09, PIN-20-0197). Seven Python codes are outside the contract enum and are dropped (UX-10). PIN-20-0205 asks what a client receives for a code missing from the enum; binding the domain enum to the contract rules that case out. The one answer whose status does not come from this table is the race answer of P0-E05-T05, which is always 409 (UD-19.i5).

- Covers: PIN-20-0197, PIN-20-0205, UX-09, UX-10

Plan item `P0-E05-T01` (technical,P0) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Given RekordException and its four families When ErrorStatusTableTest runs Then every pair of ErrorCode and family in ErrorStatusTable has exactly one HTTP status, NO_LIBRARY answers 404 as NotFound and 409 as Rejected, and SPOTIFY_FETCH_FAILED answers 404 as NotFound and 502 as UpstreamUnavailable.
- [x] #2 Given ErrorCode in app.rekord.domain.shared.error When ErrorCodeContractTest compares it with the ErrorCode enum of the pinned dist/openapi.yaml Then every domain code exists in the contract enum, and a code missing from the enum fails the test, so no such code reaches a client.
- [x] #3 Given the five codes shared with the Python stack When the table is read Then BAD_LINK is 401 (PortalGate.java:151-154), LINK_REVOKED 410 (PortalGate.java:86), FOLDER_NOT_FOUND 400 (ScanService.java:166-176), NO_LIBRARY_SELECTED 400 (LibraryRepository.java:218), and SPOTIFY_FETCH_FAILED 404 (SpotifyPlaylistFetch.java:60) or 502 (SpotifyPlaylistFetch.java:72).
- [x] #4 Given the Python-only codes BAD_DATE, BAD_KIND, BAD_POSITION, BAD_START_PREF, EMPTY_NAMES, NOTHING_MATCHED and NOTHING_MISSING When ErrorCode is read Then none of them is in it.
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [x] #1 Every acceptance criterion of the ticket is checked by at least one automated test in wedding-portal that fails when the criterion is broken.
- [x] #2 The CI run of the ticket's pull request is green, read by commit SHA; no test, gate or check is skipped, disabled or quarantined to get there.
- [x] #3 The ArchUnit rules pass with no new entry in the frozen baseline.
- [x] #4 Every contract change the ticket needs is merged and tagged in rekord-contract before the code that uses it; wedding-portal pins that tag; no generated file is edited by hand.
- [x] #5 Every STOP item the ticket touches (contract push, database migration, authentication or the access matrix, encryption or logging of personal data, deleting history, production configuration or secrets) has a human approval recorded in the pull request before the step is taken.
- [x] #6 Every refusal is answered with the error envelope {"detail":{"code","message"}} and a code from the contract's ErrorCode enum; validation follows the Notification pattern (UD-12), and a 422 lists its violations in the `errors` list (UD-19.d).
- [x] #7 Every operation the ticket builds answers with the status, error code and body shape rekord-api gives for the same request, except for the deviations recorded as user decisions (UD-9 to UD-20) in docs/rewrite/STATUS.md; each deviation the ticket implements is named in the pull request.
- [x] #8 No secret, token, password, access code or personal data appears in logs, test data, commits or documents.
- [x] #9 Every schema change is a new Flyway migration (an applied migration is never edited) and is exercised by a Testcontainers test.
- [x] #10 Every document or code-map leaf that the change makes outdated is updated in the same pull request.
- [x] #11 The pull request is reviewed and merged into main.
<!-- DOD:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Finalization evidence (2026-10-08)

Pull request https://github.com/Vikteur/wedding-portal/pull/21 was merged into main as f45d1d4670785a4d3a6de266eb4ab2902453661d. The head is 3b5f09ebb562969cead120fbde20c5c87291feed. CI run 37699063055 (workflow CI, pull_request) on that head SHA concluded success. `./gradlew test integrationTest build -Pcontract.spec=contract/dist/openapi.yaml` ended in BUILD SUCCESSFUL. `:rekord-domain:test`, `:application:test`, `integrationTest`, `resourceTest`, `startupTest` and `check` all executed. The steps "Read the pinned contract ref" and "Contract pin is a tag (required to merge)" ran with kind `tag`.

CI uploads test reports only on failure, so I re-ran the merge commit f45d1d4 locally against the pinned spec to get per-test counts. Every suite below passed, with 0 skipped, 0 failures and 0 errors:
- ErrorCodeTest: 9 tests
- RekordExceptionTest: 13 tests
- ErrorCodeContractTest: 3 tests
- ErrorStatusTableTest: 111 tests
- All 13 suites in app.rekord.architecture, including FrozenBaselineTest (5 tests). The archunit store left no working-tree changes after the run.

- AC #1: ErrorStatusTableTest covers it. `every_pair_of_code_and_family_has_exactly_one_status` and `building_the_table_refuses_a_second_row_for_the_same_pair` check one status per pair. `no_library_is_404_as_not_found_and_409_as_rejected` and `spotify_fetch_failed_is_404_as_not_found_and_502_as_upstream_unavailable` check the two named codes. `the_table_holds_exactly_those_rows` checks the 45 rows.
- AC #2: ErrorCodeContractTest. `every_domain_code_exists_in_the_contract_enum` reads `components.schemas.ErrorCode.enum` from `contract.spec`. `a_code_missing_from_the_contract_enum_fails_the_check` removes BAD_LINK from the set and expects an AssertionError that names BAD_LINK.
- AC #3: ErrorStatusTableTest `the_five_codes_shared_with_python_take_rekord_apis_status` checks BAD_LINK 401, LINK_REVOKED 410, FOLDER_NOT_FOUND 400, NO_LIBRARY_SELECTED 400, and SPOTIFY_FETCH_FAILED 404 or 502. Each check cites its rekord-api line.
- AC #4: ErrorCodeTest `the_python_only_codes_are_not_error_codes` is parameterised over the seven codes. `the_codes_are_the_43_that_rekord_api_throws` pins the exact set. ErrorCodeContractTest `the_python_only_codes_are_not_in_the_contract_either` also checks the contract.
- DoD #1: each AC above maps to tests in wedding-portal that fail when the criterion is broken.
- DoD #2: CI run 37699063055 on 3b5f09e is green. The PR diff adds no @Disabled or assumption and changes no file under .github.
- DoD #3: the merge diff changes no file under application/src/test/archunit_store. archunit.properties sets allowStoreCreation=false and allowStoreUpdate=false. The architecture suites pass on f45d1d4.
- DoD #4: the ticket needs no contract change. The PR changes neither gradle.properties nor the contract, and it edits no generated file. CI checked that the contract pin is a tag.
- DoD #5: the PR's Approval section states that the ticket touches no STOP item. The user's merge of PR #21 as f45d1d4 records the human review (UD-21.e).
- DoD #6 N/A: the ticket builds no HTTP answer. The envelope {"detail":{"code","message"}} is TASK-5.2, and the errors list is TASK-5.3/5.4/5.6. Every domain code is bound to the contract enum by ErrorCodeContractTest.
- DoD #7 N/A: the ticket builds no operation, so there is no request to compare. The table takes rekord-api's statuses (UX-09). The dropped Python-only codes (UX-10) are not codes rekord-api has, so no UD deviation is implemented.
- DoD #8: the diff holds no secret, token, password, access code or personal data. Test data is codes and the message "x".
- DoD #9 N/A: no schema change. The PR adds no Flyway migration.
- DoD #10: docs/memory.md has a new section, "TASK-5.1 error families and code table". No code-map leaf in docs/code-maps covers error handling, so none was outdated.
- DoD #11: the user reviewed PR #21 and merged it into main as f45d1d4 (UD-21.e).

Follow-ups, named here only and neither created nor started: TASK-5.2 (envelope mapper, including the UNKNOWN catch-all), TASK-5.5 (409 race answer). Both already exist in the backlog.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added the sealed RekordException over NotFound, Rejected, NotPermitted and UpstreamUnavailable, the 43-code domain ErrorCode (no Python-only codes, no UNKNOWN), and ErrorStatusTable with 45 rows: one rekord-api status per code and family. ErrorCodeContractTest binds ErrorCode to the pinned contract enum. Evidence: PR https://github.com/Vikteur/wedding-portal/pull/21 merged as f45d1d4670785a4d3a6de266eb4ab2902453661d. CI run 37699063055 on head 3b5f09ebb562969cead120fbde20c5c87291feed is green. A local re-run of f45d1d4 passed ErrorCodeTest, RekordExceptionTest, ErrorCodeContractTest, ErrorStatusTableTest and the ArchUnit suites with 0 skipped. No contract change, no new ArchUnit baseline entry, no STOP item. The merge records the human review (UD-21.e).
<!-- SECTION:FINAL_SUMMARY:END -->
