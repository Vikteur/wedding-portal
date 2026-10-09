#!/usr/bin/env bash
# Tests for scripts/scaffold.sh. Run from anywhere: bash scripts/test/scaffold.test.sh
set -uo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
script="$root/scripts/scaffold.sh"
templates="$root/docs/Moustache scripts"
pass=0; fail=0
ok()  { pass=$((pass+1)); echo "PASS: $1"; }
bad() { fail=$((fail+1)); echo "FAIL: $1${2:+ -- $2}"; }

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
mkdir -p "$work/repo" && cd "$work/repo" || exit 2
record='{"package": "com.example.guest", "Name": "Guest", "fields": [{"type": "List<String>", "name": "names", "last": true}]}'

# Templates live in docs/Moustache scripts (owner decision, portal 957e5e9).
if [ -f "$templates/domain/record.mustache" ] && [ ! -e "$root/docs/code-maps/scaffold" ]; then
  ok "templates live in docs/Moustache scripts"; else bad "templates live in docs/Moustache scripts"; fi

# Renders by layer/name, with generics left unescaped.
out=guest-domain/src/main/java/Guest.java
if bash "$script" domain/record "$record" "$out" >/dev/null 2>&1 && grep -qF 'List<String> names' "$out" \
   && grep -qF 'package com.example.guest.domain;' "$out"; then
  ok "renders domain/record"; else bad "renders domain/record" "$(cat "$out" 2>/dev/null | head -3)"; fi

# Never overwrites.
bash "$script" domain/record "$record" "$out" >/dev/null 2>&1; rc=$?
if [ "$rc" -eq 1 ]; then ok "refuses to overwrite"; else bad "refuses to overwrite" "exit $rc"; fi

# Only relative targets inside the working tree.
bash "$script" domain/record "$record" "$work/abs/Guest.java" >/dev/null 2>&1; rc=$?
if [ "$rc" -eq 1 ] && [ ! -e "$work/abs/Guest.java" ]; then ok "refuses an absolute target"; else bad "refuses an absolute target" "exit $rc"; fi
bash "$script" domain/record "$record" "x/../../Guest.java" >/dev/null 2>&1; rc=$?
if [ "$rc" -eq 1 ] && [ ! -e "$work/Guest.java" ]; then ok "refuses a '..' target"; else bad "refuses a '..' target" "exit $rc"; fi

# --test: the test-writer's mode, only test source trees (its agent-scopes fence).
bash "$script" --test domain/record "$record" "guest-domain/src/main/java/Other.java" >/dev/null 2>&1; rc=$?
if [ "$rc" -eq 1 ] && [ ! -e guest-domain/src/main/java/Other.java ]; then
  ok "--test refuses a main-source target"; else bad "--test refuses a main-source target" "exit $rc"; fi
if bash "$script" --test domain/record "$record" "guest-domain/src/test/java/GuestTest.java" >/dev/null 2>&1 \
   && [ -f guest-domain/src/test/java/GuestTest.java ]; then
  ok "--test renders into src/test"; else bad "--test renders into src/test"; fi

# The test-writer may run the script only in --test mode.
scopes="$root/.claude/hooks/agent-scopes.json"
tw="$(node -e 'const a=require(process.argv[1]).agents;console.log(a["test-writer"].commandAllowlist.filter(c=>c.startsWith("scripts/scaffold.sh")).join("|"))' "$scopes")"
if [ "$tw" = "scripts/scaffold.sh --test" ]; then ok "test-writer allowlist is scaffold --test only"; else bad "test-writer allowlist is scaffold --test only" "$tw"; fi
if grep -qF '"Bash(scripts/scaffold.sh --test *)"' "$root/.claude/agents/test-writer.md"; then
  ok "test-writer tools carry scaffold --test only"; else bad "test-writer tools carry scaffold --test only"; fi

# Unknown template.
bash "$script" domain/nope '{}' "nope/A.java" >/dev/null 2>&1; rc=$?
if [ "$rc" -eq 1 ]; then ok "unknown template fails"; else bad "unknown template fails" "exit $rc"; fi

# The controller imports the generated interface and DTO.
ctl='{"package": "com.example.guest", "Name": "Guest", "Api": "GuestApi", "apiPackage": "com.example.api", "modelPackage": "com.example.api.model", "UseCase": "GetGuestUseCase", "useCase": "getGuestUseCase", "operationId": "getGuest", "Domain": "Guest"}'
out=guest-adapter/src/main/java/GuestController.java
if bash "$script" adapter/controller "$ctl" "$out" >/dev/null 2>&1 && grep -qF 'import com.example.api.GuestApi;' "$out" \
   && grep -qF 'import com.example.api.model.GuestDTO;' "$out"; then
  ok "controller imports the generated Api and DTO"; else bad "controller imports the generated Api and DTO"; fi

# Who the user is stays hand-written (STOP item): no template renders an authentication type.
hits="$(grep -rlE 'AuthenticatedCustomer|AuthenticationFacade|#authenticated' "$templates" 2>/dev/null | sed "s#$templates/##" | paste -sd, -)"
if [ -z "$hits" ]; then ok "no template renders authentication types"; else bad "no template renders authentication types" "$hits"; fi

# The renderer is pinned by a lockfile, not fetched by npx at run time.
if [ -f "$root/scripts/scaffold/package-lock.json" ] && ! grep -q 'npx' "$script"; then
  ok "mustache pinned by a lockfile"; else bad "mustache pinned by a lockfile"; fi

echo "scaffold: $pass passed, $fail failed"
[ "$fail" -eq 0 ]
