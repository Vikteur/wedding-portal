#!/usr/bin/env bash
# Tests for .archon/scripts/stop-run.sh and retro-stopped.sh (TASK-34): a run that archon ends outright (a rejected
# approval gate, a cancel, an abandon) never reaches the retro node, so these scripts write down why it stopped.
# Runs against throwaway umbrella clones and a stubbed archon that answers from fixture files.
# Usage: bash .archon/scripts/test/retro-stopped.test.sh   (exit 0 = all pass)
set -uo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
stopped="$here/retro-stopped.sh"
stop="$here/stop-run.sh"
pass=0; fail=0
root="$(mktemp -d)"
export ARCHON_HOME="$root/no-archon-home"   # the real ~/.archon is never read
trap 'rm -rf "$root"' EXIT

check() { # check <name> <condition exit code>
  if [ "$2" -eq 0 ]; then pass=$((pass+1)); else fail=$((fail+1)); echo "FAIL: $1"; echo "$out" | sed 's/^/    /'; fi
}
has() { grep -qF -- "$1" "$2"; }
native() { (cd "$1" && { pwd -W 2>/dev/null || pwd; }); } # the path node sees (C:/... under Git Bash)

# The stubbed archon answers from $STUB/<run id>.get.json and .jsonl and $STUB/runs.json, and logs every cancel,
# reject and abandon to $STUB/calls. A cancel or reject ends the run as archon does: status cancelled, and a reject
# also writes its gate_decision event to the transcript.
mkdir -p "$root/bin"
cat > "$root/bin/archon" <<'STUB'
#!/usr/bin/env bash
end_run() { sed -i 's/"status":"\(running\|paused\)"/"status":"cancelled"/' "$STUB/$1.get.json"; }
case "$1 $2" in
  "workflow get") cat "$STUB/$3.get.json" 2>/dev/null || { echo "Run not found: $3" >&2; exit 1; } ;;
  "workflow logs") cat "$STUB/$3.jsonl" 2>/dev/null; exit 0 ;;
  "workflow runs") [ -f "$STUB/runs.fail" ] && exit 1; cat "$STUB/runs.json" ;;
  "workflow cancel"|"workflow abandon") echo "$2 $3" >> "$STUB/calls"; end_run "$3" ;;
  "workflow reject")
    echo "reject $3 $4 $5" >> "$STUB/calls"; end_run "$3"
    printf '{"type":"gate_decision","step":"approve","decision":"rejected","content":"%s"}\n' "$5" >> "$STUB/$3.jsonl" ;;
  *) exit 1 ;;
esac
STUB
chmod +x "$root/bin/archon"

R1=aaaaaaaa-1111-4111-8111-111111111111 # rejected at the approval gate, superseded by R7
R2=bbbbbbbb-2222-4222-8222-222222222222 # cancelled with a reason recorded by stop-run.sh
R3=cccccccc-3333-4333-8333-333333333333 # cancelled outside stop-run.sh, no ticket in the request
R4=eeeeeeee-4444-4444-8444-444444444444 # cancelled and already documented
R5=ffffffff-5555-4555-8555-555555555555 # cancelled, another workflow
R6=99999999-6666-4666-8666-666666666666 # completed
R7=dddddddd-7777-4777-8777-777777777777 # the run that adopted R1

# get <id> <status> <request> <workflow> <nodes json> [approval node]: a fixture `archon workflow get --json`
get() {
  local approval=""
  [ -n "${6:-}" ] && approval=",\"approval\":{\"nodeId\":\"$6\",\"type\":\"approval\"}"
  local term=null
  case "$2" in cancelled|completed|failed)
    term="{\"run_id\":\"$1\",\"status\":\"$2\",\"nodes\":$5,\"artifacts\":{\"root\":\"$ws/artifacts/runs/$1\",\"files\":[]}}" ;;
  esac
  printf '{"id":"%s","workflow_name":"%s","user_message":"%s","status":"%s","adopted_from_run_id":null,' "$1" "$4" "$3" "$2"
  printf '"started_at":"2026-10-01T09:00:00.000Z","completed_at":%s,"last_activity_at":"2026-10-01T09:20:00.000Z",' \
    "$([ "$term" = null ] && echo null || echo '"2026-10-01T09:21:00.000Z"')"
  printf '"output_root":"%s","metadata":{"dispatch":{"base_branch":"main"}%s},"terminal_record":%s}\n' "$ws" "$approval" "$term"
}
nodes_gate='[{"node_id":"ticket","state":"completed"},{"node_id":"plan","state":"completed"},{"node_id":"approve","state":"running"},{"node_id":"implement","state":"pending"}]'
nodes_plan='[{"node_id":"ticket","state":"completed"},{"node_id":"plan","state":"running"},{"node_id":"approve","state":"pending"}]'
transcript() { # transcript <id> <request> [through approve]
  printf '{"type":"workflow_start","workflow_name":"build-feature","content":"%s","workflow_id":"%s"}\n' "$2" "$1"
  echo '{"type":"node_start","step":"ticket","execution":{"node":{"id":"ticket","kind":"exec"}}}'
  echo '{"type":"node_complete","step":"ticket","duration_ms":2000}'
  echo '{"type":"node_start","step":"plan","execution":{"node":{"id":"plan","kind":"agent"}}}'
  if [ -n "${3:-}" ]; then
    echo '{"type":"node_complete","step":"plan","duration_ms":300000}'
    echo '{"type":"node_start","step":"approve","execution":{"node":{"id":"approve","kind":"gate"}}}'
    echo '{"type":"node_suspended","step":"approve"}'
  fi
}

# fixtures <name>: a stub dir with the seven runs, an umbrella clone on main with an origin; sets STUB ws u f
fixtures() {
  local d="$root/$1"; mkdir -p "$d/stub" "$d/ws/artifacts/runs"
  STUB="$d/stub"; ws=$(native "$d/ws")
  get "$R1" cancelled "Build TASK-7.1" build-feature "$nodes_gate" approve > "$STUB/$R1.get.json"
  transcript "$R1" "Build TASK-7.1" through > "$STUB/$R1.jsonl"
  echo '{"type":"gate_decision","step":"approve","decision":"rejected","content":"the plan skips the migration test"}' >> "$STUB/$R1.jsonl"
  get "$R2" cancelled "Build TASK-7.1" build-feature "$nodes_plan" > "$STUB/$R2.get.json"
  transcript "$R2" "Build TASK-7.1" > "$STUB/$R2.jsonl"
  mkdir -p "$d/ws/artifacts/runs/$R2"
  printf '{"reason":"wrong base branch","action":"cancel","status":"running","at":"2026-10-01T09:20:30.000Z"}\n' \
    > "$d/ws/artifacts/runs/$R2/stop-reason.json"
  get "$R3" cancelled "fix the typo" build-feature "$nodes_plan" > "$STUB/$R3.get.json"
  transcript "$R3" "fix the typo" > "$STUB/$R3.jsonl"
  get "$R4" cancelled "Build TASK-7.1" build-feature "$nodes_plan" > "$STUB/$R4.get.json"
  get "$R5" cancelled "Build TASK-7.1" probe "$nodes_plan" > "$STUB/$R5.get.json"
  get "$R6" completed "Build TASK-7.1" build-feature "$nodes_plan" > "$STUB/$R6.get.json"
  get "$R7" completed "Build TASK-7.1" build-feature "$nodes_plan" > "$STUB/$R7.get.json"
  { printf '{"runs":['
    # Newest first, as archon lists them; the last field is the day of September the run started.
    for r in "$R7:completed:build-feature:\"$R1\":11" "$R1:cancelled:build-feature:null:10" \
             "$R3:cancelled:build-feature:null:07" "$R6:completed:build-feature:null:06" \
             "$R2:cancelled:build-feature:null:05" "$R5:cancelled:probe:null:02" "$R4:cancelled:build-feature:null:01"; do
      IFS=: read -r id st wf from day <<< "$r"
      printf '{"id":"%s","status":"%s","workflow_name":"%s","adopted_from_run_id":%s,"started_at":"2026-09-%sT09:00:00.000Z"},' \
        "$id" "$st" "$wf" "$from" "$day"
    done | sed 's/,$//'
    printf '],"total":7}\n'; } > "$STUB/runs.json"

  git init -q --bare -b main "$d/origin.git"; git clone -q "$d/origin.git" "$d/u" 2>/dev/null
  u="$d/u"; git -C "$u" config user.name t; git -C "$u" config user.email t@t
  mkdir -p "$u/docs/retro/TASK-7.1" "$u/backlog"
  printf '# Retro\n\nFacts.\n\n<!-- retro-index:start -->\n<!-- retro-index:end -->\n' > "$u/docs/retro/README.md"
  echo 'project_name: "t"' > "$u/backlog/config.yml"
  f="$u/docs/retro/TASK-7.1"
  printf '# Retro — TASK-7.1\n\n## Run eeeeeeee — 2026-09-30 — cancelled\n\n### What happened\n- Stopped at plan.\n\n### Why it stopped\nTold so.\n' \
    > "$f/lessons-learned.md"
  git -C "$u" add -A 2>/dev/null; git -C "$u" commit -qm base; git -C "$u" push -q origin main
}
sh_() { # sh_ <script> <args...>: runs it from the umbrella with the stub first on PATH; sets out rc
  local s=$1; shift
  out=$( (cd "$u" && env PATH="$root/bin:$PATH" STUB="$STUB" bash "$s" "$@") 2>&1 ); rc=$?
}
origin_log() { git --git-dir="$root/$1/origin.git" log --format=%s main; }
pushed() { git --git-dir="$root/$1/origin.git" show "main:$2"; } # pushed <name> <path>: the file as pushed
section() { # section <file> <id8>: the run's section, up to the next run
  awk -v h="## Run $2 " 'index($0, h) == 1 { on = 1; print; next } /^## / { on = 0 } on' "$1"
}

# ---------- retro-stopped.sh <run id> ----------
# S1. A run rejected at the approval gate: its section says where, why, and which run took over; committed and pushed.
fixtures s1; sh_ "$stopped" "$R1"
check "S1 exit 0" $([ "$rc" -eq 0 ]; echo $?)
pushed s1 docs/retro/TASK-7.1/lessons-learned.md > "$root/s1.md" 2>/dev/null
section "$root/s1.md" aaaaaaaa > "$root/s1.sec"
check "S1 section header with the date and outcome" $(has '## Run aaaaaaaa — 2026-10-01 — rejected' "$root/s1.sec"; echo $?)
check "S1 has What happened" $(has '### What happened' "$root/s1.sec"; echo $?)
check "S1 has Why it stopped" $(has '### Why it stopped' "$root/s1.sec"; echo $?)
check "S1 names the request" $(has 'Build TASK-7.1' "$root/s1.sec"; echo $?)
check "S1 names where it stopped" $(has 'Stopped at: approve (approval gate)' "$root/s1.sec"; echo $?)
check "S1 gives the rejection reason" $(has 'the plan skips the migration test' "$root/s1.sec"; echo $?)
check "S1 names the run that adopted it" $(has 'Superseded by run dddddddd' "$root/s1.sec"; echo $?)
check "S1 node timeline" $(has '- plan: completed (300s)' "$root/s1.sec"; echo $?)
check "S1 earlier section kept" $(has '## Run eeeeeeee' "$root/s1.md"; echo $?)
# The subject is captured first: under pipefail, `git log | head -1 | grep` flakes when git's next write hits the
# closed pipe (SIGPIPE, exit 141) after head has already exited (TASK-38, seen in CI).
check "S1 pushed as a retro commit" $(grep -q '^retro: TASK-7.1 .*run aaaaaaaa' <<< "$(origin_log s1 | sed -n 1p)"; echo $?)
check "S1 index regenerated" $(pushed s1 docs/retro/README.md | grep -qF '[TASK-7.1](TASK-7.1/lessons-learned.md) — last: Run aaaaaaaa'; echo $?)

# S2. A run that did not stop (completed) is refused and nothing is written.
fixtures s2; sh_ "$stopped" "$R6"
check "S2 completed run refused" $([ "$rc" -ne 0 ]; echo $?)
check "S2 nothing pushed" $([ "$(origin_log s2 | head -1)" = base ]; echo $?)
check "S2 lessons untouched" $(git -C "$u" diff --quiet; echo $?)

# S3. One run asked for again: its stopped-run section is rewritten in place, never added twice.
fixtures s3; sh_ "$stopped" "$R4"
check "S3 exit 0" $([ "$rc" -eq 0 ]; echo $?)
pushed s3 docs/retro/TASK-7.1/lessons-learned.md > "$root/s3.md" 2>/dev/null
check "S3 still one section" $([ "$(grep -c '^## Run eeeeeeee' "$root/s3.md")" -eq 1 ]; echo $?)
check "S3 section rewritten from the evidence" $(! has 'Told so.' "$root/s3.md" && has 'No reason was recorded' "$root/s3.md"; echo $?)
check "S3 title kept" $(head -1 "$root/s3.md" | grep -qF '# Retro — TASK-7.1'; echo $?)

# S3b. A run that already has a full retro section (any other outcome) is left alone.
fixtures s3b
printf '\n## Run bbbbbbbb — 2026-10-01 — failed\n\n### What happened\n- written by the retro agent\n' >> "$f/lessons-learned.md"
git -C "$u" commit -qam "full retro"; git -C "$u" push -q origin main
sh_ "$stopped" "$R2"
check "S3b exit 0" $([ "$rc" -eq 0 ]; echo $?)
check "S3b nothing pushed" $([ "$(origin_log s3b | head -1)" = "full retro" ]; echo $?)
check "S3b full section kept" $(has 'written by the retro agent' "$f/lessons-learned.md" && git -C "$u" diff --quiet; echo $?)

# ---------- retro-stopped.sh --sweep ----------
# S4. Every stopped build-feature run without a section gets one; other workflows and finished runs are left out.
fixtures s4; sh_ "$stopped" --sweep
check "S4 exit 0" $([ "$rc" -eq 0 ]; echo $?)
pushed s4 docs/retro/TASK-7.1/lessons-learned.md > "$root/s4.md" 2>/dev/null
section "$root/s4.md" bbbbbbbb > "$root/s4b.sec"
check "S4 rejected run documented" $(has '## Run aaaaaaaa — 2026-10-01 — rejected' "$root/s4.md"; echo $?)
check "S4 cancelled run documented" $(has '## Run bbbbbbbb — 2026-10-01 — cancelled' "$root/s4b.sec"; echo $?)
check "S4 reason recorded by stop-run.sh" $(has 'wrong base branch' "$root/s4b.sec"; echo $?)
check "S4 cancelled at the running node" $(has 'Stopped at: plan' "$root/s4b.sec"; echo $?)
pushed s4 docs/retro/run-cccccccc/lessons-learned.md > "$root/s4c.md" 2>/dev/null
check "S4 run without a ticket in its run folder" $(has '## Run cccccccc — 2026-10-01 — cancelled' "$root/s4c.md"; echo $?)
check "S4 run folder file has its title" $(head -1 "$root/s4c.md" | grep -qF '# Retro — run-cccccccc'; echo $?)
check "S4 missing reason said so" $(has 'No reason was recorded' "$root/s4c.md"; echo $?)
check "S4 oldest run first, as in the template" $(awk '/^## Run bbbbbbbb/{b=NR} /^## Run aaaaaaaa/{a=NR} END{exit !(b && a && b < a)}' "$root/s4.md"; echo $?)
check "S4 documented run not repeated" $([ "$(grep -c '^## Run eeeeeeee' "$root/s4.md")" -eq 1 ]; echo $?)
check "S4 other workflow left out" $(! grep -rqF 'ffffffff' "$u/docs/retro"; echo $?)
check "S4 completed run left out" $(! grep -rqF '99999999' "$u/docs/retro"; echo $?)
check "S4 one retro commit per run" $([ "$(origin_log s4 | grep -c '^retro: ')" -eq 3 ]; echo $?)

# S5. A second sweep finds nothing new.
sh_ "$stopped" --sweep
check "S5 exit 0" $([ "$rc" -eq 0 ]; echo $?)
check "S5 no new commit" $([ "$(origin_log s4 | grep -c '^retro: ')" -eq 3 ]; echo $?)

# S6. The sweep never fails its caller: umbrella off main, or archon unavailable, is a warning.
fixtures s6; git -C "$u" checkout -q -b side; sh_ "$stopped" --sweep
check "S6 off main: exit 0" $([ "$rc" -eq 0 ]; echo $?)
check "S6 off main: warns" $(echo "$out" | grep -q 'not on main'; echo $?)
check "S6 off main: nothing written" $(git -C "$u" diff --quiet && [ ! -e "$u/docs/retro/run-cccccccc" ]; echo $?)
fixtures s6b; touch "$STUB/runs.fail"; sh_ "$stopped" --sweep
check "S6 archon unavailable: exit 0" $([ "$rc" -eq 0 ]; echo $?)

# ---------- stop-run.sh <run id> <reason> ----------
running() { # running <id> <status> [approval node]: replaces the fixture with a live run of that status
  get "$1" "$2" "Build TASK-7.1" build-feature "$nodes_plan" ${3:-} > "$STUB/$1.get.json"
  transcript "$1" "Build TASK-7.1" ${3:+through} > "$STUB/$1.jsonl"
}
L1=12345678-aaaa-4aaa-8aaa-aaaaaaaaaaaa

# T1. A reason is required: nothing is stopped without one.
fixtures t1; running "$L1" running; sh_ "$stop" "$L1"
check "T1 no reason refused" $([ "$rc" -ne 0 ]; echo $?)
check "T1 nothing stopped" $([ ! -s "$STUB/calls" ]; echo $?)
sh_ "$stop" "$L1" "   "
check "T1 blank reason refused" $([ "$rc" -ne 0 ] && [ ! -s "$STUB/calls" ]; echo $?)

# T2. A running run is cancelled, its reason kept beside its artifacts, and documented.
fixtures t2; running "$L1" running; sh_ "$stop" "$L1" "the ticket was split; TASK-7.2 replaces it"
check "T2 exit 0" $([ "$rc" -eq 0 ]; echo $?)
check "T2 cancelled through archon" $(grep -qx "cancel $L1" "$STUB/calls"; echo $?)
check "T2 reason kept in the run's artifacts" $(has 'the ticket was split; TASK-7.2 replaces it' "$root/t2/ws/artifacts/runs/$L1/stop-reason.json"; echo $?)
pushed t2 docs/retro/TASK-7.1/lessons-learned.md > "$root/t2.md" 2>/dev/null
section "$root/t2.md" 12345678 > "$root/t2.sec"
check "T2 documented as cancelled" $(grep -qE '^## Run 12345678 — [0-9]{4}-[0-9]{2}-[0-9]{2} — cancelled$' "$root/t2.sec"; echo $?)
check "T2 documented with the reason" $(has 'the ticket was split; TASK-7.2 replaces it' "$root/t2.sec"; echo $?)

# T3. A run paused at an approval gate is rejected with the reason, which archon also keeps in the transcript.
fixtures t3; running "$L1" paused approve; sh_ "$stop" "$L1" "plan misses the rollback"
check "T3 exit 0" $([ "$rc" -eq 0 ]; echo $?)
check "T3 rejected with the reason" $(grep -qx "reject $L1 --reason plan misses the rollback" "$STUB/calls"; echo $?)
check "T3 not cancelled" $(! grep -q '^cancel' "$STUB/calls"; echo $?)
pushed t3 docs/retro/TASK-7.1/lessons-learned.md > "$root/t3.md" 2>/dev/null
section "$root/t3.md" 12345678 > "$root/t3.sec"
check "T3 documented as rejected" $(grep -qE '^## Run 12345678 — .* — rejected$' "$root/t3.sec"; echo $?)
check "T3 reason given once" $([ "$(grep -c 'plan misses the rollback' "$root/t3.sec")" -eq 1 ]; echo $?)

# T4. A finished run is not stopped again.
fixtures t4; sh_ "$stop" "$R6" "too late"
check "T4 completed run refused" $([ "$rc" -ne 0 ] && [ ! -s "$STUB/calls" ]; echo $?)

# T5. A run already cancelled without a reason gets one after the fact.
fixtures t5; sh_ "$stop" "$R3" "duplicate of TASK-7.1"
check "T5 exit 0" $([ "$rc" -eq 0 ]; echo $?)
check "T5 not cancelled again" $([ ! -s "$STUB/calls" ]; echo $?)
pushed t5 docs/retro/run-cccccccc/lessons-learned.md > "$root/t5.md" 2>/dev/null
check "T5 documented with the late reason" $(has 'duplicate of TASK-7.1' "$root/t5.md"; echo $?)
check "T5 no missing-reason line" $(! has 'No reason was recorded' "$root/t5.md"; echo $?)

# T6. The umbrella off main: the run is still stopped and the reason kept, the failure is reported.
fixtures t6; running "$L1" running; git -C "$u" checkout -q -b side; sh_ "$stop" "$L1" "kept for the sweep"
check "T6 exit non-zero" $([ "$rc" -ne 0 ]; echo $?)
check "T6 still cancelled" $(grep -qx "cancel $L1" "$STUB/calls"; echo $?)
check "T6 reason kept for the sweep" $(has 'kept for the sweep' "$root/t6/ws/artifacts/runs/$L1/stop-reason.json"; echo $?)

# T7. A run the sweep documented without a reason gets its reason later: the section is rewritten, not repeated.
fixtures t7; sh_ "$stopped" --sweep; sh_ "$stop" "$R3" "superseded by a smaller ticket"
check "T7 exit 0" $([ "$rc" -eq 0 ]; echo $?)
pushed t7 docs/retro/run-cccccccc/lessons-learned.md > "$root/t7.md" 2>/dev/null
check "T7 one section" $([ "$(grep -c '^## Run cccccccc' "$root/t7.md")" -eq 1 ]; echo $?)
check "T7 reason added" $(has 'superseded by a smaller ticket' "$root/t7.md" && ! has 'No reason was recorded' "$root/t7.md"; echo $?)

# T8. The stopped-run sweep also archives every finished Archon run (archive-runs.sh), once, at the end.
fixtures t8
mkdir -p "$root/ah8/workspaces/Vikteur/weddingapp/logs"
printf '%s
' '{"type":"workflow_start","workflow_name":"build-feature","content":"TASK-7.1: x","ts":"2026-10-09T10:00:00Z"}' '{"type":"workflow_complete","ts":"2026-10-09T11:00:00Z"}' > "$root/ah8/workspaces/Vikteur/weddingapp/logs/aaaabbbbcccc.jsonl"
ARCHON_HOME="$root/ah8" sh_ "$stopped" --sweep
check "T8 exit 0" $([ "$rc" -eq 0 ]; echo $?)
check "T8 the finished run is archived" $(git --git-dir="$root/t8/origin.git" cat-file -e main:docs/retro/TASK-7.1/runs/aaaabbbb/run.json 2>/dev/null; echo $?)

echo "retro-stopped: $pass passed, $fail failed"
[ "$fail" -eq 0 ]
