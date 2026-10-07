package app.rekord.domain.shared.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ErrorCodeTest {

    private static Set<String> names() {
        return Arrays.stream(ErrorCode.values()).map(Enum::name).collect(Collectors.toSet());
    }

    @ParameterizedTest
    @ValueSource(strings = {"BAD_DATE", "BAD_KIND", "BAD_POSITION", "BAD_START_PREF", "EMPTY_NAMES",
            "NOTHING_MATCHED", "NOTHING_MISSING"})
    void the_python_only_codes_are_not_error_codes(String code) {
        assertThat(names()).doesNotContain(code);
    }

    @Test
    void unknown_is_left_to_the_mapper() {
        assertThat(names()).doesNotContain("UNKNOWN");
    }

    @Test
    void the_codes_are_the_43_that_rekord_api_throws() {
        Set<String> expected = Set.of(
                "NO_LIBRARY", "NO_LIBRARY_SELECTED", "EMPTY_NAME", "DUPLICATE_NAME", "NO_SOURCE", "EMPTY_FILE",
                "FILE_TOO_LARGE", "BAD_XML", "BAD_PLAYLIST", "NOTHING_RESOLVED", "NO_PLAYLIST", "FOLDER_NOT_FOUND",
                "SCAN_IN_PROGRESS", "BAD_URL", "SPOTIFY_FETCH_FAILED", "SPOTIFY_PARSE_FAILED", "NO_TRACKS",
                "UNKNOWN_TRACK", "NO_PREFERENCE", "SEARCH_UNAVAILABLE", "RATE_LIMITED", "NOTHING_SKIPPED",
                "LIST_FULL", "UID_CONFLICT", "UNKNOWN_ENTRY", "EMPTY_TITLE",
                "BAD_LINK", "LINK_REVOKED", "LINK_EXPIRED", "CODE_LOCKED", "NOT_SIGNED_IN", "FORBIDDEN",
                "BAD_CREDENTIALS", "ACCOUNT_DISABLED", "DUPLICATE_USERNAME", "NO_USER", "LAST_ADMIN",
                "INVITE_INVALID", "INVITE_EXPIRED", "NO_WEDDING", "NO_VENDOR", "NO_TASK", "VALIDATION_FAILED");
        assertThat(expected).hasSize(43);
        assertThat(names()).isEqualTo(expected);
    }
}
