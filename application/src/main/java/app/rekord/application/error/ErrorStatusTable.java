package app.rekord.application.error;

import app.rekord.domain.shared.error.ErrorCode;
import app.rekord.domain.shared.error.NotFoundException;
import app.rekord.domain.shared.error.NotPermittedException;
import app.rekord.domain.shared.error.RejectedException;
import app.rekord.domain.shared.error.RekordException;
import app.rekord.domain.shared.error.UpstreamUnavailableException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The only place where an {@link ErrorCode} in a family becomes an HTTP status (rekord-api's statuses). */
public final class ErrorStatusTable {

    public record Row(ErrorCode code, Class<? extends RekordException> family, int status) {
    }

    private static final List<Row> ROWS = List.of(
            row(ErrorCode.NO_LIBRARY, NotFoundException.class, 404),
            row(ErrorCode.NO_LIBRARY, RejectedException.class, 409),
            row(ErrorCode.NO_LIBRARY_SELECTED, RejectedException.class, 400),
            row(ErrorCode.EMPTY_NAME, RejectedException.class, 400),
            row(ErrorCode.DUPLICATE_NAME, RejectedException.class, 409),
            row(ErrorCode.NO_SOURCE, NotFoundException.class, 404),
            row(ErrorCode.EMPTY_FILE, RejectedException.class, 400),
            row(ErrorCode.FILE_TOO_LARGE, RejectedException.class, 413),
            row(ErrorCode.BAD_XML, RejectedException.class, 400),
            row(ErrorCode.BAD_PLAYLIST, RejectedException.class, 400),
            row(ErrorCode.NOTHING_RESOLVED, RejectedException.class, 400),
            row(ErrorCode.NO_PLAYLIST, NotFoundException.class, 404),
            row(ErrorCode.FOLDER_NOT_FOUND, RejectedException.class, 400),
            row(ErrorCode.SCAN_IN_PROGRESS, RejectedException.class, 409),
            row(ErrorCode.BAD_URL, RejectedException.class, 400),
            row(ErrorCode.SPOTIFY_FETCH_FAILED, NotFoundException.class, 404),
            row(ErrorCode.SPOTIFY_FETCH_FAILED, UpstreamUnavailableException.class, 502),
            row(ErrorCode.SPOTIFY_PARSE_FAILED, UpstreamUnavailableException.class, 502),
            row(ErrorCode.NO_TRACKS, RejectedException.class, 400),
            row(ErrorCode.UNKNOWN_TRACK, RejectedException.class, 400),
            row(ErrorCode.NO_PREFERENCE, NotFoundException.class, 404),
            row(ErrorCode.SEARCH_UNAVAILABLE, UpstreamUnavailableException.class, 503),
            row(ErrorCode.RATE_LIMITED, RejectedException.class, 429),
            row(ErrorCode.NOTHING_SKIPPED, RejectedException.class, 400),
            row(ErrorCode.LIST_FULL, RejectedException.class, 409),
            row(ErrorCode.UID_CONFLICT, RejectedException.class, 409),
            row(ErrorCode.UNKNOWN_ENTRY, NotFoundException.class, 404),
            row(ErrorCode.EMPTY_TITLE, RejectedException.class, 400),
            row(ErrorCode.BAD_LINK, NotPermittedException.class, 401),
            row(ErrorCode.LINK_REVOKED, RejectedException.class, 410),
            row(ErrorCode.LINK_EXPIRED, RejectedException.class, 410),
            row(ErrorCode.CODE_LOCKED, RejectedException.class, 429),
            row(ErrorCode.NOT_SIGNED_IN, NotPermittedException.class, 401),
            row(ErrorCode.FORBIDDEN, NotPermittedException.class, 403),
            row(ErrorCode.BAD_CREDENTIALS, NotPermittedException.class, 401),
            row(ErrorCode.ACCOUNT_DISABLED, NotPermittedException.class, 403),
            row(ErrorCode.DUPLICATE_USERNAME, RejectedException.class, 409),
            row(ErrorCode.NO_USER, NotFoundException.class, 404),
            row(ErrorCode.LAST_ADMIN, RejectedException.class, 400),
            row(ErrorCode.INVITE_INVALID, NotFoundException.class, 404),
            row(ErrorCode.INVITE_EXPIRED, RejectedException.class, 410),
            row(ErrorCode.NO_WEDDING, NotFoundException.class, 404),
            row(ErrorCode.NO_VENDOR, NotFoundException.class, 404),
            row(ErrorCode.NO_TASK, NotFoundException.class, 404),
            row(ErrorCode.VALIDATION_FAILED, RejectedException.class, 422));

    private static final Map<ErrorCode, Map<Class<? extends RekordException>, Integer>> BY_CODE = index();

    private ErrorStatusTable() {
    }

    public static List<Row> rows() {
        return ROWS;
    }

    public static int statusOf(ErrorCode code, Class<? extends RekordException> family) {
        Integer status = BY_CODE.getOrDefault(code, Map.of()).get(family);
        if (status == null) {
            throw new IllegalArgumentException("No status for " + code + " as " + family.getSimpleName());
        }
        return status;
    }

    public static int statusOf(RekordException e) {
        return statusOf(e.code(), e.getClass());
    }

    private static Row row(ErrorCode code, Class<? extends RekordException> family, int status) {
        return new Row(code, family, status);
    }

    private static Map<ErrorCode, Map<Class<? extends RekordException>, Integer>> index() {
        Map<ErrorCode, Map<Class<? extends RekordException>, Integer>> byCode = new HashMap<>();
        for (Row r : ROWS) {
            Integer previous = byCode.computeIfAbsent(r.code(), k -> new HashMap<>()).put(r.family(), r.status());
            if (previous != null) {
                throw new IllegalStateException("Duplicate row for " + r.code() + " as " + r.family().getSimpleName());
            }
        }
        Map<ErrorCode, Map<Class<? extends RekordException>, Integer>> frozen = new HashMap<>();
        byCode.forEach((k, v) -> frozen.put(k, Map.copyOf(v)));
        return Map.copyOf(frozen);
    }
}
