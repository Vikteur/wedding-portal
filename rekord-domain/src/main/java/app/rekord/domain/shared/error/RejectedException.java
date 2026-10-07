package app.rekord.domain.shared.error;

import java.util.Objects;

/** The request was understood but refused; the HTTP status comes from the code's row in ErrorStatusTable. */
public final class RejectedException extends RekordException {

    /** Why the request was refused; informational only, the HTTP status always comes from ErrorStatusTable. */
    public enum Kind { VALIDATION, CONFLICT }

    private final Kind kind;

    public RejectedException(Kind kind, ErrorCode code, String message) {
        super(code, message);
        this.kind = Objects.requireNonNull(kind, "kind");
    }

    public Kind kind() {
        return kind;
    }
}
