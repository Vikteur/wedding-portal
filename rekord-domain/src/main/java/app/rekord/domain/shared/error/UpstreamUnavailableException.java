package app.rekord.domain.shared.error;

public final class UpstreamUnavailableException extends RekordException {

    public UpstreamUnavailableException(ErrorCode code, String message) {
        super(code, message);
    }
}
