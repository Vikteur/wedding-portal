package app.rekord.domain.shared.error;

/** The caller may not do this; named "NotPermitted" so it never clashes with the framework's ForbiddenException. */
public final class NotPermittedException extends RekordException {

    public NotPermittedException(ErrorCode code, String message) {
        super(code, message);
    }
}
