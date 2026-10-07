package app.rekord.domain.shared.error;

public final class NotPermittedException extends RekordException {

    public NotPermittedException(ErrorCode code, String message) {
        super(code, message);
    }
}
