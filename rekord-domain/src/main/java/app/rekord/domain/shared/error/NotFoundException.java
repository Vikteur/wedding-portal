package app.rekord.domain.shared.error;

public final class NotFoundException extends RekordException {

    public NotFoundException(ErrorCode code, String message) {
        super(code, message);
    }
}
