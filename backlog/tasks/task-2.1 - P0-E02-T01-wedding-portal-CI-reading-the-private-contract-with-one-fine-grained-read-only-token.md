---
id: TASK-2.1
title: >-
  P0-E02-T01 wedding-portal CI reading the private contract with one
  fine-grained read-only token
status: Done
assignee:
  - '@archon'
created_date: '2026-10-07 05:45'
updated_date: '2026-10-07 10:40'
labels:
  - technical
  - P0
  - stop-production-config
milestone: m-0
dependencies:
  - TASK-1.1
references:
  - 'rekord-api/.github/workflows/deploy.yml:28-45'
  - 'docs/rewrite/STATUS.md:321-329'
  - 'docs/rewrite/architecture-conventions.md:942-1002'
documentation:
  - docs/rewrite/architecture-conventions.md
parent_task_id: TASK-2
priority: high
type: task
ordinal: 201
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a developer of wedding-portal, I want CI to check out the private contract with one read-only token on every push and pull request so that the cross-repo build is green from the first commit.

All cross-repo CI of the POC is red because the private contract checkout is refused (RISK-51; 16 R-01); rekord-api's workflow falls back to the workflow's own token, which cannot read another private repository (rekord-api/.github/workflows/deploy.yml:28-33). UD-15.e settles it: one fine-grained token, read-only on rekord-contract, stored as a CI secret. Creating and storing that secret is a STOP item. Triggers, toolchain and the verdict by commit SHA follow architecture-conventions §14.4 and .claude/CLAUDE.md P5.

- STOP (human approval in the pull request): production-config
- Covers: UD-15.e, RISK-51

Plan item `P0-E02-T01` (technical,P0,stop-production-config) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Given a push to main, a push to a feature/** branch or a pull request into main of wedding-portal When CI starts Then one workflow runs the Gradle tasks test, integrationTest and build on Temurin 25, and its result is fetched by the commit SHA.
- [x] #2 Given the CI secret holding one fine-grained token with read-only contents access to rekord-contract and no other repository When the workflow checks out rekord-contract Then the checkout succeeds, the workflow names the token only by its secret name, and no step falls back to the workflow's own token.
- [x] #3 Given the rekord-contract checkout made with that token When a step tries to push to rekord-contract Then the push is refused because the token is read-only.
- [x] #4 Given a CI run whose contract checkout fails When the workflow ends Then the run is red, names the failed checkout step, and no later step runs.
- [x] #5 Given the workflow files of wedding-portal When their uses: lines are read Then every third-party action is pinned by a 40-character commit SHA.
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
Finalization evidence — PR https://github.com/Vikteur/wedding-portal/pull/4, merged as d6ec576d534ef087319915624e5bc7a235bf58b7; head 5502145f0e328cfe05ffad099fba14f34b87ba8b; CI run 37601782755 (pull_request, workflow CI) green, read by SHA with `gh run list --commit 5502145`.
- AC #1: ci.yml triggers push [main, feature/**], pull_request [main]; step 'Build and test' (id gradle) runs ./gradlew test integrationTest build on Temurin 25 — run 37601782755 'Build and test' success (BUILD SUCCESSFUL); tests ci_runs_on_push_to_main_and_feature_branches_and_on_pull_requests_into_main, exactly_one_workflow_runs_on_push_and_pull_request, build_job_runs_test_integration_test_and_build_on_temurin_25, ci_result_is_read_by_the_commit_sha. The push to main of the merge commit started run 37603227217 (in progress at finalization).
- AC #2: run 37601782755 step 'Check out rekord-contract' success with secrets.CONTRACT_TOKEN, 'Contract bundle present' success; tests build_job_checks_out_rekord_contract_with_the_contract_token_secret, no_workflow_falls_back_to_the_workflows_own_token, the_contract_token_is_named_only_by_its_secret_name, contract_bundle_is_checked_before_the_build.
- AC #3: run 37601782755 step 'Contract token is read-only' logged 'OK: push to rekord-contract refused' (dry-run push refused by GitHub); tests build_job_proves_the_contract_token_is_read_only_right_after_the_checkout, read_only_check_fails_when_the_push_is_accepted_and_accepts_only_a_refusal; local run of the script against an accepting bare repo exits 1 with 'FAIL: the contract token can push'.
- AC #4: tests no_step_runs_after_a_failed_contract_checkout, every_other_job_needs_the_build_job and the negative controls a_later_step_with_always_or_bare_failure_would_run_after_the_checkout_fails, continue_on_error_on_the_checkout_would_hide_its_failure; actions/checkout requires a non-empty token (no implicit github.token fallback). No red run was provoked on purpose.
- AC #5: every uses: in ci.yml pinned by 40-hex SHA with version comment; tests every_action_in_every_workflow_is_pinned_by_a_40_character_commit_sha and negative control a_tag_or_branch_or_short_sha_reference_counts_as_unpinned.
- DoD #1: CiWorkflowTest (19 tests) + BuildLayoutTest.gitignore_ignores_the_contract_checkout; each rule has a negative control.
- DoD #2: run 37601782755 green on head 5502145, jobs build and image success; only 'Upload test reports' skipped by its failure-only condition, no test or gate disabled.
- DoD #3 N/A: no ArchUnit suite exists yet (TASK-3.1); no production class changed.
- DoD #4 N/A: no contract change; the hub is read at its default branch, the tag and pin come with TASK-2.2.
- DoD #6 N/A: no refusal path built.
- DoD #7 N/A: no operation built.
- DoD #8: diff scanned for token patterns, none; the secret is named only as secrets.CONTRACT_TOKEN; docs/memory.md holds no token value, prefix or account data.
- DoD #9 N/A: no schema change.
- DoD #10: docs/memory.md section '2026-10-07 — TASK-2.1 contract checkout in CI' added in the PR.
- DoD #11: reviewed by the workflow's review node (commit 5502145) and merged into main by Vikteur.
- DoD #5 NOT CHECKED: the STOP item (production config / secrets, CONTRACT_TOKEN) needs a human approval recorded in the pull request. PR #4 has no review and no comment; its body asked the user to confirm the token is fine-grained with Repository access = only Vikteur/rekord-contract and Contents = Read-only, and no confirmation is recorded. CI proves the token cannot push to rekord-contract, but not that it reaches no other repository. To close: the user comments the confirmation on PR #4, then finalization is rerun. Status stays In Progress.
- Follow-ups (not created): TASK-2.2 (contract tag and pin), TASK-2.3 (contract.spec to Gradle / openApiGenerate).

- DoD #5 resolved: the user recorded the approval of the STOP item (production config / secrets, CONTRACT_TOKEN) on PR #4 — https://github.com/Vikteur/wedding-portal/pull/4#issuecomment-6036197459 (Vikteur, 2026-10-07T10:39:54Z): fine-grained token, Repository access = only Vikteur/rekord-contract, Contents = Read-only. The PR comment came after the merge; before any code was written, the user approved the same STOP item at the Archon stop-gate of run 6c6e102f, and created the secret themselves in the GitHub UI.
- Push CI on the merge commit d6ec576 (run 37603227217) is green.
- Workflow gap (named, not created): the Archon stop-gate approval is not written to the pull request, so DoD #5 cannot close on its own; fix by having build-feature post the gate approval as a PR comment.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
wedding-portal CI now checks out the private rekord-contract with one fine-grained read-only token (secret CONTRACT_TOKEN) and proves it cannot push; every action is pinned by SHA; push triggers on main and feature/**, pull requests into main; ./gradlew test integrationTest build on Temurin 25.

Evidence: PR https://github.com/Vikteur/wedding-portal/pull/4 merged as d6ec576; CI run 37601782755 green on head 5502145 (jobs build and image, "OK: push to rekord-contract refused"); push run 37603227217 green on d6ec576; CiWorkflowTest (19 tests with negative controls); STOP approval recorded on PR #4 (issuecomment-6036197459).
<!-- SECTION:FINAL_SUMMARY:END -->
