#!/usr/bin/env bash
# Tests for .archon/scripts/retro-evidence.sh and retro-commit.sh (TASK-34) against throwaway repos, stubbed gh/archon
# and a fixture transcript.
# Usage: bash .archon/scripts/test/retro.test.sh   (exit 0 = all pass)
set -uo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
evidence="$here/retro-evidence.sh"
commit="$here/retro-commit.sh"
pass=0; fail=0
root="$(mktemp -d)"
trap 'rm -rf "$root"' EXIT
run_id=73ef1981-f10d-44d1-a4f5-5f9ca3a3d373

check() { # check <name> <condition exit code>
  if [ "$2" -eq 0 ]; then pass=$((pass+1)); else fail=$((fail+1)); echo "FAIL: $1"; echo "$out" | sed 's/^/    /'; fi
}
has() { grep -qF -- "$1" "$2"; }
same() { [ "$(echo "$out" | tail -1 | node -e 'let s="";process.stdin.on("data",d=>s+=d).on("end",()=>console.log(JSON.parse(s)[process.argv[1]]))' "$1")" -ef "$2" ]; } # same <json field> <dir>: same directory

# Stubs first on PATH: gh answers from STUB_* files; archon prints the fixture transcript for `workflow logs`.
mkdir -p "$root/bin"
cat > "$root/bin/gh" <<'STUB'
#!/usr/bin/env bash
case "$*" in
  "pr list"*) [ -n "${STUB_PR:-}" ] && echo "$STUB_PR" ;;
  "pr view"*) cat "$STUB_PR_JSON" ;;
  "run list"*) cat "$STUB_RUNS_JSON" ;;
  *) exit 1 ;;
esac
STUB
cat > "$root/bin/archon" <<'STUB'
#!/usr/bin/env bash
[ "$1 $2" = "workflow logs" ] && [ "$3" = "$STUB_RUN" ] && cat "$STUB_TRANSCRIPT"
STUB
chmod +x "$root/bin/gh" "$root/bin/archon"

cat > "$root/transcript.jsonl" <<EOF
{"type":"workflow_start","workflow_name":"build-feature","content":"TASK-7.1 add the thing","workflow_id":"$run_id","ts":"2026-10-07T10:00:00.000Z"}
{"type":"node_start","step":"ticket","execution":{"node":{"id":"ticket","kind":"exec"}},"ts":"2026-10-07T10:00:01.000Z"}
{"type":"node_complete","step":"ticket","duration_ms":2283,"ts":"2026-10-07T10:00:03.000Z"}
{"type":"node_start","step":"plan","execution":{"node":{"id":"plan","kind":"agent"}},"ts":"2026-10-07T10:00:04.000Z"}
{"type":"assistant","content":"Reading the module builds first.","ts":"2026-10-07T10:00:05.000Z"}
{"type":"tool","tool_name":"Bash","tool_input":{"command":"ls"},"ts":"2026-10-07T10:00:06.000Z"}
{"type":"tool","tool_name":"Read","tool_input":{"file_path":"a"},"ts":"2026-10-07T10:00:07.000Z"}
{"type":"assistant","content":"Plan written: 3 steps, no STOP item because the change adds no migration.","ts":"2026-10-07T10:06:00.000Z"}
{"type":"node_complete","step":"plan","duration_ms":384000,"ts":"2026-10-07T10:06:28.000Z"}
{"type":"node_skipped","step":"stop-gate","content":"when_condition","cause":{"kind":"condition","expr":"\$stop-route.output.pause == 'true'"},"ts":"2026-10-07T10:06:29.000Z"}
{"type":"node_error","step":"ready","error":"Bash node 'ready' failed [exit 1]: Pull request o/repo#15 is closed.","ts":"2026-10-07T11:00:00.000Z"}
{"type":"node_skipped","step":"merged","content":"trigger_rule","cause":{"kind":"upstream_failed","origin":"ready"},"ts":"2026-10-07T11:00:01.000Z"}
EOF
cat > "$root/pr.json" <<'EOF'
{"number":15,"title":"feat: add the thing (TASK-7.1)","state":"MERGED","mergedAt":"2026-10-07T10:59:00Z",
 "mergeCommit":{"oid":"5f7f1df0000000000000000000000000000000000"},"headRefOid":"fd40198000000000000000000000000000000000",
 "headRefName":"archon/task-build-feature-1","commits":[
  {"oid":"abc1234000000000000000000000000000000000","messageHeadline":"test(step 1): the thing is missing"},
  {"oid":"def5678000000000000000000000000000000000","messageHeadline":"feat: add the thing"}],
 "reviews":[{"author":{"login":"Vikteur"},"state":"COMMENTED","body":"Looks right, but name the port."}],
 "comments":[{"author":{"login":"Vikteur"},"body":"Merging; the STOP item is approved."}]}
EOF
cat > "$root/runs.json" <<'EOF'
[{"databaseId":37585096973,"headSha":"fd40198000000000000000000000000000000000","conclusion":"success","status":"completed","workflowName":"build","createdAt":"2026-10-07T10:50:00Z"},
 {"databaseId":37585090000,"headSha":"def5678000000000000000000000000000000000","conclusion":"failure","status":"completed","workflowName":"build","createdAt":"2026-10-07T10:40:00Z"}]
EOF

# layout <name>: umbrella (Backlog + an earlier retro with two ADRs) beside a product repo's main checkout and a run
# worktree; sets umb wt art.
layout() {
  local d="$root/$1"; mkdir -p "$d"
  umb="$d/umbrella"; mkdir -p "$umb/backlog" "$umb/docs/retro/TASK-7.1/adr"
  echo 'project_name: "t"' > "$umb/backlog/config.yml"
  echo x > "$umb/docs/retro/TASK-7.1/adr/ADR-01-one.md"; echo x > "$umb/docs/retro/TASK-7.1/adr/ADR-02-two.md"
  git init -q -b main "$d/wp"; git -C "$d/wp" config user.name t; git -C "$d/wp" config user.email t@t
  echo base > "$d/wp/README.md"; git -C "$d/wp" add -A; git -C "$d/wp" commit -qm base
  git -C "$d/wp" worktree add -q -b archon/task-build-feature-1 "$d/wt"
  echo work > "$d/wt/work.txt"; git -C "$d/wt" add -A; git -C "$d/wt" commit -qm "feat: local work"
  wt="$d/wt"; art="$d/artifacts"; mkdir -p "$art/red"; echo plan > "$art/plan.md"; echo red > "$art/red/step-1.log"
}
evid() { # evid <request> [env...]: runs retro-evidence.sh in the worktree; sets out rc ev
  local req=$1; shift
  out=$( (cd "$wt" && env PATH="$root/bin:$PATH" STUB_RUN="$run_id" STUB_TRANSCRIPT="$root/transcript.jsonl" \
    STUB_PR_JSON="$root/pr.json" STUB_RUNS_JSON="$root/runs.json" "$@" \
    bash "$evidence" "$req" "$run_id" "$art" main) 2>&1 ); rc=$?
  ev="$art/retro-evidence.md"
}

# ---------- retro-evidence.sh ----------
# E1. A ticket run with a pull request: folder, next ADR number, and every evidence section.
layout e1; evid "TASK-7.1 add the thing" STUB_PR=https://github.com/o/repo/pull/15
check "E1 exit 0" $([ "$rc" -eq 0 ]; echo $?)
check "E1 folder docs/retro/TASK-7.1" $(echo "$out" | grep -qF '"dir":"docs/retro/TASK-7.1"'; echo $?)
check "E1 next ADR continues at 03" $(echo "$out" | grep -qF '"next_adr":"03"'; echo $?)
check "E1 umbrella found beside the product repo" $(same umbrella "$umb"; echo $?)
check "E1 task reported" $(echo "$out" | grep -qF '"task":"TASK-7.1"'; echo $?)
check "E1 evidence path reported" $(echo "$out" | grep -qF '"evidence":'; echo $?)
check "E1 evidence written" $([ -s "$ev" ]; echo $?)
check "E1 completed node with duration" $(has '- plan: completed (384s)' "$ev"; echo $?)
check "E1 failed node with its error" $(has "- ready: failed: Bash node 'ready' failed [exit 1]: Pull request o/repo#15 is closed." "$ev"; echo $?)
check "E1 skipped by upstream failure" $(has '- merged: skipped (upstream_failed: ready)' "$ev"; echo $?)
check "E1 skipped by condition" $(has "- stop-gate: skipped (condition: \$stop-route.output.pause == 'true')" "$ev"; echo $?)
check "E1 agent summary is the last message" $(has 'Plan written: 3 steps, no STOP item because the change adds no migration.' "$ev"; echo $?)
check "E1 agent summary drops earlier messages" $(! has 'Reading the module builds first.' "$ev"; echo $?)
check "E1 agent tool count" $(has '(2 tool calls)' "$ev"; echo $?)
check "E1 commits from the pull request" $(has 'abc1234 test(step 1): the thing is missing' "$ev"; echo $?)
check "E1 pull request state" $(has 'State: MERGED' "$ev"; echo $?)
check "E1 review body" $(has 'Looks right, but name the port.' "$ev"; echo $?)
check "E1 comment body" $(has 'Merging; the STOP item is approved.' "$ev"; echo $?)
check "E1 green CI run" $(has '37585096973 success build fd40198' "$ev"; echo $?)
check "E1 red CI run" $(has '37585090000 failure build def5678' "$ev"; echo $?)
check "E1 artifacts listed" $(has 'red/step-1.log' "$ev" && has 'plan.md' "$ev"; echo $?)

# E2. A request without a ticket and without a pull request: run folder, ADR 01, commits from git.
layout e2; evid "fix the typo"
check "E2 exit 0" $([ "$rc" -eq 0 ]; echo $?)
check "E2 run folder" $(echo "$out" | grep -qF '"dir":"docs/retro/run-73ef1981"'; echo $?)
check "E2 first ADR is 01" $(echo "$out" | grep -qF '"next_adr":"01"'; echo $?)
check "E2 no pull request said" $(has 'No pull request' "$ev"; echo $?)
check "E2 commits from git" $(has 'feat: local work' "$ev"; echo $?)

# E3. No umbrella anywhere: fails loudly.
layout e3; rm -rf "$umb"; evid "TASK-7.1"
check "E3 exit non-zero without an umbrella" $([ "$rc" -ne 0 ]; echo $?)

# E4. BACKLOG_CWD overrides the search.
layout e4; other="$root/e4/elsewhere"; mkdir -p "$other/backlog"; echo x > "$other/backlog/config.yml"
evid "TASK-7.1" BACKLOG_CWD="$other"
check "E4 BACKLOG_CWD wins" $(same umbrella "$other"; echo $?)
check "E4 next ADR in that umbrella is 01" $(echo "$out" | grep -qF '"next_adr":"01"'; echo $?)

# ---------- retro-commit.sh ----------
lessons() { # lessons <file> <run id8> [skip heading] [empty heading]
  { echo "# Retro — TASK-7.1"; echo
    echo "## Run $2 — 2026-10-07 — merged"; echo
    for h in "What happened" "Decisions" "What went well" "What dragged" "Lessons" "Actions"; do
      [ "$h" = "${3:-}" ] && continue
      echo "### $h"
      if [ "$h" = "${4:-}" ]; then echo; continue; fi
      if [ "$h" = Decisions ]; then echo "- [ADR-01: Use the port](adr/ADR-01-use-the-port.md)"; else echo "- something real"; fi
      echo
    done; } > "$1"
}
adr() { # adr <file> <title> [skip heading]
  { echo "# $2"; echo
    for h in Status Context Decision "Alternatives considered" Consequences Evidence; do
      [ "$h" = "${3:-}" ] && continue
      echo "## $h"; echo "Accepted text"; echo
    done; } > "$1"
}
umbrella() { # umbrella <name>: an umbrella clone on main with an origin, the retro README and a Backlog file
  local d="$root/$1"; mkdir -p "$d"
  git init -q --bare -b main "$d/origin.git"; git clone -q "$d/origin.git" "$d/u" 2>/dev/null
  u="$d/u"; git -C "$u" config user.name t; git -C "$u" config user.email t@t
  mkdir -p "$u/docs/retro" "$u/backlog"
  printf '# Retro\n\nFacts.\n\n<!-- retro-index:start -->\n<!-- retro-index:end -->\n' > "$u/docs/retro/README.md"
  echo task > "$u/backlog/task.md"
  git -C "$u" add -A; git -C "$u" commit -qm base; git -C "$u" push -q origin main
  f="$u/docs/retro/TASK-7.1"; mkdir -p "$f/adr"
}
commit_run() { out=$(bash "$commit" "$u" docs/retro/TASK-7.1 "$run_id" 2>&1); rc=$?; }
origin_head() { git --git-dir="$root/$1/origin.git" log -1 --format=%s main; }

# C1. A complete retro: committed and pushed, only docs/retro, index regenerated, other changes left alone.
umbrella c1; lessons "$f/lessons-learned.md" 73ef1981; adr "$f/adr/ADR-01-use-the-port.md" "ADR-01: Use the port"
echo changed >> "$u/backlog/task.md"
commit_run
check "C1 exit 0" $([ "$rc" -eq 0 ]; echo $?)
check "C1 pushed retro commit" $(origin_head c1 | grep -q '^retro: TASK-7.1 .*run 73ef1981'; echo $?)
check "C1 commit touches only docs/retro" $(! git --git-dir="$root/c1/origin.git" show --name-only --format= main | grep -qv '^docs/retro/'; echo $?)
check "C1 index links the lessons" $(has '[TASK-7.1](TASK-7.1/lessons-learned.md)' "$u/docs/retro/README.md"; echo $?)
check "C1 index links the ADR" $(has '[ADR-01: Use the port](TASK-7.1/adr/ADR-01-use-the-port.md)' "$u/docs/retro/README.md"; echo $?)
check "C1 index kept the leaf text" $(has 'Facts.' "$u/docs/retro/README.md"; echo $?)
check "C1 Backlog change not committed" $(git -C "$u" diff --quiet -- backlog/task.md; [ $? -eq 1 ]; echo $?)
check "C1 commit reported" $(echo "$out" | grep -qE '"commit":"[0-9a-f]{7,}"'; echo $?)

# C2..C7. Each broken retro fails and commits nothing.
broken() { # broken <name> <description>: asserts a failed run left origin main at base
  commit_run
  check "$1 exit non-zero ($2)" $([ "$rc" -ne 0 ]; echo $?)
  check "$1 nothing pushed ($2)" $([ "$(origin_head "$1")" = base ]; echo $?)
}
umbrella C2; lessons "$f/lessons-learned.md" 73ef1981 "What dragged"; adr "$f/adr/ADR-01-use-the-port.md" "ADR-01: Use the port"
broken C2 "a lessons section is missing"
umbrella C3; lessons "$f/lessons-learned.md" 73ef1981 "" "Lessons"; adr "$f/adr/ADR-01-use-the-port.md" "ADR-01: Use the port"
broken C3 "a lessons section is empty"
umbrella C4; lessons "$f/lessons-learned.md" 73ef1981; adr "$f/adr/ADR-01-use-the-port.md" "ADR-01: Use the port" "Alternatives considered"
broken C4 "an ADR section is missing"
umbrella C5; lessons "$f/lessons-learned.md" 73ef1981; adr "$f/adr/ADR-01-use-the-port.md" "ADR-01: Use the port"
adr "$f/adr/ADR-02-unlinked.md" "ADR-02: Unlinked"
broken C5 "an ADR is not linked from lessons-learned.md"
umbrella C6; lessons "$f/lessons-learned.md" 11111111; adr "$f/adr/ADR-01-use-the-port.md" "ADR-01: Use the port"
broken C6 "no section for this run"
umbrella C7; lessons "$f/lessons-learned.md" 73ef1981; adr "$f/adr/ADR-01-use-the-port.md" "ADR-01: Use the port"
git -C "$u" checkout -q -b side
broken C7 "the umbrella is not on main"

# C8. CRLF files written on Windows pass the same gate.
umbrella c8; lessons "$f/lessons-learned.md" 73ef1981; adr "$f/adr/ADR-01-use-the-port.md" "ADR-01: Use the port"
sed -i 's/$/\r/' "$f/lessons-learned.md" "$f/adr/ADR-01-use-the-port.md"
commit_run
check "C8 CRLF exit 0" $([ "$rc" -eq 0 ]; echo $?)

# C9. A push rejected because main moved on: rebased once and pushed.
umbrella c9; lessons "$f/lessons-learned.md" 73ef1981; adr "$f/adr/ADR-01-use-the-port.md" "ADR-01: Use the port"
git clone -q "$root/c9/origin.git" "$root/c9/other" 2>/dev/null
git -C "$root/c9/other" -c user.name=t -c user.email=t@t commit -q --allow-empty -m "backlog: elsewhere"
git -C "$root/c9/other" push -q origin main
commit_run
check "C9 exit 0 after a rebase" $([ "$rc" -eq 0 ]; echo $?)
check "C9 retro on top of the other commit" $(origin_head c9 | grep -q '^retro: TASK-7.1'; echo $?)

# C10. A stopped run's short section (cancelled or rejected) needs only What happened and Why it stopped.
short_lessons() { # short_lessons <file> <outcome> [skip heading]
  { echo "# Retro — TASK-7.1"; echo; echo "## Run 73ef1981 — 2026-10-07 — $2"; echo
    for h in "What happened" "Why it stopped"; do
      [ "$h" = "${3:-}" ] && continue
      echo "### $h"; echo "- something real"; echo
    done; } > "$1"
}
umbrella c10; short_lessons "$f/lessons-learned.md" cancelled
commit_run
check "C10 cancelled short section exit 0" $([ "$rc" -eq 0 ]; echo $?)
umbrella c10b; short_lessons "$f/lessons-learned.md" rejected
commit_run
check "C10 rejected short section exit 0" $([ "$rc" -eq 0 ]; echo $?)
umbrella C11; short_lessons "$f/lessons-learned.md" cancelled "Why it stopped"
broken C11 "a stopped run without Why it stopped"
umbrella C12; short_lessons "$f/lessons-learned.md" merged
broken C12 "a merged run with only the short section"

echo "retro: $pass passed, $fail failed"
[ "$fail" -eq 0 ]
