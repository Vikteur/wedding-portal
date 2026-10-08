package app.rekord.application.persistence.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

    // The Dev Services pin and every other test stay on PostgreSQL 17; 18 runs only the tests that name it.
    private static final String POSTGRES_17 = "postgres:17-alpine";
    private static final String POSTGRES_18 = "postgres:18";

    /** The images the NOT NULL tests run on; the comment at the NOT NULL filter of SchemaSnapshot names each version. */
    static final List<String> NOT_NULL_IMAGES = List.of(POSTGRES_17, POSTGRES_18);

    private static final Map<String, PostgreSQLContainer> CONTAINERS = new HashMap<>();

    private static PostgreSQLContainer container;

    @BeforeAll
    static void startContainer() {
        container = containerFor(POSTGRES_17);
    }

    @AfterAll
    static void stopContainers() {
        CONTAINERS.values().forEach(PostgreSQLContainer::stop);
        CONTAINERS.clear();
    }

    /** One container per image, started on first use and stopped after the last test. */
    private static synchronized PostgreSQLContainer containerFor(String image) {
        return CONTAINERS.computeIfAbsent(image, i -> {
            PostgreSQLContainer started = new PostgreSQLContainer(i);
            started.start();
            return started;
        });
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

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {POSTGRES_17, POSTGRES_18})
    void does_not_report_not_null_as_a_check_constraint(String image) throws SQLException {
        // Given a table with NOT NULL columns, which PostgreSQL 17 lists as CHECK constraints named by oid and
        // PostgreSQL 18 lists as CHECK constraints under their real names (notnull_only_a_not_null)
        try (Connection c = freshDatabase(containerFor(image))) {
            execute(c, "create table notnull_only (a integer not null, b text not null)");

            // When the schema is read
            SchemaSnapshot snapshot = SchemaSnapshot.read(c);

            // Then nullability lives on the column only
            assertThat(snapshot.constraints()).isEmpty();
            assertThat(snapshot.columns()).extracting(SchemaSnapshot.Column::nullable).containsExactly(false, false);
        }
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {POSTGRES_17, POSTGRES_18})
    void a_check_constraint_is_still_listed_beside_not_null_columns(String image) throws SQLException {
        // Given NOT NULL columns, a CHECK on one of them, and a CHECK whose clause is IS NOT NULL
        try (Connection c = freshDatabase(containerFor(image))) {
            execute(c, """
                    create table mixed (
                        a integer not null,
                        b text not null,
                        constraint mixed_a_positive check (a > 0),
                        constraint mixed_b_filled check (b is not null)
                    )""");

            // When the schema is read, then both CHECK constraints are listed and no NOT NULL is
            assertThat(SchemaSnapshot.read(c).constraints()).containsExactly(
                    new SchemaSnapshot.Constraint("mixed", "mixed_a_positive", "CHECK", List.of(), "(a > 0)"),
                    new SchemaSnapshot.Constraint("mixed", "mixed_b_filled", "CHECK", List.of(), "(b IS NOT NULL)"));
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
    void a_check_or_foreign_key_name_used_on_two_tables_is_refused_by_name() throws SQLException {
        // information_schema keys a check clause and a delete rule by schema and name only, so it cannot tell them apart
        for (String ddl : List.of(
                """
                create table first_amount (n integer constraint amount_positive check (n > 0));
                create table second_amount (n integer constraint amount_positive check (n > 1));
                """,
                """
                create table target (id bigint primary key);
                create table first_ref (t bigint constraint ref_target references target (id));
                create table second_ref (t bigint constraint ref_target references target (id) on delete cascade);
                """)) {
            // Given two tables whose constraints share one name
            try (Connection c = freshDatabase()) {
                execute(c, ddl);

                // When the schema is read, then it is refused with the shared name
                assertThatThrownBy(() -> SchemaSnapshot.read(c))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining(ddl.contains("amount_positive") ? "amount_positive" : "ref_target");
            }
        }
    }

    @Test
    void a_user_schema_is_listed_and_an_enum_or_array_column_is_typed_by_its_udt_name() throws SQLException {
        // Given a schema of its own, and an enum and an array column in public
        try (Connection c = freshDatabase()) {
            execute(c, """
                    create schema audit;
                    create type mood as enum ('calm', 'busy');
                    create table tagged (state mood not null, tags text[])
                    """);

            // When the schema is read
            SchemaSnapshot snapshot = SchemaSnapshot.read(c);

            // Then
            assertThat(snapshot.schemas()).containsExactly("audit");
            assertThat(snapshot.columns()).containsExactly(
                    new SchemaSnapshot.Column("tagged", "state", "mood", false),
                    new SchemaSnapshot.Column("tagged", "tags", "_text", true));
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

    private static final String TAGGED = """
            create table tagged (code text not null, gone_at timestamp with time zone, label text);
            create unique index ux_tagged_code on tagged (lower(code)) where gone_at is null;
            create index ix_tagged_label on tagged (label);
            """;

    private static final SchemaSnapshot.Index UX_TAGGED_CODE = new SchemaSnapshot.Index("tagged", "ux_tagged_code",
            "CREATE UNIQUE INDEX ux_tagged_code ON public.tagged USING btree (lower(code)) WHERE (gone_at IS NULL)");
    private static final SchemaSnapshot.Index IX_TAGGED_LABEL = new SchemaSnapshot.Index("tagged", "ix_tagged_label",
            "CREATE INDEX ix_tagged_label ON public.tagged USING btree (label)");

    @Test
    void lists_an_index_that_is_no_constraint_with_its_table_name_and_definition() throws SQLException {
        // Given a partial expression unique index and a plain index
        try (Connection c = freshDatabase()) {
            execute(c, TAGGED);

            // When the schema is read
            SchemaSnapshot snapshot = SchemaSnapshot.read(c);

            // Then both are listed, ordered by table and name, with PostgreSQL's pg_get_indexdef text
            assertThat(snapshot.indexes()).containsExactly(IX_TAGGED_LABEL, UX_TAGGED_CODE);
        }
    }

    @Test
    void an_index_that_backs_a_primary_key_or_unique_constraint_is_not_listed() throws SQLException {
        // Given the parent and child fixture: primary keys and a unique constraint only
        try (Connection c = freshDatabase()) {
            execute(c, PARENT_AND_CHILD);
            assertThat(SchemaSnapshot.read(c).indexes()).isEmpty();

            // And a plain unique index, which backs no constraint (a foreign key onto it is refused: see
            // a_foreign_key_onto_a_unique_index_that_backs_no_constraint_is_refused_by_name_and_table)
            execute(c, """
                    create table target (code text not null);
                    create unique index ux_target_code on target (code);
                    """);

            // When the schema is read, then that index is listed: no constraint of its own table hides it
            assertThat(SchemaSnapshot.read(c).indexes()).containsExactly(new SchemaSnapshot.Index("target",
                    "ux_target_code", "CREATE UNIQUE INDEX ux_target_code ON public.target USING btree (code)"));
        }
    }

    @Test
    void a_dropped_index_or_a_changed_predicate_expression_or_uniqueness_makes_the_snapshot_differ()
            throws SQLException {
        // Given the declared schema with its indexes
        SchemaSnapshot declared;
        try (Connection c = freshDatabase()) {
            execute(c, TAGGED);
            declared = SchemaSnapshot.read(c);
        }
        assertThat(declared.indexes()).hasSize(2);

        // When an index is dropped, or its predicate, expression or uniqueness differs, then the snapshot is not equal
        for (String change : List.of(
                "drop index ix_tagged_label",
                "drop index ux_tagged_code",
                "drop index ux_tagged_code; create unique index ux_tagged_code on tagged (lower(code))",
                "drop index ux_tagged_code; create unique index ux_tagged_code on tagged (code) where gone_at is null",
                "drop index ux_tagged_code; create unique index ux_tagged_code on tagged (lower(code))"
                        + " where gone_at is not null",
                "drop index ux_tagged_code; create unique index ux_tagged_code on tagged (upper(code))"
                        + " where gone_at is null",
                "drop index ix_tagged_label; create index ix_tagged_label on tagged (label) where gone_at is null",
                "drop index ix_tagged_label; create unique index ix_tagged_label on tagged (label)")) {
            try (Connection c = freshDatabase()) {
                execute(c, TAGGED);
                execute(c, change);
                assertThat(SchemaSnapshot.read(c)).as(change).isNotEqualTo(declared);
            }
        }
    }

    @Test
    void an_index_of_the_history_table_or_of_another_schema_is_not_listed() throws SQLException {
        // Given an index on the Flyway history table and an index in a schema of its own
        try (Connection c = freshDatabase()) {
            execute(c, """
                    create table flyway_schema_history (installed_rank integer primary key, success boolean);
                    create index flyway_schema_history_s_idx on flyway_schema_history (success);
                    create schema audit;
                    create table audit.entries (note text);
                    create index ix_entries_note on audit.entries (note);
                    """);

            // When the schema is read, then neither is listed
            assertThat(SchemaSnapshot.read(c).indexes()).isEmpty();
        }
    }

    private static final String DEFAULTED = """
            create table defaulted (
                id bigint generated always as identity primary key,
                serial_no integer not null,
                state text not null default 'NEW',
                seen_at timestamp with time zone not null default now(),
                note text
            );
            """;

    @Test
    void a_dropped_or_changed_column_default_or_identity_makes_the_snapshot_differ() throws SQLException {
        // Given the declared schema with a literal default, an expression default and an identity column
        SchemaSnapshot declared;
        try (Connection c = freshDatabase()) {
            execute(c, DEFAULTED);
            declared = SchemaSnapshot.read(c);
        }
        assertThat(declared.defaults()).hasSize(3);

        // When a default is dropped, changed or added, or an identity is added, dropped, switched or has one of its
        // options (increment, start, minimum, maximum, cycle) changed, then the snapshot is not equal
        for (String change : List.of(
                "alter table defaulted alter column state drop default",
                "alter table defaulted alter column state set default 'OLD'",
                "alter table defaulted alter column seen_at set default clock_timestamp()",
                "alter table defaulted alter column seen_at drop default",
                "alter table defaulted alter column note set default 'none'",
                "alter table defaulted alter column serial_no add generated by default as identity",
                "alter table defaulted alter column id drop identity",
                "alter table defaulted alter column id set generated by default",
                "alter table defaulted alter column id set increment by 10",
                "alter table defaulted alter column id set start with 1000",
                "alter table defaulted alter column id set minvalue 0",
                "alter table defaulted alter column id set maxvalue 1000",
                "alter table defaulted alter column id set cycle")) {
            try (Connection c = freshDatabase()) {
                execute(c, DEFAULTED);
                execute(c, change);
                assertThat(SchemaSnapshot.read(c)).as(change).isNotEqualTo(declared);
            }
        }
    }

    @Test
    void lists_a_column_default_as_postgresql_renders_it_and_an_identity_column_with_its_generation_and_options()
            throws SQLException {
        // Given a literal default, an expression default and an identity column of each generation, in two tables
        try (Connection c = freshDatabase()) {
            execute(c, DEFAULTED);
            execute(c, """
                    create table counted (
                        n integer generated by default as identity,
                        label varchar(10) default 'x',
                        plain integer
                    )""");
            execute(c, """
                    create table tuned (
                        code smallint generated always as identity
                            (start with 100 increment by 5 minvalue 10 maxvalue 500 cycle)
                    )""");

            // When the schema is read
            SchemaSnapshot snapshot = SchemaSnapshot.read(c);

            // Then each is listed once, ordered by table and column position; a column without either is not; an
            // identity reads its generation, then its start, increment, minimum and maximum, then cycle or no cycle
            assertThat(snapshot.defaults()).containsExactly(
                    new SchemaSnapshot.Default("counted", "n", null,
                            "BY DEFAULT start 1 increment 1 min 1 max 2147483647 no cycle"),
                    new SchemaSnapshot.Default("counted", "label", "'x'::character varying", null),
                    new SchemaSnapshot.Default("defaulted", "id", null,
                            "ALWAYS start 1 increment 1 min 1 max 9223372036854775807 no cycle"),
                    new SchemaSnapshot.Default("defaulted", "state", "'NEW'::text", null),
                    new SchemaSnapshot.Default("defaulted", "seen_at", "now()", null),
                    new SchemaSnapshot.Default("tuned", "code", null,
                            "ALWAYS start 100 increment 5 min 10 max 500 cycle"));
        }
    }

    @Test
    void a_default_of_the_history_table_a_view_or_another_schema_is_not_listed() throws SQLException {
        // Given a default on the Flyway history table, on a view, and on a table of a schema of its own
        try (Connection c = freshDatabase()) {
            execute(c, """
                    create table flyway_schema_history (
                        installed_rank integer primary key, installed_on timestamp default now());
                    create schema audit;
                    create table audit.entries (note text default 'x');
                    create table base (state text default 'NEW');
                    create view base_view as select state from base;
                    alter view base_view alter column state set default 'VIEW';
                    """);

            // When the schema is read, then only the default of the table of public is listed
            assertThat(SchemaSnapshot.read(c).defaults())
                    .containsExactly(new SchemaSnapshot.Default("base", "state", "'NEW'::text", null));
        }
    }

    private static final String COMPOSITE_TARGET = """
            create table target (
                x integer not null,
                y integer not null,
                constraint target_x_y_key unique (x, y)
            );
            create table pointer (a integer, b integer);
            """;

    private static SchemaSnapshot compositeForeignKey(String references) throws SQLException {
        try (Connection c = freshDatabase()) {
            execute(c, COMPOSITE_TARGET);
            execute(c, "alter table pointer add constraint pointer_target_fkey foreign key (a, b) references target "
                    + references);
            return SchemaSnapshot.read(c);
        }
    }

    @Test
    void a_composite_foreign_key_lists_the_referenced_columns_in_its_mapping_order() throws SQLException {
        // Given a foreign key (a, b) onto the unique key (x, y), mapped a to y and b to x
        // When the schema is read
        SchemaSnapshot snapshot = compositeForeignKey("(y, x)");

        // Then the referenced columns follow the mapping of the foreign key, not the order of the unique key
        assertThat(snapshot.constraints()).contains(new SchemaSnapshot.Constraint("pointer", "pointer_target_fkey",
                "FOREIGN KEY", List.of("a", "b"), "references target(y,x) on delete NO ACTION"));
    }

    @Test
    void a_composite_foreign_key_with_its_mapping_swapped_makes_the_snapshot_differ() throws SQLException {
        // Given two foreign keys (a, b) onto the same unique key (x, y), mapped a to y and b to x in one, a to x and
        // b to y in the other
        SchemaSnapshot crossed = compositeForeignKey("(y, x)");
        SchemaSnapshot straight = compositeForeignKey("(x, y)");

        // Then the straight one lists x before y, and the two snapshots are not equal
        assertThat(straight.constraints()).contains(new SchemaSnapshot.Constraint("pointer", "pointer_target_fkey",
                "FOREIGN KEY", List.of("a", "b"), "references target(x,y) on delete NO ACTION"));
        assertThat(crossed).isNotEqualTo(straight);
    }

    @Test
    void the_column_order_of_a_composite_primary_key_and_of_a_composite_unique_key_is_pinned() throws SQLException {
        // Given a primary key (b, a) and a unique key (d, c) that list their columns against the table's order
        String keyed = """
                create table keyed (
                    a integer not null, b integer not null, c integer not null, d integer not null,
                    constraint keyed_pkey primary key (b, a),
                    constraint keyed_cd_key unique (d, c)
                )""";
        SchemaSnapshot declared;
        try (Connection c = freshDatabase()) {
            execute(c, keyed);

            // When the schema is read
            declared = SchemaSnapshot.read(c);

            // Then each key lists its columns in the order of its declaration
            assertThat(declared.constraints()).containsExactly(
                    new SchemaSnapshot.Constraint("keyed", "keyed_cd_key", "UNIQUE", List.of("d", "c"), ""),
                    new SchemaSnapshot.Constraint("keyed", "keyed_pkey", "PRIMARY KEY", List.of("b", "a"), ""));
        }

        // And a key with its columns in the other order makes the snapshot differ
        for (String change : List.of(
                "alter table keyed drop constraint keyed_pkey, add constraint keyed_pkey primary key (a, b)",
                "alter table keyed drop constraint keyed_cd_key, add constraint keyed_cd_key unique (c, d)")) {
            try (Connection c = freshDatabase()) {
                execute(c, keyed);
                execute(c, change);
                assertThat(SchemaSnapshot.read(c)).as(change).isNotEqualTo(declared);
            }
        }
    }

    @Test
    void a_foreign_key_onto_a_unique_index_that_backs_no_constraint_is_refused_by_name_and_table()
            throws SQLException {
        // Given a foreign key whose target is a unique index, not a PRIMARY KEY or UNIQUE constraint
        try (Connection c = freshDatabase()) {
            execute(c, """
                    create table target (code text not null);
                    create unique index ux_target_code on target (code);
                    create table pointer (
                        target_code text,
                        constraint pointer_target_code_fkey foreign key (target_code) references target (code)
                    );
                    """);

            // When the schema is read, then it is refused with the foreign key and its table, not rendered as
            // references null(null)
            assertThatThrownBy(() -> SchemaSnapshot.read(c))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("pointer_target_code_fkey")
                    .hasMessageContaining("pointer");
        }
    }

    private static final String MOODS = """
            create type mood as enum ('calm', 'busy', 'tense');
            create table tagged (state mood not null);
            """;

    @Test
    void lists_an_enum_type_of_public_with_its_labels_in_order() throws SQLException {
        // Given enum types in public, one of them without a label, and an enum type in a schema of its own
        try (Connection c = freshDatabase()) {
            execute(c, """
                    create type size as enum ('s', 'm', 'l');
                    create type mood as enum ('calm', 'busy', 'tense');
                    create type bare as enum ();
                    create schema audit;
                    create type audit.hidden as enum ('x');
                    """);

            // When the schema is read
            SchemaSnapshot snapshot = SchemaSnapshot.read(c);

            // Then each type of public is listed once, ordered by name, with its labels in their sort order
            assertThat(snapshot.enums()).containsExactly(
                    new SchemaSnapshot.EnumType("bare", List.of()),
                    new SchemaSnapshot.EnumType("mood", List.of("calm", "busy", "tense")),
                    new SchemaSnapshot.EnumType("size", List.of("s", "m", "l")));
        }
    }

    @Test
    void an_added_renamed_reordered_or_removed_enum_label_makes_the_snapshot_differ() throws SQLException {
        // Given a snapshot that declares the enum type and the column that uses it
        SchemaSnapshot declared = new SchemaSnapshot(List.of(), List.of("tagged"),
                List.of(new SchemaSnapshot.Column("tagged", "state", "mood", false)), List.of(), List.of(), List.of(),
                List.of(new SchemaSnapshot.EnumType("mood", List.of("calm", "busy", "tense"))));
        try (Connection c = freshDatabase()) {
            execute(c, MOODS);
            assertThat(SchemaSnapshot.read(c)).isEqualTo(declared);
        }

        // When a label is added (last or in the middle), renamed, reordered or removed, then the snapshot is not equal;
        // the column keeps the type name "mood" throughout, so only the labels can show it
        String recreate = "alter table tagged alter column state type text; drop type mood; "
                + "create type mood as enum (%s); alter table tagged alter column state type mood using state::mood";
        for (String change : List.of(
                "alter type mood add value 'new'",
                "alter type mood add value 'new' before 'busy'",
                "alter type mood rename value 'tense' to 'strained'",
                recreate.formatted("'busy', 'calm', 'tense'"),
                recreate.formatted("'calm', 'busy'"))) {
            try (Connection c = freshDatabase()) {
                execute(c, MOODS);
                execute(c, change);
                assertThat(SchemaSnapshot.read(c)).as(change).isNotEqualTo(declared);
            }
        }
    }

    @Test
    void the_four_five_and_six_argument_snapshots_declare_no_enum_type() {
        // Given snapshots built with the components that predate enum types
        SchemaSnapshot four = new SchemaSnapshot(List.of(), List.of(), List.of(), List.of());
        SchemaSnapshot five = new SchemaSnapshot(List.of(), List.of(), List.of(), List.of(), List.of());
        SchemaSnapshot six = new SchemaSnapshot(List.of(), List.of(), List.of(), List.of(), List.of(), List.of());

        // Then they declare none, and equal the empty snapshot
        assertThat(four.enums()).isEmpty();
        assertThat(five.enums()).isEmpty();
        assertThat(six.enums()).isEmpty();
        assertThat(SchemaSnapshot.empty()).isEqualTo(four).isEqualTo(five).isEqualTo(six);
    }

    @Test
    void the_four_and_five_argument_snapshots_declare_no_column_default() {
        // Given snapshots built with the components that predate column defaults and identity
        SchemaSnapshot four = new SchemaSnapshot(List.of(), List.of(), List.of(), List.of());
        SchemaSnapshot five = new SchemaSnapshot(List.of(), List.of(), List.of(), List.of(), List.of());

        // Then they declare none, and equal the empty snapshot
        assertThat(four.defaults()).isEmpty();
        assertThat(five.defaults()).isEmpty();
        assertThat(SchemaSnapshot.empty()).isEqualTo(four).isEqualTo(five);
    }

    @Test
    void the_four_argument_snapshot_declares_no_index() {
        // Given a snapshot built with the four components that predate indexes
        SchemaSnapshot snapshot = new SchemaSnapshot(List.of(), List.of(), List.of(), List.of());

        // Then it declares no index, and equals the empty snapshot
        assertThat(snapshot.indexes()).isEmpty();
        assertThat(SchemaSnapshot.empty()).isEqualTo(snapshot);
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
        return freshDatabase(container);
    }

    private static Connection freshDatabase(PostgreSQLContainer on) throws SQLException {
        try (Connection admin = connect(on, "postgres");
                Statement s = admin.createStatement()) {
            String name = "snap_" + Long.toHexString(System.nanoTime());
            s.execute("create database " + name);
            return connect(on, name);
        }
    }

    private static Connection connect(PostgreSQLContainer on, String database) throws SQLException {
        String url = "jdbc:postgresql://" + on.getHost() + ":" + on.getMappedPort(5432) + "/" + database;
        return DriverManager.getConnection(url, on.getUsername(), on.getPassword());
    }

    private static void execute(Connection c, String sql) throws SQLException {
        try (Statement s = c.createStatement()) {
            s.execute(sql);
        }
    }
}
