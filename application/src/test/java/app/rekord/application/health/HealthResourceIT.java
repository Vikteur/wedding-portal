package app.rekord.application.health;

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
import java.util.Locale;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(ResourceTestProfile.class)
@Tag("resource-test")
class HealthResourceIT {

    @Test
    void answers_200_with_json_ok_true_to_a_visitor_without_a_session() {
        // Given: no cookie, no session

        // When
        Response response = given().when().get("/api/health");

        // Then
        assertThat(response.statusCode()).isEqualTo(200);
        String contentType = response.getHeader("Content-Type");
        assertThat(contentType).as("Content-Type header").isNotNull();
        String mediaType = contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
        byte[] body = response.then().extract().asByteArray();
        assertThat(mediaType).isEqualTo("application/json");
        assertThat(body).isEqualTo("{\"ok\":true}".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void answers_exactly_what_rekord_api_answers() throws Exception {
        // Given
        JsonNode fixture;
        try (InputStream in = getClass().getResourceAsStream("/fixtures/health-200.json")) {
            assertThat(in).as("fixtures/health-200.json on the test classpath").isNotNull();
            fixture = new ObjectMapper().readTree(in);
        }

        // When
        Response response = given().when().get("/api/health");

        // Then
        byte[] body = response.then().extract().asByteArray();
        assertThat(response.statusCode()).isEqualTo(fixture.get("status").asInt());
        assertThat(response.getHeader("Content-Type")).isEqualTo(fixture.get("contentType").asText());
        assertThat(body).isEqualTo(fixture.get("body").asText().getBytes(StandardCharsets.UTF_8));
    }
}
