package app.rekord.domain.shared.error;

import java.util.Objects;

/** Base of the four error families; carries a code and a message, never an HTTP status (FW-C-10). */
public abstract sealed class RekordException extends RuntimeException
        permits NotFoundException, RejectedException, NotPermittedException, UpstreamUnavailableException {

    private final ErrorCode code;

    protected RekordException(ErrorCode code, String message) {
        super(Objects.requireNonNull(message, "message"));
        this.code = Objects.requireNonNull(code, "code");
    }

    public ErrorCode code() {
        return code;
    }
}
