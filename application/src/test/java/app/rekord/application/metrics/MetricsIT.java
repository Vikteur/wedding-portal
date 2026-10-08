package app.rekord.application.metrics;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import app.rekord.application.ResourceTestProfile;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** AC4, AC5 and the label rule (PIN-16-0198): metrics sit at /q/metrics, never under /api, with templated URIs. */
@QuarkusTest
@TestProfile(ResourceTestProfile.class)
@Tag("resource-test")
class MetricsIT {

    private static final int BODY_LIMIT = 10240 * 1024;
    private static final Duration DEADLINE = Duration.ofSeconds(15);

    @TestHTTPResource("/api/no-such-route")
    URL noSuchRoute;

    @Inject
    HeldRequests held;

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

    /** The summed count of the http.server requests that ended in a reset. */
    private static double resetCount(String scrape) {
        return scrape.lines()
                .filter(l -> l.startsWith("http_server_requests_seconds_count") && l.contains("status=\"RESET\""))
                .mapToDouble(l -> Double.parseDouble(l.substring(l.lastIndexOf(' ') + 1)))
                .sum();
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

    @Test
    void a_request_reset_before_routing_is_tagged_unknown_never_its_path() throws Exception {
        // Given: a request to a path no resource matches, held in flight before routing
        String id = UUID.randomUUID().toString();
        String path = "/api/no-such-route/" + id;
        double resetsBefore = resetCount(scrape());

        // When: the client resets the connection (a close with linger 0 sends RST) while the server waits
        try (Socket socket = new Socket(noSuchRoute.getHost(), noSuchRoute.getPort())) {
            socket.setSoLinger(true, 0);
            String head = "POST " + path + " HTTP/1.1\r\nHost: localhost\r\n" + HeldRequests.HEADER + ": 1\r\n"
                    + "Content-Type: application/json\r\nContent-Length: 1000000\r\n\r\n";
            socket.getOutputStream().write(head.getBytes(StandardCharsets.US_ASCII));
            socket.getOutputStream().flush();
            held.arrival(path).get(DEADLINE.toSeconds(), TimeUnit.SECONDS);
        }
        String scrape = scrape();
        Instant deadline = Instant.now().plus(DEADLINE);
        while (resetCount(scrape) <= resetsBefore && Instant.now().isBefore(deadline)) {
            Thread.sleep(50);
            scrape = scrape();
        }

        // Then
        assertThat(resetCount(scrape)).as("a reset counted within " + DEADLINE).isGreaterThan(resetsBefore);
        assertThat(scrape).as("the scrape of a reset request").doesNotContain(id);
        assertThat(hasLine(scrape, "http_server_requests_seconds_count", "uri=\"UNKNOWN\"", "status=\"RESET\""))
                .as("a count line tagged UNKNOWN for the reset")
                .isTrue();
    }
}
