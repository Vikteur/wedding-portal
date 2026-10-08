#!/usr/bin/env bash
# Tests for .archon/scripts/groma-sync.sh against throwaway repos and a stubbed groma CLI.
# Usage: bash .archon/scripts/test/groma-sync.test.sh   (exit 0 = all pass)
set -uo pipefail

script="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/groma-sync.sh"
pass=0; fail=0
root="$(mktemp -d)"
trap 'rm -rf "$root"' EXIT

# The stub logs each call; `groma scan` adds an element with no description when STUB_SCAN=new, fails when =fail.
mkdir -p "$root/bin"
cat > "$root/bin/groma" <<'STUB'
#!/usr/bin/env bash
echo "groma $*" >> "$GROMA_LOG"
[ "$1" = scan ] || exit 0
case "${STUB_SCAN:-}" in
  new) mkdir -p groma/systems && printf -- '---\ntype: C4 Component\ntitle: New\n---\n' > groma/systems/new.md ;;
  fail) echo "scan exploded" >&2; exit 1 ;;
esac
STUB
chmod +x "$root/bin/groma"

# repo <name> [map]: a repo with one commit, with a groma/ map and a check script when map is given; sets dir log.
# The check script fails while an element under groma/ has no description, like the real one.
repo() {
  dir="$root/$1"; log="$root/$1.log"; : > "$log"
  git init -q -b main "$dir"
  git -C "$dir" config user.name t; git -C "$dir" config user.email t@t; git -C "$dir" config core.autocrlf false
  echo base > "$dir/README.md"
  if [ -n "${2:-}" ]; then
    mkdir -p "$dir/groma/systems" "$dir/.github/scripts"
    echo '{"scanners": [{"id": "java"}]}' > "$dir/groma/scanners.json"
    printf -- '---\ntype: C4 Component\ntitle: Old\ndescription: Old one.\n---\n' > "$dir/groma/systems/old.md"
    cat > "$dir/.github/scripts/groma-check.sh" <<'CHECK'
#!/usr/bin/env bash
for f in $(grep -rl '^type:' groma); do grep -q '^description:' "$f" || { echo "FAIL: $f has no description"; exit 1; }; done
echo "groma-check: all passed"
CHECK
  fi
  git -C "$dir" add -A; git -C "$dir" commit -qm base
}
run() { # run [env...]: groma-sync inside the repo; sets out (stdout), err, rc
  out=$( (cd "$dir" && env PATH="$root/bin:$PATH" GROMA_LOG="$log" "$@" bash "$script") 2>"$root/err" ); rc=$?
  err=$(cat "$root/err")
}
check() { # check <name> <condition...>
  local name=$1; shift
  if "$@"; then pass=$((pass + 1)); else fail=$((fail + 1)); echo "FAIL: $name"; echo "  out: $out"; echo "  err: $err"; fi
}
commits() { git -C "$dir" rev-list --count HEAD; }

# G1: a repo without a map needs nothing, and groma is never called.
repo g1
run
check "G1 exit 0" [ "$rc" = 0 ]
check "G1 no curation" [ "$out" = '{"curate": false}' ]
check "G1 groma not called" [ ! -s "$log" ]
check "G1 no commit" [ "$(commits)" = 1 ]

# G2: a map in sync with the source: scanned, nothing to commit, nothing to curate.
repo g2 map
run
check "G2 exit 0" [ "$rc" = 0 ]
check "G2 no curation" [ "$out" = '{"curate": false}' ]
check "G2 scanned" grep -qx "groma scan" "$log"
check "G2 no commit" [ "$(commits)" = 1 ]

# G3: the scan finds a new element: the scan is committed alone, and the undescribed element needs curating.
repo g3 map
run STUB_SCAN=new
check "G3 exit 0" [ "$rc" = 0 ]
check "G3 curation" [ "$out" = '{"curate": true}' ]
check "G3 one commit" [ "$(commits)" = 2 ]
check "G3 commit message" [ "$(git -C "$dir" log -1 --format=%s)" = "docs(groma): fold a fresh scan into the map" ]
check "G3 commit holds the scan" [ "$(git -C "$dir" show --name-only --format= HEAD)" = "groma/systems/new.md" ]
check "G3 tree clean" [ -z "$(git -C "$dir" status --porcelain)" ]
check "G3 says why" grep -q "new.md has no description" "$root/err"

# G4: uncommitted changes under groma/ are refused before any scan.
repo g4 map
echo edited >> "$dir/groma/systems/old.md"
run
check "G4 exit 1" [ "$rc" = 1 ]
check "G4 names groma/" grep -q "uncommitted changes under groma/" "$root/err"
check "G4 not scanned" [ ! -s "$log" ]

# G5: a failing scan fails the step, and commits nothing.
repo g5 map
run STUB_SCAN=fail
check "G5 exit 1" [ "$rc" = 1 ]
check "G5 says so" grep -q "groma scan failed" "$root/err"
check "G5 no commit" [ "$(commits)" = 1 ]

# G6: a map but no groma CLI fails with the install hint.
repo g6 map
run GROMA=groma-not-installed
check "G6 exit 1" [ "$rc" = 1 ]
check "G6 install hint" grep -q "npm install -g groma.md" "$root/err"

# G7: the curation output is valid JSON on stdout alone, even when the check prints.
repo g7 map
run STUB_SCAN=new
check "G7 stdout is JSON" node -e 'JSON.parse(process.argv[1])' "$out"

echo "groma-sync tests: $pass passed, $fail failed"
[ "$fail" = 0 ]
