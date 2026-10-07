package app.rekord.application.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.rekord.domain.shared.error.ErrorCode;
import app.rekord.domain.shared.error.NotFoundException;
import app.rekord.domain.shared.error.NotPermittedException;
import app.rekord.domain.shared.error.RejectedException;
import app.rekord.domain.shared.error.RekordException;
import app.rekord.domain.shared.error.UpstreamUnavailableException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class ErrorStatusTableTest {

    private static final Map<String, Class<? extends RekordException>> FAMILIES = Map.of(
            "NotFound", NotFoundException.class,
            "Rejected", RejectedException.class,
            "NotPermitted", NotPermittedException.class,
            "UpstreamUnavailable", UpstreamUnavailableException.class);

    // The 45 rows of rekord-api's error catalogue (analysis 08 section 2): code, family, status.
    private static final Set<String> ROWS = Set.of(
            "NO_LIBRARY, NotFound, 404",
            "NO_LIBRARY, Rejected, 409",
            "NO_LIBRARY_SELECTED, Rejected, 400",
            "EMPTY_NAME, Rejected, 400",
            "DUPLICATE_NAME, Rejected, 409",
            "NO_SOURCE, NotFound, 404",
            "EMPTY_FILE, Rejected, 400",
            "FILE_TOO_LARGE, Rejected, 413",
            "BAD_XML, Rejected, 400",
            "BAD_PLAYLIST, Rejected, 400",
            "NOTHING_RESOLVED, Rejected, 400",
            "NO_PLAYLIST, NotFound, 404",
            "FOLDER_NOT_FOUND, Rejected, 400",
            "SCAN_IN_PROGRESS, Rejected, 409",
            "BAD_URL, Rejected, 400",
            "SPOTIFY_FETCH_FAILED, NotFound, 404",
            "SPOTIFY_FETCH_FAILED, UpstreamUnavailable, 502",
            "SPOTIFY_PARSE_FAILED, UpstreamUnavailable, 502",
            "NO_TRACKS, Rejected, 400",
            "UNKNOWN_TRACK, Rejected, 400",
            "NO_PREFERENCE, NotFound, 404",
            "SEARCH_UNAVAILABLE, UpstreamUnavailable, 503",
            "RATE_LIMITED, Rejected, 429",
            "NOTHING_SKIPPED, Rejected, 400",
            "LIST_FULL, Rejected, 409",
            "UID_CONFLICT, Rejected, 409",
            "UNKNOWN_ENTRY, NotFound, 404",
            "EMPTY_TITLE, Rejected, 400",
            "BAD_LINK, NotPermitted, 401",
            "LINK_REVOKED, Rejected, 410",
            "LINK_EXPIRED, Rejected, 410",
            "CODE_LOCKED, Rejected, 429",
            "NOT_SIGNED_IN, NotPermitted, 401",
            "FORBIDDEN, NotPermitted, 403",
            "BAD_CREDENTIALS, NotPermitted, 401",
            "ACCOUNT_DISABLED, NotPermitted, 403",
            "DUPLICATE_USERNAME, Rejected, 409",
            "NO_USER, NotFound, 404",
            "LAST_ADMIN, Rejected, 400",
            "INVITE_INVALID, NotFound, 404",
            "INVITE_EXPIRED, Rejected, 410",
            "NO_WEDDING, NotFound, 404",
            "NO_VENDOR, NotFound, 404",
            "NO_TASK, NotFound, 404",
            "VALIDATION_FAILED, Rejected, 422"
    );

    private static Class<? extends RekordException> family(String name) {
        return FAMILIES.get(name);
    }

    @ParameterizedTest
    @MethodSource("expectedRows")
    void every_row_answers_rekord_apis_status(String expectedRow) {
        // The expectation comes from the hand-written ROWS oracle; the answer comes from the table's lookup.
        String[] parts = expectedRow.split(", ");
        ErrorCode code = ErrorCode.valueOf(parts[0]);
        int status = Integer.parseInt(parts[2]);
        assertThat(ErrorStatusTable.statusOf(code, family(parts[1]))).isEqualTo(status);
    }

    static Set<String> expectedRows() {
        return ROWS;
    }

    @Test
    void the_table_holds_exactly_those_rows() {
        Set<String> expected = ROWS;
        Set<String> actual = ErrorStatusTable.rows().stream()
                .map(r -> r.code() + ", " + r.family().getSimpleName().replace("Exception", "") + ", " + r.status())
                .collect(Collectors.toSet());
        assertThat(ErrorStatusTable.rows()).hasSize(45);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void every_pair_of_code_and_family_has_exactly_one_status() {
        Map<String, Set<Integer>> byPair = ErrorStatusTable.rows().stream().collect(Collectors.groupingBy(
                r -> r.code() + "/" + r.family().getSimpleName(),
                Collectors.mapping(ErrorStatusTable.Row::status, Collectors.toSet())));
        assertThat(byPair).hasSize(ErrorStatusTable.rows().size());
        byPair.forEach((pair, statuses) -> assertThat(statuses).as(pair).hasSize(1));
    }

    @Test
    void every_error_code_has_at_least_one_row() {
        Set<ErrorCode> covered = ErrorStatusTable.rows().stream()
                .map(ErrorStatusTable.Row::code).collect(Collectors.toSet());
        assertThat(covered).containsExactlyInAnyOrder(ErrorCode.values());
    }

    @Test
    void no_library_is_404_as_not_found_and_409_as_rejected() {
        assertThat(ErrorStatusTable.statusOf(ErrorCode.NO_LIBRARY, NotFoundException.class)).isEqualTo(404);
        assertThat(ErrorStatusTable.statusOf(ErrorCode.NO_LIBRARY, RejectedException.class)).isEqualTo(409);
    }

    @Test
    void spotify_fetch_failed_is_404_as_not_found_and_502_as_upstream_unavailable() {
        assertThat(ErrorStatusTable.statusOf(ErrorCode.SPOTIFY_FETCH_FAILED, NotFoundException.class))
                .isEqualTo(404);
        assertThat(ErrorStatusTable.statusOf(ErrorCode.SPOTIFY_FETCH_FAILED, UpstreamUnavailableException.class))
                .isEqualTo(502);
    }

    @Test
    void the_five_codes_shared_with_python_take_rekord_apis_status() {
        // rekord-api PortalGate.java:151-154
        assertThat(ErrorStatusTable.statusOf(ErrorCode.BAD_LINK, NotPermittedException.class)).isEqualTo(401);
        // rekord-api PortalGate.java:86
        assertThat(ErrorStatusTable.statusOf(ErrorCode.LINK_REVOKED, RejectedException.class)).isEqualTo(410);
        // rekord-api ScanService.java:166-176
        assertThat(ErrorStatusTable.statusOf(ErrorCode.FOLDER_NOT_FOUND, RejectedException.class)).isEqualTo(400);
        // rekord-api LibraryRepository.java:218
        assertThat(ErrorStatusTable.statusOf(ErrorCode.NO_LIBRARY_SELECTED, RejectedException.class))
                .isEqualTo(400);
        // rekord-api SpotifyPlaylistFetch.java:60 (404) and SpotifyPlaylistFetch.java:72 (502)
        assertThat(ErrorStatusTable.statusOf(ErrorCode.SPOTIFY_FETCH_FAILED, NotFoundException.class))
                .isEqualTo(404);
        assertThat(ErrorStatusTable.statusOf(ErrorCode.SPOTIFY_FETCH_FAILED, UpstreamUnavailableException.class))
                .isEqualTo(502);
    }

    @Test
    void status_of_an_exception_uses_its_family() {
        assertThat(ErrorStatusTable.statusOf(new NotFoundException(ErrorCode.NO_LIBRARY, "x"))).isEqualTo(404);
        assertThat(ErrorStatusTable.statusOf(
                new RejectedException(RejectedException.Kind.CONFLICT, ErrorCode.NO_LIBRARY, "x"))).isEqualTo(409);
    }

    static List<ErrorStatusTable.Row> tableRows() {
        return ErrorStatusTable.rows();
    }

    private static RekordException exceptionOf(ErrorStatusTable.Row row) {
        Class<? extends RekordException> family = row.family();
        if (family == NotFoundException.class) {
            return new NotFoundException(row.code(), "x");
        }
        if (family == RejectedException.class) {
            return new RejectedException(RejectedException.Kind.VALIDATION, row.code(), "x");
        }
        if (family == NotPermittedException.class) {
            return new NotPermittedException(row.code(), "x");
        }
        if (family == UpstreamUnavailableException.class) {
            return new UpstreamUnavailableException(row.code(), "x");
        }
        throw new AssertionError("Unknown family " + family);
    }

    @ParameterizedTest
    @MethodSource("tableRows")
    void the_status_of_an_exception_is_its_rows_status(ErrorStatusTable.Row row) {
        assertThat(ErrorStatusTable.statusOf(exceptionOf(row))).isEqualTo(row.status());
    }

    @ParameterizedTest
    @EnumSource(RejectedException.Kind.class)
    void a_rejected_status_comes_from_its_row_not_its_kind(RejectedException.Kind kind) {
        assertThat(ErrorStatusTable.statusOf(new RejectedException(kind, ErrorCode.NO_LIBRARY, "x"))).isEqualTo(409);
        assertThat(ErrorStatusTable.statusOf(new RejectedException(kind, ErrorCode.EMPTY_NAME, "x"))).isEqualTo(400);
    }

    @Test
    void building_the_table_refuses_a_second_row_for_the_same_pair() {
        ErrorStatusTable.Row first = new ErrorStatusTable.Row(ErrorCode.NO_LIBRARY, NotFoundException.class, 404);
        ErrorStatusTable.Row second = new ErrorStatusTable.Row(ErrorCode.NO_LIBRARY, NotFoundException.class, 409);
        assertThatThrownBy(() -> ErrorStatusTable.index(List.of(first, second)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("NO_LIBRARY");
    }

    @Test
    void an_exception_without_a_row_is_refused_and_kept_as_the_cause() {
        NotFoundException original = new NotFoundException(ErrorCode.BAD_LINK, "x");
        assertThatThrownBy(() -> ErrorStatusTable.statusOf(original))
                .isInstanceOf(IllegalArgumentException.class)
                .hasCause(original)
                .hasCauseInstanceOf(NotFoundException.class);
    }

    @Test
    void a_row_requires_a_code() {
        assertThatThrownBy(() -> new ErrorStatusTable.Row(null, NotFoundException.class, 404))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("code");
    }

    @Test
    void a_row_requires_a_family() {
        assertThatThrownBy(() -> new ErrorStatusTable.Row(ErrorCode.NO_LIBRARY, null, 404))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("family");
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 200, 399, 600})
    void a_row_requires_an_error_status(int status) {
        assertThatThrownBy(() -> new ErrorStatusTable.Row(ErrorCode.NO_LIBRARY, NotFoundException.class, status))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(String.valueOf(status));
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 599})
    void a_row_accepts_the_edges_of_the_error_range(int status) {
        assertThat(new ErrorStatusTable.Row(ErrorCode.NO_LIBRARY, NotFoundException.class, status).status())
                .isEqualTo(status);
    }

    @Test
    void a_pair_without_a_row_is_refused() {
        assertThatThrownBy(() -> ErrorStatusTable.statusOf(ErrorCode.BAD_LINK, NotFoundException.class))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
