package app.rekord.application.persistence.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * The shape of schema {@code public}, read from {@code information_schema} only and sorted deterministically, so a
 * migration test can compare it with the schema the migration declares. {@code flyway_schema_history} is never part of it.
 */
public record SchemaSnapshot(List<String> schemas, List<String> tables, List<Column> columns, List<Constraint> constraints) {

    /**
     * {@code type} is {@code data_type} plus {@code (n)} for a length, {@code (p,s)} for a numeric precision and scale,
     * {@code (p)} for a time precision other than the default 6, or the {@code udt_name} of a user-defined or array type.
     */
    public record Column(String table, String name, String type, boolean nullable) {}

    /** {@code detail}: the check clause, {@code references <table>(<cols>) on delete <rule>} for a foreign key, else empty. */
    public record Constraint(String table, String name, String type, List<String> columns, String detail) {}

    private static final String HISTORY = "flyway_schema_history";
    private static final int DEFAULT_TIME_PRECISION = 6;
    // PostgreSQL 17 lists every NOT NULL as a CHECK named <oid>_<oid>_<n>_not_null; nullability lives on the column.
    private static final String NOT_NULL = "^[0-9]+_[0-9]+_[0-9]+_not_null$";

    public static SchemaSnapshot empty() {
        return new SchemaSnapshot(List.of(), List.of(), List.of(), List.of());
    }

    public static SchemaSnapshot read(Connection connection) throws SQLException {
        return new SchemaSnapshot(schemas(connection), tables(connection), columns(connection), constraints(connection));
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
                  (select string_agg(k2.column_name, ',' order by k2.ordinal_position)
                     from information_schema.referential_constraints rc
                     join information_schema.key_column_usage k2
                       on k2.constraint_schema = rc.unique_constraint_schema and k2.constraint_name = rc.unique_constraint_name
                    where rc.constraint_schema = tc.constraint_schema and rc.constraint_name = tc.constraint_name) as ref_cols
                from information_schema.table_constraints tc
                join information_schema.tables t
                  on t.table_schema = tc.table_schema and t.table_name = tc.table_name and t.table_type = 'BASE TABLE'
                where tc.table_schema = 'public' and tc.table_name <> '%s'
                  and tc.constraint_type in ('PRIMARY KEY', 'UNIQUE', 'FOREIGN KEY', 'CHECK')
                  and tc.constraint_name !~ '%s'
                order by tc.table_name, tc.constraint_name""".formatted(HISTORY, NOT_NULL));
                ResultSet r = s.executeQuery()) {
            while (r.next()) {
                String type = r.getString("constraint_type");
                String cols = r.getString("cols");
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
