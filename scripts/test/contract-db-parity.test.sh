#!/usr/bin/env bash
# Tests for scripts/contract-db-parity.sh. Run from anywhere: bash scripts/test/contract-db-parity.test.sh
# The container cases (--schema-sql, --migrations) are skipped when docker is not running.
set -uo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
script="$root/scripts/contract-db-parity.sh"
fx="$root/scripts/test/fixtures/contract-db-parity"
pass=0; fail=0
ok()  { pass=$((pass+1)); echo "PASS: $1"; }
bad() { fail=$((fail+1)); echo "FAIL: $1${2:+ -- $2}"; }
check() { # check <name> <jq filter that must yield true> <file>
  if [ "$(jq -r "$2" "$3" 2>/dev/null)" = "true" ]; then ok "$1"; else bad "$1" "$(jq -c "$2" "$3" 2>&1 | head -c 300)"; fi
}

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

# Contract and comparison, from an already extracted schema.
out="$work/out"
bash "$script" --spec "$fx/openapi.yaml" --db-schema "$fx/db-schema.json" --map "$fx/map.json" --out "$out" >/dev/null 2>&1; rc=$?
if [ "$rc" -eq 0 ]; then ok "runs without --strict despite errors"; else bad "runs without --strict despite errors" "exit $rc"; fi
s="$out/contract-surface.json"; r="$out/parity-report.json"
for f in contract-surface.json db-schema.json parity-report.json; do
  if [ -s "$out/$f" ]; then ok "writes $f"; else bad "writes $f"; fi
done

# CRUD classification.
check "GET is read"                 '[.operations[]|select(.operationId=="listGuests")][0].crud=="read"' "$s"
check "POST with 201 is create"     '[.operations[]|select(.operationId=="createGuest")][0].crud=="create"' "$s"
check "PATCH is update"             '[.operations[]|select(.operationId=="updateGuest")][0].crud=="update"' "$s"
check "DELETE is delete"            '[.operations[]|select(.operationId=="deleteGuest")][0].crud=="delete"' "$s"
check "POST on a sub-path is action" '[.operations[]|select(.operationId=="checkInGuest")][0].crud=="action"' "$s"
check "path-level parameters merge" '[.operations[]|select(.operationId=="deleteGuest")][0].parameters|map(.name)|index("guestId")!=null' "$s"
check "a page envelope unwraps to its item" '[.operations[]|select(.operationId=="listGuests")][0].object=="guest"' "$s"
check "write ops join the response object" '.objects.guest.operations|(.create==["POST /guests (createGuest)"]) and (.update==["PATCH /guests/{guestId} (updateGuest)"]) and (.delete==["DELETE /guests/{guestId} (deleteGuest)"])' "$s"
check "a delete with no schema falls back to its path" '.objects["path:tables"].operations.delete==["DELETE /tables/{tableId} (deleteTable)"]' "$s"
check "readOnly is never writable"  '.objects.guest.fields.id|(.read==true) and (.create|not) and (.update|not)' "$s"
check "required on create"          '.objects.guest.fields.name.requiredOnCreate==true' "$s"
check "nested array items flatten"  '.objects.venue.fields["rooms[].label"].read==true' "$s"
check "an extensible enum is read"    '.objects.guest.fields.channel|(.enum==["EMAIL","SMS"]) and (.extensibleEnum==true)' "$s"
check "nullable allOf keeps the enum" '.objects.guest.fields.diet|(.enum==["VEGAN","NONE"]) and (.nullable==true)' "$s"
check "a read-only object is not writable" '.objects.venue.operations|(.create|length)+(.update|length)+(.delete|length)==0' "$s"

# Comparison findings.
has() { check "$1" "[.findings[]|select(.kind==\"$2\" and .field==\"$3\")]|length==1" "$r"; }
has "length mismatch is reported"       length-mismatch name
has "nullable into NOT NULL is reported" null-not-storable email
has "enum drift against a PG enum"      enum-mismatch status
has "int64 into integer is reported"    type-mismatch seats
has "a write-only field with no column" field-not-persisted dietaryNote
check "a stored value an extensible enum lacks is a warning" '[.findings[]|select(.field=="channel")]|map(.kind+"/"+.severity)==["enum-not-listed/warning"]' "$r"
check "matching CHECK values give no finding" '[.findings[]|select(.field=="diet")]|length==0' "$r"
check "map.json maps a field"        '.tables.guests.fields["guest.badge"].column=="badge_text"' "$r"
check "map.json null drops an object" '[.findings[]|select(.object=="venue")]|length==0' "$r"
check "ignored tables and columns"   '[.findings[]|select(.table=="audit_log" or .column=="created_at")]|length==0' "$r"
check "unexposed column is info"     '[.findings[]|select(.kind=="column-not-exposed" and .column=="org_id")][0].severity=="info"' "$r"
check "findings sort errors first"   '[.findings[].severity]|(index("info") // 1e9) > (rindex("error") // -1)' "$r"

bash "$script" --spec "$fx/openapi.yaml" --db-schema "$fx/db-schema.json" --map "$fx/map.json" --out "$work/strict" --strict >/dev/null 2>&1; rc=$?
if [ "$rc" -eq 1 ]; then ok "--strict fails on an error"; else bad "--strict fails on an error" "exit $rc"; fi
bash "$script" --spec "$fx/openapi.yaml" --only contract --out "$work/contract" >/dev/null 2>&1; rc=$?
if [ "$rc" -eq 0 ] && [ -s "$work/contract/contract-surface.json" ] && [ ! -e "$work/contract/parity-report.json" ]; then
  ok "--only contract stops after the contract"; else bad "--only contract stops after the contract" "exit $rc"; fi
bash "$script" --spec "$fx/openapi.yaml" --db-schema "$fx/db-schema.json" --schema-sql "$fx/schema.sql" --out "$work/x" >/dev/null 2>&1; rc=$?
if [ "$rc" -eq 2 ]; then ok "refuses two schema sources"; else bad "refuses two schema sources" "exit $rc"; fi

# Container cases: a designed DDL file, and Flyway migrations in version order.
if docker info >/dev/null 2>&1; then
  out="$work/ddl"
  bash "$script" --spec "$fx/openapi.yaml" --schema-sql "$fx/schema.sql" --only db --out "$out" >/dev/null 2>&1; rc=$?
  if [ "$rc" -eq 0 ]; then ok "--schema-sql applies a DDL file"; else bad "--schema-sql applies a DDL file" "exit $rc"; fi
  check "a CHECK (x IN ...) becomes allowedValues" '.tables.guests.columns.status.allowedValues==["INVITED","CONFIRMED","it'"'"'s"]' "$out/db-schema.json"
  check "varchar length is extracted"  '.tables.guests.columns.name.maxLength==100' "$out/db-schema.json"
  check "primary key is extracted"     '.tables.guests.primaryKey==["id"]' "$out/db-schema.json"

  out="$work/mig"
  bash "$script" --spec "$fx/openapi.yaml" --migrations "$fx/migrations" --only db --out "$out" >/dev/null 2>&1; rc=$?
  if [ "$rc" -eq 0 ]; then ok "--migrations applies V2 before V10"; else bad "--migrations applies V2 before V10" "exit $rc"; fi
  check "applied order is numeric, repeatables last" '.source.applied==["V1__guests.sql","V2__tables.sql","V10__guest_table.sql","R__guest_view.sql"]' "$out/db-schema.json"
  check "a PG enum is extracted"      '.enums.guest_status==["INVITED","CONFIRMED"]' "$out/db-schema.json"
  check "a foreign key is extracted"  '.tables.guests.foreignKeys[0].references=={"table":"seating_tables","columns":["id"]}' "$out/db-schema.json"
  check "a view is not a table"       '.tables|has("guest_names")|not' "$out/db-schema.json"
else
  echo "SKIP: docker is not running; container cases not run"
fi

echo "$pass passed, $fail failed"
[ "$fail" -eq 0 ]
