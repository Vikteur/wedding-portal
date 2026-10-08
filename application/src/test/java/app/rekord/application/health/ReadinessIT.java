package app.rekord.application.health;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.response.Response;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/** AC1, AC2, AC3 at run time (BR-OPS-21): readiness reports the database, health ignores it. */
@QuarkusTest
@TestProfile(ReadinessTestProfile.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReadinessIT {

    private static final String DATABASE_CHECK = "Database connections health check";
    private static final Duration DEADLINE = Duration.ofSeconds(30);
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    @Order(1)
    void ready_answers_200_up_with_the_database_check_up_while_postgresql_runs() throws Exception {
        // Given: PostgreSQL runs

        // When
        Response response = given().when().get("/q/health/ready");

        // Then
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode body = JSON.readTree(response.asByteArray());
        assertThat(body.path("status").asText()).isEqualTo("UP");
        assertThat(check(body).path("status").asText()).isEqualTo("UP");
    }

    @Test
    @Order(2)
    void ready_answers_503_down_with_the_database_check_down_once_postgresql_is_stopped() throws Exception {
        // Given
        StoppablePostgres.stopDatabase();

        // When: the pool may still hold a connection, so poll until the check notices
        Instant deadline = Instant.now().plus(DEADLINE);
        Response response = given().when().get("/q/health/ready");
        while (response.statusCode() != 503 && Instant.now().isBefore(deadline)) {
            Thread.sleep(500);
            response = given().when().get("/q/health/ready");
        }

        // Then
        assertThat(response.statusCode()).as("readiness within " + DEADLINE).isEqualTo(503);
        byte[] raw = response.asByteArray();
        JsonNode body = JSON.readTree(raw);
        assertThat(body.path("status").asText()).isEqualTo("DOWN");
        assertThat(check(body).path("status").asText()).isEqualTo("DOWN");
        String text = new String(raw, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        assertThat(text).doesNotContain(StoppablePostgres.PASSWORD.toLowerCase(Locale.ROOT));
        assertThat(text).doesNotContain(StoppablePostgres.USER.toLowerCase(Locale.ROOT));
    }

    @Test
    @Order(3)
    void health_answers_200_ok_true_while_postgresql_is_stopped() {
        // Given: PostgreSQL is stopped (stopping again is harmless when this runs alone)
        StoppablePostgres.stopDatabase();

        // When
        Response response = given().when().get("/api/health");

        // Then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.getHeader("Content-Type").split(";")[0].trim()).isEqualTo("application/json");
        assertThat(response.asByteArray()).isEqualTo("{\"ok\":true}".getBytes(StandardCharsets.UTF_8));
    }

    private static JsonNode check(JsonNode body) {
        for (JsonNode c : body.path("checks")) {
            if (DATABASE_CHECK.equals(c.path("name").asText())) {
                return c;
            }
        }
        throw new AssertionError("no check named '" + DATABASE_CHECK + "' in " + body);
    }
}
