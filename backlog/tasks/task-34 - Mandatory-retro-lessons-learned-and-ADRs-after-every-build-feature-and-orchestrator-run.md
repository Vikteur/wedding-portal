---
id: TASK-34
title: >-
  Mandatory retro: lessons learned and ADRs after every build-feature and
  orchestrator run
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-07 17:03'
updated_date: '2026-10-07 20:51'
labels:
  - technical
dependencies: []
ordinal: 41404
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Every workflow run makes decisions (plan choices, STOP calls, test and design trade-offs, CI fixes, the merge) and hits friction (red CI rounds, failed nodes, closed PRs), but nothing records them once the run ends: the Archon artifacts dir is not checked in, the existing `retro` agent is never called by any workflow, and it routes findings through a `scripts/propose-upstream.sh` that does not exist in this repo. The user wants a retro agent and skill as a mandatory last step of each ticket workflow that analyses what happened and which decisions were made, and writes a lessons-learned.md plus the ticket ADRs. Decided with the user on 2026-10-07: output lives in the umbrella under `docs/retro/<TASK>/` (lessons-learned.md, adr/ADR-NN-<slug>.md, script-generated index in docs/retro/README.md), committed straight to umbrella main by a script after the run; the workflows covered are Archon build-feature (umbrella and wedding-portal copies) and the Claude-agent orchestrator pipeline, not the rewrite Workflow scripts. Probed on Archon: a `trigger_rule: all_done` node still runs after an upstream failure, so the retro also covers failed runs.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a ticket whose pull request is merged When the merge is seen Then the retro agent is launched for that ticket (outside the build-feature workflow) and the umbrella main gains a commit touching only docs/retro/; a run that ends without a merge gets no retro unless the user asks
- [x] #2 Given retro-commit.sh When lessons-learned.md misses a required section, an ADR misses a required section, or an ADR is not linked from lessons-learned.md Then it exits non-zero and commits nothing; otherwise it commits only docs/retro/<folder> and the regenerated docs/retro/README.md index and pushes umbrella main
- [x] #3 Given retro-evidence.sh When it runs for a run Then it writes a condensed evidence file (node timeline with failures and skip causes, agent summaries, branch commits, pull request state and reviews, CI runs) and names the folder docs/retro/<TASK>, or docs/retro/run-<first 8 of the run id> without a ticket, with the next ADR number continuing earlier runs of the same ticket
- [ ] #4 Given build-feature.yaml When it is read Then it has no retro-sweep, retro-evidence, retro or retro-commit node, close-out depends only on finalize, and its description says the retro is launched after the merge
- [x] #5 Given .claude/skills/retro/SKILL.md When it is read Then it states the generic retro method and the lessons-learned and ADR templates with no project noun, and the retro agent and docs/retro/README.md hold the project facts
- [ ] #6 Given the orchestrator and closeout agents When a run is merged Then the orchestrator dispatches retro and closeout refuses to close without the committed retro folder; a run abandoned without a merge gets no retro; agent-scopes.json lists exactly the commands of the retro agent tools
- [ ] #7 Given wedding-portal When its .archon directory is compared with the umbrella one Then they are identical
- [ ] #8 Given .archon/scripts/test/retro.test.sh and scripts/test/retro-framework.test.sh When any criterion above is broken Then a test fails
- [x] #9 Given a run that is stopped with .archon/scripts/stop-run.sh <run> <reason> When it is running, pending or paused at an approval gate Then the reason is required, kept as stop-reason.json beside the run's artifacts, the run is cancelled or (at a gate) rejected with that reason, and the umbrella main gains its section with outcome cancelled or rejected and a Why it stopped section giving the reason; on an already cancelled run only the reason is added, rewriting that section
- [ ] #10 Given retro-stopped.sh --sweep When it is run by hand (it is no node of build-feature) Then it documents every cancelled build-feature run without a section, oldest first, with where it stopped, its node timeline, the run that adopted it and its reason or 'No reason was recorded', and never fails its caller
- [x] #11 Given retro-commit.sh When a run section's outcome is cancelled or rejected Then only What happened and Why it stopped are required; any other outcome still needs the full template
- [ ] #12 retro-evidence.sh takes the merged pull request (number or URL) as an argument and gathers the evidence from it (pull request, its commits, CI runs of its head SHA) after close-out deleted the run's branch and worktree, run from the umbrella's main checkout
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Tests first: .archon/scripts/test/retro.test.sh (retro-evidence.sh and retro-commit.sh against throwaway repos, stubbed gh/archon, a fixture transcript) and scripts/test/retro-framework.test.sh (skill, agent, scopes, docs leaf, build-feature wiring, orchestrator/closeout, wedding-portal mirror). Commit them red.
2. .archon/scripts/retro-evidence.sh: find the umbrella, name the folder, next ADR number, condense the transcript, gather commits/PR/CI into $ARTIFACTS_DIR/retro-evidence.md; print JSON.
3. .archon/scripts/retro-commit.sh: structure gate, regenerate the docs/retro/README.md index, commit only docs/retro paths, push umbrella main; print JSON.
4. .claude/skills/retro/SKILL.md (generic method + templates), rewrite .claude/agents/retro.md, docs/retro/README.md leaf, agent-scopes.json retro entry.
5. build-feature.yaml: retro-evidence (all_done after finalize) -> retro (@analyse) -> retro-commit -> close-out; docs/archon/setup.md mention.
6. orchestrator.md and closeout.md: retro mandatory after every merged or abandoned run.
7. Mirror .archon into wedding-portal (test commit then code commit, PR).
8. Run all tests, commit test-first, open PRs.

9. Stopped runs (user request): tests first (.archon/scripts/test/retro-stopped.test.sh, gate short form in retro.test.sh, sweep wiring in retro-framework.test.sh); then stop-run.sh, retro-stopped.sh (one run / --sweep), retro.js note|pending|stopped and the gate short form, the retro-sweep node, docs and skill; mirror to wedding-portal.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented test-first: umbrella f8c93f6 (tests, red: retro 16/52, framework 7/37) then aec3f36 (code); wedding-portal dd8345c (test, red 16/52) then 697af33. PRs: Vikteur/weddingapp#40, Vikteur/wedding-portal#18.
Evidence: retro.test.sh 52 passed (AC2 C1-C9, AC3 E1-E4), retro-framework.test.sh 53 passed (AC4 close-out depends_on [finalize, retro-commit]; AC5 skill no project noun + templates; AC6 orchestrator step 6, closeout gate, retro scope == Bash tools; AC7 diff -r .archon identical); existing suites green (close-out 9, tdd-check 12, governance-hooks 18, framework-readiness 51); archon validate workflows build-feature ok (both repos). retro-evidence.sh rendered a real transcript (wedding-portal run f6baa598) correctly.
Decisions: the retro runs before close-out (close-out removes the worktree that is the node cwd); retro-evidence reads no node output (any may be skipped) and derives task/branch/PR itself; ADRs are per ticket in docs/retro/<TASK>/adr, not backlog decisions (the CLI takes only a title); the orchestrator pipeline commits through the same retro-commit.sh gate from closeout, with the merge commit SHA as run id; retro names follow-ups, never creates tickets or edits primitives (propose-upstream removed: it does not exist here).
Known limit: a rejected STOP approval or a cancelled run ends archon outright and never reaches the retro (documented in docs/retro/README.md).
AC #1 left unchecked: it needs a live build-feature run after these PRs merge (all_done after a failure was probed earlier, but the full retro -> umbrella commit path has not run for real yet).

Stopped runs (user: 'If a run got cancelled or stopped it should also be documented why. Script this'). Archon facts checked on probe runs: a cancel keeps no reason and custom event types are refused (archon workflow event emit: unknown event type), so the reason is kept as stop-reason.json in the run's artifacts root; reject --reason lands as a gate_decision event; a gate-paused run has status paused; cancel, reject and abandon all end as status cancelled. Umbrella test e9939e1 then feat c656bb3; wedding-portal test 25079c2 then feat dcafe98, CI run 37669406450 success on dcafe98. Local: retro-stopped.test.sh 65/65, retro.test.sh 58/58, retro-framework.test.sh 65/65, archon validate workflows build-feature ok. A dry render of the 4 existing cancelled build-feature runs (cb246a85, 720743c1 rejected with their gate reasons; 96404e93, 7d2ca568 cancelled, no reason) read correctly; the first live sweep will commit them.

AC #9 verified by retro-stopped.test.sh T1-T7 (reason required, cancel vs reject, stop-reason.json, documented and pushed, late reason rewrites the section, umbrella off main keeps the reason). AC #10 by S4-S6 (sweep scope, oldest first, idempotent, never fails) and retro-framework.test.sh (retro-sweep node wiring). AC #11 by retro.test.sh C10-C12.

Decision 2026-10-07 (user): the retro is launched after the ticket is merged, not as nodes at the end of every build-feature run. The four retro nodes leave the workflow; the scripts stay. AC #1, #4, #6 and #10 rewritten for it; #7 and #8 unchecked until re-verified.
<!-- SECTION:NOTES:END -->
