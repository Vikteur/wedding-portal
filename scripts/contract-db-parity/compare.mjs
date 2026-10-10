// Compare a contract's CRUD surface (extract-contract.mjs) with a database schema (db-schema.sql) and report, as JSON,
// where they disagree: objects and fields with no table or column, type and length mismatches, nullability gaps,
// enum drift, and tables and columns the contract never exposes.
// Usage: node compare.mjs <contract-surface.json> <db-schema.json> [map.json]   (JSON on stdout)
//
// map.json (optional) settles what naming conventions cannot:
//   { "objects": { "<object>": "<table>" | null },          null: the object is not stored (computed, a wrapper)
//     "fields":  { "<object>.<field path>": "<column>" | "<table>.<column>" | null,
//                  "<object>.<path prefix>.*": "<table>.*" },  every field under the prefix, matched by name in <table>
//     "accept":  { "<object>.<field path>": { "<finding kind>": { "constraint": "<check name>", "reason": "..." } } },
//     "ignoreTables": ["<table>"], "ignoreColumns": ["<table>.<column>", "*.<column>"] }
// A field mapped into another table whose column is a scalar compares an array's items with it (one row per item).
// An accepted finding drops to info; its constraint must exist on the finding's table, so the reason cannot go stale.
import { readFileSync } from 'node:fs';

const [surfacePath, schemaPath, mapPath] = process.argv.slice(2);
if (!surfacePath || !schemaPath) {
  console.error('usage: node compare.mjs <contract-surface.json> <db-schema.json> [map.json]');
  process.exit(2);
}
const surface = JSON.parse(readFileSync(surfacePath, 'utf8'));
const db = JSON.parse(readFileSync(schemaPath, 'utf8'));
const map = mapPath ? JSON.parse(readFileSync(mapPath, 'utf8')) : {};
const objectMap = map.objects ?? {};
const fieldMap = map.fields ?? {};
const ignoreTables = new Set(map.ignoreTables ?? []);
const ignoreColumns = map.ignoreColumns ?? [];
const accepted = map.accept ?? {};

// Read models and wrappers share their object's table: weddingSummary, weddingCreated -> wedding.
const OBJECT_SUFFIXES = /(Summary|Detail|Details|View|Response|Result|Created|Issued|Preview|Dto|Resource|Model)$/;
const TEXT = new Set(['text', 'character varying', 'character', 'citext', 'varchar', 'bpchar']);
const INT32 = new Set(['integer', 'smallint']);
const INT64 = new Set(['bigint']);
const NUMBER = new Set(['numeric', 'real', 'double precision']);
const JSONISH = new Set(['json', 'jsonb']);

const findings = [];
function finding(severity, kind, detail, where) {
  const accept = where.object && where.field ? accepted[`${where.object}.${where.field}`]?.[kind] : undefined;
  if (accept && severity !== 'info') {
    const checks = tables[where.table]?.checks ?? [];
    if (accept.constraint && !checks.some((c) => c.name === accept.constraint)) {
      findings.push({ severity: 'error', kind: 'accept-constraint-missing', ...where,
        detail: `map.json accepts this ${kind} because of ${accept.constraint}, which ${where.table} does not have` });
    } else {
      findings.push({ severity: 'info', kind, ...where, detail, accepted: accept });
      return;
    }
  }
  findings.push({ severity, kind, ...where, detail });
}

const snake = (s) => s.replace(/([a-z0-9])([A-Z])/g, '$1_$2').replace(/[-\s]/g, '_').toLowerCase();
const squash = (s) => s.toLowerCase().replace(/[^a-z0-9]/g, '');
function singular(s) {
  if (s.endsWith('people')) return `${s.slice(0, -6)}person`;
  if (s.endsWith('ies')) return `${s.slice(0, -3)}y`;
  if (s.endsWith('sses') || s.endsWith('xes')) return s.slice(0, -2);
  return s.endsWith('s') && !s.endsWith('ss') ? s.slice(0, -1) : s;
}

const tables = db.tables ?? {};
const tableIndex = new Map();
for (const name of Object.keys(tables)) {
  const bare = name.includes('.') ? name.split('.').pop() : name;
  for (const key of [squash(bare), squash(singular(bare))]) if (!tableIndex.has(key)) tableIndex.set(key, name);
}

function tableFor(object) {
  if (Object.hasOwn(objectMap, object)) return { table: objectMap[object], how: 'map' };
  const base = object.replace(/^path:/, '');
  const candidates = [base, base.replace(OBJECT_SUFFIXES, '')];
  const lookup = (c) => tableIndex.get(squash(c)) ?? tableIndex.get(squash(singular(snake(c))));
  for (const [i, c] of candidates.entries()) {
    const table = lookup(c);
    if (table) return { table, how: i === 0 ? 'name' : 'name without suffix' };
  }
  // A qualified view of an entity (libraryTrack, portalTask) falls back to its head noun, longest tail first.
  for (const c of candidates) {
    const words = c.split(/(?=[A-Z])/);
    for (let k = 1; k < words.length; k++) {
      const table = lookup(words.slice(k).join(''));
      if (table) return { table, how: 'head noun' };
    }
  }
  return { table: null, how: null };
}

function ignoredColumn(table, column) {
  return ignoreColumns.includes(`${table}.${column}`) || ignoreColumns.includes(`*.${column}`);
}

// The map entry for a field: its own key, else the nearest `<object>.<prefix>.*` above it.
function fieldMapping(object, path) {
  const key = `${object}.${path}`;
  if (Object.hasOwn(fieldMap, key)) return { target: fieldMap[key], rest: null };
  const parts = path.split('.');
  for (let k = parts.length - 1; k > 0; k--) {
    const wild = `${object}.${parts.slice(0, k).join('.')}.*`;
    if (Object.hasOwn(fieldMap, wild)) return { target: fieldMap[wild], rest: parts.slice(k).join('.') };
  }
  return null;
}
const mapped = (object, path) => fieldMapping(object, path) !== null;

function byName(path, field, columns) {
  const plain = path.replace(/\[\]/g, '');
  const candidates = [snake(plain.replace(/\./g, '_')), snake(plain.split('.').pop())];
  // A nested object reference ({vendorId} inside `venue`) may be stored as its parent name plus `_id`.
  if (field.ref && field.container) candidates.push(`${snake(plain)}_id`);
  return candidates.find((c) => columns[c]) ?? null;
}

// Where a field is stored: { table, column } — table is the object's own unless the map names another.
function columnFor(object, path, field, table) {
  const m = fieldMapping(object, path);
  if (!m) return { table, column: byName(path, field, tables[table].columns), how: 'name' };
  if (m.target === null) return { table, column: null, how: 'map' };
  const [other, column] = m.target.includes('.') ? m.target.split('.') : [table, m.target];
  if (column === '*') return { table: other, column: tables[other] ? byName(m.rest, field, tables[other].columns) : null, how: 'map' };
  return { table: other, column, how: 'map' };
}

// A string whose pattern accepts a clock or calendar value is that value without a format (`clockTime`: ^..:..$).
function patternFormat(field) {
  if (!field.pattern) return null;
  try {
    const re = new RegExp(field.pattern);
    if (re.test('09:30') && !re.test('x')) return 'time';
    if (re.test('2027-06-12') && !re.test('x')) return 'date';
  } catch { /* not an ECMAScript pattern */ }
  return null;
}

function typeVerdict(field, column, enums) {
  const t = column.type;
  const isEnum = Object.hasOwn(enums, t);
  const fmt = field.format ?? patternFormat(field);
  const ok = { ok: true };
  const bad = (why) => ({ ok: false, severity: 'error', why });
  const meh = (why) => ({ ok: false, severity: 'warning', why });
  if (field.type === 'string') {
    if (fmt === 'uuid') return t === 'uuid' ? ok : TEXT.has(t) ? meh('uuid stored as text') : bad(`uuid stored as ${t}`);
    if (fmt === 'date') return t === 'date' ? ok : bad(`date stored as ${t}`);
    if (fmt === 'date-time') {
      if (t === 'timestamp with time zone') return ok;
      return t === 'timestamp without time zone' ? meh('date-time stored without a time zone') : bad(`date-time stored as ${t}`);
    }
    if (fmt === 'time') return t.startsWith('time ') ? ok : bad(`time stored as ${t}`);
    if (fmt === 'byte' || fmt === 'binary') return t === 'bytea' || TEXT.has(t) ? ok : bad(`${fmt} stored as ${t}`);
    if (TEXT.has(t) || isEnum) return ok;
    if (['uuid', 'date', 'time without time zone', 'timestamp with time zone'].includes(t)) {
      return meh(`column is ${t} but the contract declares a plain string (no format)`);
    }
    return bad(`string stored as ${t}`);
  }
  if (field.type === 'integer') {
    if (fmt === 'int64') return INT64.has(t) ? ok : INT32.has(t) ? bad(`int64 stored as ${t} (overflow)`) : bad(`int64 stored as ${t}`);
    if (INT32.has(t)) return ok;
    if (INT64.has(t)) return meh(`int32 stored as ${t}`);
    return NUMBER.has(t) ? meh(`integer stored as ${t}`) : bad(`integer stored as ${t}`);
  }
  if (field.type === 'number') return NUMBER.has(t) ? ok : INT32.has(t) || INT64.has(t) ? meh(`number stored as ${t}`) : bad(`number stored as ${t}`);
  if (field.type === 'boolean') return t === 'boolean' ? ok : bad(`boolean stored as ${t}`);
  if (field.type === 'array') return JSONISH.has(t) || t.startsWith('_') ? ok : bad(`array stored as ${t}`);
  if (field.type === 'object') return JSONISH.has(t) ? ok : bad(`object stored as ${t}`);
  return ok;
}

const enums = db.enums ?? {};
const byTable = new Map();
const unstored = [];
for (const [object, entry] of Object.entries(surface.objects ?? {})) {
  const writable = entry.operations.create.length + entry.operations.update.length + entry.operations.delete.length > 0;
  const { table, how } = tableFor(object);
  if (!table) {
    if (Object.hasOwn(objectMap, object)) continue;
    unstored.push(object);
    finding(writable ? 'warning' : 'info', writable ? 'object-without-table' : 'read-model-without-table',
      writable ? 'the contract creates, updates or deletes this object, but no table matches it'
        : 'read-only object with no matching table (computed or joined)', { object });
    continue;
  }
  if (!tables[table]) {
    finding('error', 'mapped-table-missing', `map.json points ${object} at ${table}, which the schema does not have`, { object, table });
    continue;
  }
  if (!byTable.has(table)) byTable.set(table, []);
  byTable.get(table).push({ object, entry, how });
}

const tableReport = {};
const used = new Map(Object.keys(tables).map((t) => [t, new Set()]));
for (const [table, objects] of [...byTable.entries()].sort(([a], [b]) => a.localeCompare(b))) {
  const report = (tableReport[table] = { objects: objects.map((o) => ({ object: o.object, matchedBy: o.how })), fields: {} });

  for (const { object, entry } of objects) {
    const collections = new Set();
    for (const [path, field] of Object.entries(entry.fields)) {
      // Items of a nested array belong to a child table (or a json column), never to this table's own columns.
      const inArray = path.match(/^(.*?)\[\]\./);
      if (inArray) {
        if (!collections.has(inArray[1]) && !mapped(object, path)) {
          collections.add(inArray[1]);
          const name = snake(inArray[1].split('.').pop());
          const child = [`${singular(table)}_${name}`, name].find((t) => tables[t]);
          finding('info', 'nested-collection', child ? `items of ${inArray[1]}[] are likely stored in ${child}`
            : `items of ${inArray[1]}[] need a child table or a json column; none matches by name`, { object, field: `${inArray[1]}[]`, table });
        }
        if (!mapped(object, path)) continue;
      }
      if (field.container && !field.ref) continue;
      const writable = field.create || field.update;
      const { table: home, column, how } = columnFor(object, path, field, table);
      const where = { object, field: path, table: home };
      if (!column) {
        if (fieldMapping(object, path)?.target === null) continue;
        if (field.container) continue;
        report.fields[`${object}.${path}`] = { column: null };
        finding(writable ? 'warning' : 'info', writable ? 'field-not-persisted' : 'derived-field',
          writable ? 'the contract accepts this field, but no column matches it' : 'read-only field with no matching column', where);
        continue;
      }
      if (!tables[home]?.columns[column]) {
        finding('error', 'mapped-column-missing', `map.json points this field at ${home}.${column}, which the schema does not have`, where);
        continue;
      }
      used.get(home).add(column);
      const col = tables[home].columns[column];
      // An array kept one row per item in another table compares its items with the column.
      const perRow = home !== table && field.type === 'array' && field.items && !col.type.startsWith('_') && !JSONISH.has(col.type);
      const subject = perRow ? { ...field.items, nullable: false } : field;
      const issues = [];
      Object.assign(where, { column });
      report.fields[`${object}.${path}`] = { column: home === table ? column : `${home}.${column}`, matchedBy: how, issues };
      const verdict = typeVerdict(subject, col, enums);
      if (!verdict.ok) {
        issues.push('type');
        finding(verdict.severity, 'type-mismatch', verdict.why, where);
      }
      if (subject.maxLength && col.maxLength && subject.maxLength > col.maxLength) {
        issues.push('maxLength');
        finding('error', 'length-mismatch', `contract allows ${subject.maxLength} characters, the column holds ${col.maxLength}`, where);
      } else if (writable && !subject.maxLength && !subject.enum && col.maxLength && subject.type === 'string' && !subject.format) {
        issues.push('unbounded');
        finding('warning', 'unbounded-into-bounded', `contract sets no maxLength, the column holds ${col.maxLength}`, where);
      }
      const hasDefault = col.default !== null || col.identity || col.generated;
      if (writable && subject.nullable && !col.nullable) {
        issues.push('nullability');
        finding('error', 'null-not-storable', 'contract accepts null, the column is NOT NULL', where);
      } else if (!perRow && field.create && !field.requiredOnCreate && !col.nullable && !hasDefault) {
        issues.push('optional-on-create');
        finding('warning', 'optional-but-not-null', 'optional on create, but the column is NOT NULL with no default: the app must fill it', where);
      }
      if (!perRow && field.read && field.alwaysPresent && !field.nullable && col.nullable) {
        issues.push('db-allows-null');
        finding('warning', 'db-allows-null', 'contract promises a value, the column allows NULL', where);
      }
      const allowed = enums[col.type] ?? col.allowedValues;
      if (subject.enum && allowed) {
        const api = new Set(subject.enum.filter((v) => v !== null));
        const dbValues = new Set(allowed);
        const onlyApi = [...api].filter((v) => !dbValues.has(v));
        const onlyDb = [...dbValues].filter((v) => !api.has(v));
        // A contract value the database cannot store is always an error. A stored value the contract does not list is
        // one too, unless the contract's enum is extensible: clients must then accept values it does not list yet.
        if (onlyApi.length || (onlyDb.length && !subject.extensibleEnum)) {
          issues.push('enum');
          finding('error', 'enum-mismatch', `only in contract: [${onlyApi.join(', ')}]; only in database: [${onlyDb.join(', ')}]`, where);
        } else if (onlyDb.length) {
          issues.push('enum');
          finding('warning', 'enum-not-listed', `the contract's extensible enum does not list yet: [${onlyDb.join(', ')}]`, where);
        }
      }
    }
  }
}
// A table only a cross-table field mapping reaches still reports the columns no field covers.
const reached = new Set([...byTable.keys(), ...[...used].filter(([, cols]) => cols.size).map(([t]) => t)]);
for (const table of [...reached].sort()) {
  const objects = byTable.get(table) ?? [];
  const creatable = objects.some((o) => o.entry.operations.create.length);
  for (const [column, col] of Object.entries(tables[table].columns)) {
    if (used.get(table).has(column) || ignoredColumn(table, column)) continue;
    const mustSet = creatable && !col.nullable && col.default === null && !col.identity && !col.generated;
    finding('info', 'column-not-exposed', mustSet ? 'NOT NULL with no default and not in the contract: the app sets it on create'
      : 'not in the contract', { table, column });
  }
}
for (const table of Object.keys(tables)) {
  if (!reached.has(table) && !ignoreTables.has(table)) finding('info', 'table-not-exposed', 'no contract object maps to this table', { table });
}

const order = { error: 0, warning: 1, info: 2 };
findings.sort((a, b) => order[a.severity] - order[b.severity] || (a.table ?? '').localeCompare(b.table ?? '')
  || (a.object ?? '').localeCompare(b.object ?? '') || (a.field ?? a.column ?? '').localeCompare(b.field ?? b.column ?? ''));

const count = (s) => findings.filter((f) => f.severity === s).length;
process.stdout.write(`${JSON.stringify({
  sources: { contract: surface.source, database: db.source ?? null, map: mapPath?.replace(/\\/g, '/') ?? null },
  summary: {
    errors: count('error'), warnings: count('warning'), info: count('info'), accepted: findings.filter((f) => f.accepted).length,
    objects: Object.keys(surface.objects ?? {}).length, tables: Object.keys(tables).length,
    matchedTables: reached.size, objectsWithoutTable: unstored.length,
  },
  tables: tableReport,
  findings,
}, null, 2)}\n`);
