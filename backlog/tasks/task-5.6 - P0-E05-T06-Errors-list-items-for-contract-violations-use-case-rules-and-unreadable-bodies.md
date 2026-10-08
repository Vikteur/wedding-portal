---
id: TASK-5.6
title: >-
  P0-E05-T06 Errors-list items for contract violations, use-case rules and
  unreadable bodies
status: To Do
assignee: []
created_date: '2026-10-07 05:46'
labels:
  - user-story
  - P0
milestone: m-0
dependencies:
  - TASK-5.3
  - TASK-5.4
references:
  - 'docs/rewrite/STATUS.md:394-405'
  - 'rekord-api/src/main/java/app/rekord/error/ErrorMappers.java:89-121'
  - 'docs/rewrite/analysis/11-planner-weddings.md:203-204'
  - 'rekord-contract/dist/openapi.yaml:2019-2038'
documentation:
  - docs/rewrite/architecture-conventions.md
parent_task_id: TASK-5
priority: high
type: feature
ordinal: 506
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a member, I want every mistake of a refused request listed with its field and a fixed code so that the app can point me at each field to fix.

UD-19.d settles the items of the UD-12.b errors list that P0-E05-T04 adds to the contract. Each item is {field, code, message}; field is the request property path in the contract's snake_case with list indexes (lines[2].name), or null for a rule that checks no single property. Bean validation maps @NotNull, @NotBlank and @NotEmpty to REQUIRED, @Size below its minimum to TOO_SHORT and above its maximum to TOO_LONG, @Min, @Max, @DecimalMin and @DecimalMax to OUT_OF_RANGE, @Pattern and @Email to INVALID_FORMAT; a use-case rule uses INVALID_VALUE (UD-19.d1). The use case runs only after the contract check passes, so a request that breaks both lists only the contract violations (UD-19.d2). A body that is not JSON, a value of the wrong JSON type and an unknown enum value answer 422 VALIDATION_FAILED (UD-19.d3); a body string of the right JSON type that does not parse to its format (date, date-time, UUID) is a malformed value of the same kind and answers INVALID_FORMAT, as criterion 10 of this ticket expects for event_date "2027-13-40", while rekord-api answers 500 UNKNOWN for an unparseable date (docs/rewrite/analysis/11-planner-weddings.md:204). rekord-api answers the UD-19.d3 cases through its framework mappers and its catch-all (docs/rewrite/analysis/11-planner-weddings.md:203; ErrorMappers.java:105-121), and Jackson's default scalar coercion accepts the string "12" for a number, so each answer is recorded from rekord-api before the deviation is built. UD-19.d speaks of request body properties: a path or query parameter that does not convert raises a Jakarta REST NotFoundException, which keeps rekord-api's 404 NO_WEDDING (11-planner-weddings.md:203; P0-E05-T02). The item message of a bean-validation violation is the default English message of Hibernate Validator. The top-level message renders each item as the last segment of its field path without list index in lower camel case, a space and the item message (the rendering of P0-E05-T02 and P0-E05-T03, ErrorMappers.java:95-97), and an item with field null as its message alone, joined by "; "; a repeated rendering is written once. Bean validation reports violations as an unordered set, so contract-violation items, and their renderings, are sorted by field path in text order; use-case items keep rule order. The criteria use the test-only resource POST /api/test/validation of wedding-portal's test sources. Its test-only body class declares lower camel case properties with snake_case JSON names, as the generated DTOs do: display_name (@NotNull, @Size(min = 2, max = 80)), guest_count (integer, @Min(0), @Max(500)), budget (number, @DecimalMin("0.00")), contact_email (@Email), ref_code (@Pattern "[A-Z]{3}"), kind (an enum of opening and party), confirmed (boolean), meta (an object with the string source), tags (a list of strings, @NotEmpty), lines (a list whose name is @NotBlank and @Size(max = 80)), event_date (date), starts_at (date-time) and venue_id (UUID). V is the valid body {"display_name":"Test Wedding","guest_count":10,"budget":100.00,"contact_email":"contact@example.com","ref_code":"ABC","kind":"party","confirmed":true,"meta":{"source":"test"},"tags":["x"],"lines":[{"name":"Song"}],"event_date":"2027-06-12","starts_at":"2027-06-12T14:00:00Z","venue_id":"0b6f6d2e-3c1a-4d8e-9f00-1a2b3c4d5e6f"}. An item written as field CODE "message" in the criteria stands for {"field":"field","code":"CODE","message":"message"}, and null CODE "message" for an item whose field is null.

- Covers: UD-19.d1, UD-19.d2, UD-19.d3

Plan item `P0-E05-T06` (user-story,P0) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given rekord-api run locally with ./mvnw quarkus:dev When POST /api/auth/login (login) is sent, each time with Content-Type application/json, the body {, the body {"email":"planner@example.com","password":12345678} and the body {"email":"planner@example.com","password":"pw-test-0001","expected_role":"DANCER"} Then the status, the Content-Type header and the body of each answer are stored in wedding-portal as application/src/test/resources/fixtures/login-unreadable-body.json, login-wrong-type.json and login-unknown-enum.json.
- [ ] #2 Given the test-only resource POST /api/test/validation and the valid body V When V without display_name, with tags [] and with lines [{"name":"   "},{"name":""}] is posted with Content-Type application/json Then the answer is 422 VALIDATION_FAILED with the message "displayName must not be null; name must not be blank; tags must not be empty" and the errors list holds exactly display_name REQUIRED "must not be null", lines[0].name REQUIRED "must not be blank", lines[1].name REQUIRED "must not be blank" and tags REQUIRED "must not be empty", in that order (UD-19.d1).
- [ ] #3 Given that resource and the body V When V with display_name "A", guest_count 501, budget -0.01, contact_email "not-an-address", ref_code "ab1" and three lines whose third name has 81 characters is posted with Content-Type application/json Then the errors list holds exactly the items budget OUT_OF_RANGE, contact_email INVALID_FORMAT, display_name TOO_SHORT, guest_count OUT_OF_RANGE, lines[2].name TOO_LONG and ref_code INVALID_FORMAT in that order, and V with a display_name of 81 characters gives exactly the one item display_name TOO_LONG (UD-19.d1).
- [ ] #4 Given that resource and the body V When V with display_name "A", guest_count 501 and contact_email "not-an-address" is posted with Content-Type application/json Then the answer is 422 VALIDATION_FAILED with the message "contactEmail must be a well-formed email address; displayName size must be between 2 and 80; guestCount must be less than or equal to 500" and the errors list holds exactly contact_email INVALID_FORMAT "must be a well-formed email address", display_name TOO_SHORT "size must be between 2 and 80" and guest_count OUT_OF_RANGE "must be less than or equal to 500", in that order (UD-19.d1).
- [ ] #5 Given a test-only use case behind that resource with the rule "is reserved" on display_name, which refuses "Reserved", followed by the rule "a wedding needs guests or a budget", which checks no single property and refuses guest_count 0 with budget 0.00 When V with display_name "Reserved", guest_count 0 and budget 0.00 is posted with Content-Type application/json Then the answer is 422 VALIDATION_FAILED with the message "displayName is reserved; a wedding needs guests or a budget" and the errors list holds exactly display_name INVALID_VALUE "is reserved" and null INVALID_VALUE "a wedding needs guests or a budget", in that order (UD-19.d1).
- [ ] #6 Given that use case When V with display_name "A", guest_count 0 and budget 0.00, which breaks the @Size of display_name and the rule "a wedding needs guests or a budget", is posted with Content-Type application/json Then the answer is 422 VALIDATION_FAILED whose errors list holds exactly display_name TOO_SHORT "size must be between 2 and 80", and a test spy records zero calls of the use case (UD-19.d2).
- [ ] #7 Given that resource When the body { is posted with Content-Type application/json Then the answer is 422 VALIDATION_FAILED with the message "request body is not valid JSON" and the errors list holds exactly null INVALID_FORMAT "request body is not valid JSON" (deviation UD-19.d3; rekord-api answers as recorded in fixtures/login-unreadable-body.json).
- [ ] #8 Given that resource and the body V When V with guest_count "12", V with display_name 5, V with confirmed "yes", V with tags "x" and V with meta [] are posted, each with Content-Type application/json Then each answer is 422 VALIDATION_FAILED whose errors list holds exactly one item, guest_count INVALID_FORMAT "must be a number", display_name INVALID_FORMAT "must be a string", confirmed INVALID_FORMAT "must be true or false", tags INVALID_FORMAT "must be a list" and meta INVALID_FORMAT "must be an object" in turn, with the message "guestCount must be a number" for the first (deviation UD-19.d3; rekord-api answers as recorded in fixtures/login-wrong-type.json).
- [ ] #9 Given that resource and the body V When V with kind "dance" is posted with Content-Type application/json Then the answer is 422 VALIDATION_FAILED with the message "kind must be one of opening, party" and the errors list holds exactly kind INVALID_VALUE "must be one of opening, party", the allowed values in contract order joined by ", " (deviation UD-19.d3; rekord-api answers as recorded in fixtures/login-unknown-enum.json).
- [ ] #10 Given that resource and the body V When V with event_date "2027-13-40", V with starts_at "2027-06-12 14:00" and V with venue_id "abc" are posted, each with Content-Type application/json Then each answer is 422 VALIDATION_FAILED whose errors list holds exactly one item, event_date INVALID_FORMAT "must be a date such as 2027-06-12", starts_at INVALID_FORMAT "must be a date-time such as 2027-06-12T14:00:00Z" and venue_id INVALID_FORMAT "must be a UUID" in turn, with the message "venueId must be a UUID" for the third (deviation UD-19.d3; rekord-api answers 500 UNKNOWN to an unparseable date, docs/rewrite/analysis/11-planner-weddings.md:204).
- [ ] #11 Given the test-only resource GET /api/test/validation/{id} of wedding-portal's test sources, with id a UUID path parameter and kind an optional query parameter of the enum opening and party When GET /api/test/validation/abc and GET /api/test/validation/0b6f6d2e-3c1a-4d8e-9f00-1a2b3c4d5e6f?kind=dance are sent Then each answer is 404 with the body {"detail":{"code":"NO_WEDDING","message":"There is nothing here."}} and no errors field, the answer rekord-api gives to a malformed path or query value (docs/rewrite/analysis/11-planner-weddings.md:203; P0-E05-T02), because UD-19.d settles request body properties only.
- [ ] #12 Given a refusal other than VALIDATION_FAILED, the 401 NOT_SIGNED_IN and the 404 NO_WEDDING of P0-E05-T02 among them When it is answered Then the body carries no errors field, so its bytes equal rekord-api's answer.
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
