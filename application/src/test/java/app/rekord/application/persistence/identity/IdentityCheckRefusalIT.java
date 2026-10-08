package app.rekord.application.persistence.identity;

import static org.assertj.core.api.Assertions.assertThat;

import app.rekord.adapter.persistence.identity.OrganizationEntity;
import app.rekord.adapter.persistence.identity.UserEntity;
import app.rekord.application.persistence.AbstractRepositoryTest;
import io.quarkus.test.junit.QuarkusTest;
import java.util.HexFormat;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The check constraints of the identity tables, one refused statement per conjunct and per status list. Plain SQL
 * inserts, so no entity code stands between the statement and the check: each row is refused with SQLState 23514 and
 * the name of the constraint, and the valid control rows of the same shape are accepted, which shows the refusals come
 * from the check and not from a key or a missing column. The two refusals that {@code SessionTableIT} already holds
 * (a user session with a portal, a portal session without a wedding) are not repeated.
 */
@QuarkusTest
class IdentityCheckRefusalIT extends AbstractRepositoryTest {

    private static final UUID ORG_ID = IdentityRows.organization().getId();
    private static final UUID USER_ID = IdentityRows.planner().getId();
    private static final UUID PORTAL_ID = IdentityRows.id(900);
    private static final UUID WEDDING_ID = IdentityRows.id(901);

    private static final String CHECK_VIOLATION = "23514";

    @BeforeEach
    void anOrganisationAndAUser() {
        OrganizationEntity org = IdentityRows.organization();
        UserEntity user = IdentityRows.planner();
        inNewTransaction(() -> {
            em.persist(org);
            em.persist(user);
        });
    }

    static Stream<Arguments> refusedStatements() {
        return Stream.of(
                // ck_sessions_subject: the conjuncts of the USER branch and of the PORTAL branch
                Arguments.of(
                        "a user session without an account",
                        session("'USER'", "null", "null", "null"),
                        "ck_sessions_subject"),
                Arguments.of(
                        "a portal session without a portal",
                        session("'PORTAL'", "null", "null", uuid(WEDDING_ID)),
                        "ck_sessions_subject"),
                Arguments.of(
                        "a portal session that also names an account",
                        session("'PORTAL'", uuid(USER_ID), uuid(PORTAL_ID), uuid(WEDDING_ID)),
                        "ck_sessions_subject"),
                // The kind check (sessions_subject_kind_check) sorts after the subject check, which fires first
                Arguments.of(
                        "a session of an unknown kind",
                        session("'NOPE'", uuid(USER_ID), "null", "null"),
                        "ck_sessions_subject"),
                Arguments.of(
                        "a membership with an unknown status",
                        membership("'PLANNER'", "'NOPE'"),
                        "memberships_status_check"),
                Arguments.of(
                        "a membership with an unknown role",
                        membership("'OWNER'", "'ACTIVE'"),
                        "memberships_role_check"),
                Arguments.of("a user with an unknown status", user("'NOPE'"), "users_status_check"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("refusedStatements")
    void a_statement_that_breaks_a_check_is_refused_on_that_check(String name, String sql, String constraint) {
        assertThat(refusal(() -> em.createNativeQuery(sql).executeUpdate()))
                .isEqualTo(new Refusal(CHECK_VIOLATION, constraint));
    }

    @Test
    void the_valid_control_rows_of_the_same_shape_are_accepted() {
        // When a user session, a portal session, a membership and a user that break no check are inserted
        inNewTransaction(() -> {
            em.createNativeQuery(session("'USER'", uuid(USER_ID), "null", "null", 1)).executeUpdate();
            em.createNativeQuery(session("'PORTAL'", "null", uuid(PORTAL_ID), uuid(WEDDING_ID), 2)).executeUpdate();
            em.createNativeQuery(membership("'PLANNER'", "'ACTIVE'")).executeUpdate();
            em.createNativeQuery(user("'INVITED'")).executeUpdate();
        });

        // Then all four rows exist
        assertThat(count("SessionEntity")).isEqualTo(2L);
        assertThat(count("MembershipEntity")).isEqualTo(1L);
        assertThat(count("UserEntity")).isEqualTo(2L);
    }

    private long count(String entity) {
        return inNewTransactionReturning(
                () -> em.createQuery("select count(e) from " + entity + " e", Long.class).getSingleResult());
    }

    private static String session(String kind, String userId, String portalId, String weddingId) {
        return session(kind, userId, portalId, weddingId, 1);
    }

    /** An insert of a session whose subject columns are the given SQL literals; the other columns are valid. */
    private static String session(String kind, String userId, String portalId, String weddingId, int n) {
        return "insert into sessions (id, subject_kind, user_id, portal_id, wedding_id, org_id, roles, token_hash,"
                + " created_at, last_seen_at, idle_expires_at, absolute_expires_at) values ("
                + uuid(IdentityRows.id(800 + n)) + ", " + kind + ", " + userId + ", " + portalId + ", " + weddingId
                + ", " + uuid(ORG_ID) + ", array['PLANNER'], decode('"
                + HexFormat.of().formatHex(IdentityRows.tokenHash(n)) + "', 'hex'), now(), now(), now(), now())";
    }

    /** An insert of a membership whose role and status are the given SQL literals. */
    private static String membership(String role, String status) {
        return "insert into memberships (id, org_id, user_id, role, status, created_at, updated_at) values ("
                + uuid(IdentityRows.id(850)) + ", " + uuid(ORG_ID) + ", " + uuid(USER_ID) + ", " + role + ", " + status
                + ", now(), now())";
    }

    /** An insert of a user (another one than the planner of the fixture) whose status is the given SQL literal. */
    private static String user(String status) {
        return "insert into users (id, email, display_name, status, created_at, updated_at) values ("
                + uuid(IdentityRows.id(860)) + ", 'control@example.com', 'Cory Control', " + status + ", now(), now())";
    }

    private static String uuid(UUID id) {
        return "'" + id + "'";
    }
}
