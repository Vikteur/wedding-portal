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
}
