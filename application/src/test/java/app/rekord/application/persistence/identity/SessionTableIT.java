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
import java.sql.Statement;
import java.time.Instant;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class SessionTableIT extends AbstractRepositoryTest {

    private static final UUID PORTAL = IdentityRows.id(900);
    private static final UUID WEDDING = IdentityRows.id(901);
    private static final String AUCKLAND = "Pacific/Auckland";
    /** 22:00 in Pacific/Auckland on that day (UTC+12). */
    private static final Instant CREATED_AT = IdentityRows.T0;

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
    void an_instant_stored_in_pacific_auckland_reads_back_unchanged_through_hibernate() throws SQLException {
        whileTheJvmZoneIsAuckland(() -> {
            // Given a session stored on a connection whose zone is Pacific/Auckland (UTC+12 in June)
            UUID id = storedInAuckland();

            // When it is read on another such connection, through a fresh persistence context
            Instant readBack = inNewTransactionReturning(() -> {
                sessionZoneIsAuckland();
                return em.find(SessionEntity.class, id).getCreatedAt();
            });

            // Then the instant is the same (BR-DM-03). With jdbc.timezone=UTC Hibernate also returns it unchanged
            // for a plain timestamp column, so this alone does not pin the column type: the tests below do
            assertThat(readBack).isEqualTo(CREATED_AT);
        });
    }

    @Test
    void an_instant_stored_in_pacific_auckland_reads_back_unchanged_over_jdbc_in_that_zone() throws SQLException {
        whileTheJvmZoneIsAuckland(() -> {
            // Given a session stored on a connection whose zone is Pacific/Auckland
            UUID id = storedInAuckland();

            // When plain JDBC reads the column on another such connection, with the JVM zone Auckland too
            Instant asTimestamp = jdbcInAuckland("select created_at from sessions where id = ?", id,
                    rs -> rs.getTimestamp(1).toInstant());
            long epoch = jdbcInAuckland("select extract(epoch from created_at)::bigint from sessions where id = ?", id,
                    rs -> rs.getLong(1));

            // Then it is the stored instant: a plain timestamp column holds 10:00 wall time and reads 12 hours off
            assertThat(asTimestamp).isEqualTo(CREATED_AT);
            assertThat(epoch).isEqualTo(CREATED_AT.getEpochSecond());
        });
    }

    @Test
    void a_connection_in_pacific_auckland_renders_the_stored_instant_with_its_offset() throws SQLException {
        whileTheJvmZoneIsAuckland(() -> {
            // Given a session stored on a connection whose zone is Pacific/Auckland
            UUID id = storedInAuckland();

            // When a connection in that zone renders it as text
            String rendered = jdbcInAuckland("select created_at::text from sessions where id = ?", id,
                    rs -> rs.getString(1));

            // Then 10:00 UTC shows as 22:00 with the +12 offset (a plain timestamp has no offset)
            assertThat(rendered).isEqualTo("2027-06-12 22:00:00+12");
        });
    }

    @Test
    void every_instant_column_of_sessions_is_timestamptz() throws SQLException {
        // Then the five instant columns are timestamptz, so the zone of a connection never moves a stored instant
        try (Connection c = dataSource.getConnection();
                PreparedStatement s = c.prepareStatement("select column_name, data_type from information_schema.columns"
                        + " where table_name = 'sessions' and column_name in ('created_at', 'last_seen_at',"
                        + " 'idle_expires_at', 'absolute_expires_at', 'revoked_at') order by column_name");
                ResultSet rs = s.executeQuery()) {
            Map<String, String> types = new TreeMap<>();
            while (rs.next()) {
                types.put(rs.getString(1), rs.getString(2));
            }
            assertThat(types).containsOnlyKeys("absolute_expires_at", "created_at", "idle_expires_at", "last_seen_at",
                    "revoked_at");
            assertThat(types.values()).containsOnly("timestamp with time zone");
        }
    }

    /** Stores a user session whose created_at is {@link #CREATED_AT}, on a connection whose zone is Auckland. */
    private UUID storedInAuckland() {
        SessionEntity session = IdentityRows.userSession(1, org, user);
        session.setCreatedAt(CREATED_AT);
        inNewTransaction(() -> {
            sessionZoneIsAuckland();
            em.persist(session);
        });
        return session.getId();
    }

    /** Sets the zone of this transaction's connection; it ends with the transaction, so the pooled one stays clean. */
    private void sessionZoneIsAuckland() {
        em.createNativeQuery("set local time zone '" + AUCKLAND + "'").executeUpdate();
    }

    /** Reads one row of the sessions table over plain JDBC on a connection whose zone is Auckland. */
    private <T> T jdbcInAuckland(String select, UUID id, RowReader<T> reader) throws SQLException {
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try (Statement zone = c.createStatement();
                    PreparedStatement s = c.prepareStatement(select)) {
                zone.execute("set local time zone '" + AUCKLAND + "'");
                s.setObject(1, id);
                try (ResultSet rs = s.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    return reader.read(rs);
                }
            } finally {
                c.rollback();
                c.setAutoCommit(true);
            }
        }
    }

    private static void whileTheJvmZoneIsAuckland(SqlAction action) throws SQLException {
        TimeZone before = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone(AUCKLAND));
        try {
            action.run();
        } finally {
            TimeZone.setDefault(before);
        }
    }

    @FunctionalInterface
    private interface RowReader<T> {
        T read(ResultSet rs) throws SQLException;
    }

    @FunctionalInterface
    private interface SqlAction {
        void run() throws SQLException;
    }
}
