---
id: m-1
title: "P1 Planner"
---

## Description

Planner backend: accounts and sign-in, roles with the new ADMIN role, members and invites, the vendor directory, weddings with the COMPLETED rule, people and the team, portal link management, the timeline and the tasks, a daily cleanup job and a minimal audit_log. It builds the 44 planner and account operations plus the new role operation, test-first where rekord-api has no test, with the deviations UD-10.c, UD-10.d, UD-11, UD-14, UX-03, UX-12 and UX-13, and those of UD-19: the approved role contract with sessions ended on a role change (UD-19.g), the team fields filled, foreign and unknown ids answered 422 alike and member removal clearing its links (UD-19.i1 to UD-19.i3), the role-denied 403 in the error envelope (UD-19.i4), unique-constraint races answered 409 (UD-19.i5), a revived portal taking the live code (UD-19.i6), the cleanup job (UD-19.j) and the audit_log (UD-19.k); the behaviours of UD-19.h stay as in rekord-api.
