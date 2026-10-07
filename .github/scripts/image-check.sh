#!/usr/bin/env bash
# Checks a built image against the container contract of architecture-conventions §14.5:
# fast-jar under /app, uid/gid 10001, JAVA_OPTS, port, entrypoint, health check, and a running container.
# Usage: image-check.sh <image>
set -euo pipefail

image="${1:?usage: image-check.sh <image>}"
repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
port=18080

fail() {
  echo "FAIL: $1" >&2
  exit 1
}

expect() { # <what> <expected> <actual>
  if [[ "$2" != "$3" ]]; then
    fail "$1: expected '$2' but was '$3'"
  fi
}

config="$(docker image inspect "$image" --format '{{json .Config}}')"
cfg() { jq -c "$1" <<<"$config"; }

echo "Check: fast-jar layout under /app"
docker run --rm --entrypoint sh "$image" -c \
  'test -f /app/quarkus-run.jar && test -d /app/lib && test -d /app/app && test -d /app/quarkus' \
  || fail "/app does not hold quarkus-run.jar, lib/, app/ and quarkus/"

echo "Check: user is 10001:10001"
expect "Config.User" '"10001:10001"' "$(cfg '.User')"

echo "Check: JAVA_OPTS"
expect "JAVA_OPTS in Config.Env" 'true' \
  "$(cfg '.Env | index("JAVA_OPTS=-XX:MaxRAMPercentage=70 -Duser.timezone=UTC") != null')"

echo "Check: exposed port 8080/tcp"
expect "8080/tcp in Config.ExposedPorts" 'true' "$(cfg '.ExposedPorts | has("8080/tcp")')"

echo "Check: entrypoint"
expect "Config.Entrypoint" \
  '["sh","-c","exec java $JAVA_OPTS -jar /app/quarkus-run.jar"]' "$(cfg '.Entrypoint')"

echo "Check: health check"
expect "Healthcheck.Test" \
  '["CMD-SHELL","curl -fsS --max-time 3 http://127.0.0.1:8080/api/health || exit 1"]' \
  "$(cfg '.Healthcheck.Test')"
expect "Healthcheck.Interval" '30000000000' "$(cfg '.Healthcheck.Interval')"
expect "Healthcheck.Timeout" '5000000000' "$(cfg '.Healthcheck.Timeout')"
expect "Healthcheck.StartPeriod" '40000000000' "$(cfg '.Healthcheck.StartPeriod')"
expect "Healthcheck.Retries" '3' "$(cfg '.Healthcheck.Retries')"

echo "Check: running container"
container="$(docker run -d -p "127.0.0.1:$port:8080" "$image")"
workdir="$(mktemp -d)"
cleanup() {
  status=$?
  if [[ $status -ne 0 ]]; then
    docker logs "$container" >&2 || true
  fi
  docker rm -f "$container" >/dev/null 2>&1 || true
  rm -rf "$workdir"
  exit $status
}
trap cleanup EXIT

health=""
for _ in $(seq 1 60); do
  health="$(docker inspect --format '{{.State.Health.Status}}' "$container")"
  running="$(docker inspect --format '{{.State.Running}}' "$container")"
  [[ "$running" == "true" ]] || fail "container exited before becoming healthy"
  [[ "$health" != "healthy" ]] || break
  [[ "$health" != "unhealthy" ]] || fail "container became unhealthy"
  sleep 2
done
expect "health status within 120 s" "healthy" "$health"

expect "name of PID 1" "java" "$(docker exec "$container" cat /proc/1/comm)"
expect "uid of PID 1" "10001" "$(docker exec "$container" stat -c %u /proc/1)"
expect "gid of PID 1" "10001" "$(docker exec "$container" stat -c %g /proc/1)"

code="$(curl -sS -o "$workdir/body" -w '%{http_code}' "http://127.0.0.1:$port/api/health")"
expect "GET /api/health status" "200" "$code"
expected_body="$(jq -r .body "$repo_root/application/src/test/resources/fixtures/health-200.json")"
expect "GET /api/health body" "$expected_body" "$(cat "$workdir/body")"

echo "OK: $image"
