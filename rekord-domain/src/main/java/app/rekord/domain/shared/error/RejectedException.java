package app.rekord.domain.shared.error;

import java.util.Objects;

public final class RejectedException extends RekordException {

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
