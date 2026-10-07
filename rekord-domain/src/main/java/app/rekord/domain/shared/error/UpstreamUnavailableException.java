package app.rekord.domain.shared.error;

/** A service this one depends on failed or is unavailable. */
public final class UpstreamUnavailableException extends RekordException {

    public UpstreamUnavailableException(ErrorCode code, String message) {
        super(code, message);
    }
}
