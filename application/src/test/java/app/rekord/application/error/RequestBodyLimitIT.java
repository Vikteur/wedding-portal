package app.rekord.application.error;

import static org.assertj.core.api.Assertions.assertThat;

import app.rekord.application.ResourceTestProfile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URL;
import java.util.Arrays;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(ResourceTestProfile.class)
@Tag("resource-test")
class RequestBodyLimitIT {

    private static final int LIMIT = 10240 * 1024;

    @TestHTTPResource("/api/test-only/error-envelope/invite-accept")
    URL probe;

    private HttpResponse<byte[]> post(int bytes) throws Exception {
        byte[] body = new byte[bytes];
        Arrays.fill(body, (byte) 'a');
        HttpRequest request = HttpRequest.newBuilder(URI.create(probe.toString()))
                .header("Content-Type", "application/json")
                .expectContinue(true)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        return HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build().send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    @Test
    void a_body_one_byte_over_the_limit_answers_exactly_what_rekord_api_answers() throws Exception {
        // Given
        JsonNode fixture;
        try (InputStream in = getClass().getResourceAsStream("/fixtures/error-body-too-large-413.json")) {
            assertThat(in).isNotNull();
            fixture = new ObjectMapper().readTree(in);
        }

        // When: 10240K + 1, sent the way curl sends it (Expect: 100-continue)
        HttpResponse<byte[]> response = post(LIMIT + 1);

        // Then
        assertThat(response.statusCode()).isEqualTo(fixture.get("status").asInt());
        assertThat(response.headers().firstValue("Content-Type").orElse(""))
                .isEqualTo(fixture.get("contentType").asText());
        assertThat(response.body()).isEqualTo(fixture.get("body").asText().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(new String(response.body(), java.nio.charset.StandardCharsets.UTF_8)).doesNotContain("\"detail\"");
    }

    @Test
    void a_body_of_exactly_the_limit_is_not_answered_413() throws Exception {
        assertThat(post(LIMIT).statusCode()).isNotEqualTo(413);
    }
}
