package app.rekord.domain.shared.error;

/** The thing asked for does not exist. */
public final class NotFoundException extends RekordException {

    public NotFoundException(ErrorCode code, String message) {
        super(code, message);
    }
}
