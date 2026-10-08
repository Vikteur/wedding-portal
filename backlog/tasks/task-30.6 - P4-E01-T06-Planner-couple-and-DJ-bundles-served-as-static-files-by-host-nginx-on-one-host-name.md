---
id: TASK-30.6
title: >-
  P4-E01-T06 Planner, couple and DJ bundles served as static files by host nginx
  on one host name
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
  - TASK-30.5
references:
  - 'rekord-api/deploy/nginx/rekord.conf:11-15'
  - 'rekord-api/deploy/nginx/rekord.conf:57-100'
  - 'rekord-couple/nginx.conf:25-46'
  - 'rekord-dj/nginx.conf:26-39'
  - 'planner/vite.config.ts:17-20'
  - 'rekord-couple/vite.config.ts:7'
  - 'rekord-dj/vite.config.ts:7-25'
  - 'docs/rewrite/analysis/16-build-test-deploy-ops.md:150-152'
  - 'rekord-api/src/main/java/app/rekord/account/AccountsResource.java:120'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/analysis/16-build-test-deploy-ops.md
parent_task_id: TASK-30
priority: high
type: feature
ordinal: 40106
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a partner of the couple, I want our link to open the couple app on the wedding-portal host without the link reaching other sites, shared caches or logs so that nobody else can open our portal.

UD-19.n1: the release serves the three unchanged frontend bundles (planner, rekord-couple, rekord-dj) as static files through nginx on the new server, so BR-OPS-15, BR-OPS-16 and BR-OPS-17 are part of the site file of wedding-portal. The oracle is rekord-api's site file, which serves the same three bundles from one web root (rekord-api/deploy/nginx/rekord.conf:57-100): the DJ bundle owns / because it is built without a base (rekord-dj/vite.config.ts:7-25), the planner is built with base /planner/ (planner/vite.config.ts:17-20) and the couple bundle with base /guest/, its magic link /g/<token> answered with the couple's index.html (rekord-couple/vite.config.ts:7). A magic link is a secret in the URL: rekord-api's host nginx sends no-referrer and noindex on /g/ and /guest/ and writes /g/ hits to a separate log with the path redacted (rekord.conf:11-15, :62-77), and the couple container adds Cache-Control no-store on /g/ (rekord-couple/nginx.conf:35-40); the release runs no couple container, so the host site file sends that header itself, a deliberate deviation from rekord-api's host site file, which does not (UD-19.n1). Content-hashed assets are cached for a year and the SPA shells are not (rekord.conf:93-100). The apps call /api on their own origin (BR-OPS-18), so serving them on the host of PUBLIC_HOST needs no frontend change. An invite is opened in the browser at its accept_url, the public base followed by /invite/ and the token (rekord-api/src/main/java/app/rekord/account/AccountsResource.java:120). rekord-api's site file has no /invite/ location, so that path falls into location /, which writes the full path to the default access log, and the assets that page loads send it as Referer under the server-level strict-origin-when-cross-origin policy, which the combined log format writes (16 R-08 names only the API paths). The site file therefore answers /invite/<token> in a location ^~ /invite/ of its own with the DJ shell that location / serves, a redacted log line, no-referrer, noindex and no-store, and the access log format of the server writes the referrer field as "-" for every request (fixed defect H10, RISK-13). The web root is /opt/wedding-portal/web (P4-E01-T01). Building and copying the real bundles is P4-E02-T05. The test web root of this ticket holds planner/index.html, guest/index.html and index.html with the texts planner-shell, couple-shell and dj-shell, and the asset files planner/assets/index-1a2b3c4d.js, guest/assets/index-1a2b3c4d.js and assets/index-1a2b3c4d.js. A STOP item (production configuration, and what gets logged for the redacted magic-link log). Its criteria run in CI; the real bundles on the new server are checked in the release, P4-E04-T02.

- STOP (human approval in the pull request): production-config, crypto-logging
- Covers: BR-OPS-15, BR-OPS-16, BR-OPS-17, UD-19.n1, H10, RISK-13

Plan item `P4-E01-T06` (user-story,P4,stop-production-config,stop-crypto-logging) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given the CI nginx of P4-E01-T03 serving the test web root of this ticket When https://portal.example.com/planner/ and https://portal.example.com/planner/weddings/42 are requested Then both answer 200 with the text planner-shell (BR-OPS-17).
- [ ] #2 Given the CI nginx of P4-E01-T03 serving the test web root of this ticket When https://portal.example.com/g/tok-example-123, https://portal.example.com/guest/ and https://portal.example.com/guest/songs are requested Then each answers 200 with the text couple-shell (BR-OPS-17).
- [ ] #3 Given the CI nginx of P4-E01-T03 serving the test web root of this ticket When https://portal.example.com/, https://portal.example.com/library and https://portal.example.com/api/health are requested Then the first two answer 200 with the text dj-shell, and the third answers 200 {"ok":true} from wedding-portal (BR-OPS-17).
- [ ] #4 Given the CI nginx of P4-E01-T03 serving the test web root of this ticket When https://portal.example.com/g/tok-example-123 and https://portal.example.com/guest/ are requested Then both answers carry Referrer-Policy no-referrer, X-Robots-Tag "noindex, nofollow, noarchive" and X-Content-Type-Options nosniff, and the /g/ answer also carries Cache-Control no-store (BR-OPS-15; Cache-Control no-store is a deliberate deviation UD-19.n1: rekord-api's host nginx sends none on /g/, rekord-couple/nginx.conf:35-40 does).
- [ ] #5 Given the CI nginx of P4-E01-T03 serving the test web root of this ticket When https://portal.example.com/invite/inv-example-123 is requested Then it answers 200 with the text dj-shell and carries Referrer-Policy no-referrer, X-Robots-Tag "noindex, nofollow, noarchive", Cache-Control no-store and X-Content-Type-Options nosniff (fixed defect H10; rekord-api serves /invite/ through location / without the first three).
- [ ] #6 Given the CI nginx of P4-E01-T03 serving the test web root of this ticket When https://portal.example.com/g/tok-example-123 is requested and every nginx log of the run is searched afterwards Then the request is logged once, with the request line GET /g/[redacted] and the referrer field "-", and tok-example-123 appears in no nginx log (BR-OPS-15, H10).
- [ ] #7 Given the CI nginx of P4-E01-T03 serving the test web root of this ticket When https://portal.example.com/invite/inv-example-123 is requested, then https://portal.example.com/assets/index-1a2b3c4d.js with the header Referer https://portal.example.com/invite/inv-example-123, and every nginx log of the run is searched afterwards Then the first request is logged with the request line GET /invite/[redacted] and the referrer field "-", the second with the referrer field "-", and inv-example-123 appears in no nginx log (fixed defect H10, RISK-13; rekord-api logs both in full).
- [ ] #8 Given the CI nginx of P4-E01-T03 serving the test web root of this ticket When https://portal.example.com/planner/assets/index-1a2b3c4d.js, https://portal.example.com/guest/assets/index-1a2b3c4d.js and https://portal.example.com/assets/index-1a2b3c4d.js are requested Then each answers 200 with the Cache-Control values max-age=31536000 and "public, immutable" and an Expires header one year after its Date header (BR-OPS-16).
- [ ] #9 Given the CI nginx of P4-E01-T03 serving the test web root of this ticket When https://portal.example.com/planner/, https://portal.example.com/g/tok-example-123 and https://portal.example.com/ are requested Then none of the three answers carries max-age=31536000 or immutable (BR-OPS-16).
- [ ] #10 Given the CI nginx of P4-E01-T03 serving the test web root of this ticket When https://portal.example.com/planner/assets/missing-00000000.js is requested Then the answer is 404 and carries no shell (rekord-api/deploy/nginx/rekord.conf:96-97).
- [ ] #11 Given the CI nginx of P4-E01-T03 serving the test web root of this ticket When the headers of the answers to https://portal.example.com/planner/ and https://portal.example.com/ are read Then each carries X-Content-Type-Options nosniff, Strict-Transport-Security "max-age=31536000; includeSubDomains" and Referrer-Policy "strict-origin-when-cross-origin" (rekord-api/deploy/nginx/rekord.conf:40-42).
- [ ] #12 Given the wedding-portal site file in git When its root directive is read Then it reads root /opt/wedding-portal/web; (P4-E01-T01), and the path /opt/rekordmatch appears nowhere in the file.
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
