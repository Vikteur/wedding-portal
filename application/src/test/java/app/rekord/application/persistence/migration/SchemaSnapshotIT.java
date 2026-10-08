package app.rekord.application.persistence.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

class SchemaSnapshotIT {

    private static final String PARENT_AND_CHILD = """
            create table parent (
                id bigint primary key,
                code varchar(40) not null unique,
                seen_at timestamp with time zone
            );
            create table child (
                id bigint primary key,
                parent_id bigint not null references parent (id) on delete cascade,
                amount integer not null,
                constraint child_amount_positive check (amount > 0)
            );
            """;

    private static PostgreSQLContainer container;

    @BeforeAll
    static void startContainer() {
        container = new PostgreSQLContainer("postgres:17-alpine");
        container.start();
    }

    @AfterAll
    static void stopContainer() {
        container.stop();
    }

    @Test
    void reads_the_tables_columns_with_type_and_nullability_and_constraints_of_a_fixture_schema() throws SQLException {
        // Given a parent and a child table
        try (Connection c = freshDatabase()) {
            execute(c, PARENT_AND_CHILD);

            // When the schema is read
            SchemaSnapshot snapshot = SchemaSnapshot.read(c);

            // Then every table, column and constraint is listed in a deterministic order
            assertThat(snapshot).isEqualTo(declared());
        }
    }

    @Test
    void does_not_report_not_null_as_a_check_constraint() throws SQLException {
        // Given a table with NOT NULL columns, which PostgreSQL 17 also lists as CHECK constraints
        try (Connection c = freshDatabase()) {
            execute(c, "create table notnull_only (a integer not null, b text not null)");

            // When the schema is read
            SchemaSnapshot snapshot = SchemaSnapshot.read(c);

            // Then nullability lives on the column only
            assertThat(snapshot.constraints()).isEmpty();
            assertThat(snapshot.columns()).extracting(SchemaSnapshot.Column::nullable).containsExactly(false, false);
        }
    }

    @Test
    void a_changed_type_nullability_or_constraint_makes_the_snapshot_differ() throws SQLException {
        // Given the declared schema
        SchemaSnapshot declared = declared();

        // When one column type, one nullability or one constraint differs, then the snapshot is not equal
        for (String change : List.of(
                "alter table parent alter column seen_at type timestamp without time zone",
                "alter table child alter column amount drop not null",
                "alter table child drop constraint child_amount_positive")) {
            try (Connection c = freshDatabase()) {
                execute(c, PARENT_AND_CHILD);
                execute(c, change);
                assertThat(SchemaSnapshot.read(c)).as(change).isNotEqualTo(declared);
            }
        }
    }

    @Test
    void a_numeric_or_time_column_carries_its_precision_and_scale() throws SQLException {
        // Given numeric and time columns with and without an explicit precision
        try (Connection c = freshDatabase()) {
            execute(c, """
                    create table priced (
                        amount numeric(10,2) not null,
                        ratio numeric,
                        paid_at timestamp(3) with time zone,
                        seen_at timestamp with time zone,
                        starts time(0) without time zone
                    )""");

            // When the schema is read
            SchemaSnapshot snapshot = SchemaSnapshot.read(c);

            // Then precision and scale are part of the type; the default time precision (6) is not written out
            assertThat(snapshot.columns()).containsExactly(
                    new SchemaSnapshot.Column("priced", "amount", "numeric(10,2)", false),
                    new SchemaSnapshot.Column("priced", "ratio", "numeric", true),
                    new SchemaSnapshot.Column("priced", "paid_at", "timestamp(3) with time zone", true),
                    new SchemaSnapshot.Column("priced", "seen_at", "timestamp with time zone", true),
                    new SchemaSnapshot.Column("priced", "starts", "time(0) without time zone", true));

            // And a changed scale or time precision makes the snapshot differ
            execute(c, "alter table priced alter column amount type numeric(10,4)");
            assertThat(SchemaSnapshot.read(c)).isNotEqualTo(snapshot);
            execute(c, "alter table priced alter column amount type numeric(10,2)");
            execute(c, "alter table priced alter column paid_at type timestamp(6) with time zone");
            assertThat(SchemaSnapshot.read(c)).isNotEqualTo(snapshot);
        }
    }

    @Test
    void an_empty_database_has_the_empty_snapshot() throws SQLException {
        // Given a fresh database, even with a Flyway history table
        try (Connection c = freshDatabase()) {
            assertThat(SchemaSnapshot.read(c)).isEqualTo(SchemaSnapshot.empty());
            execute(c, "create table flyway_schema_history (installed_rank integer primary key, version varchar(50))");

            // When the schema is read, then the history table is not part of it
            assertThat(SchemaSnapshot.read(c)).isEqualTo(SchemaSnapshot.empty());
        }
    }

    private static SchemaSnapshot declared() {
        return new SchemaSnapshot(
                List.of(),
                List.of("child", "parent"),
                List.of(
                        new SchemaSnapshot.Column("child", "id", "bigint", false),
                        new SchemaSnapshot.Column("child", "parent_id", "bigint", false),
                        new SchemaSnapshot.Column("child", "amount", "integer", false),
                        new SchemaSnapshot.Column("parent", "id", "bigint", false),
                        new SchemaSnapshot.Column("parent", "code", "character varying(40)", false),
                        new SchemaSnapshot.Column("parent", "seen_at", "timestamp with time zone", true)),
                List.of(
                        new SchemaSnapshot.Constraint("child", "child_amount_positive", "CHECK", List.of(), "(amount > 0)"),
                        new SchemaSnapshot.Constraint(
                                "child", "child_parent_id_fkey", "FOREIGN KEY", List.of("parent_id"),
                                "references parent(id) on delete CASCADE"),
                        new SchemaSnapshot.Constraint("child", "child_pkey", "PRIMARY KEY", List.of("id"), ""),
                        new SchemaSnapshot.Constraint("parent", "parent_code_key", "UNIQUE", List.of("code"), ""),
                        new SchemaSnapshot.Constraint("parent", "parent_pkey", "PRIMARY KEY", List.of("id"), "")));
    }

    private static Connection freshDatabase() throws SQLException {
        try (Connection admin = connect("postgres");
                Statement s = admin.createStatement()) {
            String name = "snap_" + Long.toHexString(System.nanoTime());
            s.execute("create database " + name);
            return connect(name);
        }
    }

    private static Connection connect(String database) throws SQLException {
        String url = "jdbc:postgresql://" + container.getHost() + ":" + container.getMappedPort(5432) + "/" + database;
        return DriverManager.getConnection(url, container.getUsername(), container.getPassword());
    }

    private static void execute(Connection c, String sql) throws SQLException {
        try (Statement s = c.createStatement()) {
            s.execute(sql);
        }
    }
}
