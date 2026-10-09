---
name: scheduled-tasks
description: Idempotent scheduled background jobs on quarkus-scheduler. Use when adding cron or fixed-rate work.
---
# Scheduled background tasks

> **Generic "how" only.** No concrete job/schedule values in the body — those live in the code-map.

## When to use
Periodic background work — cache warming, polling, cleanup, refresh — that runs on a timer rather than
in response to a request.

## How
- Annotate a method of an `@ApplicationScoped` bean with `io.quarkus.scheduler.Scheduled`, setting
  `concurrentExecution = SKIP` so a slow run is never overlapped by the next one. Nothing to enable.
- Take the cadence from a configuration property (`cron = "{<property>}"` / `every = "{<property>}"`);
  never inline it.
- Make the task **idempotent** — it may run again after a failure.
- **Multi-instance**: with a single instance no guard is needed; add a lock or leader election before
  a second instance runs, so a job doesn't double-execute across replicas. A lock table needs a
  migration, so decide it deliberately.
- Keep the scheduled method thin: delegate to a use-case; catch + log failures so one bad run doesn't
  kill the schedule.
- Make it observable (log start/finish + outcome).

## Pattern signals (discovery cues)
`@Scheduled` methods with `concurrentExecution`; cadence from a configuration property; the method
delegating to a use-case.

## Project specifics → see docs
- Code map (the scheduled jobs, cadences, guards) → `docs/code-maps/scheduled-tasks.md` *(per-repo map; if marked `kind: worked-example`, follow its structure and map its pseudonymized names to this repo's own)*

## Guardrails (what NOT to do)
- Don't scale to a second instance without a guard against concurrent runs.
- Don't let an exception escape and silently stop the schedule — catch + log.
- Don't bury business logic in the scheduled method — delegate to a use-case.

## Definition of done
- [ ] `@Scheduled` thin entry delegating to a use-case; `concurrentExecution = SKIP`; idempotent; cadence externalized; failures logged, schedule survives.
- [ ] Verified by: a test invoking the scheduled method twice and asserting the second run is a no-op.
