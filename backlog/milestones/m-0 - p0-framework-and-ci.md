---
id: m-0
title: "P0 Framework and CI"
---

## Description

Framework readiness: the new repository wedding-portal builds with Gradle on Java 25 and Quarkus, generates its server interfaces from a pinned rekord-contract tag, enforces the architecture rules, owns an empty PostgreSQL schema with its own Flyway history from V1, answers every refusal with the error envelope and the Notification pattern, and runs a green CI on every push. Nothing is deployed (UD-13). The only operation is health. It sets the rules every later phase follows: contract tags v<major>.<minor>.<patch> from v0.1.0 on, each equal to info.version (UD-19.e, UD-20.a, UD-20.b); the 422 errors list of {field, code, message} items (UD-12, UD-19.d); a unique-constraint race past a domain check answered 409 with that check's refusal (UD-19.i5); and no token, access code, password, e-mail address, name or phone number in any log line (UD-19.f).
