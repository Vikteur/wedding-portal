package app.rekord.application.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** The error fixtures recorded from rekord-api: present, attributed to the oracle commit, and free of leaks. */
class ErrorFixturesTest {

    private static final String ORACLE_COMMIT = "ec65ae35c182e6e25f571c76d45b15a78f183c10";
    private static final String BODY_TOO_LARGE = "error-body-too-large-413";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern ADDRESS = Pattern.compile("[A-Za-z0-9._%+-]+@([A-Za-z0-9.-]+)");

    private static final List<String> NAMES = List.of(
            "error-not-signed-in-401",
            "error-authentication-failed-401",
            "error-forbidden-403",
            "error-family-not-found-404",
            "error-family-rejected-409",
            "error-family-not-permitted-401",
            "error-family-upstream-unavailable-503",
            "error-unknown-route-404",
            "error-q-metrics-404",
            "error-malformed-uuid-404",
            "error-validation-422",
            "error-unmapped-409",
            "error-method-not-allowed-405",
            "error-unhandled-500",
            BODY_TOO_LARGE);

    static Stream<String> names() {
        return NAMES.stream();
    }

    static Stream<String> envelopeNames() {
        return NAMES.stream().filter(n -> !n.equals(BODY_TOO_LARGE));
    }

    private static JsonNode load(String name) throws IOException {
        try (InputStream in = ErrorFixturesTest.class.getResourceAsStream("/fixtures/" + name + ".json")) {
            assertThat(in).as("fixture %s.json exists", name).isNotNull();
            return JSON.readTree(in);
        }
    }

    @Test
    void every_error_fixture_on_disk_is_one_of_the_expected_names() throws IOException {
        var dir = ErrorFixturesTest.class.getResource("/fixtures/");
        assertThat(dir).isNotNull();
        try (var files = java.nio.file.Files.list(java.nio.file.Path.of(java.net.URI.create(dir.toString())))) {
            var found = files.map(p -> p.getFileName().toString())
                    .filter(f -> f.startsWith("error-"))
                    .map(f -> f.substring(0, f.length() - ".json".length()))
                    .toList();
            assertThat(found).containsExactlyInAnyOrderElementsOf(NAMES);
        }
    }

    @ParameterizedTest
    @MethodSource("names")
    void the_fixture_names_the_oracle_commit_and_holds_status_content_type_and_body(String name) throws IOException {
        JsonNode fixture = load(name);
        assertThat(fixture.path("capturedFrom").asText()).contains(ORACLE_COMMIT);
        assertThat(fixture.path("status").isInt()).isTrue();
        if (name.equals(BODY_TOO_LARGE)) {
            // Vert.x answers 413 itself: no Content-Type, no body.
            assertThat(fixture.path("contentType").asText()).isEmpty();
        } else {
            assertThat(fixture.path("contentType").asText()).isNotBlank();
        }
        assertThat(fixture.path("body").isTextual()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("envelopeNames")
    void an_envelope_body_is_exactly_detail_with_code_and_message(String name) throws IOException {
        JsonNode body = JSON.readTree(load(name).path("body").asText());
        assertThat(body.fieldNames()).toIterable().containsExactly("detail");
        assertThat(body.path("detail").fieldNames()).toIterable().containsExactlyInAnyOrder("code", "message");
        assertThat(body.path("detail").path("code").asText()).isNotBlank();
        assertThat(body.path("detail").path("message").isTextual()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("names")
    void no_body_leaks_an_exception_a_stack_frame_or_sql(String name) throws IOException {
        String body = load(name).path("body").asText();
        assertThat(body).doesNotContain("Exception").doesNotContain("\tat ").doesNotContain("SQL");
    }

    @ParameterizedTest
    @MethodSource("names")
    void no_fixture_holds_an_address_outside_example_com(String name) throws IOException {
        Matcher m = ADDRESS.matcher(load(name).toString());
        while (m.find()) {
            assertThat(m.group(1)).isEqualTo("example.com");
        }
    }
}
