package app.rekord.application.persistence.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The shape of schema {@code public} as {@code information_schema} shows it, sorted deterministically, so a migration
 * test can compare it with the shape the migration declares: base tables, columns (type and nullability) and PRIMARY
 * KEY, UNIQUE, FOREIGN KEY and CHECK constraints, the column defaults and identity settings (generation, start,
 * increment, minimum, maximum and cycle), plus the names of the other non-system schemas, the indexes that back no
 * constraint of their own table (partial and expression indexes included) as {@code pg_index} shows them, and the enum
 * types of {@code public} with their labels in order as {@code pg_enum} shows them.
 * {@code flyway_schema_history} is never part of it.
 * <p>
 * Defaults and identity are read from {@code information_schema.columns}: {@code column_default} as PostgreSQL renders
 * it, and {@code identity_generation} with {@code identity_start}, {@code identity_increment},
 * {@code identity_minimum}, {@code identity_maximum} and {@code identity_cycle}. The columns of a key are listed in
 * the order of the key (a composite primary key or unique key declared {@code (b, a)} lists {@code b} before
 * {@code a}); the columns a foreign key references are listed in the order of the foreign key's mapping, so the nth
 * referenced column is the one the nth column of the foreign key points at, whatever order the referenced unique key
 * declares them in. A foreign key whose target is a unique index that backs no constraint is refused, since
 * {@code information_schema} cannot name its target. Not covered: the position of an identity sequence
 * ({@code restart}, a {@code nextval}), which is data and no schema shape; generated columns (PostgreSQL leaves their
 * {@code column_default} empty), sequences, views, functions, triggers and extensions, enum types of other schemas,
 * collation, exclusion constraints, foreign key on-update, match and deferrability, and the content of other
 * schemas.
 */
public record SchemaSnapshot(List<String> schemas, List<String> tables, List<Column> columns, List<Constraint> constraints,
        List<Index> indexes, List<Default> defaults, List<EnumType> enums) {

    /**
     * {@code type} is {@code data_type} plus {@code (n)} for a length, {@code (p,s)} for a numeric precision and scale,
     * {@code (p)} for a time precision other than the default 6, or the {@code udt_name} of a user-defined or array type.
     */
    public record Column(String table, String name, String type, boolean nullable) {}

    /**
     * {@code detail}: the check clause, {@code references <table>(<cols>) on delete <rule>} for a foreign key, else
     * empty. The referenced {@code <cols>} of a foreign key follow its mapping: the nth one is referenced by the nth of
     * {@code columns}.
     */
    public record Constraint(String table, String name, String type, List<String> columns, String detail) {}

    /**
     * An index of {@code public} that backs no PRIMARY KEY, UNIQUE or EXCLUSION constraint of its own table;
     * {@code definition} is PostgreSQL's {@code pg_get_indexdef} text.
     */
    public record Index(String table, String name, String definition) {}

    /**
     * A column of {@code public} that has a default or is an identity column. {@code expression} is
     * {@code column_default} as PostgreSQL renders it, null for an identity column; {@code identity} is
     * {@code identity_generation} ({@code ALWAYS} or {@code BY DEFAULT}) followed by the options of the identity
     * sequence, for example {@code BY DEFAULT start 1 increment 1 min 1 max 2147483647 no cycle}, and null when the
     * column is no identity column.
     */
    public record Default(String table, String column, String expression, String identity) {}

    /** An enum type of {@code public} with its labels in the order PostgreSQL sorts them. */
    public record EnumType(String name, List<String> labels) {}

    /** A snapshot that declares no enum type. */
    public SchemaSnapshot(List<String> schemas, List<String> tables, List<Column> columns,
            List<Constraint> constraints, List<Index> indexes, List<Default> defaults) {
        this(schemas, tables, columns, constraints, indexes, defaults, List.of());
    }

    /** A snapshot that declares no column default and no identity. */
    public SchemaSnapshot(List<String> schemas, List<String> tables, List<Column> columns,
            List<Constraint> constraints, List<Index> indexes) {
        this(schemas, tables, columns, constraints, indexes, List.of());
    }

    /** A snapshot that declares no index, no column default and no identity. */
    public SchemaSnapshot(List<String> schemas, List<String> tables, List<Column> columns,
            List<Constraint> constraints) {
        this(schemas, tables, columns, constraints, List.of(), List.of());
    }

    private static final String HISTORY = "flyway_schema_history";
    private static final int DEFAULT_TIME_PRECISION = 6;
    // A NOT NULL is no CHECK here: nullability lives on the column. PostgreSQL 17 lists every NOT NULL as a CHECK named
    // <oid>_<oid>_<n>_not_null, which this pattern matches. PostgreSQL 18 keeps it in pg_constraint (type 'n') under a
    // real name, <table>_<column>_not_null, which a pattern cannot tell from a CHECK of that name: NOT_NULL_ROW asks
    // pg_constraint instead. Checked on PostgreSQL 17 (postgres:17-alpine) and PostgreSQL 18 (postgres:18) by
    // SchemaSnapshotIT.does_not_report_not_null_as_a_check_constraint; check again on the next major version.
    private static final String NOT_NULL = "^[0-9]+_[0-9]+_[0-9]+_not_null$";

    /** True for the row of {@code tc} ({@code information_schema.table_constraints}) that is a NOT NULL. */
    private static final String NOT_NULL_ROW = """
            (tc.constraint_name ~ '%s' or exists (select 1 from pg_constraint pc
                join pg_class pt on pt.oid = pc.conrelid
                join pg_namespace pn on pn.oid = pt.relnamespace
               where pc.contype = 'n' and pc.conname = tc.constraint_name
                 and pn.nspname = tc.table_schema and pt.relname = tc.table_name))""".formatted(NOT_NULL);

    public static SchemaSnapshot empty() {
        return new SchemaSnapshot(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    public static SchemaSnapshot read(Connection connection) throws SQLException {
        return new SchemaSnapshot(schemas(connection), tables(connection), columns(connection), constraints(connection),
                indexes(connection), defaults(connection), enums(connection));
    }

    private static List<String> schemas(Connection c) throws SQLException {
        return strings(c, """
                select schema_name from information_schema.schemata
                where left(schema_name, 3) <> 'pg_' and schema_name not in ('information_schema', 'public')
                order by schema_name""");
    }

    private static List<String> tables(Connection c) throws SQLException {
        return strings(c, """
                select table_name from information_schema.tables
                where table_schema = 'public' and table_type = 'BASE TABLE' and table_name <> '%s'
                order by table_name""".formatted(HISTORY));
    }

    private static List<Column> columns(Connection c) throws SQLException {
        List<Column> result = new ArrayList<>();
        try (PreparedStatement s = c.prepareStatement("""
                select c.table_name, c.column_name, c.data_type, c.character_maximum_length, c.udt_name, c.is_nullable,
                  c.numeric_precision, c.numeric_scale, c.datetime_precision
                from information_schema.columns c
                join information_schema.tables t
                  on t.table_schema = c.table_schema and t.table_name = c.table_name and t.table_type = 'BASE TABLE'
                where c.table_schema = 'public' and c.table_name <> '%s'
                order by c.table_name, c.ordinal_position""".formatted(HISTORY));
                ResultSet r = s.executeQuery()) {
            while (r.next()) {
                String type = r.getString("data_type");
                if (type.equals("USER-DEFINED") || type.equals("ARRAY")) {
                    type = r.getString("udt_name");
                } else if (r.getObject("character_maximum_length") != null) {
                    type += "(" + r.getInt("character_maximum_length") + ")";
                } else if (type.equals("numeric") && r.getObject("numeric_precision") != null) {
                    type += "(" + r.getInt("numeric_precision") + "," + r.getInt("numeric_scale") + ")";
                } else if (type.startsWith("time") && r.getInt("datetime_precision") != DEFAULT_TIME_PRECISION) {
                    // Written as PostgreSQL does: timestamp(3) with time zone
                    type = type.replaceFirst("^(timestamp|time)", "$1(" + r.getInt("datetime_precision") + ")");
                }
                result.add(new Column(r.getString("table_name"), r.getString("column_name"), type,
                        r.getString("is_nullable").equals("YES")));
            }
        }
        return result;
    }

    private static List<Constraint> constraints(Connection c) throws SQLException {
        // information_schema keys a check clause and a referential rule by schema and name only, not by table
        List<String> shared = strings(c, """
                select constraint_name from information_schema.check_constraints
                where constraint_schema = 'public' and constraint_name !~ '%s'
                  and constraint_name not in (select conname from pg_constraint where contype = 'n')
                group by constraint_name having count(*) > 1
                union
                select constraint_name from information_schema.referential_constraints
                where constraint_schema = 'public'
                group by constraint_name having count(*) > 1
                order by 1""".formatted(NOT_NULL));
        if (!shared.isEmpty()) {
            throw new IllegalStateException("constraint names " + shared + " are used more than once in schema public;"
                    + " information_schema cannot tell their definitions apart, so give each a table-specific name");
        }
        List<Constraint> result = new ArrayList<>();
        try (PreparedStatement s = c.prepareStatement("""
                select tc.table_name, tc.constraint_name, tc.constraint_type,
                  (select coalesce(string_agg(k.column_name, ',' order by k.ordinal_position), '')
                     from information_schema.key_column_usage k
                    where k.constraint_schema = tc.constraint_schema and k.constraint_name = tc.constraint_name
                      and k.table_name = tc.table_name) as cols,
                  (select cc.check_clause from information_schema.check_constraints cc
                    where cc.constraint_schema = tc.constraint_schema and cc.constraint_name = tc.constraint_name) as clause,
                  (select rc.delete_rule from information_schema.referential_constraints rc
                    where rc.constraint_schema = tc.constraint_schema and rc.constraint_name = tc.constraint_name) as delete_rule,
                  (select max(k2.table_name) from information_schema.referential_constraints rc
                     join information_schema.key_column_usage k2
                       on k2.constraint_schema = rc.unique_constraint_schema and k2.constraint_name = rc.unique_constraint_name
                    where rc.constraint_schema = tc.constraint_schema and rc.constraint_name = tc.constraint_name) as ref_table,
                  (select string_agg(k2.column_name, ',' order by k.ordinal_position)
                     from information_schema.key_column_usage k
                     join information_schema.referential_constraints rc
                       on rc.constraint_schema = k.constraint_schema and rc.constraint_name = k.constraint_name
                     join information_schema.key_column_usage k2
                       on k2.constraint_schema = rc.unique_constraint_schema and k2.constraint_name = rc.unique_constraint_name
                      and k2.ordinal_position = k.position_in_unique_constraint
                    where k.constraint_schema = tc.constraint_schema and k.constraint_name = tc.constraint_name
                      and k.table_name = tc.table_name) as ref_cols
                from information_schema.table_constraints tc
                join information_schema.tables t
                  on t.table_schema = tc.table_schema and t.table_name = tc.table_name and t.table_type = 'BASE TABLE'
                where tc.table_schema = 'public' and tc.table_name <> '%s'
                  and tc.constraint_type in ('PRIMARY KEY', 'UNIQUE', 'FOREIGN KEY', 'CHECK')
                  and not %s
                order by tc.table_name, tc.constraint_name""".formatted(HISTORY, NOT_NULL_ROW));
                ResultSet r = s.executeQuery()) {
            while (r.next()) {
                String type = r.getString("constraint_type");
                String cols = r.getString("cols");
                if (type.equals("FOREIGN KEY") && r.getString("ref_table") == null) {
                    // information_schema names the target of a foreign key by its PRIMARY KEY or UNIQUE constraint only
                    throw new IllegalStateException("foreign key " + r.getString("constraint_name") + " of table "
                            + r.getString("table_name") + " references a unique index that backs no constraint;"
                            + " information_schema cannot name its target, so declare the target columns as a"
                            + " PRIMARY KEY or UNIQUE constraint");
                }
                String detail = switch (type) {
                    case "CHECK" -> r.getString("clause");
                    case "FOREIGN KEY" -> "references %s(%s) on delete %s"
                            .formatted(r.getString("ref_table"), r.getString("ref_cols"), r.getString("delete_rule"));
                    default -> "";
                };
                result.add(new Constraint(r.getString("table_name"), r.getString("constraint_name"), type,
                        cols.isEmpty() ? List.of() : List.of(cols.split(",")), detail));
            }
        }
        return result;
    }

    private static List<Index> indexes(Connection c) throws SQLException {
        List<Index> result = new ArrayList<>();
        try (PreparedStatement s = c.prepareStatement("""
                select t.relname as table_name, i.relname as index_name, pg_get_indexdef(x.indexrelid) as definition
                from pg_index x
                join pg_class i on i.oid = x.indexrelid
                join pg_class t on t.oid = x.indrelid
                join pg_namespace n on n.oid = t.relnamespace
                where n.nspname = 'public' and t.relname <> '%s'
                  and not exists (select 1 from pg_constraint k
                                   where k.conindid = x.indexrelid and k.conrelid = x.indrelid
                                     and k.contype in ('p', 'u', 'x'))
                order by t.relname, i.relname""".formatted(HISTORY));
                ResultSet r = s.executeQuery()) {
            while (r.next()) {
                result.add(new Index(r.getString("table_name"), r.getString("index_name"), r.getString("definition")));
            }
        }
        return result;
    }

    private static List<Default> defaults(Connection c) throws SQLException {
        List<Default> result = new ArrayList<>();
        try (PreparedStatement s = c.prepareStatement("""
                select c.table_name, c.column_name, c.column_default, c.identity_generation, c.identity_start,
                       c.identity_increment, c.identity_minimum, c.identity_maximum, c.identity_cycle
                from information_schema.columns c
                join information_schema.tables t
                  on t.table_schema = c.table_schema and t.table_name = c.table_name and t.table_type = 'BASE TABLE'
                where c.table_schema = 'public' and c.table_name <> '%s'
                  and (c.column_default is not null or c.is_identity = 'YES')
                order by c.table_name, c.ordinal_position""".formatted(HISTORY));
                ResultSet r = s.executeQuery()) {
            while (r.next()) {
                result.add(new Default(r.getString("table_name"), r.getString("column_name"),
                        r.getString("column_default"), identity(r)));
            }
        }
        return result;
    }

    /** The generation and the sequence options of an identity column, null when the column is no identity column. */
    private static String identity(ResultSet r) throws SQLException {
        String generation = r.getString("identity_generation");
        if (generation == null) {
            return null;
        }
        return "%s start %s increment %s min %s max %s %s".formatted(generation, r.getString("identity_start"),
                r.getString("identity_increment"), r.getString("identity_minimum"), r.getString("identity_maximum"),
                "YES".equals(r.getString("identity_cycle")) ? "cycle" : "no cycle");
    }

    private static List<EnumType> enums(Connection c) throws SQLException {
        // left join: an enum type without a label is still a type
        Map<String, List<String>> labelsByType = new LinkedHashMap<>();
        try (PreparedStatement s = c.prepareStatement("""
                select t.typname, e.enumlabel
                from pg_type t
                join pg_namespace n on n.oid = t.typnamespace
                left join pg_enum e on e.enumtypid = t.oid
                where n.nspname = 'public' and t.typtype = 'e'
                order by t.typname, e.enumsortorder""");
                ResultSet r = s.executeQuery()) {
            while (r.next()) {
                List<String> labels = labelsByType.computeIfAbsent(r.getString("typname"), name -> new ArrayList<>());
                if (r.getString("enumlabel") != null) {
                    labels.add(r.getString("enumlabel"));
                }
            }
        }
        List<EnumType> result = new ArrayList<>();
        labelsByType.forEach((name, labels) -> result.add(new EnumType(name, List.copyOf(labels))));
        return result;
    }

    private static List<String> strings(Connection c, String sql) throws SQLException {
        List<String> result = new ArrayList<>();
        try (PreparedStatement s = c.prepareStatement(sql); ResultSet r = s.executeQuery()) {
            while (r.next()) {
                result.add(r.getString(1));
            }
        }
        return result;
    }
}
