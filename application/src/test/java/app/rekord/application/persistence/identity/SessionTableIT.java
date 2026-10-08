package app.rekord.application.persistence.identity;

import static org.assertj.core.api.Assertions.assertThat;

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
import java.util.TimeZone;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class SessionTableIT extends AbstractRepositoryTest {

    private static final UUID PORTAL = IdentityRows.id(900);
    private static final UUID WEDDING = IdentityRows.id(901);

    private OrganizationEntity org;
    private UserEntity user;

    @BeforeEach
    void anOrganisationAndAUser() {
        org = IdentityRows.organization();
        user = IdentityRows.planner();
        inNewTransaction(() -> {
            em.persist(org);
            em.persist(user);
        });
    }

    @Test
    void a_valid_user_session_and_a_valid_portal_session_are_accepted() {
        // When a user session and a portal session of a wedding are stored
        inNewTransaction(() -> {
            em.persist(IdentityRows.userSession(1, org, user));
            em.persist(IdentityRows.portalSession(2, org, PORTAL, WEDDING));
        });

        // Then both rows exist: the refusals below come from the subject check and the token index alone
        assertThat(inNewTransactionReturning(
                        () -> em.createQuery("select count(s) from SessionEntity s", Long.class).getSingleResult()))
                .isEqualTo(2L);
    }

    @Test
    void a_user_session_with_a_portal_id_is_refused_on_ck_sessions_subject() {
        // Given a user session that also names a portal
        SessionEntity session = IdentityRows.userSession(1, org, user);
        session.setPortalId(PORTAL);

        // Then the subject check refuses it (BR-DM-12)
        assertThat(refusal(() -> em.persist(session))).isEqualTo(new Refusal("23514", "ck_sessions_subject"));
    }

    @Test
    void a_portal_session_without_a_wedding_id_is_refused_on_ck_sessions_subject() {
        // Given a portal session that names no wedding
        SessionEntity session = IdentityRows.portalSession(1, org, PORTAL, null);

        // Then the subject check refuses it (BR-DM-12)
        assertThat(refusal(() -> em.persist(session))).isEqualTo(new Refusal("23514", "ck_sessions_subject"));
    }

    @Test
    void a_second_session_with_the_same_token_hash_is_refused_on_ux_sessions_token() {
        // Given a stored session
        inNewTransaction(() -> em.persist(IdentityRows.userSession(1, org, user)));

        // When another session carries the same token hash
        SessionEntity other = IdentityRows.userSession(2, org, user);
        other.setTokenHash(IdentityRows.tokenHash(1));

        // Then the unique token index refuses it (BR-ID-09)
        assertThat(refusal(() -> em.persist(other))).isEqualTo(new Refusal("23505", "ux_sessions_token"));
    }

    @Test
    void an_instant_stored_while_the_jvm_zone_is_pacific_auckland_reads_back_unchanged() throws SQLException {
        // Given a JVM zone that is not UTC
        TimeZone before = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Auckland"));
        SessionEntity session = IdentityRows.userSession(1, org, user);
        Instant createdAt = Instant.parse("2027-06-12T10:00:00Z");
        session.setCreatedAt(createdAt);
        try {
            // When the session is stored, flushed and read back through a cleared persistence context
            Instant readBack = inNewTransactionReturning(() -> {
                em.persist(session);
                em.flush();
                em.clear();
                return em.find(SessionEntity.class, session.getId()).getCreatedAt();
            });

            // Then the instant is the same (BR-DM-03)
            assertThat(readBack).isEqualTo(createdAt);
        } finally {
            TimeZone.setDefault(before);
        }

        // And the column holds that instant, so a value stored shifted fails even when the read shifts it back
        try (Connection c = dataSource.getConnection();
                PreparedStatement s = c.prepareStatement(
                        "select extract(epoch from created_at) from sessions where id = ?")) {
            s.setObject(1, session.getId());
            try (ResultSet rs = s.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getLong(1)).isEqualTo(createdAt.getEpochSecond());
            }
        }

        // And the column is timestamptz: with the JVM and JDBC zone both UTC a plain timestamp would round-trip too
        try (Connection c = dataSource.getConnection();
                PreparedStatement s = c.prepareStatement("select data_type from information_schema.columns"
                        + " where table_name = 'sessions' and column_name = 'created_at'");
                ResultSet rs = s.executeQuery()) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString(1)).isEqualTo("timestamp with time zone");
        }
    }
}
