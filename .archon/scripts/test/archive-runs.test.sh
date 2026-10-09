#!/usr/bin/env bash
# Tests for .archon/scripts/archive-runs.sh against a fake ARCHON_HOME, a throwaway umbrella with an origin, and stubbed
# gh and archon. The real ~/.archon is never read.
# Usage: bash .archon/scripts/test/archive-runs.test.sh   (exit 0 = all pass)
set -uo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
sweep="$here/archive-runs.sh"
pass=0; fail=0
root="$(mktemp -d)"
trap 'rm -rf "$root"' EXIT
out=""

check() { # check <name> <condition exit code>
  if [ "$2" -eq 0 ]; then pass=$((pass+1)); else fail=$((fail+1)); echo "FAIL: $1"; echo "$out" | sed 's/^/    /'; fi
}

# Stubs: gh answers `pr view <n> --repo Vikteur/<repo>` from $STUB_GH/pr-<repo>-<n>.json (else fails); archon answers
# `workflow runs` from $STUB_ARCHON_RUNS (else fails).
mkdir -p "$root/bin" "$root/gh"
cat > "$root/bin/gh" <<'STUB'
#!/usr/bin/env bash
[ "$1 $2" = "pr view" ] || exit 1
n=$3; repo=${5#*/}
f="$STUB_GH/pr-$repo-$n.json"
[ -f "$f" ] && cat "$f" || exit 1
STUB
cat > "$root/bin/archon" <<'STUB'
#!/usr/bin/env bash
[ "$1 $2" = "workflow runs" ] && [ -f "${STUB_ARCHON_RUNS:-}" ] && cat "$STUB_ARCHON_RUNS" || exit 1
STUB
chmod +x "$root/bin/gh" "$root/bin/archon"
export PATH="$root/bin:$PATH" STUB_GH="$root/gh"
unset STUB_ARCHON_RUNS

inorigin() { git --git-dir="$root/$1/origin.git" cat-file -e "main:$2" 2>/dev/null; }
origin_head() { git --git-dir="$root/$1/origin.git" log -1 --format=%s main; }
origin_files() { git --git-dir="$root/$1/origin.git" ls-tree -r --name-only main; }
origin_json() { # origin_json <name> <path> <field>
  git --git-dir="$root/$1/origin.git" show "main:$2" |
    node -e 'let s="";process.stdin.on("data",d=>s+=d).on("end",()=>console.log(JSON.parse(s)[process.argv[1]]))' "$3"
}

setup() { # setup <name>: an umbrella on main with an origin, and an empty fake archon home in $AH
  local d="$root/$1"; mkdir -p "$d"
  git init -q --bare -b main "$d/origin.git"; git clone -q "$d/origin.git" "$d/u" 2>/dev/null
  u="$d/u"; git -C "$u" config user.name t; git -C "$u" config user.email t@t
  mkdir -p "$u/docs/retro"; echo x > "$u/docs/retro/README.md"
  git -C "$u" add -A; git -C "$u" commit -qm base; git -C "$u" push -q origin main
  AH="$d/archon"; export ARCHON_HOME="$AH"
  mkdir -p "$AH/workspaces/Vikteur"
}
mkrun() { # mkrun <repo> <run id> <workflow> <content> <terminal event type or ->: a log, plus the artifacts folder
  local l="$AH/workspaces/Vikteur/$1/logs"
  mkdir -p "$l"
  node -e 'const [wf, c, t] = process.argv.slice(1);
    const o = [{ type: "workflow_start", workflow_name: wf, content: c, ts: "2026-10-09T10:00:00.000Z" },
      { type: "node_start", step: "a", ts: "2026-10-09T10:01:00.000Z" }];
    if (t !== "-") o.push({ type: t, ts: "2026-10-09T11:00:00.000Z" });
    console.log(o.map(e => JSON.stringify(e)).join("\n"));' "$3" "$4" "$5" > "$l/$2.jsonl"
  mkdir -p "$AH/workspaces/Vikteur/$1/artifacts/runs/$2"
}
runfolder() { # runfolder <repo> <run id>: key files, logs at depth, review files, retro files and dotfiles
  local r="$AH/workspaces/Vikteur/$1/artifacts/runs/$2"
  mkdir -p "$r/review" "$r/retro" "$r/red" "$r/mutations" "$r/.archon/typed-artifacts" "$r/.archon/checkout"
  for n in plan pr-body ci-diagnosis retro-evidence other; do echo "$n" > "$r/$n.md"; done
  echo log > "$r/green.log"; echo log > "$r/red/t1.log"; echo log > "$r/mutations/m1.log"; echo txt > "$r/red/notes.txt"
  for n in consolidated-review fix-report scope extra; do echo "$n" > "$r/review/$n.md"; done
  for n in transcript.jsonl runs.json meta.json commits.txt pr.json artifacts.txt other.bin; do echo "$n" > "$r/retro/$n"; done
  echo secret > "$r/.archon/typed-artifacts/a.json"; echo secret > "$r/.archon/checkout/b"; echo hidden > "$r/.hidden.log"
}
ticketmd() { printf 'File: x\n\nTask %s - Something\n' "$3" > "$AH/workspaces/Vikteur/$1/artifacts/runs/$2/ticket.md"; }
prjson() { # prjson <repo> <n> <branch> <title> [body]
  node -e 'const [b, t, y] = process.argv.slice(1);
    console.log(JSON.stringify({ headRefName: b, title: t, body: y }));' "$3" "$4" "${5:-}" > "$root/gh/pr-$1-$2.json"
}
run_sweep() { out=$(bash "$sweep" "$u" 2>"$root/stderr.txt"); rc=$?; json=$(echo "$out" | tail -1); out="$out
$(cat "$root/stderr.txt")"; }
njson() { echo "$json" | node -e 'let s="";process.stdin.on("data",d=>s+=d).on("end",()=>{const j=JSON.parse(s);process.exit(eval(process.argv[1])?0:1)})' "$1"; }

B1=11111111-aaaa-bbbb-cccc-000000000001   # build run, ticket.md
R1=22222222aaaabbbbccccdddd00000002       # review run, .pr-number
R2=33333333aaaabbbbccccdddd00000003       # review run, PR only in the workflow_start content
U1=44444444aaaabbbbccccdddd00000004       # nothing links it
P1=55555555aaaabbbbccccdddd00000005       # still running
F1=66666666aaaabbbbccccdddd00000006       # failed
C1=77777777aaaabbbbccccdddd00000007       # cancelled: no terminal event in the log, archon says so

# S1..S9. One sweep over all kinds of runs.
setup s1
mkrun wedding-portal $B1 build-feature "TASK-7.3: do it" workflow_complete; runfolder wedding-portal $B1; ticketmd wedding-portal $B1 task-7.3
mkrun weddingapp $R1 archon-comprehensive-pr-review "Review PR #46" workflow_complete; runfolder weddingapp $R1
echo 46 > "$AH/workspaces/Vikteur/weddingapp/artifacts/runs/$R1/.pr-number"
prjson weddingapp 46 feat/task-7.1-the-thing "TASK-7.1 the thing"
mkrun rekord-contract $R2 archon-comprehensive-pr-review "Review pull request PR #9 of the contract" workflow_complete
prjson rekord-contract 9 fix/something "chore: tidy (TASK-12.2)"
mkrun weddingapp $U1 archon-comprehensive-pr-review "Review something with no number" workflow_complete; runfolder weddingapp $U1
mkrun weddingapp $P1 build-feature "TASK-9.9: still going" -; runfolder weddingapp $P1; ticketmd weddingapp $P1 task-9.9
mkrun weddingapp $F1 build-feature "TASK-8.1: broke" workflow_error; ticketmd weddingapp $F1 task-8.1
mkrun weddingapp $C1 build-feature "TASK-8.2: stopped" -; ticketmd weddingapp $C1 task-8.2
echo "{\"runs\":[{\"id\":\"$C1\",\"status\":\"cancelled\"},{\"id\":\"$P1\",\"status\":\"running\"}]}" > "$root/archon-runs.json"
export STUB_ARCHON_RUNS="$root/archon-runs.json"
mkdir -p "$AH/workspaces/Vikteur/artifacts/runs" "$AH/workspaces/Vikteur/weddingapp/artifacts/runs/88888888nolog"   # a stray folder and a run without a log
run_sweep
d1=docs/retro/TASK-7.3/runs/11111111
check "S1 exit 0" $([ "$rc" -eq 0 ]; echo $?)
check "S1 one JSON line with the commit and the six archived runs" $(njson 'j.commit.length >= 7 && j.archived.length === 6'; echo $?)
check "S1 commit message" $(origin_head s1 | grep -q '^retro: archive'; echo $?)
for fl in ticket.md plan.md pr-body.md ci-diagnosis.md retro-evidence.md green.log red/t1.log mutations/m1.log \
  review/consolidated-review.md retro/transcript.jsonl retro/meta.json transcript.jsonl run.json; do
  check "S1 build run: $fl" $(inorigin s1 "$d1/$fl"; echo $?)
done
for fl in other.md red/notes.txt review/extra.md retro/other.bin .hidden.log .archon/typed-artifacts/a.json .archon/checkout/b; do
  check "S1 not copied: $fl" $(! inorigin s1 "$d1/$fl"; echo $?)
done
check "S1 transcript.jsonl is the agent log" $(git --git-dir="$root/s1/origin.git" show "main:$d1/transcript.jsonl" | head -1 | grep -q '"workflow_name":"build-feature"'; echo $?)
check "S1 manifest: run id" $([ "$(origin_json s1 "$d1/run.json" run)" = "$B1" ]; echo $?)
check "S1 manifest: repo" $([ "$(origin_json s1 "$d1/run.json" repo)" = wedding-portal ]; echo $?)
check "S1 manifest: workflow" $([ "$(origin_json s1 "$d1/run.json" workflow)" = build-feature ]; echo $?)
check "S1 manifest: state" $([ "$(origin_json s1 "$d1/run.json" state)" = completed ]; echo $?)
check "S1 manifest: ticket" $([ "$(origin_json s1 "$d1/run.json" ticket)" = TASK-7.3 ]; echo $?)
check "S1 manifest: rule" $([ "$(origin_json s1 "$d1/run.json" rule)" = ticket.md ]; echo $?)
check "S1 manifest: start" $([ "$(origin_json s1 "$d1/run.json" started)" = 2026-10-09T10:00:00.000Z ]; echo $?)
check "S1 manifest: end" $([ "$(origin_json s1 "$d1/run.json" ended)" = 2026-10-09T11:00:00.000Z ]; echo $?)
d2=docs/retro/TASK-7.1/runs/22222222
check "S2 review run linked through its PR to the branch's task" $(inorigin s1 "$d2/run.json"; echo $?)
check "S2 manifest: rule and PR" $([ "$(origin_json s1 "$d2/run.json" rule)" = pr ] && [ "$(origin_json s1 "$d2/run.json" pr)" = 46 ]; echo $?)
check "S2 its files are copied" $(inorigin s1 "$d2/review/consolidated-review.md"; echo $?)
check "S3 review run with the PR only in the content, task from the title" $(inorigin s1 docs/retro/TASK-12.2/runs/33333333/run.json; echo $?)
check "S3 no artifact files: transcript still copied" $(inorigin s1 docs/retro/TASK-12.2/runs/33333333/transcript.jsonl; echo $?)
du=docs/retro/unlinked/runs/44444444
check "S4 unlinked run under docs/retro/unlinked" $(inorigin s1 "$du/run.json"; echo $?)
check "S4 manifest: rule unlinked" $([ "$(origin_json s1 "$du/run.json" rule)" = unlinked ]; echo $?)
check "S5 running run is skipped" $(! origin_files s1 | grep -q '55555555'; echo $?)
check "S6 failed run archived with its state" $([ "$(origin_json s1 docs/retro/TASK-8.1/runs/66666666/run.json state)" = failed ]; echo $?)
check "S7 cancelled run (state from archon) archived" $([ "$(origin_json s1 docs/retro/TASK-8.2/runs/77777777/run.json state)" = cancelled ]; echo $?)
check "S8 a run without a log is skipped" $(! origin_files s1 | grep -q '88888888'; echo $?)
check "S9 nothing but runs folders is committed" $(! git --git-dir="$root/s1/origin.git" show --name-only --format= main | grep -qvE '^docs/retro/[^/]+/runs/'; echo $?)
check "S9 no dotfile anywhere" $(! origin_files s1 | grep -qE '(^|/)\.'; echo $?)

# S10. Idempotent: a second sweep archives nothing and commits nothing.
before=$(git --git-dir="$root/s1/origin.git" rev-parse main)
run_sweep
check "S10 exit 0" $([ "$rc" -eq 0 ]; echo $?)
check "S10 nothing archived" $(echo "$json" | grep -q '"archived":\[\]'; echo $?)
check "S10 nothing pushed" $([ "$(git --git-dir="$root/s1/origin.git" rev-parse main)" = "$before" ]; echo $?)
check "S10 empty commit field" $(echo "$json" | grep -q '"commit":""'; echo $?)

# S11. A run that finishes later is archived on the next sweep.
mkrun weddingapp $P1 build-feature "TASK-9.9: still going" workflow_complete
echo "{\"runs\":[{\"id\":\"$C1\",\"status\":\"cancelled\"}]}" > "$root/archon-runs.json"
run_sweep
check "S11 the finished run is archived" $(inorigin s1 docs/retro/TASK-9.9/runs/55555555/run.json; echo $?)
check "S11 only that run" $(njson 'j.archived.length === 1'; echo $?)

# S12. A run that was unlinked is moved when its ticket becomes known.
setup s12
mkrun weddingapp $R1 archon-comprehensive-pr-review "Review PR #46" workflow_complete; runfolder weddingapp $R1
rm -f "$root/gh/pr-weddingapp-46.json"
run_sweep
check "S12 first unlinked" $(inorigin s12 docs/retro/unlinked/runs/22222222/run.json; echo $?)
prjson weddingapp 46 feat/task-7.1-the-thing "x"
run_sweep
check "S12 now linked" $(inorigin s12 docs/retro/TASK-7.1/runs/22222222/run.json; echo $?)
check "S12 no longer under unlinked" $(! origin_files s12 | grep -q '^docs/retro/unlinked/'; echo $?)

# S13. Not on main: nothing is committed.
setup s13; mkrun weddingapp $B1 build-feature "TASK-7.3: x" workflow_complete; ticketmd weddingapp $B1 task-7.3
git -C "$u" checkout -q -b side
run_sweep
check "S13 exit non-zero" $([ "$rc" -ne 0 ]; echo $?)
check "S13 nothing pushed" $([ "$(origin_head s13)" = base ]; echo $?)

# S14. A push rejected because main moved on: rebased once and pushed.
setup s14; mkrun weddingapp $B1 build-feature "TASK-7.3: x" workflow_complete; ticketmd weddingapp $B1 task-7.3
git clone -q "$root/s14/origin.git" "$root/s14/other" 2>/dev/null
git -C "$root/s14/other" -c user.name=t -c user.email=t@t commit -q --allow-empty -m "backlog: elsewhere"
git -C "$root/s14/other" push -q origin main
run_sweep
check "S14 exit 0 after a rebase" $([ "$rc" -eq 0 ]; echo $?)
check "S14 archived on top of the other commit" $(origin_head s14 | grep -q '^retro: archive'; echo $?)
check "S14 the other commit is in the history" $(git --git-dir="$root/s14/origin.git" log --format=%s main | grep -q 'backlog: elsewhere'; echo $?)

# S15. Other changes in the umbrella are left alone.
setup s15; mkrun weddingapp $B1 build-feature "TASK-7.3: x" workflow_complete; ticketmd weddingapp $B1 task-7.3
echo wip > "$u/docs/retro/README.md"
run_sweep
check "S15 other change not committed" $(git -C "$u" diff --quiet -- docs/retro/README.md; [ $? -eq 1 ]; echo $?)

# S16. A run without ticket.md is linked by the TASK id in its request.
setup s16; mkrun weddingapp $F1 build-feature "TASK-8.1: broke early" workflow_error
run_sweep
check "S16 linked by the request" $([ "$(origin_json s16 docs/retro/TASK-8.1/runs/66666666/run.json rule)" = request ]; echo $?)

# T1..T6. The token reports (scripts/tokenomics/commit-reports.sh): called once with the umbrella dir after the archive, a
# failure there is only a warning, run.json names the ticket's report, and a PR-linked run feeds attribution.json.
reports_stub() { # reports_stub <exit code>: a commit-reports.sh stub that records its calls and what was archived by then
  mkdir -p "$u/scripts/tokenomics"
  cat > "$u/scripts/tokenomics/commit-reports.sh" <<STUB
#!/usr/bin/env bash
echo "\$*" >> "$(dirname "$u")/reports.args"
git -C "\$1" log -1 --format=%s >> "$(dirname "$u")/reports.after"
echo '{"commit":"abc"}'
exit $1
STUB
  chmod +x "$u/scripts/tokenomics/commit-reports.sh"
}
setup t1; reports_stub 0
mkrun weddingapp $B1 build-feature "TASK-7.3: x" workflow_complete; ticketmd weddingapp $B1 task-7.3
mkrun weddingapp $R1 archon-comprehensive-pr-review "Review PR #46" workflow_complete
prjson weddingapp 46 feat/task-7.1-the-thing "x"
mkrun weddingapp $U1 archon-comprehensive-pr-review "Review something" workflow_complete
mkdir -p "$u/docs/tokenomics"; echo '{"hand-made": "TASK-1"}' > "$u/docs/tokenomics/attribution.json"
git -C "$u" add -A; git -C "$u" commit -qm "tokenomics files"; git -C "$u" push -q origin main
run_sweep
check "T1 exit 0" $([ "$rc" -eq 0 ]; echo $?)
check "T1 the report script is called once with the umbrella dir" $([ "$(cat "$root/t1/reports.args")" = "$u" ]; echo $?)
check "T1 it is called after the archive commit" $(grep -q '^retro: archive' "$root/t1/reports.after"; echo $?)
check "T1 its JSON goes to stderr, the last stdout line is the sweep result" $(njson 'j.commit.length >= 7' && ! echo "$json" | grep -q abc; echo $?)
check "T2 run.json names the ticket's token report" $([ "$(origin_json t1 docs/retro/TASK-7.3/runs/11111111/run.json tokenReport)" = docs/tokenomics/ticket-token-reports/TASK-7.3.md ]; echo $?)
check "T2 a PR-linked run names it too" $([ "$(origin_json t1 docs/retro/TASK-7.1/runs/22222222/run.json tokenReport)" = docs/tokenomics/ticket-token-reports/TASK-7.1.md ]; echo $?)
check "T2 an unlinked run has null" $([ "$(origin_json t1 docs/retro/unlinked/runs/44444444/run.json tokenReport)" = null ]; echo $?)
check "T3 the token report is not copied into a run folder" $(! origin_files t1 | grep -q 'ticket-token-reports'; echo $?)
att="$u/docs/tokenomics/attribution.json"
check "T4 attribution.json gets the PR-linked run" $([ "$(node -e 'console.log(JSON.parse(require("fs").readFileSync(process.argv[1],"utf8"))[process.argv[2]])' "$att" "$R1")" = TASK-7.1 ]; echo $?)
check "T4 a ticket.md-linked run is not added" $(! grep -q "$B1" "$att"; echo $?)
check "T4 an unlinked run is not added" $(! grep -q "$U1" "$att"; echo $?)
check "T4 a hand-made entry is kept" $(grep -q '"hand-made"' "$att"; echo $?)
check "T4 attribution.json is not in the archive commit" $(! git --git-dir="$root/t1/origin.git" show --name-only --format= main | grep -q tokenomics; echo $?)
setup t5; reports_stub 1
mkrun weddingapp $B1 build-feature "TASK-7.3: x" workflow_complete; ticketmd weddingapp $B1 task-7.3
run_sweep
check "T5 a failing report script: exit 0" $([ "$rc" -eq 0 ]; echo $?)
check "T5 it was called" $([ -f "$root/t5/reports.args" ]; echo $?)
check "T5 warning on stderr" $(grep -q "token reports were not committed" "$root/stderr.txt"; echo $?)
check "T5 the sweep result is still the last line" $(njson 'j.archived.length === 1'; echo $?)
setup t6
mkrun weddingapp $B1 build-feature "TASK-7.3: x" workflow_complete; ticketmd weddingapp $B1 task-7.3
run_sweep
check "T6 without the report script: exit 0 and no word about it" $([ "$rc" -eq 0 ] && ! grep -q "token reports" "$root/stderr.txt"; echo $?)

echo "archive-runs: $pass passed, $fail failed"
[ "$fail" -eq 0 ]
