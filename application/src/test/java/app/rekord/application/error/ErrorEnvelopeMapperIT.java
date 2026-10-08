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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@QuarkusTest
@TestProfile(ResourceTestProfile.class)
@Tag("resource-test")
class ErrorEnvelopeMapperIT {

    private static final String PROBE = "/api/test-only/error-envelope";

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
        "/test-only/error-envelope/unauthorized,error-authentication-failed-401",
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
}
