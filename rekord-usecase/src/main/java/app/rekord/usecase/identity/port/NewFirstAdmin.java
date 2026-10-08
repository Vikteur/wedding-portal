package app.rekord.usecase.identity.port;

import java.time.Instant;
import java.util.UUID;

/** The three rows of the first admin. Every timestamp, and the password change, is {@code now}. */
public record NewFirstAdmin(
        UUID businessId,
        String businessName,
        String businessSlug,
        String timezone,
        UUID accountId,
        String email,
        String displayName,
        String passwordHash,
        UUID membershipId,
        String accountStatus,
        String role,
        String membershipStatus,
        Instant now) {

    /** Prints no address, name, hash or slug. */
    @Override
    public String toString() {
        return "NewFirstAdmin[businessId=" + businessId + ", accountId=" + accountId + ", membershipId="
                + membershipId + "]";
    }
}
