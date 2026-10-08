package app.rekord.application.persistence.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.rekord.application.error.LogCapture;
import app.rekord.application.persistence.AbstractRepositoryTest;
import app.rekord.domain.shared.error.ErrorCode;
import app.rekord.domain.shared.error.RejectedException;
import app.rekord.usecase.identity.port.FirstAdminRepository;
import app.rekord.usecase.identity.port.NewFirstAdmin;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import java.util.logging.LogRecord;
import org.junit.jupiter.api.Test;

/** The adapter behind the first-admin bootstrap, over a real database; rows are seeded by JDBC. */
@QuarkusTest
class DefaultFirstAdminRepositoryIT extends AbstractRepositoryTest {

    private static final Instant NOW = Instant.parse("2027-06-12T10:00:00Z");
    private static final String HASH = "scrypt$16384$8$1$00112233445566778899aabbccddeeff$"
            + "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Inject
    FirstAdminRepository repository;

    private static UUID id(long n) {
        return new UUID(0L, n);
    }

    private static NewFirstAdmin admin(String email, String slug) {
        return new NewFirstAdmin(id(1), "Rekord Match", slug, "Europe/Amsterdam", id(2), email, "The planner", HASH,
                id(3), "ACTIVE", "ADMIN", "ACTIVE", NOW);
    }

    private boolean hasActiveAdmin() {
        return inNewTransactionReturning(() -> repository.hasActiveAdmin());
    }

    private void seedOrganization(long n, String slug) {
        update("insert into organizations (id, name, slug) values (?, ?, ?)", id(n), "Seeded " + n, slug);
    }

    private void seedUser(long n, String email, String status, Instant deletedAt) {
        update("insert into users (id, email, display_name, status, deleted_at) values (?, ?, ?, ?, ?)",
                id(n), email, "Seeded " + n, status, deletedAt == null ? null : Timestamp.from(deletedAt));
    }

    private void seedMembership(long n, long org, long user, String role, String status) {
        update("insert into memberships (id, org_id, user_id, role, status) values (?, ?, ?, ?, ?)",
                id(n), id(org), id(user), role, status);
    }

    private void update(String sql, Object... params) {
        try (Connection c = dataSource.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                s.setObject(i + 1, params[i]);
            }
            s.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private long count(String table) {
        try (Connection c = dataSource.getConnection();
                PreparedStatement s = c.prepareStatement("select count(*) from " + table);
                ResultSet rs = s.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private void emptyThe(String... tables) {
        for (String table : tables) {
            update("delete from " + table);
        }
    }

    /** Seeds an organisation, an account and a membership; the caller gives the variable part. */
    private void seedAdmin(String userStatus, Instant userDeletedAt, String role, String membershipStatus) {
        seedOrganization(10, "seeded-org");
        seedUser(11, "owner@example.com", userStatus, userDeletedAt);
        seedMembership(12, 10, 11, role, membershipStatus);
    }

    @Test
    void an_empty_database_has_no_active_admin() {
        assertThat(hasActiveAdmin()).isFalse();
    }

    @Test
    void an_active_admin_membership_of_an_active_live_account_counts() {
        seedAdmin("ACTIVE", null, "ADMIN", "ACTIVE");

        assertThat(hasActiveAdmin()).isTrue();
    }

    @Test
    void a_disabled_membership_a_disabled_or_invited_account_a_deleted_account_or_another_role_does_not_count() {
        seedAdmin("ACTIVE", null, "ADMIN", "DISABLED");
        assertThat(hasActiveAdmin()).as("disabled membership").isFalse();
        emptyThe("memberships", "users", "organizations");

        seedAdmin("DISABLED", null, "ADMIN", "ACTIVE");
        assertThat(hasActiveAdmin()).as("disabled account").isFalse();
        emptyThe("memberships", "users", "organizations");

        seedAdmin("INVITED", null, "ADMIN", "ACTIVE");
        assertThat(hasActiveAdmin()).as("invited account").isFalse();
        emptyThe("memberships", "users", "organizations");

        seedAdmin("ACTIVE", NOW, "ADMIN", "ACTIVE");
        assertThat(hasActiveAdmin()).as("deleted account").isFalse();
        emptyThe("memberships", "users", "organizations");

        seedAdmin("ACTIVE", null, "PLANNER", "ACTIVE");
        assertThat(hasActiveAdmin()).as("planner role").isFalse();
    }

    @Test
    void saving_writes_the_business_the_account_and_the_admin_membership() throws SQLException {
        inNewTransaction(() -> repository.saveFirstAdmin(admin("admin@example.com", "rekord-match")));

        assertThat(count("organizations")).isEqualTo(1);
        assertThat(count("users")).isEqualTo(1);
        assertThat(count("memberships")).isEqualTo(1);
        try (Connection c = dataSource.getConnection();
                PreparedStatement s = c.prepareStatement("""
                        select o.id as org_id, o.name, o.slug, o.timezone, o.created_at as org_created,
                               u.id as user_id, u.email, u.display_name, u.status as user_status, u.password_hash,
                               u.password_changed_at, u.created_at as user_created,
                               m.id as membership_id, m.org_id as m_org, m.user_id as m_user, m.role,
                               m.status as membership_status, m.created_at as m_created
                        from organizations o, users u, memberships m""");
                ResultSet rs = s.executeQuery()) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getObject("org_id", UUID.class)).isEqualTo(id(1));
            assertThat(rs.getString("name")).isEqualTo("Rekord Match");
            assertThat(rs.getString("slug")).isEqualTo("rekord-match");
            assertThat(rs.getString("timezone")).isEqualTo("Europe/Amsterdam");
            assertThat(rs.getTimestamp("org_created").toInstant()).isEqualTo(NOW);
            assertThat(rs.getObject("user_id", UUID.class)).isEqualTo(id(2));
            assertThat(rs.getString("email")).isEqualTo("admin@example.com");
            assertThat(rs.getString("display_name")).isEqualTo("The planner");
            assertThat(rs.getString("user_status")).isEqualTo("ACTIVE");
            assertThat(rs.getString("password_hash")).isEqualTo(HASH);
            assertThat(rs.getTimestamp("password_changed_at").toInstant()).isEqualTo(NOW);
            assertThat(rs.getTimestamp("user_created").toInstant()).isEqualTo(NOW);
            assertThat(rs.getObject("membership_id", UUID.class)).isEqualTo(id(3));
            assertThat(rs.getObject("m_org", UUID.class)).isEqualTo(id(1));
            assertThat(rs.getObject("m_user", UUID.class)).isEqualTo(id(2));
            assertThat(rs.getString("role")).isEqualTo("ADMIN");
            assertThat(rs.getString("membership_status")).isEqualTo("ACTIVE");
            assertThat(rs.getTimestamp("m_created").toInstant()).isEqualTo(NOW);
        }
        assertThat(hasActiveAdmin()).isTrue();
    }

    @Test
    void a_taken_address_is_refused_by_its_constraint_and_shows_no_value() {
        seedUser(50, "admin@example.com", "DISABLED", null);
        LogCapture capture = new LogCapture();
        capture.start();
        Throwable thrown;
        try {
            thrown = catchThrowable(() -> inNewTransaction(
                    () -> repository.saveFirstAdmin(admin("ADMIN@example.com", "rekord-match"))));
        } finally {
            capture.stop();
        }

        assertThat(thrown).isInstanceOf(RejectedException.class);
        RejectedException refused = (RejectedException) thrown;
        assertThat(refused.kind()).isEqualTo(RejectedException.Kind.CONFLICT);
        assertThat(refused.code()).isEqualTo(ErrorCode.DUPLICATE_USERNAME);
        assertThat(refused.getCause()).isNull();
        assertThat(refused.getMessage()).doesNotContain("example.com");
        for (LogRecord record : capture.records()) {
            String text = LogCapture.text(record);
            assertThat(text).doesNotContain("example.com").doesNotContain("The planner").doesNotContain(HASH);
        }
        assertThat(count("organizations")).isZero();
        assertThat(count("memberships")).isZero();
    }

    @Test
    void a_taken_slug_is_refused_as_duplicate_name() {
        seedOrganization(60, "rekord-match");

        assertThatThrownBy(() -> inNewTransaction(
                        () -> repository.saveFirstAdmin(admin("new@example.com", "Rekord-Match"))))
                .isInstanceOfSatisfying(RejectedException.class, refused -> {
                    assertThat(refused.kind()).isEqualTo(RejectedException.Kind.CONFLICT);
                    assertThat(refused.code()).isEqualTo(ErrorCode.DUPLICATE_NAME);
                    assertThat(refused.getCause()).isNull();
                    assertThat(refused.getMessage()).doesNotContain("example.com");
                });
        assertThat(count("users")).isZero();
    }

    private static Throwable catchThrowable(Runnable action) {
        try {
            action.run();
            return null;
        } catch (Throwable t) {
            return t;
        }
    }
}
