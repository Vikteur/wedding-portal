package app.rekord.application.health;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import app.rekord.application.error.LogCapture;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.response.Response;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.logging.LogRecord;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/** AC1, AC2, AC3 at run time (BR-OPS-21): readiness reports the database, health and liveness ignore it. */
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
    void the_scrape_names_no_database_user_password_or_address_while_postgresql_runs() {
        // Given: PostgreSQL runs, so the datasource is active

        // When
        String scrape =
                given().accept("text/plain").when().get("/q/metrics").asString().toLowerCase(Locale.ROOT);

        // Then
        assertThat(scrape).as("a scrape with the JVM meters in it").contains("jvm_memory_used_bytes");
        assertThat(scrape)
                .doesNotContain(StoppablePostgres.USER.toLowerCase(Locale.ROOT))
                .doesNotContain(StoppablePostgres.PASSWORD.toLowerCase(Locale.ROOT))
                .doesNotContain("jdbc:postgresql");
    }

    @Test
    @Order(3)
    void ready_answers_503_down_with_the_database_check_down_once_postgresql_is_stopped() throws Exception {
        // Given: SmallRye Health logs a DOWN response at INFO, so the log is watched as well as the body
        LogCapture log = new LogCapture();
        log.start();
        Response response;
        try {
            StoppablePostgres.stopDatabase();

            // When: the pool may still hold a connection, so poll until the check notices
            Instant deadline = Instant.now().plus(DEADLINE);
            response = given().when().get("/q/health/ready");
            while (response.statusCode() != 503 && Instant.now().isBefore(deadline)) {
                Thread.sleep(500);
                response = given().when().get("/q/health/ready");
            }
        } finally {
            log.stop();
        }

        // Then
        assertThat(response.statusCode())
                .as("readiness within %s, last body: %s", DEADLINE, response.asString())
                .isEqualTo(503);
        byte[] raw = response.asByteArray();
        JsonNode body = JSON.readTree(raw);
        assertThat(body.path("status").asText()).isEqualTo("DOWN");
        assertThat(check(body).path("status").asText()).isEqualTo("DOWN");
        // D3: a refused connection names neither the password nor the user (a failed login would name the user)
        String text = new String(raw, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        assertThat(text).doesNotContain(StoppablePostgres.PASSWORD.toLowerCase(Locale.ROOT));
        assertThat(text).doesNotContain(StoppablePostgres.USER.toLowerCase(Locale.ROOT));
        // The log holds the DOWN response, so the password check below cannot pass on an empty capture
        assertThat(log.records())
                .as("the DOWN response is logged")
                .anyMatch(r -> LogCapture.text(r).contains(DATABASE_CHECK));
        for (LogRecord record : log.records()) {
            assertThat(LogCapture.text(record).toLowerCase(Locale.ROOT))
                    .as("log record %s", record.getLoggerName())
                    .doesNotContain(StoppablePostgres.PASSWORD.toLowerCase(Locale.ROOT));
        }
    }

    @Test
    @Order(4)
    void health_answers_200_ok_true_while_postgresql_is_stopped() {
        // Given: PostgreSQL is stopped (this call stops it when the test runs alone; after Order 3 it is a no-op)
        StoppablePostgres.stopDatabase();

        // When
        Response response = given().when().get("/api/health");

        // Then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.getHeader("Content-Type").split(";")[0].trim()).isEqualTo("application/json");
        assertThat(response.asByteArray()).isEqualTo("{\"ok\":true}".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @Order(5)
    void live_answers_200_up_without_the_database_check_while_postgresql_is_stopped() throws Exception {
        // Given: PostgreSQL is stopped (this call stops it when the test runs alone; after Order 3 it is a no-op)
        StoppablePostgres.stopDatabase();

        // When
        Response response = given().when().get("/q/health/live");

        // Then
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode body = JSON.readTree(response.asByteArray());
        assertThat(body.path("status").asText()).isEqualTo("UP");
        assertThat(body.path("checks").findValuesAsText("name")).doesNotContain(DATABASE_CHECK);
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
