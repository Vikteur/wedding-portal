---
id: TASK-33.2
title: >-
  P4-E04-T02 Release of the tagged version to the new server through the
  pipeline
status: To Do
assignee: []
created_date: '2026-10-07 07:18'
labels:
  - user-story
  - P4
  - stop-production-config
  - stop-crypto-logging
milestone: m-4
dependencies:
  - TASK-33.1
  - TASK-30.7
references:
  - 'docs/rewrite/STATUS.md:279-297'
  - 'docs/rewrite/analysis/16-build-test-deploy-ops.md:239'
  - 'rekord-api/deploy/nginx/rekord.conf:11-15'
  - 'rekord-api/src/test/java/app/rekord/security/RouteGuardTest.java:49-64'
  - 'rekord-api/deploy/nginx/rekord.conf:30-31'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/analysis/16-build-test-deploy-ops.md
parent_task_id: TASK-33
priority: high
type: feature
ordinal: 40402
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As an admin, I want the finished wedding-portal released once to the new server with my first sign-in working so that I can start planning weddings in wedding-portal.

UD-13.d: wedding-portal is released once, after phase 3, to a new server, by a pipeline that avoids H1-H10. H4: no POC release workflow has ever succeeded, each failing at a checkout step; the read-only contract token of P0-E02-T01 removes that cause. The first admin comes from the secret settings (UD-14.g). The new server sits behind Cloudflare, and its nginx serves the Cloudflare origin certificate kept as a server secret (UD-19.b). The host name is the deployment setting PUBLIC_HOST, set on the server and named in no document, so the criteria write it as <PUBLIC_HOST> (UD-19.n2). The release serves the three unchanged frontend bundles built by P4-E02-T05 (UD-19.n1). Backup target, schedule and retention are deployment settings without defaults that the user decides later, and the release stops while any of them is unset (UD-19.n3); that stop, and the stop on a missing origin certificate file placed by P4-E01-T07, is the pre-flight step of P4-E02-T03, proven in CI, so on the server it is checked from the run log. The app derives its public base address from PUBLIC_HOST (P4-E01-T01), so every couple link and invite link of the release starts with https://<PUBLIC_HOST>. A STOP item (production configuration). Because P4-E01 closes before this single deploy and checks its configuration in CI, this ticket repeats those checks on the new server and on the released commit SHA (H1, H3, H8, H10), together with the TLS answers of P4-E01-T03 and the first scheduled backup of P4-E03 (H9). Its log check verifies on the server the logging decisions of P0-E06-T05, P4-E01-T05 and P4-E01-T06, so the ticket is a STOP item for logging as well.

- STOP (human approval in the pull request): production-config, crypto-logging
- Covers: UD-13.d, H4, UD-19.b, UD-19.n1, UD-19.n2, UD-19.n3

Plan item `P4-E04-T02` (user-story,P4,stop-production-config,stop-crypto-logging) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given phases 0 to 3 complete and CI green on the tagged commit of main When the release pipeline runs Then every job, the contract and frontend checkouts included, is green, and the result is fetched by that commit SHA.
- [ ] #2 Given the run log of the release run on the new server When its pre-flight step of P4-E02-T03 is read Then it reports PUBLIC_HOST, BACKUP_TARGET, BACKUP_SCHEDULE and BACKUP_RETENTION_DAYS as set and the certificate file cloudflare-origin.pem and the key file cloudflare-origin.key as present, before the step that runs docker compose pull (UD-19.n2, UD-19.n3, UD-19.b).
- [ ] #3 Given the first admin signed in through https://<PUBLIC_HOST> and a test wedding created by that admin When getWeddingPortal of that wedding is read and createInvite is called for dj@example.com Then the url of each portal starts with https://<PUBLIC_HOST>/g/ and the accept_url starts with https://<PUBLIC_HOST>/invite/ (UD-19.n2).
- [ ] #4 Given the wedding-portal database on the new server right after the release and before any sign-in When its contents are listed Then flyway_schema_history starts at version 1, the only user row is the first admin created at first start (UD-14) with its business, and no wedding row exists.
- [ ] #5 Given the first admin's address and password in the secret settings When that admin calls login at https://<PUBLIC_HOST> through Cloudflare Then login answers 200 with the rm_session cookie set, and getMe with that cookie answers 200 whose user has role PLANNER and roles ["ADMIN"] (UD-14).
- [ ] #6 Given the released commit SHA and the release on the new server When the CI run of that SHA is read and a visitor without a session calls GET https://<PUBLIC_HOST>/api/weddings Then the route-guard test of P0-E06-T01 passed on that SHA, and the call answers 401 with {"detail":{"code":"NOT_SIGNED_IN","message":"Sign in to continue."}} (H3).
- [ ] #7 Given the new server after the release When its compose projects and the settings file of wedding-portal are listed Then wedding-portal runs as compose project wedding-portal from its own app directory and settings file, and no other compose project uses that directory or file (H1).
- [ ] #8 Given the new server after the release When the active nginx site file is compared with the site file in git at the released commit with ${PUBLIC_HOST} replaced by the value of PUBLIC_HOST from the server's settings file Then they are identical, and the release installed it through the installer of P4-E01-T02 with no step that activates a site file by hand (H8, UD-19.n2).
- [ ] #9 Given the release on the new server after the first admin signed in When a smoke run through https://<PUBLIC_HOST> opens /g/ with a test portal token, calls the portal with that token, opens /invite/ with a test invite token and calls previewInvite with it, and sends a test access code, and the host nginx logs and the container logs since the first start are searched Then none of the three test values appears, and neither the first admin's address nor password appears (H10).
- [ ] #10 Given the new server after the release When the first run of the backup schedule set in BACKUP_SCHEDULE has passed Then a dump of the wedding-portal database exists in the BACKUP_TARGET directory outside the database container and its volume, and the job's exit status is 0 (H9, UD-19.n3).
- [ ] #11 Given the new server after the release When GET /api/health is sent on the server to its own nginx on 127.0.0.1, over plain http on port 80 and over https on port 443, each with the host name <PUBLIC_HOST> Then port 80 answers 301 with a Location header naming the same path over https, and port 443 presents the certificate from the certificate file cloudflare-origin.pem that the site file names, issued by the Cloudflare origin certificate authority, whose subject alternative names cover <PUBLIC_HOST>, and answers 200 with the three security headers of P4-E01-T03 (UD-19.b).
- [ ] #12 Given the release on the new server When https://<PUBLIC_HOST>/api/health, https://<PUBLIC_HOST>/planner/, https://<PUBLIC_HOST>/g/ with a test portal token and https://<PUBLIC_HOST>/ are requested through Cloudflare Then the first answers 200 {"ok":true}, and the other three answer 200 with the index.html built by P4-E02-T05 from the pinned commit of planner, rekord-couple and rekord-dj respectively (UD-19.n1).
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
