package app.rekord.application.metrics;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import app.rekord.application.ResourceTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.response.Response;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** AC4, AC5 and the label rule (PIN-16-0198): metrics sit at /q/metrics, never under /api, with templated URIs. */
@QuarkusTest
@TestProfile(ResourceTestProfile.class)
@Tag("resource-test")
class MetricsIT {

    private static String scrape() {
        Response response = given().when().get("/q/metrics");
        assertThat(response.statusCode()).as("scrape status").isEqualTo(200);
        return response.asString();
    }

    private static boolean hasLine(String scrape, String prefix, String... fragments) {
        return scrape.lines().anyMatch(line -> {
            if (!line.startsWith(prefix)) {
                return false;
            }
            for (String fragment : fragments) {
                if (!line.contains(fragment)) {
                    return false;
                }
            }
            return true;
        });
    }

    @Test
    void q_metrics_answers_200_in_the_prometheus_text_format_with_a_type_line() {
        // When
        Response response = given().when().get("/q/metrics");

        // Then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.getHeader("Content-Type")).startsWith("text/plain");
        assertThat(response.asString().lines().anyMatch(l -> l.startsWith("# TYPE"))).isTrue();
    }

    @Test
    void api_q_metrics_answers_404_with_no_metrics_text() {
        // When
        Response response = given().when().get("/api/q/metrics");

        // Then
        assertThat(response.statusCode()).isEqualTo(404);
        String body = response.asString();
        assertThat(body.lines().anyMatch(l -> l.startsWith("# TYPE"))).isFalse();
        assertThat(body).doesNotContain("http_server_requests").doesNotContain("jvm_");
    }

    @Test
    void a_templated_route_is_tagged_with_its_template_never_its_id() {
        // Given
        String id = UUID.randomUUID().toString();

        // When
        assertThat(given().when().get("/api/test-only/metrics/" + id).statusCode()).isEqualTo(200);
        String scrape = scrape();

        // Then
        assertThat(hasLine(scrape, "http_server_requests_seconds_count", "uri=\"/api/test-only/metrics/{id}\""))
                .as("a count line tagged with the template")
                .isTrue();
        assertThat(scrape).doesNotContain(id);
    }

    @Test
    void an_unknown_route_is_tagged_not_found_never_its_path() {
        // Given
        String id = UUID.randomUUID().toString();

        // When
        assertThat(given().when().get("/api/no-such-route/" + id).statusCode()).isEqualTo(404);
        String scrape = scrape();

        // Then
        assertThat(hasLine(scrape, "http_server_requests_seconds_count", "uri=\"NOT_FOUND\"", "status=\"404\""))
                .as("a count line tagged NOT_FOUND")
                .isTrue();
        assertThat(scrape).doesNotContain(id);
    }
}
