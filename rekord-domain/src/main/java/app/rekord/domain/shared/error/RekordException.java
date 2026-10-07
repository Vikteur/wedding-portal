package app.rekord.domain.shared.error;

import java.util.Objects;

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
