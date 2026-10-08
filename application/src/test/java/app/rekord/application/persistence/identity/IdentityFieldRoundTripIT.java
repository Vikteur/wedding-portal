package app.rekord.application.persistence.identity;

import static org.assertj.core.api.Assertions.assertThat;

import app.rekord.adapter.persistence.identity.MembershipEntity;
import app.rekord.adapter.persistence.identity.OrganizationEntity;
import app.rekord.adapter.persistence.identity.SessionEntity;
import app.rekord.adapter.persistence.identity.UserEntity;
import app.rekord.application.persistence.AbstractRepositoryTest;
import io.quarkus.test.junit.QuarkusTest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

/**
 * Every mapped field of an identity entity reaches its own column and comes back from it. A field written to the wrong
 * column, a column left unwritten and a value changed on the way each fail here. Every value in a row is distinct, so a
 * swap of two columns of the same type shows.
 *
 * <p>Every expectation is a literal or a constant of this test, never a getter of the entity under test: the entities
 * use field access, so an accessor wired to the wrong field would give the same wrong value on both sides of a
 * comparison and the test would stay green.
 *
 * <p>Sessions and users are read back over JDBC by column name, which catches two fields whose column names are
 * swapped (the persistence layer would swap them back on its own read). Organisations and memberships are persisted,
 * the persistence context is cleared and the row is loaded again, which catches a field that is not written or not
 * read.
 */
@QuarkusTest
class IdentityFieldRoundTripIT extends AbstractRepositoryTest {

    /** A token hash of known bytes, with a zero byte and a 0xff byte at the ends of the value. */
    private static final byte[] TOKEN_HASH =
            HexFormat.of().parseHex("00ff10ef20df30cf40bf50af609f708f807f906fa05fb04fc03fd02fe01ff00f");

    private static final Instant CREATED_AT = Instant.parse("2027-06-12T10:00:01.000001Z");
    private static final Instant UPDATED_AT = Instant.parse("2027-06-12T10:07:02.000002Z");
    private static final Instant FIRST = Instant.parse("2027-06-12T11:13:03.000003Z");
    private static final Instant SECOND = Instant.parse("2027-06-12T13:41:05.000005Z");
    private static final Instant THIRD = Instant.parse("2027-06-12T18:29:04.000004Z");
    private static final Instant FOURTH = Instant.parse("2027-06-13T08:15:06.000006Z");

    @Test
    void a_user_session_returns_every_field_from_its_own_column() throws SQLException {
        // Given an organisation, a user and a user session with a distinct value in every field
        OrganizationEntity org = IdentityRows.organization();
        UserEntity user = IdentityRows.planner();
        inNewTransaction(() -> {
            em.persist(org);
            em.persist(user);
        });
        SessionRow row = new SessionRow(IdentityRows.id(701), "USER", user.getId(), null, IdentityRows.id(702),
                org.getId(), List.of("PLANNER", "DJ", "ADMIN"), TOKEN_HASH, CREATED_AT, UPDATED_AT, FIRST, THIRD,
                SECOND, "Example Browser 2.1");

        // When it is stored
        inNewTransaction(() -> em.persist(row.toEntity()));

        // Then each column holds the value of its own field
        assertSessionRow(row);
    }

    @Test
    void a_portal_session_returns_every_field_from_its_own_column() throws SQLException {
        // Given an organisation and a portal session with a distinct value in every field but the account
        OrganizationEntity org = IdentityRows.organization();
        inNewTransaction(() -> em.persist(org));
        SessionRow row = new SessionRow(IdentityRows.id(711), "PORTAL", null, IdentityRows.id(712),
                IdentityRows.id(713), org.getId(), List.of("COUPLE", "GUEST"), TOKEN_HASH, CREATED_AT, UPDATED_AT,
                SECOND, FOURTH, FIRST, "Example Browser 3.4");

        // When it is stored
        inNewTransaction(() -> em.persist(row.toEntity()));

        // Then each column holds the value of its own field (user_id is null: a portal session has no account)
        assertSessionRow(row);
    }

    @Test
    void a_user_returns_every_field_from_its_own_column() throws SQLException {
        // Given a user with a distinct value in every field
        UUID id = IdentityRows.id(721);
        String email = "Round.Trip@example.com";
        String passwordHash = "$argon2id$made-up-hash-for-the-round-trip";
        String displayName = "Rae Roundtrip";
        String phone = "+12025550100";
        String status = "DISABLED";
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setDisplayName(displayName);
        user.setPhone(phone);
        user.setStatus(status);
        user.setLastLoginAt(FIRST);
        user.setPasswordChangedAt(SECOND);
        user.setCreatedAt(CREATED_AT);
        user.setUpdatedAt(UPDATED_AT);
        user.setDeletedAt(THIRD);

        // When it is stored
        inNewTransaction(() -> em.persist(user));

        // Then each column holds the value of its own field
        try (Connection c = dataSource.getConnection();
                PreparedStatement s = c.prepareStatement("select * from users where id = ?")) {
            s.setObject(1, id);
            try (ResultSet rs = s.executeQuery()) {
                assertThat(rs.next()).isTrue();
                SoftAssertions softly = new SoftAssertions();
                softly.assertThat(rs.getObject("id", UUID.class)).as("id").isEqualTo(id);
                softly.assertThat(rs.getString("email")).as("email").isEqualTo(email);
                softly.assertThat(rs.getString("password_hash")).as("password_hash").isEqualTo(passwordHash);
                softly.assertThat(rs.getString("display_name")).as("display_name").isEqualTo(displayName);
                softly.assertThat(rs.getString("phone")).as("phone").isEqualTo(phone);
                softly.assertThat(rs.getString("status")).as("status").isEqualTo(status);
                softly.assertThat(instant(rs, "last_login_at")).as("last_login_at").isEqualTo(FIRST);
                softly.assertThat(instant(rs, "password_changed_at")).as("password_changed_at").isEqualTo(SECOND);
                softly.assertThat(instant(rs, "created_at")).as("created_at").isEqualTo(CREATED_AT);
                softly.assertThat(instant(rs, "updated_at")).as("updated_at").isEqualTo(UPDATED_AT);
                softly.assertThat(instant(rs, "deleted_at")).as("deleted_at").isEqualTo(THIRD);
                softly.assertAll();
            }
        }
    }

    @Test
    void an_organization_is_loaded_again_with_every_field_it_was_stored_with() {
        // Given an organisation with a distinct value in every field (a zone other than the column default)
        UUID id = IdentityRows.id(731);
        String name = "Round Trip Weddings";
        String slug = "round-trip-weddings";
        String timezone = "Pacific/Auckland";
        OrganizationEntity stored = new OrganizationEntity();
        stored.setId(id);
        stored.setName(name);
        stored.setSlug(slug);
        stored.setTimezone(timezone);
        stored.setCreatedAt(CREATED_AT);
        stored.setUpdatedAt(UPDATED_AT);
        stored.setDeletedAt(THIRD);

        // When it is persisted, the persistence context is cleared and the row is loaded again
        OrganizationEntity loaded = inNewTransactionReturning(() -> {
            em.persist(stored);
            em.flush();
            em.clear();
            return em.find(OrganizationEntity.class, id);
        });

        // Then every field is the one that was stored
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(loaded).as("a new instance, read from the row").isNotSameAs(stored);
        softly.assertThat(loaded.getId()).as("id").isEqualTo(id);
        softly.assertThat(loaded.getName()).as("name").isEqualTo(name);
        softly.assertThat(loaded.getSlug()).as("slug").isEqualTo(slug);
        softly.assertThat(loaded.getTimezone()).as("timezone").isEqualTo(timezone);
        softly.assertThat(loaded.getCreatedAt()).as("createdAt").isEqualTo(CREATED_AT);
        softly.assertThat(loaded.getUpdatedAt()).as("updatedAt").isEqualTo(UPDATED_AT);
        softly.assertThat(loaded.getDeletedAt()).as("deletedAt").isEqualTo(THIRD);
        softly.assertAll();
    }

    @Test
    void a_membership_is_loaded_again_with_every_field_it_was_stored_with() {
        // Given an organisation, a user and a membership with a distinct value in every field
        OrganizationEntity org = IdentityRows.organization();
        UserEntity user = IdentityRows.dj();
        inNewTransaction(() -> {
            em.persist(org);
            em.persist(user);
        });
        UUID id = IdentityRows.id(741);
        String role = "DJ";
        String status = "DISABLED";
        MembershipEntity stored = new MembershipEntity();
        stored.setId(id);
        stored.setOrgId(org.getId());
        stored.setUserId(user.getId());
        stored.setRole(role);
        stored.setStatus(status);
        stored.setCreatedAt(CREATED_AT);
        stored.setUpdatedAt(UPDATED_AT);

        // When it is persisted, the persistence context is cleared and the row is loaded again
        MembershipEntity loaded = inNewTransactionReturning(() -> {
            em.persist(stored);
            em.flush();
            em.clear();
            return em.find(MembershipEntity.class, id);
        });

        // Then every field is the one that was stored
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(loaded).as("a new instance, read from the row").isNotSameAs(stored);
        softly.assertThat(loaded.getId()).as("id").isEqualTo(id);
        softly.assertThat(loaded.getOrgId()).as("orgId").isEqualTo(org.getId());
        softly.assertThat(loaded.getUserId()).as("userId").isEqualTo(user.getId());
        softly.assertThat(loaded.getRole()).as("role").isEqualTo(role);
        softly.assertThat(loaded.getStatus()).as("status").isEqualTo(status);
        softly.assertThat(loaded.getCreatedAt()).as("createdAt").isEqualTo(CREATED_AT);
        softly.assertThat(loaded.getUpdatedAt()).as("updatedAt").isEqualTo(UPDATED_AT);
        softly.assertAll();
    }

    /** The values of one sessions row, written down before the entity is built: the expectation of the read. */
    private record SessionRow(UUID id, String subjectKind, UUID userId, UUID portalId, UUID weddingId, UUID orgId,
            List<String> roles, byte[] tokenHash, Instant createdAt, Instant lastSeenAt, Instant idleExpiresAt,
            Instant absoluteExpiresAt, Instant revokedAt, String userAgent) {

        SessionEntity toEntity() {
            SessionEntity session = new SessionEntity();
            session.setId(id);
            session.setSubjectKind(subjectKind);
            session.setUserId(userId);
            session.setPortalId(portalId);
            session.setWeddingId(weddingId);
            session.setOrgId(orgId);
            session.setRoles(roles.toArray(String[]::new));
            session.setTokenHash(tokenHash);
            session.setCreatedAt(createdAt);
            session.setLastSeenAt(lastSeenAt);
            session.setIdleExpiresAt(idleExpiresAt);
            session.setAbsoluteExpiresAt(absoluteExpiresAt);
            session.setRevokedAt(revokedAt);
            session.setUserAgent(userAgent);
            return session;
        }
    }

    /** Reads the sessions row over JDBC, every column by name, and compares it with the values written down. */
    private void assertSessionRow(SessionRow expected) throws SQLException {
        try (Connection c = dataSource.getConnection();
                PreparedStatement s = c.prepareStatement("select * from sessions where id = ?")) {
            s.setObject(1, expected.id());
            try (ResultSet rs = s.executeQuery()) {
                assertThat(rs.next()).isTrue();
                SoftAssertions softly = new SoftAssertions();
                softly.assertThat(rs.getObject("id", UUID.class)).as("id").isEqualTo(expected.id());
                softly.assertThat(rs.getString("subject_kind")).as("subject_kind").isEqualTo(expected.subjectKind());
                softly.assertThat(rs.getObject("user_id", UUID.class)).as("user_id").isEqualTo(expected.userId());
                softly.assertThat(rs.getObject("portal_id", UUID.class)).as("portal_id").isEqualTo(expected.portalId());
                softly.assertThat(rs.getObject("wedding_id", UUID.class))
                        .as("wedding_id")
                        .isEqualTo(expected.weddingId());
                softly.assertThat(rs.getObject("org_id", UUID.class)).as("org_id").isEqualTo(expected.orgId());
                softly.assertThat(Arrays.asList((String[]) rs.getArray("roles").getArray()))
                        .as("roles")
                        .containsExactlyElementsOf(expected.roles());
                softly.assertThat(rs.getBytes("token_hash")).as("token_hash").isEqualTo(expected.tokenHash());
                softly.assertThat(instant(rs, "created_at")).as("created_at").isEqualTo(expected.createdAt());
                softly.assertThat(instant(rs, "last_seen_at")).as("last_seen_at").isEqualTo(expected.lastSeenAt());
                softly.assertThat(instant(rs, "idle_expires_at"))
                        .as("idle_expires_at")
                        .isEqualTo(expected.idleExpiresAt());
                softly.assertThat(instant(rs, "absolute_expires_at"))
                        .as("absolute_expires_at")
                        .isEqualTo(expected.absoluteExpiresAt());
                softly.assertThat(instant(rs, "revoked_at")).as("revoked_at").isEqualTo(expected.revokedAt());
                softly.assertThat(rs.getString("user_agent")).as("user_agent").isEqualTo(expected.userAgent());
                softly.assertAll();
            }
        }
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
}
