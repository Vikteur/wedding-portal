---
id: TASK-36
title: >-
  Move the backend tickets into the wedding-portal backlog and route Archon to a
  ticket's backlog by its ID prefix
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-08 00:34'
updated_date: '2026-10-08 00:35'
labels:
  - archon
  - tooling
dependencies: []
ordinal: 43404
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Every ticket lives in the umbrella backlog today, although nearly all of them (TASK-1 to TASK-33, the P0-P4 rewrite epics) change wedding-portal code. The user wants tickets to live in the repo whose code they change: tickets and code then land in one pull request, and Groma (initialised in wedding-portal) can pin a ticket to the architecture components it touches, which it only does for a backlog in its own repo. A frontend repo with its own backlog is expected later. Archon cannot cope with that today: ticket.sh, wait-merge.sh and the retro scripts take the first backlog/config.yml found in the run's main checkout or beside it, so a second backlog silently wins (a backlog in wedding-portal makes every portal run read it), and the retro scripts use that same backlog/ as the marker of the umbrella. Backlog.md has no command to move a task between repos or rename an ID, and changing task_prefix hides every task with the old prefix (probed), so the TASK prefix has to stay with the moved tickets and the umbrella takes a new prefix for its future tickets. Decided with the user on 2026-10-08: tickets live in the repo whose code they change; do the work by hand, not through Archon.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given the umbrella and wedding-portal backlogs When listed Then wedding-portal holds every TASK-n ticket and the five phase milestones with their IDs, history and relations intact and backlog doctor reports no problem, and the umbrella backlog holds none of them
- [ ] #2 Given each repo's backlog/config.yml When read Then every backlog has a task_prefix no other repo uses: wedding-portal keeps task, the umbrella has its own prefix for new tickets
- [ ] #3 Given a build-feature request naming a ticket ID When ticket.sh runs in any repo Then it reads the ticket from the backlog whose task_prefix matches the ID prefix (searched in the run repo main checkout and its siblings), fails naming the ID when no backlog has that prefix, and BACKLOG_CWD still overrides
- [ ] #4 Given a merged build-feature pull request When wait-merge.sh and close-out.sh run Then the ticket is finalized, committed and pushed in its own backlog, including when that backlog is in the same repo as the pull request whose main is behind origin after the merge
- [ ] #5 Given the retro scripts When they look for the umbrella Then they find the repo with docs/retro/README.md, not the first backlog/, so a backlog in a product repo never becomes the retro home
- [ ] #6 Given wedding-portal When its .archon directory is compared with the umbrella one Then they are identical
- [ ] #7 Given the .archon script tests When any criterion above is broken Then a test fails; every existing suite still passes
- [ ] #8 Given docs/archon/setup.md and docs/backlog-md-guide.md When read Then they say where tickets live, the prefix per repo, and how a run finds a ticket's backlog; wedding-portal AGENTS.md carries the Backlog.md instructions and its CLAUDE.md loads AGENTS.md
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Tests first (red, committed alone): .archon/scripts/test/backlog-home.test.sh for ticket.sh and wait-merge.sh against throwaway sibling repos with differing task_prefix (route by prefix, ambiguous prefix fails, unknown ticket fails, no ID = no ticket, BACKLOG_CWD overrides, the run repo itself wins); close-out.test.sh scenario where the backlog is in the pull request repo whose main is behind origin; retro.test.sh: umbrella found by docs/retro/README.md even when a product repo has a backlog, RETRO_HOME override, a non-task prefix names the retro folder.
2. .archon/scripts/backlog-home.sh (sourced): list backlogs (run repo main checkout, then siblings), read task_prefix, find the first ticket ID with a known prefix and its backlog; ambiguous prefix fails.
3. ticket.sh and wait-merge.sh use it; build-feature passes the ticket to wait-merge.sh. close-out.sh pulls the backlog repo main before committing.
4. retro-umbrella.sh finds the umbrella by docs/retro/README.md (RETRO_HOME overrides); retro-evidence.sh and retro.js name the folder from any known prefix.
5. Docs: docs/archon/setup.md, docs/backlog-md-guide.md (tickets live where their code is, prefix per repo).
6. Move every task-*.md and the five milestones verbatim into wedding-portal/backlog (Backlog.md has no move or rename command; files copied unchanged keep IDs, history and relations), umbrella task_prefix becomes tool; backlog doctor and task list in both repos.
7. Mirror .archon into wedding-portal; wedding-portal AGENTS.md Backlog block and CLAUDE.md loading AGENTS.md.
8. All .archon and scripts/test suites green in both repos; PRs: umbrella, wedding-portal (stacked on the Groma init PR).
<!-- SECTION:PLAN:END -->
