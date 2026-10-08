package app.rekord.application.security;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import app.rekord.application.ResourceTestProfile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.response.Response;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(ResourceTestProfile.class)
@Tag("resource-test")
class RouteGuardIT {

    @Test
    void an_authenticated_route_refuses_a_visitor_without_a_session_with_the_not_signed_in_fixture_and_never_runs_its_body()
            throws IOException {
        assertRefusedAsNotSignedIn("/api/test-only/route-guard/authenticated");
    }

    @Test
    void a_roles_allowed_route_refuses_a_visitor_without_a_session_the_same_way() throws IOException {
        assertRefusedAsNotSignedIn("/api/test-only/route-guard/roles-allowed");
    }

    @Test
    void the_health_route_still_answers_200_ok_true_to_a_visitor_without_a_session() {
        // Given: no cookie, no session

        // When
        Response response = given().when().get("/api/health");

        // Then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.getHeader("Content-Type")).isEqualTo("application/json;charset=UTF-8");
        assertThat(response.then().extract().asByteArray()).isEqualTo("{\"ok\":true}".getBytes(StandardCharsets.UTF_8));
    }

    private static void assertRefusedAsNotSignedIn(String path) throws IOException {
        // Given: the fixture of the 401 answer, no session, and a body that has not run yet
        JsonNode fixture;
        try (InputStream in = RouteGuardIT.class.getResourceAsStream("/fixtures/error-not-signed-in-401.json")) {
            assertThat(in).as("fixtures/error-not-signed-in-401.json on the test classpath").isNotNull();
            fixture = new ObjectMapper().readTree(in);
        }
        RouteGuardProbeResource.CALLS.set(0);

        // When
        Response response = given().when().get(path);

        // Then
        byte[] body = response.then().extract().asByteArray();
        assertThat(response.statusCode()).isEqualTo(fixture.get("status").asInt());
        assertThat(response.getHeader("Content-Type")).isEqualTo(fixture.get("contentType").asText());
        assertThat(body).isEqualTo(fixture.get("body").asText().getBytes(StandardCharsets.UTF_8));
        assertThat(RouteGuardProbeResource.CALLS.get()).as("calls of the route body").isZero();
    }
}
