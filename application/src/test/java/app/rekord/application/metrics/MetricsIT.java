package app.rekord.application.metrics;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import app.rekord.application.ResourceTestProfile;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.response.Response;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** AC4, AC5 and the label rule (PIN-16-0198): metrics sit at /q/metrics, never under /api, with templated URIs. */
@QuarkusTest
@TestProfile(ResourceTestProfile.class)
@Tag("resource-test")
class MetricsIT {

    private static final int BODY_LIMIT = 10240 * 1024;

    @TestHTTPResource("/api/no-such-route")
    URL noSuchRoute;

    private static String scrape() {
        Response response = given().accept("text/plain").when().get("/q/metrics");
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
        Response response = given().accept("text/plain").when().get("/q/metrics");

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
        assertThat(hasLine(scrape, "http_server_requests_seconds_count", "uri=\"/test-only/metrics/{id}\""))
                .as("a count line tagged with the template (Micrometer leaves the /api application path out)")
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

    @Test
    void an_untemplated_refusal_is_tagged_unknown_never_its_path() throws Exception {
        // Given: a body over quarkus.http.limits.max-body-size, sent to a path no resource matches
        String id = UUID.randomUUID().toString();
        byte[] body = new byte[BODY_LIMIT + 1];
        Arrays.fill(body, (byte) 'a');
        HttpRequest request = HttpRequest.newBuilder(URI.create(noSuchRoute + "/" + id))
                .header("Content-Type", "application/json")
                .expectContinue(true)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        // When
        HttpResponse<byte[]> response = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build()
                .send(request, HttpResponse.BodyHandlers.ofByteArray());
        String scrape = scrape();

        // Then
        assertThat(response.statusCode()).isEqualTo(413);
        assertThat(hasLine(scrape, "http_server_requests_seconds_count", "uri=\"UNKNOWN\"", "status=\"413\""))
                .as("a count line tagged UNKNOWN for the 413")
                .isTrue();
        assertThat(scrape).doesNotContain(id);
    }
}
