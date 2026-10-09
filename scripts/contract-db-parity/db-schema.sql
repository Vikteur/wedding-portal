-- Print the database schema as one JSON document: per table its columns, primary key, foreign keys, unique and check
-- constraints, plus every enum type. Run with psql -At against a migrated database. The migration tool's own history
-- table is left out. A column whose values a one-column CHECK (col IN (...)) limits gets them as allowedValues, so a
-- text-plus-CHECK enum compares like a PostgreSQL ENUM type.
WITH user_tables AS (
  SELECT c.oid, n.nspname AS schema_name, c.relname AS table_name,
         CASE WHEN n.nspname = 'public' THEN c.relname ELSE n.nspname || '.' || c.relname END AS table_key
  FROM pg_class c
  JOIN pg_namespace n ON n.oid = c.relnamespace
  WHERE c.relkind IN ('r', 'p')
    AND n.nspname NOT IN ('pg_catalog', 'information_schema')
    AND n.nspname NOT LIKE 'pg_toast%'
    AND c.relname NOT IN ('flyway_schema_history')
),
check_values AS (
  -- pg_get_constraintdef renders col IN ('A', 'B') as ((col = ANY (ARRAY['A'::text, 'B'::text]))).
  SELECT con.conrelid, a.attname,
         (SELECT json_agg(replace(m[1], '''''', '''') ORDER BY ord)
            FROM regexp_matches(pg_get_constraintdef(con.oid), '''((?:[^'']|'''')*)''::', 'g') WITH ORDINALITY AS r(m, ord)
         ) AS allowed
  FROM pg_constraint con
  JOIN user_tables t ON t.oid = con.conrelid
  JOIN pg_attribute a ON a.attrelid = con.conrelid AND a.attnum = con.conkey[1]
  WHERE con.contype = 'c' AND cardinality(con.conkey) = 1
    AND pg_get_constraintdef(con.oid) ~ '= ANY \(\(?ARRAY\['
),
cols AS (
  SELECT t.table_key,
         json_object_agg(col.column_name, json_build_object(
           'type', CASE WHEN col.data_type IN ('USER-DEFINED', 'ARRAY') THEN col.udt_name ELSE col.data_type END,
           'nullable', col.is_nullable = 'YES',
           'default', col.column_default,
           'maxLength', col.character_maximum_length,
           'numericPrecision', col.numeric_precision,
           'numericScale', col.numeric_scale,
           'identity', col.is_identity = 'YES',
           'generated', col.is_generated = 'ALWAYS',
           'position', col.ordinal_position,
           'allowedValues', cv.allowed
         ) ORDER BY col.ordinal_position) AS columns
  FROM user_tables t
  JOIN information_schema.columns col ON col.table_schema = t.schema_name AND col.table_name = t.table_name
  LEFT JOIN check_values cv ON cv.conrelid = t.oid AND cv.attname = col.column_name
  GROUP BY t.table_key
),
constraint_cols AS (
  SELECT con.oid, con.conrelid, con.contype, con.conname, con.confrelid,
         array_agg(a.attname ORDER BY k.ord) AS columns,
         array_agg(fa.attname ORDER BY k.ord) FILTER (WHERE fa.attname IS NOT NULL) AS ref_columns
  FROM pg_constraint con
  JOIN user_tables t ON t.oid = con.conrelid
  CROSS JOIN LATERAL unnest(con.conkey) WITH ORDINALITY AS k(attnum, ord)
  JOIN pg_attribute a ON a.attrelid = con.conrelid AND a.attnum = k.attnum
  LEFT JOIN pg_attribute fa ON fa.attrelid = con.confrelid AND fa.attnum = con.confkey[k.ord]
  WHERE con.contype IN ('p', 'f', 'u')
  GROUP BY con.oid, con.conrelid, con.contype, con.conname, con.confrelid
),
keys AS (
  SELECT t.table_key,
         (SELECT to_json(cc.columns) FROM constraint_cols cc WHERE cc.conrelid = t.oid AND cc.contype = 'p') AS primary_key,
         (SELECT json_agg(json_build_object('name', cc.conname, 'columns', cc.columns,
                   'references', json_build_object('table', rt.table_key, 'columns', cc.ref_columns)) ORDER BY cc.conname)
            FROM constraint_cols cc LEFT JOIN user_tables rt ON rt.oid = cc.confrelid
           WHERE cc.conrelid = t.oid AND cc.contype = 'f') AS foreign_keys,
         (SELECT json_agg(json_build_object('name', cc.conname, 'columns', cc.columns) ORDER BY cc.conname)
            FROM constraint_cols cc WHERE cc.conrelid = t.oid AND cc.contype = 'u') AS unique_constraints,
         (SELECT json_agg(json_build_object('name', i.relname, 'definition', pg_get_indexdef(ix.indexrelid)) ORDER BY i.relname)
            FROM pg_index ix JOIN pg_class i ON i.oid = ix.indexrelid
           WHERE ix.indrelid = t.oid AND ix.indisunique AND NOT ix.indisprimary
             AND NOT EXISTS (SELECT 1 FROM pg_constraint c WHERE c.conindid = ix.indexrelid)) AS unique_indexes,
         (SELECT json_agg(json_build_object('name', con.conname, 'definition', pg_get_constraintdef(con.oid)) ORDER BY con.conname)
            FROM pg_constraint con WHERE con.conrelid = t.oid AND con.contype = 'c') AS checks
  FROM user_tables t
),
enums AS (
  SELECT json_object_agg(ty.typname, labels ORDER BY ty.typname) AS enums
  FROM (
    SELECT e.enumtypid, json_agg(e.enumlabel ORDER BY e.enumsortorder) AS labels
    FROM pg_enum e GROUP BY e.enumtypid
  ) l
  JOIN pg_type ty ON ty.oid = l.enumtypid
)
SELECT json_build_object(
  'tables', COALESCE((
    SELECT json_object_agg(k.table_key, json_build_object(
             'columns', c.columns,
             'primaryKey', COALESCE(k.primary_key, '[]'::json),
             'foreignKeys', COALESCE(k.foreign_keys, '[]'::json),
             'uniqueConstraints', COALESCE(k.unique_constraints, '[]'::json),
             'uniqueIndexes', COALESCE(k.unique_indexes, '[]'::json),
             'checks', COALESCE(k.checks, '[]'::json)
           ) ORDER BY k.table_key)
    FROM keys k JOIN cols c ON c.table_key = k.table_key
  ), '{}'::json),
  'enums', COALESCE((SELECT enums FROM enums), '{}'::json)
);
