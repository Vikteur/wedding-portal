package app.rekord.application.persistence.identity;

import static org.assertj.core.api.Assertions.assertThat;

import app.rekord.adapter.persistence.identity.MembershipEntity;
import app.rekord.adapter.persistence.identity.OrganizationEntity;
import app.rekord.adapter.persistence.identity.SessionEntity;
import app.rekord.adapter.persistence.identity.UserEntity;
import app.rekord.application.persistence.FreshDatabase;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Table;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

/** AC #1: the application starts on an empty database, Flyway applies V2 and Hibernate validates the entities. */
@QuarkusTest
class IdentitySchemaIT {

    @Inject
    DataSource dataSource;

    @Inject
    EntityManager em;

    @Test
    void wedding_portal_started_on_an_empty_database_and_flyway_applied_the_identity_migration() throws SQLException {
        try (Connection c = dataSource.getConnection()) {
            // Given a database that this run started
            FreshDatabase.assertStartedAfterThisJvm(c);

            // Then the history starts with V1 and then V2, each successful; a later migration may follow
            List<String> history = new ArrayList<>();
            try (Statement s = c.createStatement();
                    ResultSet rs = s.executeQuery(
                            "select version, script, success from flyway_schema_history order by installed_rank")) {
                while (rs.next()) {
                    assertThat(rs.getBoolean("success")).isTrue();
                    history.add(rs.getString("version") + " " + rs.getString("script"));
                }
            }
            assertThat(history).startsWith("1 db/migration/V1__baseline.sql", "2 db/migration/V2__identity.sql");
        }
    }

    @Test
    void the_identity_tables_hold_the_oracle_columns_without_failed_login_count_and_locked_until()
            throws SQLException {
        assertThat(columns("organizations")).containsExactly(
                "id", "name", "slug", "timezone", "created_at", "updated_at", "deleted_at");
        assertThat(columns("users")).containsExactly(
                "id", "email", "password_hash", "display_name", "phone", "status", "last_login_at",
                "password_changed_at", "created_at", "updated_at", "deleted_at");
        assertThat(columns("memberships")).containsExactly(
                "id", "org_id", "user_id", "role", "status", "created_at", "updated_at");
        assertThat(columns("sessions")).containsExactly(
                "id", "subject_kind", "user_id", "portal_id", "wedding_id", "org_id", "roles", "token_hash",
                "created_at", "last_seen_at", "idle_expires_at", "absolute_expires_at", "revoked_at", "ip",
                "user_agent");

        // And an account has no lock-out counter (S20 UX-13)
        assertThat(columns("users")).doesNotContain("failed_login_count", "locked_until");

        // And a session stores the hash of the token, never the token (BR-ID-09)
        assertThat(columns("sessions")).contains("token_hash").doesNotContain("token");
        assertThat(columnType("sessions", "token_hash")).isEqualTo("bytea");
    }

    @Test
    void hibernate_validated_the_four_identity_entities_at_start() {
        // Then the schema is validated, never created or changed
        assertThat(ConfigProvider.getConfig().getValue("quarkus.hibernate-orm.schema-management.strategy", String.class))
                .isEqualTo("validate");

        // And the four entities are in the metamodel, mapped to the four tables
        Set<Class<?>> mapped = em.getMetamodel().getEntities().stream()
                .map(e -> e.getJavaType())
                .collect(Collectors.toSet());
        assertThat(mapped).contains(
                OrganizationEntity.class, UserEntity.class, MembershipEntity.class, SessionEntity.class);
        assertThat(OrganizationEntity.class.getAnnotation(Table.class).name()).isEqualTo("organizations");
        assertThat(UserEntity.class.getAnnotation(Table.class).name()).isEqualTo("users");
        assertThat(MembershipEntity.class.getAnnotation(Table.class).name()).isEqualTo("memberships");
        assertThat(SessionEntity.class.getAnnotation(Table.class).name()).isEqualTo("sessions");
    }

    private List<String> columns(String table) throws SQLException {
        List<String> names = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
                Statement s = c.createStatement();
                ResultSet rs = s.executeQuery("select column_name from information_schema.columns"
                        + " where table_schema = 'public' and table_name = '" + table + "' order by ordinal_position")) {
            while (rs.next()) {
                names.add(rs.getString(1));
            }
        }
        return names;
    }

    private String columnType(String table, String column) throws SQLException {
        try (Connection c = dataSource.getConnection();
                Statement s = c.createStatement();
                ResultSet rs = s.executeQuery("select udt_name from information_schema.columns where table_schema = 'public'"
                        + " and table_name = '" + table + "' and column_name = '" + column + "'")) {
            assertThat(rs.next()).isTrue();
            return rs.getString(1);
        }
    }
}
