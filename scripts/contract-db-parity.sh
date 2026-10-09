#!/usr/bin/env bash
# Extract an OpenAPI contract's CRUD surface and a database schema as JSON, and compare them.
# Usage: scripts/contract-db-parity.sh --spec <openapi.yaml> (--schema-sql <file> | --migrations <dir> | --db-schema <json>)
#          [--map <map.json>] [--out <dir>] [--image <postgres image>] [--only contract|db] [--strict]
#   --schema-sql: one DDL file (a designed schema, not yet a migration), applied to a throwaway Postgres container.
#   --migrations: Flyway SQL files (V<version>__*.sql, then R__*.sql), applied the same way.
#   --db-schema:  an already extracted db-schema.json; skips the container. Default image: postgres:18.
#   Writes <out>/contract-surface.json, db-schema.json and parity-report.json (default out: build/contract-db-parity).
#   --strict: exit 1 when the report holds an error-severity finding.
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
# yaml@2.8.1, pinned with its integrity hash in scripts/contract-db-parity/package-lock.json; npm ci installs it on first use.
TOOL_DIR="$root/scripts/contract-db-parity"

usage() { sed -n '3,9p' "$0" | sed 's/^# \{0,1\}//' >&2; exit 2; }

spec="" schema_sql="" migrations="" db_schema="" map="" out="$root/build/contract-db-parity" image="postgres:18" only="" strict=0
while [[ $# -gt 0 ]]; do
  case "$1" in
    --spec) spec="${2:-}"; shift 2 ;;
    --schema-sql) schema_sql="${2:-}"; shift 2 ;;
    --migrations) migrations="${2:-}"; shift 2 ;;
    --db-schema) db_schema="${2:-}"; shift 2 ;;
    --map) map="${2:-}"; shift 2 ;;
    --out) out="${2:-}"; shift 2 ;;
    --image) image="${2:-}"; shift 2 ;;
    --only) only="${2:-}"; shift 2 ;;
    --strict) strict=1; shift ;;
    *) usage ;;
  esac
done
case "$only" in ""|contract|db) ;; *) usage ;; esac
[[ "$only" == db || -n "$spec" ]] || usage
[[ "$only" == contract || -n "$schema_sql$migrations$db_schema" ]] || usage
sources=0; for s in "$schema_sql" "$migrations" "$db_schema"; do [[ -n "$s" ]] && sources=$((sources+1)); done
[[ $sources -le 1 ]] || { echo "[parity] pass one of --schema-sql, --migrations or --db-schema" >&2; exit 2; }
[[ -z "$schema_sql" || -f "$schema_sql" ]] || { echo "[parity] schema file not found: $schema_sql" >&2; exit 1; }
[[ -z "$spec" || -f "$spec" ]] || { echo "[parity] spec not found: $spec" >&2; exit 1; }
[[ -z "$migrations" || -d "$migrations" ]] || { echo "[parity] migrations directory not found: $migrations" >&2; exit 1; }
[[ -z "$db_schema" || -f "$db_schema" ]] || { echo "[parity] db schema not found: $db_schema" >&2; exit 1; }
[[ -z "$map" || -f "$map" ]] || { echo "[parity] map not found: $map" >&2; exit 1; }

mkdir -p "$out"
[[ -d "$TOOL_DIR/node_modules/yaml" ]] || npm ci --prefix "$TOOL_DIR" --silent --no-audit --no-fund >&2

# 1. The contract's CRUD surface.
if [[ "$only" != db ]]; then
  node "$TOOL_DIR/extract-contract.mjs" "$spec" > "$out/contract-surface.json"
  echo "[parity] $out/contract-surface.json"
fi
[[ "$only" == contract ]] && exit 0

# Flyway order: versioned migrations by numeric version (V1_1 and V1.1 are 1.1), then repeatable ones by description.
ordered_migrations() {
  local dir="$1" f base
  for f in "$dir"/V*__*.sql; do
    [[ -e "$f" ]] || continue
    base="$(basename "$f")"
    printf '%s\t%s\n' "$(sed -E 's/^V([0-9._]+)__.*/\1/; s/_/./g' <<<"$base")" "$f"
  done | sort -t. -k1,1n -k2,2n -k3,3n -k4,4n | cut -f2-
  for f in "$dir"/R__*.sql; do [[ -e "$f" ]] && printf '%s\n' "$f"; done | sort
}

# 2. The database schema, from a throwaway container the DDL or the migrations run against.
if [[ -n "$schema_sql$migrations" ]]; then
  command -v docker >/dev/null || { echo "[parity] docker is required for --schema-sql/--migrations (or pass --db-schema)" >&2; exit 1; }
  if [[ -n "$schema_sql" ]]; then
    files=("$schema_sql"); ddl_source="$schema_sql"
  else
    mapfile -t files < <(ordered_migrations "$migrations"); ddl_source="$migrations"
    [[ ${#files[@]} -gt 0 ]] || { echo "[parity] no V*__*.sql or R__*.sql files in $migrations" >&2; exit 1; }
  fi
  if grep -l '\${' "${files[@]}" >/dev/null 2>&1; then
    echo "[parity] warning: a migration uses \${...} placeholders, which this script does not substitute" >&2
  fi
  container="$(docker run -d --rm -e POSTGRES_PASSWORD=parity -e POSTGRES_DB=parity "$image")"
  trap 'docker rm -f "$container" >/dev/null 2>&1 || true' EXIT
  # The server listens on TCP only once initdb has finished and it has restarted.
  for _ in $(seq 1 60); do
    docker exec "$container" pg_isready -q -h 127.0.0.1 -U postgres -d parity 2>/dev/null && break
    sleep 1
  done
  docker exec "$container" pg_isready -q -h 127.0.0.1 -U postgres -d parity \
    || { echo "[parity] postgres did not become ready" >&2; exit 1; }
  psql=(docker exec -i "$container" psql -h 127.0.0.1 -U postgres -d parity -v ON_ERROR_STOP=1 -q)
  for f in "${files[@]}"; do
    "${psql[@]}" --single-transaction < "$f" >/dev/null || { echo "[parity] applying $f failed (is --image new enough for it?)" >&2; exit 1; }
  done
  db_schema="$out/db-schema.json"
  "${psql[@]}" -At < "$TOOL_DIR/db-schema.sql" \
    | node -e '
        const raw = require("fs").readFileSync(0, "utf8");
        const [image, dir, ...files] = process.argv.slice(1);
        const doc = JSON.parse(raw);
        const out = { source: { ddl: dir, image, applied: files.map((f) => f.split("/").pop()) }, ...doc };
        process.stdout.write(JSON.stringify(out, null, 2) + "\n");' "$image" "${ddl_source//\\//}" "${files[@]}" > "$db_schema.tmp"
  mv "$db_schema.tmp" "$db_schema"
  echo "[parity] $db_schema"
elif [[ "$(cd "$(dirname "$db_schema")" && pwd)/$(basename "$db_schema")" != "$(cd "$out" && pwd)/db-schema.json" ]]; then
  cp "$db_schema" "$out/db-schema.json"
fi
[[ "$only" == db ]] && exit 0

# 3. The comparison.
node "$TOOL_DIR/compare.mjs" "$out/contract-surface.json" "$out/db-schema.json" ${map:+"$map"} > "$out/parity-report.json"
echo "[parity] $out/parity-report.json"
node -e '
  const r = JSON.parse(require("fs").readFileSync(process.argv[1], "utf8"));
  const s = r.summary;
  console.log(`[parity] ${s.errors} errors, ${s.warnings} warnings, ${s.info} info; ${s.matchedTables}/${s.tables} tables matched, ${s.objectsWithoutTable} objects without a table`);
  if (process.argv[2] === "1" && s.errors > 0) process.exit(1);' "$out/parity-report.json" "$strict"
