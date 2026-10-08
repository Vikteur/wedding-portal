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
import java.util.HexFormat;
import java.util.UUID;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

/**
 * Every mapped field of an identity entity reaches its own column and comes back from it. A field written to the wrong
 * column, a column left unwritten and a value changed on the way each fail here. Every value in a row is distinct, so a
 * swap of two columns of the same type shows.
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
        SessionEntity session = new SessionEntity();
        session.setId(IdentityRows.id(701));
        session.setSubjectKind("USER");
        session.setUserId(user.getId());
        session.setPortalId(null);
        session.setWeddingId(IdentityRows.id(702));
        session.setOrgId(org.getId());
        session.setRoles(new String[] {"PLANNER", "DJ", "ADMIN"});
        session.setTokenHash(TOKEN_HASH);
        session.setCreatedAt(CREATED_AT);
        session.setLastSeenAt(UPDATED_AT);
        session.setIdleExpiresAt(FIRST);
        session.setAbsoluteExpiresAt(THIRD);
        session.setRevokedAt(SECOND);
        session.setUserAgent("Example Browser 2.1");

        // When it is stored
        inNewTransaction(() -> em.persist(session));

        // Then each column holds the value of its own field
        assertSessionRow(session);
    }

    @Test
    void a_portal_session_returns_every_field_from_its_own_column() throws SQLException {
        // Given an organisation and a portal session with a distinct value in every field but the account
        OrganizationEntity org = IdentityRows.organization();
        inNewTransaction(() -> em.persist(org));
        SessionEntity session = new SessionEntity();
        session.setId(IdentityRows.id(711));
        session.setSubjectKind("PORTAL");
        session.setUserId(null);
        session.setPortalId(IdentityRows.id(712));
        session.setWeddingId(IdentityRows.id(713));
        session.setOrgId(org.getId());
        session.setRoles(new String[] {"COUPLE", "GUEST"});
        session.setTokenHash(TOKEN_HASH);
        session.setCreatedAt(CREATED_AT);
        session.setLastSeenAt(UPDATED_AT);
        session.setIdleExpiresAt(SECOND);
        session.setAbsoluteExpiresAt(FOURTH);
        session.setRevokedAt(FIRST);
        session.setUserAgent("Example Browser 3.4");

        // When it is stored
        inNewTransaction(() -> em.persist(session));

        // Then each column holds the value of its own field (user_id is null: a portal session has no account)
        assertSessionRow(session);
    }

    @Test
    void a_user_returns_every_field_from_its_own_column() throws SQLException {
        // Given a user with a distinct value in every field
        UserEntity user = new UserEntity();
        user.setId(IdentityRows.id(721));
        user.setEmail("Round.Trip@example.com");
        user.setPasswordHash("$argon2id$made-up-hash-for-the-round-trip");
        user.setDisplayName("Rae Roundtrip");
        user.setPhone("+12025550100");
        user.setStatus("DISABLED");
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
            s.setObject(1, user.getId());
            try (ResultSet rs = s.executeQuery()) {
                assertThat(rs.next()).isTrue();
                SoftAssertions softly = new SoftAssertions();
                softly.assertThat(rs.getObject("id", UUID.class)).as("id").isEqualTo(user.getId());
                softly.assertThat(rs.getString("email")).as("email").isEqualTo(user.getEmail());
                softly.assertThat(rs.getString("password_hash")).as("password_hash").isEqualTo(user.getPasswordHash());
                softly.assertThat(rs.getString("display_name")).as("display_name").isEqualTo(user.getDisplayName());
                softly.assertThat(rs.getString("phone")).as("phone").isEqualTo(user.getPhone());
                softly.assertThat(rs.getString("status")).as("status").isEqualTo(user.getStatus());
                softly.assertThat(instant(rs, "last_login_at")).as("last_login_at").isEqualTo(user.getLastLoginAt());
                softly.assertThat(instant(rs, "password_changed_at"))
                        .as("password_changed_at")
                        .isEqualTo(user.getPasswordChangedAt());
                softly.assertThat(instant(rs, "created_at")).as("created_at").isEqualTo(user.getCreatedAt());
                softly.assertThat(instant(rs, "updated_at")).as("updated_at").isEqualTo(user.getUpdatedAt());
                softly.assertThat(instant(rs, "deleted_at")).as("deleted_at").isEqualTo(user.getDeletedAt());
                softly.assertAll();
            }
        }
    }

    @Test
    void an_organization_is_loaded_again_with_every_field_it_was_stored_with() {
        // Given an organisation with a distinct value in every field (a zone other than the column default)
        OrganizationEntity stored = new OrganizationEntity();
        stored.setId(IdentityRows.id(731));
        stored.setName("Round Trip Weddings");
        stored.setSlug("round-trip-weddings");
        stored.setTimezone("Pacific/Auckland");
        stored.setCreatedAt(CREATED_AT);
        stored.setUpdatedAt(UPDATED_AT);
        stored.setDeletedAt(THIRD);

        // When it is persisted, the persistence context is cleared and the row is loaded again
        OrganizationEntity loaded = inNewTransactionReturning(() -> {
            em.persist(stored);
            em.flush();
            em.clear();
            return em.find(OrganizationEntity.class, stored.getId());
        });

        // Then every field is the one that was stored
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(loaded).as("a new instance, read from the row").isNotSameAs(stored);
        softly.assertThat(loaded.getId()).as("id").isEqualTo(stored.getId());
        softly.assertThat(loaded.getName()).as("name").isEqualTo(stored.getName());
        softly.assertThat(loaded.getSlug()).as("slug").isEqualTo(stored.getSlug());
        softly.assertThat(loaded.getTimezone()).as("timezone").isEqualTo(stored.getTimezone());
        softly.assertThat(loaded.getCreatedAt()).as("createdAt").isEqualTo(stored.getCreatedAt());
        softly.assertThat(loaded.getUpdatedAt()).as("updatedAt").isEqualTo(stored.getUpdatedAt());
        softly.assertThat(loaded.getDeletedAt()).as("deletedAt").isEqualTo(stored.getDeletedAt());
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
        MembershipEntity stored = new MembershipEntity();
        stored.setId(IdentityRows.id(741));
        stored.setOrgId(org.getId());
        stored.setUserId(user.getId());
        stored.setRole("DJ");
        stored.setStatus("DISABLED");
        stored.setCreatedAt(CREATED_AT);
        stored.setUpdatedAt(UPDATED_AT);

        // When it is persisted, the persistence context is cleared and the row is loaded again
        MembershipEntity loaded = inNewTransactionReturning(() -> {
            em.persist(stored);
            em.flush();
            em.clear();
            return em.find(MembershipEntity.class, stored.getId());
        });

        // Then every field is the one that was stored
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(loaded).as("a new instance, read from the row").isNotSameAs(stored);
        softly.assertThat(loaded.getId()).as("id").isEqualTo(stored.getId());
        softly.assertThat(loaded.getOrgId()).as("orgId").isEqualTo(stored.getOrgId());
        softly.assertThat(loaded.getUserId()).as("userId").isEqualTo(stored.getUserId());
        softly.assertThat(loaded.getRole()).as("role").isEqualTo(stored.getRole());
        softly.assertThat(loaded.getStatus()).as("status").isEqualTo(stored.getStatus());
        softly.assertThat(loaded.getCreatedAt()).as("createdAt").isEqualTo(stored.getCreatedAt());
        softly.assertThat(loaded.getUpdatedAt()).as("updatedAt").isEqualTo(stored.getUpdatedAt());
        softly.assertAll();
    }

    /** Reads the sessions row of the entity over JDBC, every column by name, and compares it field by field. */
    private void assertSessionRow(SessionEntity expected) throws SQLException {
        try (Connection c = dataSource.getConnection();
                PreparedStatement s = c.prepareStatement("select * from sessions where id = ?")) {
            s.setObject(1, expected.getId());
            try (ResultSet rs = s.executeQuery()) {
                assertThat(rs.next()).isTrue();
                SoftAssertions softly = new SoftAssertions();
                softly.assertThat(rs.getObject("id", UUID.class)).as("id").isEqualTo(expected.getId());
                softly.assertThat(rs.getString("subject_kind")).as("subject_kind").isEqualTo(expected.getSubjectKind());
                softly.assertThat(rs.getObject("user_id", UUID.class)).as("user_id").isEqualTo(expected.getUserId());
                softly.assertThat(rs.getObject("portal_id", UUID.class))
                        .as("portal_id")
                        .isEqualTo(expected.getPortalId());
                softly.assertThat(rs.getObject("wedding_id", UUID.class))
                        .as("wedding_id")
                        .isEqualTo(expected.getWeddingId());
                softly.assertThat(rs.getObject("org_id", UUID.class)).as("org_id").isEqualTo(expected.getOrgId());
                softly.assertThat((String[]) rs.getArray("roles").getArray())
                        .as("roles")
                        .containsExactly(expected.getRoles());
                softly.assertThat(rs.getBytes("token_hash")).as("token_hash").isEqualTo(expected.getTokenHash());
                softly.assertThat(instant(rs, "created_at")).as("created_at").isEqualTo(expected.getCreatedAt());
                softly.assertThat(instant(rs, "last_seen_at")).as("last_seen_at").isEqualTo(expected.getLastSeenAt());
                softly.assertThat(instant(rs, "idle_expires_at"))
                        .as("idle_expires_at")
                        .isEqualTo(expected.getIdleExpiresAt());
                softly.assertThat(instant(rs, "absolute_expires_at"))
                        .as("absolute_expires_at")
                        .isEqualTo(expected.getAbsoluteExpiresAt());
                softly.assertThat(instant(rs, "revoked_at")).as("revoked_at").isEqualTo(expected.getRevokedAt());
                softly.assertThat(rs.getString("user_agent")).as("user_agent").isEqualTo(expected.getUserAgent());
                softly.assertAll();
            }
        }
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
}
