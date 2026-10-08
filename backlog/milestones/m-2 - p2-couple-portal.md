---
id: m-2
title: "P2 Couple portal"
---

## Description

Couple portal backend: the access-code gate and the portal session with the computed end of UD-9 and UD-10, the couple's details and state, the read-only timeline and tasks, the song wishes, the never list and the song search. It builds the 13 portal operations, test-first where rekord-api has no test, with the deviations of UD-18, UD-19 and UD-20: friends only add songs, with the 403 text of UD-19.l1 and an identical repeat answered 200 (UD-19.l2); a wedding date the couple sends is ignored and leaves the contract (UD-20.c); the names the couple enters show to the couple and their friends only and survive a planner rename (UD-18.b, UD-19.a, UD-19.l4); no list is ever SUBMITTED or LOCKED and the never list leaves the progress count; start_pref is removed from the contract; the expiry is evaluated on every request; the per-portal lockout is the only limit of the code gate; and only rm_portal counts on portal routes, only rm_session on account routes (UD-19.l3).
