package app.rekord.application.error;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import app.rekord.application.ResourceTestProfile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.response.Response;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@QuarkusTest
@TestProfile(ResourceTestProfile.class)
@Tag("resource-test")
class ErrorEnvelopeMapperIT {

    private static final String PROBE = "/api/test-only/error-envelope";

    private final LogCapture log = new LogCapture();

    @BeforeEach
    void startCapture() {
        log.start();
    }

    @AfterEach
    void stopCapture() {
        log.stop();
    }

    private static void assertAnswersAsFixture(Response response, String fixtureName) throws Exception {
        JsonNode fixture;
        try (InputStream in = ErrorEnvelopeMapperIT.class.getResourceAsStream("/fixtures/" + fixtureName + ".json")) {
            assertThat(in).as("fixtures/%s.json on the test classpath", fixtureName).isNotNull();
            fixture = new ObjectMapper().readTree(in);
        }
        byte[] body = response.then().extract().asByteArray();
        assertThat(response.statusCode()).isEqualTo(fixture.get("status").asInt());
        assertThat(response.getHeader("Content-Type")).isEqualTo(fixture.get("contentType").asText());
        assertThat(body).isEqualTo(fixture.get("body").asText().getBytes(StandardCharsets.UTF_8));
    }

    @ParameterizedTest
    @CsvSource({
        "not-found,error-family-not-found-404",
        "rejected,error-family-rejected-409",
        "not-permitted,error-family-not-permitted-401",
        "upstream-unavailable,error-family-upstream-unavailable-503"
    })
    void a_family_error_answers_exactly_what_rekord_api_answers(String probe, String fixture) throws Exception {
        // When
        Response response = given().when().get(PROBE + "/family/" + probe);

        // Then
        assertAnswersAsFixture(response, fixture);
    }

    @ParameterizedTest
    @CsvSource({
        "/test-only/error-envelope/auth-failed,error-authentication-failed-401",
        "/test-only/error-envelope/unauthorized,error-not-signed-in-401",
        "/test-only/error-envelope/forbidden,error-forbidden-403",
        "/no-such-route,error-unknown-route-404",
        "/q/metrics,error-q-metrics-404",
        "/test-only/error-envelope/uuid/not-a-uuid,error-malformed-uuid-404"
    })
    void the_framework_refusals_answer_exactly_what_rekord_api_answers(String path, String fixture) throws Exception {
        // When
        Response response = given().when().get("/api" + path);

        // Then
        assertAnswersAsFixture(response, fixture);
    }

    @Test
    void the_role_denied_forbidden_is_left_to_the_framework_not_the_catch_all() {
        // When
        Response response = given().when().get(PROBE + "/role-denied");

        // Then: the framework's 403, not the catch-all's 500 envelope, and nothing logged
        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.asString()).doesNotContain("\"detail\"");
        assertThat(log.fromMapper()).isEmpty();
        assertThat(log.errors()).isEmpty();
    }

    @Test
    void a_broken_generated_dto_answers_422_validation_failed_as_rekord_api_does() throws Exception {
        // Given
        JsonNode fixture;
        try (InputStream in = getClass().getResourceAsStream("/fixtures/error-validation-422.json")) {
            assertThat(in).isNotNull();
            fixture = new ObjectMapper().readTree(in);
        }
        JsonNode expected = new ObjectMapper().readTree(fixture.get("body").asText());
        String[] parts = expected.at("/detail/message").asText().split("; ");

        // When
        Response response = given().contentType("application/json")
                .body("{\"display_name\":\"\",\"password\":\"short\"}")
                .when()
                .post(PROBE + "/invite-accept");

        // Then: the set has no defined order, so either order of the same two parts
        assertThat(response.statusCode()).isEqualTo(fixture.get("status").asInt());
        assertThat(response.getHeader("Content-Type")).isEqualTo(fixture.get("contentType").asText());
        JsonNode body = new ObjectMapper().readTree(response.asByteArray());
        assertThat(body.at("/detail/code").asText()).isEqualTo(expected.at("/detail/code").asText());
        assertThat(parts).hasSize(2);
        assertThat(body.at("/detail/message").asText())
                .isIn(parts[0] + "; " + parts[1], parts[1] + "; " + parts[0]);
    }

    @Test
    void an_unmapped_web_application_exception_answers_exactly_what_rekord_api_answers() throws Exception {
        assertAnswersAsFixture(
                given().when().get(PROBE + "/web-application-exception"), "error-unmapped-409");
    }

    @Test
    void a_post_to_a_get_only_route_answers_exactly_what_rekord_api_answers() throws Exception {
        assertAnswersAsFixture(given().when().post("/api/health"), "error-method-not-allowed-405");
    }

    // --- the catch-all: 500 UNKNOWN, one redacted ERROR line (STOP crypto-logging) ---

    private void assertLoggedOnceRedacted(String path) {
        assertThat(log.errors()).as("ERROR records across all loggers").hasSize(1);
        var record = log.errors().get(0);
        assertThat(record.getLoggerName()).isEqualTo(ErrorEnvelopeMapper.class.getName());
        assertThat(record.getMessage()).isEqualTo("Unhandled exception");
        assertThat(record.getParameters()).isNullOrEmpty();
        assertThat(record.getThrown()).isNotNull();
        for (var captured : log.records()) {
            String text = LogCapture.text(captured);
            for (String sentinel : ErrorEnvelopeProbeResource.SENTINELS) {
                assertThat(text).as("record text").doesNotContain(sentinel);
            }
            assertThat(text).doesNotContain(path);
        }
    }

    @Test
    void an_unhandled_exception_answers_the_500_fixture_and_logs_one_redacted_error() throws Exception {
        // When
        Response response = given().when().get(PROBE + "/unhandled");

        // Then
        assertAnswersAsFixture(response, "error-unhandled-500");
        assertLoggedOnceRedacted("/test-only/error-envelope/unhandled");
        String text = LogCapture.text(log.errors().get(0));
        assertThat(text)
                .contains("java.lang.IllegalStateException")
                .contains("Caused by: java.sql.SQLException")
                .contains("ErrorEnvelopeProbeResource");
    }

    @Test
    void a_web_application_exception_of_503_answers_the_500_fixture_and_logs_once() throws Exception {
        Response response = given().when().get(PROBE + "/service-unavailable");

        assertAnswersAsFixture(response, "error-unhandled-500");
        assertLoggedOnceRedacted("/test-only/error-envelope/service-unavailable");
    }

    @Test
    void a_web_application_exception_of_exactly_500_answers_the_500_fixture_and_logs_once() throws Exception {
        // When
        Response response = given().when().get(PROBE + "/internal-server-error");

        // Then: 500 is a server fault, so it is logged like the 503, not kept as a quiet client refusal
        assertAnswersAsFixture(response, "error-unhandled-500");
        assertLoggedOnceRedacted("/test-only/error-envelope/internal-server-error");
    }

    @Test
    void a_family_error_without_a_table_row_answers_the_500_fixture_and_logs_once() throws Exception {
        Response response = given().when().get(PROBE + "/pair-without-row");

        assertAnswersAsFixture(response, "error-unhandled-500");
        assertLoggedOnceRedacted("/test-only/error-envelope/pair-without-row");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "/test-only/error-envelope/family/not-found",
                "/test-only/error-envelope/family/rejected",
                "/test-only/error-envelope/family/not-permitted",
                "/test-only/error-envelope/family/upstream-unavailable",
                "/test-only/error-envelope/auth-failed",
                "/test-only/error-envelope/unauthorized",
                "/test-only/error-envelope/forbidden",
                "/test-only/error-envelope/web-application-exception",
                "/test-only/error-envelope/uuid/not-a-uuid",
                "/no-such-route"
            })
    void a_refusal_below_500_writes_no_log_record_from_the_mapper(String path) {
        given().when().get("/api" + path).then().extract().asByteArray();

        assertThat(log.fromMapper()).isEmpty();
        assertThat(log.errors()).isEmpty();
    }

    @Test
    void a_405_and_a_422_write_no_log_record_from_the_mapper() {
        given().when().post("/api/health").then().extract().asByteArray();
        given().contentType("application/json")
                .body("{\"display_name\":\"\",\"password\":\"short\"}")
                .when()
                .post(PROBE + "/invite-accept")
                .then()
                .extract()
                .asByteArray();

        assertThat(log.fromMapper()).isEmpty();
        assertThat(log.errors()).isEmpty();
    }
}
