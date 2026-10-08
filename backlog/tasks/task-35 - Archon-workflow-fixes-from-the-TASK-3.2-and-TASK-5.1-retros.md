---
id: TASK-35
title: Archon workflow fixes from the TASK-3.2 and TASK-5.1 retros
status: Done
assignee: []
created_date: '2026-10-07 23:12'
updated_date: '2026-10-07 23:45'
labels:
  - archon
  - tooling
dependencies: []
references:
  - docs/retro/TASK-3.2/lessons-learned.md
  - docs/retro/TASK-5.1/lessons-learned.md
ordinal: 42404
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The TASK-3.2 and TASK-5.1 retros named four workflow faults that cost time on both runs. Close-out failed twice with 'Permission denied': git unregisters the run worktree, but the directory is still the running workflow's cwd, so Windows will not delete it; archon complete then stops and keeps the branch. tdd-check failed TASK-5.1 because a verify-only plan step (a full build, no new code) had no red run, and nothing lets the plan mark such a step. ci-by-sha.sh reads HEAD, so run from a main checkout it answered for main instead of the pull request. The smart review's classifier skipped three of five agents on TASK-3.2 because the changed ArchUnit gates live under src/test. The .archon folder is kept identical in the umbrella and in wedding-portal.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 close-out.sh exits 0 with a warning, deletes the merged branch locally and on origin, when git no longer lists the run worktree and only an empty directory is left; close-out.test.sh covers it
- [x] #2 A plan step marked (verify only) needs no red run in tdd-check.sh, while code commits still need a test commit first; the plan, write-tests and build-step prompts in build-feature.yaml handle the marker; tdd-check.test.sh covers it
- [x] #3 ci-by-sha.sh takes an optional commit SHA or pull-request number, and refuses with a non-zero exit when no SHA is given and HEAD is on the default branch; a test covers both
- [x] #4 docs/archon/workflows.json picks archon-comprehensive-pr-review when a PR changes code or an architecture gate, including tests under src/test, and keeps archon-smart-pr-review for docs- or config-only PRs
- [x] #5 The .archon folder is identical in the umbrella and in wedding-portal after the change
- [x] #6 The matching Actions in docs/retro/TASK-3.2 and docs/retro/TASK-5.1 lessons-learned.md are marked solved, naming this ticket
- [x] #7 The wedding-portal CI image job runs only on a push to main (after a merge), not on pull requests or feature-branch pushes; a test on ci.yml guards it
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Validation: local .archon tests in both repos: close-out 24/24, tdd-check 15/15, ci-by-sha 17/17, retro 77/77, retro-stopped 65/65; diff -r of the two .archon folders empty. wedding-portal PR #22 CI green on f15b510 (build success, image skipped on the pull_request run). Main CI run 37703489123 on merge 3a22a36: build and image success (image runs on push to main). Umbrella PR #43 has no CI workflows; merged as c805cba.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Fixed the Archon workflow issues from the TASK-3.2 and TASK-5.1 retros, test-first, mirrored in both repos: close-out.sh finishes when only a locked empty worktree directory is left (and sweeps such leftovers, never beside the main checkout); (verify only) plan steps need no red run; ci-by-sha.sh takes a sha or PR number and refuses HEAD on main; workflows.json defaults to the comprehensive review for code/test/gate PRs; retro actions marked Solved (TASK-35). Also, on the user's request, the wedding-portal CI image job now runs only on a push to main, guarded by ContractSpecWiringTest. Verified by the local .archon suites, PR CI green on f15b510 and main CI 37703489123 (build + image green). PRs: wedding-portal #22, weddingapp #43.
<!-- SECTION:FINAL_SUMMARY:END -->
