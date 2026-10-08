---
id: TASK-30.3
title: >-
  P4-E01-T03 TLS with a Cloudflare origin certificate, a 301 from port 80, a
  300m body cap and headers
status: To Do
assignee: []
created_date: '2026-10-07 07:18'
labels:
  - technical
  - P4
  - stop-production-config
  - stop-crypto-logging
  - stop-auth-access
milestone: m-4
dependencies:
  - TASK-30.2
references:
  - 'rekord-api/deploy/nginx/rekord.conf:17-56'
  - 'docs/rewrite/analysis/16-build-test-deploy-ops.md:197-198'
  - 'rekord-api/deploy/nginx/rekord.conf:89-91'
  - 'docs/rewrite/analysis/16-build-test-deploy-ops.md:252'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/analysis/16-build-test-deploy-ops.md
parent_task_id: TASK-30
priority: high
type: task
ordinal: 40103
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As an operator of wedding-portal, I want TLS, the upload cap and the security headers set by the proxy configuration in git so that every request reaches wedding-portal over HTTPS with the same limits rekord-api uses.

BR-OPS-14 and UD-19.b: the new server sits behind Cloudflare, as the POC does, and TLS terminates at host nginx with a Cloudflare origin certificate in the file cloudflare-origin.pem and its key in the file cloudflare-origin.key, at the system paths rekord-api's site file names for them (rekord-api/deploy/nginx/rekord.conf:30-31). The certificate and its key are server secrets: they are kept only on the new server and are never written into a document, the repository or a CI secret. Plain HTTP gets a 301 to HTTPS and upload bodies are capped at 300m at the proxy (rekord-api/deploy/nginx/rekord.conf:17-36). The server block sets nosniff, HSTS and the Referrer-Policy (rekord.conf:40-42) and proxies /api/ to the app on loopback with the forwarded headers and a 300 s read timeout (rekord.conf:45-56). The locations of the frontend bundles that set headers of their own belong to P4-E01-T06; as in rekord.conf they do not repeat the server-level headers, which nginx drops in a location that adds its own (16 R-13). One change from rekord.conf: X-Forwarded-For is set to the peer address with proxy_set_header X-Forwarded-For $remote_addr instead of appended with $proxy_add_x_forwarded_for (rekord.conf:49), because Vert.x takes the leftmost entry and an appended header would let a client choose the address the sign-in throttle of P1-E01-T05 counts. Behind Cloudflare the peer address nginx sees is a Cloudflare edge address, and rekord-api has no real_ip or CF-Connecting-IP handling (16 R-14), so with the X-Forwarded-For change alone the throttle of UD-16.UX-13, which counts per client, would count per Cloudflare edge and let a stranger behind the same edge lock out an address with 5 wrong passwords. The site file therefore restores the client address with the nginx realip module: real_ip_header CF-Connecting-IP, trusted only from the address ranges Cloudflare publishes at https://www.cloudflare.com/ips-v4 and https://www.cloudflare.com/ips-v6, kept as set_real_ip_from lines in the tracked file cloudflare-real-ip.conf that the site file includes (a deliberate deviation UD-16.UX-13 from rekord.conf, which has none). $remote_addr, and with it X-Real-IP, X-Forwarded-For and the access log, then holds the real client, and a peer outside those ranges keeps its own address whatever CF-Connecting-IP or X-Forwarded-For it sends. The CI test uses Docker networks with the subnets 173.245.48.0/24, inside the published Cloudflare range 173.245.48.0/20, and 192.0.2.0/24, outside every published range. The ticket sets the TLS key nginx serves and the client address the sign-in throttle counts, so it is a STOP item for crypto and logging and for authentication as well. The host name is the deployment setting PUBLIC_HOST (UD-19.n2). The SmallRye endpoints under /q/ stay on loopback (16 section 2). Its criteria run in CI with PUBLIC_HOST set to portal.example.com and a self-signed test certificate generated in the run; the origin certificate on the new server is checked in the release, P4-E04-T02.

- STOP (human approval in the pull request): production-config, crypto-logging, auth-access
- Covers: BR-OPS-14, UD-19.b, UD-19.n2

Plan item `P4-E01-T03` (technical,P4,stop-production-config,stop-crypto-logging,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given nginx started in CI on the runner's host network next to the compose stack of P4-E01-T01, with the wedding-portal site file installed by the installer of P4-E01-T02 with PUBLIC_HOST set to portal.example.com and a self-signed test certificate generated in the run at the file paths the site file names When a request is sent to port 80 of portal.example.com Then the status is 301 with a Location header naming the same path over https.
- [ ] #2 Given the TLS server block in git When it is read Then its ssl_certificate and ssl_certificate_key directives name the files cloudflare-origin.pem and cloudflare-origin.key at the same paths as rekord-api/deploy/nginx/rekord.conf:30-31 (UD-19.b), no tracked file of wedding-portal holds a certificate or a private key, and no step of the release workflow writes either file on the new server.
- [ ] #3 Given nginx started in CI on the runner's host network next to the compose stack of P4-E01-T01, with the wedding-portal site file installed by the installer of P4-E01-T02 with PUBLIC_HOST set to portal.example.com and a self-signed test certificate generated in the run at the file paths the site file names, and an upload body of 300 MiB plus one byte (client_max_body_size 300m; rekord-api/deploy/nginx/rekord.conf:36) When it reaches nginx Then nginx answers 413 and the request never reaches wedding-portal.
- [ ] #4 Given nginx started in CI on the runner's host network next to the compose stack of P4-E01-T01, with the wedding-portal site file installed by the installer of P4-E01-T02 with PUBLIC_HOST set to portal.example.com and a self-signed test certificate generated in the run at the file paths the site file names When the headers of the HTTPS answers to /api/health and /api/weddings are read Then each carries X-Content-Type-Options nosniff, Strict-Transport-Security "max-age=31536000; includeSubDomains" and Referrer-Policy "strict-origin-when-cross-origin".
- [ ] #5 Given nginx started in CI on the runner's host network next to the compose stack of P4-E01-T01, with the wedding-portal site file installed by the installer of P4-E01-T02 with PUBLIC_HOST set to portal.example.com and a self-signed test certificate generated in the run at the file paths the site file names, and a request to /api/ When nginx passes it on Then it goes to 127.0.0.1 on the app port with the headers Host, X-Real-IP, X-Forwarded-For and X-Forwarded-Proto and a proxy read timeout of 300 s, and the site file sets X-Forwarded-For with $remote_addr and names $proxy_add_x_forwarded_for nowhere.
- [ ] #6 Given the nginx site file of wedding-portal When its locations are read Then only location /api/ passes requests to wedding-portal, so a request for /q/health or /q/metrics from outside the server never reaches the app (rekord-api/deploy/nginx/rekord.conf:45-46).
- [ ] #7 Given the CI nginx of the first criterion, with PUBLIC_HOST set to portal.example.com and a self-signed test certificate, and a CI client container at 192.0.2.10, outside every published Cloudflare range When that client sends 5 logins for planner@example.com with a wrong password, each with a different CF-Connecting-IP from 203.0.113.1 to 203.0.113.5 and a different X-Forwarded-For from 198.51.100.1 to 198.51.100.5, then one with the right password, CF-Connecting-IP 198.51.100.4 and X-Forwarded-For 198.51.100.4 Then the 6th answers 429 RATE_LIMITED, because nginx ignores both headers from that peer and forwards X-Forwarded-For as the peer address 192.0.2.10 (P1-E01-T05).
- [ ] #8 Given the CI nginx of the first criterion, with PUBLIC_HOST set to portal.example.com and a self-signed test certificate, and a CI client container at 173.245.48.10, inside the published Cloudflare range 173.245.48.0/20 When that client sends 5 logins for planner@example.com with a wrong password and the header CF-Connecting-IP 203.0.113.9, then one with the right password and CF-Connecting-IP 198.51.100.4, then one with the right password and CF-Connecting-IP 203.0.113.9 Then the 6th answers 200 and the 7th answers 429 RATE_LIMITED, because the throttle counts per client (deviation UD-16.UX-13; rekord-api has no sign-in throttle and rekord.conf reads no CF-Connecting-IP).
- [ ] #9 Given cloudflare-real-ip.conf and the site file in git When CI runs on a pull request of wedding-portal Then the site file includes cloudflare-real-ip.conf and sets real_ip_header CF-Connecting-IP, and the run is red when the set_real_ip_from lines of cloudflare-real-ip.conf differ from the ranges published at that moment at https://www.cloudflare.com/ips-v4 and https://www.cloudflare.com/ips-v6.
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
