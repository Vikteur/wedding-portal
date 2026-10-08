#!/usr/bin/env bash
# Tests for the ticket-to-backlog routing of .archon/scripts/ticket.sh and wait-merge.sh (TASK-36) against throwaway
# sibling repos whose backlogs use different task prefixes, with stubbed backlog and gh.
# Usage: bash .archon/scripts/test/backlog-home.test.sh   (exit 0 = all pass)
set -uo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
pass=0; fail=0
root="$(mktemp -d)"
trap 'rm -rf "$root"' EXIT

check() { # check <name> <condition exit code>
  if [ "$2" -eq 0 ]; then pass=$((pass+1)); else fail=$((fail+1)); echo "FAIL: $1"; echo "$out" | sed 's/^/    /'; fi
}
field() { echo "$out" | tail -1 | node -e 'let s="";process.stdin.on("data",d=>s+=d).on("end",()=>{try{console.log(JSON.parse(s)[process.argv[1]])}catch(e){}})' "$1"; }
used() { [ "$(tail -1 "$root/backlog.log")" -ef "$1" ]; } # used <dir>: the last backlog call ran in that backlog

# Stubs first on PATH: backlog answers from the ticket files under $BACKLOG_CWD and logs where it ran; gh reports a
# merged pull request with a green head.
mkdir -p "$root/bin"
cat > "$root/bin/backlog" <<'STUB'
#!/usr/bin/env bash
echo "${BACKLOG_CWD:-}" >> "$STUB_LOG"
id=$3; f="${BACKLOG_CWD:-.}/backlog/tasks/$(printf '%s' "$id" | tr '[:upper:]' '[:lower:]')"
[ -f "$f" ] || { echo "Task $id not found." >&2; exit 1; }
case "$4" in
  --json) printf '{"task":{"id":"%s","labels":[%s]}}\n' "$id" "$(cat "$f")" ;;
  *) echo "Task $id" ;;
esac
STUB
cat > "$root/bin/gh" <<'STUB'
#!/usr/bin/env bash
case "$*" in
  *"--json state"*) echo MERGED ;;
  *mergeCommit*) echo m1 ;;
  *headRefOid*) echo h1 ;;
  *headRefName*) echo feat ;;
  "run list"*) echo success ;;
esac
STUB
chmod +x "$root/bin/backlog" "$root/bin/gh"

# repo <dir> <task_prefix or ""> [ticket...]: a main checkout with a backlog/ holding the tickets (each with no labels).
repo() {
  local d=$1 p=$2; shift 2
  git init -q -b main "$d"; git -C "$d" config user.name t; git -C "$d" config user.email t@t; git -C "$d" config core.autocrlf false
  mkdir -p "$d/backlog/tasks"
  { echo 'project_name: "t"'; [ -n "$p" ] && echo "task_prefix: \"$p\""; } > "$d/backlog/config.yml"
  for t in "$@"; do : > "$d/backlog/tasks/$t"; done
  echo base > "$d/README.md"; git -C "$d" add -A; git -C "$d" commit -qm base
}
# layout <name>: umbrella (tool), wedding-portal (task) and a frontend (fe) side by side, and a run worktree of each
# product repo elsewhere, as Archon makes them; sets d umb wp fe wt_wp wt_umb.
layout() {
  d="$root/$1"; mkdir -p "$d/repos" "$d/runs"
  umb="$d/repos/umbrella"; wp="$d/repos/wedding-portal"; fe="$d/repos/frontend"
  repo "$umb" tool tool-1
  repo "$wp" task task-7.2
  repo "$fe" fe fe-3
  echo '"stop-db-migration"' > "$wp/backlog/tasks/task-7.2"; git -C "$wp" commit -qam label
  git -C "$wp" worktree add -q -b run "$d/runs/wp"; wt_wp="$d/runs/wp"
  git -C "$umb" worktree add -q -b run "$d/runs/umb"; wt_umb="$d/runs/umb"
  : > "$root/backlog.log"
}
ticket() { # ticket <dir> <request> [env...]: runs ticket.sh there; sets out rc
  local dir=$1 req=$2; shift 2
  out=$( (cd "$dir" && env PATH="$root/bin:$PATH" STUB_LOG="$root/backlog.log" ARTIFACTS_DIR="$d" "$@" \
    bash "$here/ticket.sh" "$req") 2>&1 ); rc=$?
}
merged() { # merged <dir> <ticket> [env...]: runs wait-merge.sh there; sets out rc
  local dir=$1 t=$2; shift 2
  out=$( (cd "$dir" && env PATH="$root/bin:$PATH" STUB_LOG="$root/backlog.log" "$@" \
    bash "$here/wait-merge.sh" https://github.com/o/wedding-portal/pull/7 "$t") 2>&1 ); rc=$?
}

# ---------- ticket.sh ----------
# T1. A portal run reads a TASK ticket from the portal backlog and reports its STOP label.
layout t1; ticket "$wt_wp" "Build TASK-7.2"
check "T1 exit 0" $([ "$rc" -eq 0 ]; echo $?)
check "T1 task" $([ "$(field task)" = TASK-7.2 ]; echo $?)
check "T1 stop label" $([ "$(field stop)" = stop-db-migration ]; echo $?)
check "T1 read from the portal backlog" $(used "$wp"; echo $?)
check "T1 ticket written" $([ -s "$d/ticket.md" ]; echo $?)

# T2. A ticket of another repo's prefix is read from that sibling's backlog.
layout t2; ticket "$wt_wp" "Build FE-3"
check "T2 task" $([ "$(field task)" = FE-3 ]; echo $?)
check "T2 read from the frontend backlog" $(used "$fe"; echo $?)

# T3. An umbrella run reads its own prefix from its own backlog.
layout t3; ticket "$wt_umb" "build tool-1 please"
check "T3 task upper case" $([ "$(field task)" = TOOL-1 ]; echo $?)
check "T3 read from the umbrella backlog" $(used "$umb"; echo $?)

# T4. No ID of a known prefix: no ticket, no failure, Backlog not asked. Unknown prefixes (UTF-8) are not IDs.
layout t4; ticket "$wt_wp" "Fix the UTF-8 typo"
check "T4 exit 0" $([ "$rc" -eq 0 ]; echo $?)
check "T4 no task" $([ -z "$(field task)" ]; echo $?)
check "T4 backlog not called" $([ ! -s "$root/backlog.log" ]; echo $?)

# T5. A known prefix the backlog has no ticket for fails, naming the ticket.
layout t5; ticket "$wt_wp" "Build TASK-99"
check "T5 exit non-zero" $([ "$rc" -ne 0 ]; echo $?)
check "T5 names the ticket" $(echo "$out" | grep -q 'TASK-99'; echo $?)

# T6. Two backlogs with the same prefix: refused, naming both, instead of taking the first.
layout t6; repo "$d/repos/old-umbrella" task task-7.2; ticket "$wt_wp" "Build TASK-7.2"
check "T6 exit non-zero" $([ "$rc" -ne 0 ]; echo $?)
check "T6 names both backlogs" $(echo "$out" | grep -q 'old-umbrella' && echo "$out" | grep -q 'wedding-portal'; echo $?)

# T7. BACKLOG_CWD overrides the search.
layout t7; ticket "$wt_umb" "Build FE-3" BACKLOG_CWD="$fe"
check "T7 task" $([ "$(field task)" = FE-3 ]; echo $?)
check "T7 read from BACKLOG_CWD" $(used "$fe"; echo $?)

# T8. A backlog without task_prefix uses Backlog's default prefix, task.
layout t8; rm -rf "$wp"/backlog; mkdir -p "$wp/backlog/tasks"; echo 'project_name: "t"' > "$wp/backlog/config.yml"
: > "$wp/backlog/tasks/task-4"; ticket "$wt_wp" "Build TASK-4"
check "T8 default prefix task" $([ "$(field task)" = TASK-4 ]; echo $?)
check "T8 read from the portal backlog" $(used "$wp"; echo $?)

# T9. The first ID in the request wins, whatever its prefix.
layout t9; ticket "$wt_wp" "FE-3 needs TASK-7.2"
check "T9 first ID wins" $([ "$(field task)" = FE-3 ]; echo $?)

# ---------- wait-merge.sh ----------
# W1. The ticket is finalized in the backlog its prefix names: the portal's main checkout, never the run worktree.
layout w1; merged "$wt_wp" TASK-7.2
check "W1 exit 0" $([ "$rc" -eq 0 ]; echo $?)
check "W1 backlog is the portal main checkout" $([ "$(field backlog)" -ef "$wp" ]; echo $?)

# W2. A ticket of a sibling's prefix is finalized in that sibling.
layout w2; merged "$wt_wp" FE-3
check "W2 backlog is the frontend" $([ "$(field backlog)" -ef "$fe" ]; echo $?)

# W3. No ticket: no backlog needed, the wait still reports the merge.
layout w3; merged "$wt_wp" ""
check "W3 exit 0" $([ "$rc" -eq 0 ]; echo $?)
check "W3 merged" $([ "$(field state)" = merged ]; echo $?)
check "W3 empty backlog" $([ -z "$(field backlog)" ]; echo $?)

# W4. Two backlogs with the ticket's prefix: refused.
layout w4; repo "$d/repos/old-umbrella" task task-7.2; merged "$wt_wp" TASK-7.2
check "W4 exit non-zero" $([ "$rc" -ne 0 ]; echo $?)

# W5. BACKLOG_CWD overrides.
layout w5; merged "$wt_wp" TASK-7.2 BACKLOG_CWD="$umb"
check "W5 BACKLOG_CWD wins" $([ "$(field backlog)" -ef "$umb" ]; echo $?)

echo "backlog-home tests: $pass passed, $fail failed"
[ "$fail" -eq 0 ]
