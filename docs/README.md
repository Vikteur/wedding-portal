# Docs map: what goes where

Every agent reads this before it writes a doc. One kind of fact has one home; put it there and nowhere else.

| Where | What goes there |
|---|---|
| `docs/memory.md` | Project-wide decisions and approaches tried and abandoned, one dated bullet each, with the ticket id. |
| `docs/code-maps/<skill>.md` | How a skill is applied, one leaf per skill (P3). `kind: worked-example` leaves show the structure with pseudonymized names, not project facts. See `docs/code-maps/README.md`. |

Kept in the umbrella repo `../weddingapp`, not here (its `docs/README.md` has the full map):
- the tickets (`backlog/`, only via the `backlog` CLI) and their specs (`docs/rewrite/tickets/`);
- owner decisions for the whole rewrite (`docs/rewrite/STATUS.md`, `UD-<n>`);
- retros and ADRs (`docs/retro/<TASK>/`) and follow-ups (`docs/follow-ups/`, one JSON file each).

The contract lives in the hub repo, rekord-contract, and only the `contract-agent` writes it.
