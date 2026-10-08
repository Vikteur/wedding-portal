---
id: TASK-30.2
title: >-
  P4-E01-T02 nginx site configuration installed from git through a checked
  installer
status: To Do
assignee: []
created_date: '2026-10-07 07:18'
labels:
  - technical
  - P4
  - stop-production-config
milestone: m-4
dependencies:
  - TASK-30.1
references:
  - 'rekord-api/.github/workflows/deploy.yml:208-214'
  - 'rekord-backend/deploy/bootstrap.sh:57-121'
  - 'rekord-api/.gitattributes:1-9'
  - 'rekord-api/deploy/nginx/rekord.conf:1-101'
  - 'rekord-api/deploy/nginx/rekord.conf:17-28'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/analysis/16-build-test-deploy-ops.md
parent_task_id: TASK-30
priority: high
type: task
ordinal: 40102
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As an operator of wedding-portal, I want the proxy configuration applied only from git through an installer that tests it first so that a routing change is reproducible and a broken file never goes live.

H8: rekord-api's nginx config is shipped as rekord.conf.new and activated by hand, so a routing change is not reproducible from git (F-14; rekord-api/.github/workflows/deploy.yml:208-214). BR-OPS-20: the Python stack already installs its site from git through a root-owned, argument-less installer that runs nginx -t and restores the old file on failure (rekord-backend/deploy/bootstrap.sh:57-121). UX-16: LF line endings keep the file valid on Linux (rekord-api/.gitattributes:1-9). UD-19.n2: the host name is the deployment setting PUBLIC_HOST, set in the settings file /opt/wedding-portal/.env on the server; the site file in git names the host only as the placeholder ${PUBLIC_HOST}, which the installer fills in before it tests the file, so no tracked file or document names the value (rekord-api's site file writes the host name into git, rekord-api/deploy/nginx/rekord.conf:20, :28). A STOP item (production configuration). Its criteria run in CI; that the new server serves the site file of the released commit is checked in the release, P4-E04-T02.

- STOP (human approval in the pull request): production-config
- Covers: H8, UX-16, BR-OPS-20, UD-19.n2

Plan item `P4-E01-T02` (technical,P4,stop-production-config) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given the compose stack of P4-E01-T01 started in CI with an nginx container in front, a root-owned installer that takes no arguments in that container, PUBLIC_HOST set to portal.example.com in /opt/wedding-portal/.env of that container, copied from ci/test.env, and a changed site file of wedding-portal in the checkout When CI runs the installer Then it copies the site file and the files the site file includes from the checkout into place with every ${PUBLIC_HOST} replaced by portal.example.com and every other nginx variable left as written, runs nginx -t and reloads nginx only when the test passes.
- [ ] #2 Given a site file that nginx -t rejects When the installer runs Then it puts the previous site file back, nginx keeps serving with it, and the installer exits non-zero so the pipeline run is red.
- [ ] #3 Given /opt/wedding-portal/.env with PUBLIC_HOST unset or empty When the installer runs Then it exits non-zero before it copies the site file, names PUBLIC_HOST, and nginx keeps serving with the previous site file (UD-19.n2).
- [ ] #4 Given the site file of wedding-portal in git When its server_name directives are read Then each reads server_name ${PUBLIC_HOST}; and no host name is written anywhere else in the file (UD-19.n2).
- [ ] #5 Given the .gitattributes of wedding-portal When the site file and the shell scripts are checked out on Windows Then their line endings are LF.
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
