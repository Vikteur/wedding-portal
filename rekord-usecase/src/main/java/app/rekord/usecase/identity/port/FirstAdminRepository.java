package app.rekord.usecase.identity.port;

/** What the first-admin bootstrap needs from storage. */
public interface FirstAdminRepository {

    /** True when a membership with role ADMIN and status ACTIVE belongs to a live, ACTIVE account. */
    boolean hasActiveAdmin();

    /** Stores the business, the account and the membership as one unit; a taken address or slug is refused. */
    void saveFirstAdmin(NewFirstAdmin admin);
}
