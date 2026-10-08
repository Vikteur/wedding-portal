# Agent instructions for wedding-portal

This repo's tickets live in the Backlog.md backlog of the sibling `weddingapp` checkout (`../weddingapp`), not here. Run every `backlog` command against it: from `../weddingapp`, or with `BACKLOG_CWD=../weddingapp`. Groma's Backlog task links (below) therefore name files of this repo in a ticket of that one. The Groma architecture map lives here, under `groma/`. Open it with `bash scripts/groma-web.sh` (not plain `groma web`): it attaches the weddingapp backlog, so the To Do and In Progress tickets that reference map elements or name changed files of this repo show on the map. Switch on the versioned pre-push hook once per clone with `bash scripts/install-hooks.sh`: it runs `.github/scripts/groma-check.sh` before every push (linked worktrees included) and stops the push when the map is out of step with the code.

Docs: read `docs/README.md` before you write any doc. It says what goes where (decisions in `docs/memory.md`, project facts per skill in `docs/code-maps/<skill>.md`; retros, follow-ups and ticket specs in `../weddingapp`).

<!-- groma:start -->
## Groma

This project uses Groma. Before you scan, inspect, or curate architecture, or change files for a Backlog task, run `groma agent-instructions` and read the guide it names for that job. When it reports a first scan, ask the user whether they want you to curate the architecture. Do not edit Groma-owned architecture files directly.
<!-- groma:end -->

<!-- BACKLOG.MD GUIDELINES START -->
<!-- backlog.md-instructions-version: 1.53.0 -->
<CRITICAL_INSTRUCTION>

## Backlog.md Workflow

This project uses Backlog.md for task and project management.

**At the beginning of each conversation in this project, run `backlog instructions overview` before answering or taking action. Re-read it only if you have not read it yet in the current conversation.**

Use the overview to decide whether to search, read, create, or update Backlog tasks.

Before task lifecycle actions, read the matching detailed guide:
- `backlog instructions task-creation` before creating or splitting tasks
- `backlog instructions task-execution` before planning, changing status or assignee, adding a plan or implementation notes, or implementing task work
- `backlog instructions task-finalization` before checking acceptance criteria, writing final summaries, or moving tasks to terminal statuses

Use `backlog <command> --help` before running unfamiliar commands. Help shows options, fields, and examples.

Do not edit Backlog task, draft, document, decision, or milestone markdown files directly. Use the `backlog` CLI so metadata, relationships, and history stay consistent.

</CRITICAL_INSTRUCTION>
<!-- BACKLOG.MD GUIDELINES END -->
