package app.rekord.application.persistence.identity;

import app.rekord.adapter.persistence.identity.MembershipEntity;
import app.rekord.adapter.persistence.identity.OrganizationEntity;
import app.rekord.adapter.persistence.identity.SessionEntity;
import app.rekord.adapter.persistence.identity.UserEntity;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** Test data builder: example.com addresses, made-up names, token hashes of made-up strings (DoD #8). */
public final class IdentityRows {

    public static final Instant T0 = Instant.parse("2027-06-12T10:00:00Z");
    public static final String PLANNER_EMAIL = "planner@example.com";
    public static final String DJ_EMAIL = "dj@example.com";
    public static final String ADMIN_EMAIL = "admin@example.com";
    public static final String PASSWORD_HASH = "$argon2id$made-up-hash";

    private IdentityRows() {}

    public static UUID id(int n) {
        return new UUID(0L, n);
    }

    /** SHA-256 of the made-up string {@code made-up-token-<n>}. */
    public static byte[] tokenHash(int n) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(("made-up-token-" + n).getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static OrganizationEntity organization() {
        return organization(1, "Example Weddings", "example-weddings");
    }

    public static OrganizationEntity organization(int n, String name, String slug) {
        OrganizationEntity org = new OrganizationEntity();
        org.setId(id(100 + n));
        org.setName(name);
        org.setSlug(slug);
        org.setTimezone("Europe/Amsterdam");
        org.setCreatedAt(T0);
        org.setUpdatedAt(T0);
        return org;
    }

    public static UserEntity planner() {
        return user(11, PLANNER_EMAIL, "Pat Planner");
    }

    public static UserEntity dj() {
        return user(12, DJ_EMAIL, "Dee Jay");
    }

    public static UserEntity admin() {
        return user(13, ADMIN_EMAIL, "Ada Admin");
    }

    public static UserEntity user(int n, String email, String displayName) {
        UserEntity user = new UserEntity();
        user.setId(id(200 + n));
        user.setEmail(email);
        user.setPasswordHash(PASSWORD_HASH);
        user.setDisplayName(displayName);
        user.setStatus("ACTIVE");
        user.setCreatedAt(T0);
        user.setUpdatedAt(T0);
        return user;
    }

    public static MembershipEntity membership(int n, OrganizationEntity org, UserEntity user, String role) {
        MembershipEntity membership = new MembershipEntity();
        membership.setId(id(300 + n));
        membership.setOrgId(org.getId());
        membership.setUserId(user.getId());
        membership.setRole(role);
        membership.setStatus("ACTIVE");
        membership.setCreatedAt(T0);
        membership.setUpdatedAt(T0);
        return membership;
    }

    /** A sign-in session of an account, with the token hash of {@code made-up-token-<n>}. */
    public static SessionEntity userSession(int n, OrganizationEntity org, UserEntity user) {
        SessionEntity session = session(n, org);
        session.setSubjectKind("USER");
        session.setUserId(user.getId());
        session.setRoles(new String[] {"PLANNER"});
        return session;
    }

    /** A portal session, scoped to one wedding. */
    public static SessionEntity portalSession(int n, OrganizationEntity org, UUID portalId, UUID weddingId) {
        SessionEntity session = session(n, org);
        session.setSubjectKind("PORTAL");
        session.setPortalId(portalId);
        session.setWeddingId(weddingId);
        session.setRoles(new String[] {"COUPLE"});
        return session;
    }

    private static SessionEntity session(int n, OrganizationEntity org) {
        SessionEntity session = new SessionEntity();
        session.setId(id(400 + n));
        session.setOrgId(org.getId());
        session.setTokenHash(tokenHash(n));
        session.setCreatedAt(T0);
        session.setLastSeenAt(T0);
        session.setIdleExpiresAt(T0.plus(Duration.ofHours(1)));
        session.setAbsoluteExpiresAt(T0.plus(Duration.ofHours(8)));
        session.setUserAgent("Example Browser 1.0");
        return session;
    }
}
