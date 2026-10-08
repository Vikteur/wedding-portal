---
id: TASK-30.7
title: >-
  P4-E01-T07 Cloudflare origin certificate and key placed on the new server by
  the operator
status: To Do
assignee: []
created_date: '2026-10-07 07:18'
labels:
  - technical
  - P4
  - stop-production-config
  - stop-crypto-logging
milestone: m-4
dependencies:
  - TASK-30.3
references:
  - 'rekord-api/deploy/nginx/rekord.conf:30-31'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/analysis/16-build-test-deploy-ops.md
parent_task_id: TASK-30
priority: high
type: task
ordinal: 40107
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As an operator of wedding-portal, I want the Cloudflare origin certificate and its key placed on the new server by hand after a recorded approval so that nginx serves TLS without the key ever reaching git, a document or CI.

UD-19.b: the new server sits behind Cloudflare and its nginx serves a Cloudflare origin certificate; the certificate and its key are server secrets, never written into a document or the repository. P4-E01-T03 names them in the site file at the paths rekord-api uses (rekord-api/deploy/nginx/rekord.conf:30-31: the certificate file cloudflare-origin.pem in the ssl/certs directory and the key file cloudflare-origin.key in the ssl/private directory of the system configuration directory) and forbids any release step to write them, and the pre-flight step of the release script (P4-E02-T03) stops the release while either file is missing, so a person places them on the new server once, before the first release run, the only step of P4-E01 done on the new server rather than in CI. A Cloudflare origin certificate is often issued for a wildcard, so the check is that its subject alternative names cover the host of PUBLIC_HOST (UD-19.n2), written <PUBLIC_HOST> because no document names it. TLS key handling and the release settings are STOP items.

- STOP (human approval in the pull request): production-config, crypto-logging

Plan item `P4-E01-T07` (technical,P4,stop-production-config,stop-crypto-logging) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a recorded human approval and a Cloudflare origin certificate whose subject alternative names cover <PUBLIC_HOST> When the operator places the certificate and its key on the new server before the first run of the release pipeline Then the certificate file cloudflare-origin.pem is owned by root with mode 644 and the key file cloudflare-origin.key is owned by root with mode 600 (UD-19.b).
- [ ] #2 Given both files on the new server When the public key of the certificate file cloudflare-origin.pem is compared with the public key derived from the key file cloudflare-origin.key and the certificate's issuer is read Then the two public keys are equal and the issuer is the Cloudflare origin certificate authority (UD-19.b).
- [ ] #3 Given the two files placed When the tracked files of wedding-portal, the documents under docs/ and the CI secrets of wedding-portal are searched Then neither the certificate nor the key appears in any of them (UD-19.b).
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 Every acceptance criterion of the ticket is checked by at least one automated test in wedding-portal that fails when the criterion is broken.
- [ ] #2 The CI run of the ticket's pull request is green, read by commit SHA; no test, gate or check is skipped, disabled or quarantined to get there.
- [ ] #3 The ArchUnit rules pass with no new entry in the frozen baseline.
- [ ] #4 Every contract change the ticket needs is merged and tagged in rekord-contract before the code that uses it; wedding-portal pins that tag; no generated file is edited by hand.
- [ ] #5 Every STOP item the ticket touches (contract push, database migration, authentication or the access matrix, encryption or logging of personal data, deleting history, production configuration or secrets) has a human approval recorded in the pull request before the step is taken.
- [ ] #6 Every refusal is answered with the error envelope {"detail":{"code","message"}} and a code from the contract's ErrorCode enum; validation follows the Notification pattern (UD-12), and a 422 lists its violations in the `errors` list (UD-19.d).
- [ ] #7 Every operation the ticket builds answers with the status, error code and body shape rekord-api gives for the same request, except for the deviations recorded as user decisions (UD-9 to UD-20) in docs/rewrite/STATUS.md; each deviation the ticket implements is named in the pull request.
- [ ] #8 No secret, token, password, access code or personal data appears in logs, test data, commits or documents.
- [ ] #9 Every schema change is a new Flyway migration (an applied migration is never edited) and is exercised by a Testcontainers test.
- [ ] #10 Every document or code-map leaf that the change makes outdated is updated in the same pull request.
- [ ] #11 The pull request is reviewed and merged into main.
<!-- DOD:END -->
