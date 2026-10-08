---
id: TASK-2.2
title: P0-E02-T02 Contract tag on every main merge and a pinned tag in wedding-portal
status: Done
assignee:
  - '@claude'
created_date: '2026-10-07 05:45'
updated_date: '2026-10-07 13:16'
labels:
  - technical
  - P0
  - stop-contract-push
milestone: m-0
dependencies:
  - TASK-2.1
references:
  - 'rekord-contract/README.md:145-151'
  - 'rekord-contract/.github/workflows/contract.yml:1-52'
  - 'docs/rewrite/repo-and-contract-decision.md:866-943'
  - 'docs/rewrite/STATUS.md:321-329'
  - 'docs/rewrite/STATUS.md:406-408'
documentation:
  - docs/rewrite/architecture-conventions.md
parent_task_id: TASK-2
priority: high
type: task
ordinal: 202
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a maintainer of rekord-contract, I want every merge to main tagged and every consumer pinned to a tag so that a contract change reaches wedding-portal only through a version it names.

UD-15.b keeps one hub, rekord-contract, authored in files per audience: paths/auth.yaml, paths/dj.yaml, paths/planner.yaml and paths/portal.yaml with components per domain, bundled into dist/openapi.yaml. UD-15.c adds a tag on every merge to main, a pinned tag in each consumer and one feature-branch name across every involved repository, which pins the contract's feature branch until it merges. UD-19.e names the tags v<major>.<minor>.<patch> and lets a label on the contract pull request pick the part the hub workflow raises: semver:major for a breaking change, semver:minor for an additive change (the default when no label is set) and semver:patch for a documentation-only change. UD-20.b keeps info.version of openapi.yaml always equal to the tag: the author raises it in the pull request by the part the semver label names (minor when no label is set), CI checks it, and after the merge the hub workflow tags the merge commit v<info.version>; the workflow pushes only the tag, and nothing but the merge of a pull request pushes to main. dist/openapi.yaml is bundled from the sources, so its info.version follows (P0-E02-T04 fails a stale bundle); the version in package.json is not compared by the check, because UD-20.b names info.version only. UD-20.a: rekord-contract has no tag yet and its openapi.yaml, dist/openapi.yaml and package.json hold 0.1.0, so the first merge into main after this ticket's workflow is in place is tagged v0.1.0, and while no v tag exists the check expects info.version 0.1.0 whatever the label. The POC consumers read the default branch and pin nothing (RISK-57, 16 R-11; rekord-contract/README.md:145-151). Changing the hub's workflows is a STOP item (contract push).

- STOP (human approval in the pull request): contract-push
- Covers: UD-15.b, UD-15.c, UD-19.e, UD-20.a, UD-20.b

Plan item `P0-E02-T02` (technical,P0,stop-contract-push) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Given rekord-contract whose highest tag is v1.4.2 and, in three separate runs that each start from that state, a pull request labelled semver:major with info.version 2.0.0, one labelled semver:minor with info.version 1.5.0 and one labelled semver:patch with info.version 1.4.3 When each is merged into main and the hub's workflow runs on the merge commit Then the workflow tags that commit v2.0.0, v1.5.0 and v1.4.3 in turn, read from info.version, and no existing tag is moved or deleted (UD-19.e, UD-20.b).
- [x] #2 Given rekord-contract whose highest tag is v1.4.2 and a merged pull request that carries no semver label and info.version 1.5.0 When the hub's workflow runs on the merge commit Then it tags that commit v1.5.0 (UD-19.e, UD-20.b).
- [x] #3 Given rekord-contract with the tags v1.9.3 and v1.10.0 When the hub's workflow picks the highest tag Then it picks v1.10.0 by semantic-version order, never by text order.
- [x] #4 Given rekord-contract whose highest tag is v1.4.2 and a pull request labelled semver:minor When the contract CI runs with info.version 1.4.2, 1.6.0 or 2.0.0 in openapi.yaml, and again after the label is changed to semver:patch with info.version 1.5.0 Then each of those runs fails the version check naming the expected version (1.5.0, and 1.4.3 after the label change), and the check passes once info.version equals the expected version (UD-20.b).
- [x] #5 Given rekord-contract with no v tag and openapi.yaml, dist/openapi.yaml and package.json at version 0.1.0 When the first pull request after the tagging workflow of this ticket is in place is merged into main, whatever semver label it carries Then the version check passes with info.version 0.1.0, the hub's workflow tags the merge commit v0.1.0, and no other tag exists (UD-20.a).
- [x] #6 Given a merge into main of rekord-contract When the hub's workflow tags the merge commit Then the workflow pushes the tag only and no commit, and main holds no commit other than the merge commits of pull requests (UD-20.b).
- [x] #7 Given the sources of rekord-contract When an operation is changed Then the change is made in its audience file under paths/ (auth.yaml, dj.yaml, planner.yaml or portal.yaml) or its domain file under components/, and dist/openapi.yaml is the one bundle built from them.
- [x] #8 Given the build files of wedding-portal on main When the contract version is read Then they name one rekord-contract tag, never a branch, and CI on main checks out rekord-contract at that tag.
- [x] #9 Given a wedding-portal feature branch whose build file pins the rekord-contract branch of the same name When CI runs Then the contract checkout uses that pinned branch.
- [x] #10 Given a feature merged into main of rekord-contract and tagged When wedding-portal merges the same feature into its main Then its pin names the new tag in that merge, and CI on main checks out that tag.
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

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Approved by the user 2026-10-07 (STOP contract-push). Branch feat/task-2.2-contract-tags in both repos.
1. rekord-contract: scripts/version.mjs (highest v tag by semver order; expected version from the semver label, minor by default, 0.1.0 while no v tag exists) with node:test tests (AC #3, #4, #5).
2. rekord-contract: version CI job on pull_request incl. labeled/unlabeled, failing with the expected version when info.version differs (AC #4).
3. rekord-contract: tag workflow on push to main: tag the merge commit v<info.version>, refuse an existing tag or a commit that is not a pull request merge, push the tag only; tested against a throwaway git repo with fixture tags (AC #1, #2, #5, #6).
4. rekord-contract: test that every operation in openapi.yaml is a $ref into paths/ and dist/openapi.yaml is the bundle (AC #7).
5. After PR 1 merges and v0.1.0 exists: wedding-portal pins rekordContractTag=v0.1.0 in gradle.properties; CI checks out that tag, a check refuses anything but an existing vX.Y.Z tag; JUnit test on the pin (AC #8).
Done with approval: semver:major/minor/patch labels created; rekord-contract allows merge commits only (squash and rebase off).
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Evidence (finalized 2026-10-07):
- rekord-contract PR #1 (head 4c6a082, merge 055b133, merge commits only). CI by SHA: contract run 37624493455 (pull_request, 4c6a082) green, npm test 22 pass. Push to main: contract run 37624785695 green; tag run 37624785770 green on 055b133.
- AC #1, #2, #6: test/tag-merge.test.mjs runs scripts/tag-merge.sh in a throwaway repo with fixture tags (v2.0.0/v1.5.0/v1.4.3 from v1.4.2, no-label 1.5.0, existing tag refused, only refs/tags/<tag> pushed, non-merge refused).
- AC #3, #4: test/version.test.mjs (v1.10.0 > v1.9.3; expected 1.5.0 for minor and 1.4.3 for patch; 1.4.2/1.6.0/2.0.0 fail naming the expected version). The version job in contract.yml runs on labeled/unlabeled.
- AC #5 live: the first merge after the workflow (055b133) was tagged v0.1.0 by run 37624785770; git ls-remote shows only v0.1.0.
- AC #7: test/sources.test.mjs (every operation is a $ref into paths/, dist/openapi.yaml is the bundle). /health moved to paths/auth.yaml.
- wedding-portal PR #8 (head dea8b9a, merge f723708, merged by the user). CI by SHA: run 37625969970 (PR head) green; run 37626532860 (main, f723708) green. Its log shows 'rekord-contract tag: v0.1.0' and 'OK: rekord-contract is at tag v0.1.0 (055b133)'.
- AC #8, #9, #10: ContractPinTest (15 tests): pin in gradle.properties; tag/branch pins per event; branch pin refused on main and when it names another branch; checkout ref and step order; the last step contract-pin-merge-check.sh fails while the pin is a branch, so a merge into main names a tag. CiWorkflowTest still passes.
- DoD #1: AC #1-#7 describe rekord-contract's own workflow and sources, so their tests live in rekord-contract (npm test, run in its CI). AC #8-#10 are tested in wedding-portal (ContractPinTest).
- DoD #3 N/A: no ArchUnit suite exists yet (TASK-3.1), and no Java production code changed.
- DoD #4: the contract change merged and tagged v0.1.0 (055b133) before PR #8 pinned it. No generated file was touched.
- DoD #5: the STOP contract push was approved by the user before the push, recorded on rekord-contract PR #1 (comment 'Approved' 2026-10-07T12:56:35Z, then the merge). PR #8 changes no contract.
- DoD #6 N/A: no operation or refusal is built.
- DoD #7 N/A: no operation is built; no rekord-api behaviour applies.
- DoD #8: CONTRACT_TOKEN is referenced by name only; no secret appears in code, logs or docs.
- DoD #9 N/A: no schema change or migration.
- DoD #10: rekord-contract README ('Changing it') and wedding-portal docs/memory.md were updated in their PRs.
- DoD #11: both PRs were merged into main by the user.
- Beyond the approved plan: the pin also allows a same-name contract branch on feature branches (AC #9), and a merge check makes a branch pin unmergeable green (AC #10).
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
rekord-contract (PR #1, merge 055b133, tag v0.1.0): scripts/version.mjs + a version CI job check info.version against the semver label (minor by default, 0.1.0 while no tag exists); tag.yml tags each PR merge commit v<info.version> and pushes only that tag; operations live in paths/. Tests: npm test (22), CI runs 37624493455 / 37624785695 / 37624785770 green; v0.1.0 created live on the first merge. wedding-portal (PR #8, merge f723708): gradle.properties pins rekordContractTag=v0.1.0; CI checks the hub out at that ref and proves HEAD is the pinned tag; a feature branch may pin its same-name contract branch, but its PR stays red until the pin names a tag; main always needs a tag. Tests: ContractPinTest (15); CI green by SHA on dea8b9a (run 37625969970) and on main f723708 (run 37626532860, contract at v0.1.0).
<!-- SECTION:FINAL_SUMMARY:END -->
