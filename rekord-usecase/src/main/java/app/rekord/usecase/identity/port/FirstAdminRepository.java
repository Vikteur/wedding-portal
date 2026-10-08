package app.rekord.usecase.identity.port;

/** What the first-admin bootstrap needs from storage. */
public interface FirstAdminRepository {

    /** True when a membership with role ADMIN and status ACTIVE belongs to a live, ACTIVE account. */
    boolean hasActiveAdmin();

    /**
     * Stores the business, the account and the membership in the caller's transaction; the caller's rollback is what
     * makes them one unit. A taken address is refused as a {@code RejectedException} with code
     * {@code DUPLICATE_USERNAME}, a taken slug as {@code DUPLICATE_NAME}, and a unique violation whose constraint
     * cannot be named as {@code VALIDATION_FAILED}. The caller relies on three things about a refusal: it is a
     * {@code RejectedException}, it has no cause, and neither it nor its message holds a stored value, so the caller
     * may log it. Any other failure is thrown unchanged, with its cause, and is not a refusal: the caller must not
     * catch it.
     */
    void saveFirstAdmin(NewFirstAdmin admin);
}
