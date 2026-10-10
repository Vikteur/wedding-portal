// Extract the CRUD surface of an OpenAPI 3.x contract as JSON: every operation with its parameters and bodies,
// and per object (the entity schema an operation returns) which fields can be read, created and updated, and
// whether the object can be deleted.
// Usage: node extract-contract.mjs <openapi.yaml|json>   (JSON on stdout)
import { readFileSync } from 'node:fs';
import { parse } from 'yaml';

const METHODS = ['get', 'post', 'put', 'patch', 'delete'];
// A request schema named after an object plus one of these suffixes belongs to that object (guestPatch -> guest).
const REQUEST_SUFFIXES = /(Create|Update|Patch|Put|Upsert|Request|Input|Body|Command|Form|New|Draft)$/;
// A response is a page or list envelope when its one array of objects sits under one of these names, or the schema is
// named like a collection. A plain object that happens to hold one list (a vendor with its contacts) is not.
const ENVELOPE_PROPS = new Set(['items', 'data', 'content', 'results', 'records', 'entries', 'rows', 'elements']);
const ENVELOPE_NAMES = /(Page|List|Collection|Results)$/;

const specPath = process.argv[2];
if (!specPath) {
  console.error('usage: node extract-contract.mjs <openapi.yaml|json>');
  process.exit(2);
}
const spec = parse(readFileSync(specPath, 'utf8'));
if (!spec?.openapi?.startsWith('3.')) {
  console.error(`[contract-db-parity] not an OpenAPI 3.x document: ${specPath}`);
  process.exit(1);
}

function refName(ref) {
  return ref.split('/').pop();
}

function lookup(ref) {
  if (!ref.startsWith('#/')) {
    throw new Error(`external $ref not supported, bundle the spec first: ${ref}`);
  }
  const target = ref.slice(2).split('/').reduce((node, key) => node?.[key.replace(/~1/g, '/').replace(/~0/g, '~')], spec);
  if (target === undefined) throw new Error(`unresolved $ref: ${ref}`);
  return target;
}

// Follow $ref chains; returns the target and the name of the first schema ref on the way.
function deref(node) {
  let name = null;
  while (node?.$ref) {
    name ??= refName(node.$ref);
    node = lookup(node.$ref);
  }
  return { node: node ?? {}, name };
}

// OpenAPI 3.0 says `nullable: true`, 3.1 puts "null" in a type array.
function typeOf(schema) {
  if (Array.isArray(schema.type)) {
    const types = schema.type.filter((t) => t !== 'null');
    return { type: types[0] ?? 'null', nullable: types.length < schema.type.length };
  }
  if (schema.type) return { type: schema.type, nullable: schema.nullable === true };
  if (schema.properties || schema.additionalProperties) return { type: 'object', nullable: schema.nullable === true };
  if (schema.items) return { type: 'array', nullable: schema.nullable === true };
  return { type: schema.enum || schema['x-extensible-enum'] ? 'string' : 'any', nullable: schema.nullable === true };
}

// Value keywords an allOf part carries: `nullable: true, allOf: [$ref: someEnum]` keeps the enum, format and bounds.
const VALUE_KEYWORDS = ['format', 'enum', 'x-extensible-enum', 'maxLength', 'minLength', 'pattern', 'minimum', 'maximum', 'items'];

// Merge allOf parts into one object schema; oneOf/anyOf variants are merged too, but none of their fields is required.
function merged(schema, stack) {
  const { node } = deref(schema);
  const parts = node.allOf?.map((p) => merged(p, stack));
  const variants = [...(node.oneOf ?? []), ...(node.anyOf ?? [])].map((p) => merged(p, stack));
  if (!parts && !variants.length) return node;
  const out = { ...node, properties: { ...(node.properties ?? {}) }, required: [...(node.required ?? [])] };
  delete out.allOf;
  delete out.oneOf;
  delete out.anyOf;
  for (const part of parts ?? []) {
    Object.assign(out.properties, part.properties ?? {});
    out.required.push(...(part.required ?? []));
    out.type ??= part.type;
    for (const key of VALUE_KEYWORDS) if (part[key] !== undefined) out[key] ??= part[key];
    if (part.nullable) out.nullable = true;
  }
  for (const variant of variants) {
    for (const [key, prop] of Object.entries(variant.properties ?? {})) out.properties[key] ??= { ...prop, 'x-variant': true };
    out.type ??= variant.type;
    if (!variant.properties && variant.type) out.variantTypes = [...(out.variantTypes ?? []), variant.type];
  }
  return out;
}

function leaf(schema, required) {
  const { type, nullable } = typeOf(schema);
  const field = { type, nullable, required };
  for (const key of ['format', 'enum', 'maxLength', 'minLength', 'pattern', 'minimum', 'maximum', 'readOnly', 'writeOnly', 'default']) {
    if (schema[key] !== undefined) field[key] = schema[key];
  }
  // An extensible enum lists today's values; a client must accept others (a value added later is not breaking).
  if (!field.enum && Array.isArray(schema['x-extensible-enum'])) Object.assign(field, { enum: schema['x-extensible-enum'], extensibleEnum: true });
  if (schema['x-variant']) field.variant = true;
  return field;
}

// Flatten a schema into dotted field paths: `venue.name`, `roles[].role`. Objects and arrays of objects are kept as
// `container` entries so a nested shape is visible; arrays of scalars stay one field with their item type.
function flatten(schema, prefix = '', required = false, fields = {}, stack = []) {
  const { node, name } = deref(schema);
  if (name && stack.includes(name)) {
    fields[prefix] = { type: 'object', ref: name, recursive: true, required, nullable: false };
    return fields;
  }
  const nextStack = name ? [...stack, name] : stack;
  const resolved = merged(node, nextStack);
  const { type } = typeOf(resolved);

  if (type === 'object' && resolved.properties) {
    if (prefix) fields[prefix] = { ...leaf(resolved, required), type: 'object', container: true, ...(name && { ref: name }) };
    const req = new Set(resolved.required ?? []);
    for (const [key, prop] of Object.entries(resolved.properties)) {
      flatten(prop, prefix ? `${prefix}.${key}` : key, req.has(key), fields, nextStack);
    }
    return fields;
  }
  if (type === 'array') {
    const item = merged(deref(resolved.items ?? {}).node, nextStack);
    if (typeOf(item).type === 'object' && item.properties) {
      fields[prefix] = { ...leaf(resolved, required), container: true, items: 'object' };
      return flatten(resolved.items, `${prefix}[]`, true, fields, stack.concat(name ? [name] : []));
    }
    fields[prefix] = { ...leaf(resolved, required), items: leaf(item, true) };
    return fields;
  }
  if (prefix) fields[prefix] = { ...leaf(resolved, required), ...(name && { ref: name }) };
  return fields;
}

function pickContent(content) {
  if (!content) return null;
  const types = Object.keys(content);
  const json = types.find((t) => t === 'application/json' || t.endsWith('+json')) ?? types[0];
  return json ? { mediaType: json, schema: content[json].schema } : null;
}

// The object an operation is about: a page envelope ({items: [X], total}) or a bare array unwraps to X.
function entityOf(schema) {
  if (!schema) return null;
  const { node, name } = deref(schema);
  const resolved = merged(node, []);
  if (typeOf(resolved).type === 'array') return deref(resolved.items ?? {}).name;
  const props = Object.entries(resolved.properties ?? {});
  const arrays = props.filter(([, p]) => typeOf(merged(p, [])).type === 'array');
  if (arrays.length === 1) {
    const item = deref(merged(arrays[0][1], []).items ?? {});
    const othersScalar = props.every(([, p]) => p === arrays[0][1] || !['object', 'array'].includes(typeOf(merged(p, [])).type));
    const envelope = ENVELOPE_PROPS.has(arrays[0][0]) || ENVELOPE_NAMES.test(name ?? '');
    if (envelope && item.name && item.node.properties && othersScalar) return item.name;
  }
  return name;
}

function parameters(pathItem, op) {
  const byKey = new Map();
  for (const raw of [...(pathItem.parameters ?? []), ...(op.parameters ?? [])]) {
    const { node: p } = deref(raw);
    byKey.set(`${p.in}:${p.name}`, p);
  }
  return [...byKey.values()].map((p) => {
    const { node: schema, name } = deref(p.schema ?? {});
    return { name: p.name, in: p.in, ...leaf(merged(schema, []), p.required === true), ...(name && { ref: name }) };
  });
}

function crudOf(method, path, responses) {
  if (method === 'get') return { crud: 'read', reason: 'GET' };
  if (method === 'delete') return { crud: 'delete', reason: 'DELETE' };
  if (method === 'put' || method === 'patch') return { crud: 'update', reason: method.toUpperCase() };
  if (Object.keys(responses).includes('201')) return { crud: 'create', reason: 'POST answering 201' };
  const itemPath = new RegExp(`^${path.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}/\\{[^/]+\\}$`);
  if (Object.keys(spec.paths).some((p) => itemPath.test(p))) return { crud: 'create', reason: 'POST on a collection' };
  return { crud: 'action', reason: 'POST on a non-collection path' };
}

const operations = [];
for (const [path, pathItem] of Object.entries(spec.paths ?? {})) {
  for (const method of METHODS) {
    const op = pathItem[method];
    if (!op) continue;
    const responses = {};
    for (const [status, raw] of Object.entries(op.responses ?? {})) {
      if (!/^2/.test(status)) continue;
      const content = pickContent(deref(raw).node.content);
      responses[status] = content
        ? { mediaType: content.mediaType, schema: deref(content.schema ?? {}).name, entity: entityOf(content.schema) }
        : { mediaType: null, schema: null, entity: null };
    }
    const body = op.requestBody ? deref(op.requestBody).node : null;
    const bodyContent = pickContent(body?.content);
    const { crud, reason } = crudOf(method, path, responses);
    operations.push({
      operationId: op.operationId ?? `${method} ${path}`,
      method: method.toUpperCase(),
      path,
      tags: op.tags ?? [],
      crud,
      crudReason: reason,
      parameters: parameters(pathItem, op),
      requestBody: bodyContent
        ? { required: body.required === true, mediaType: bodyContent.mediaType, schema: deref(bodyContent.schema ?? {}).name,
            fields: flatten(bodyContent.schema ?? {}) }
        : null,
      responses,
      // Raw schema refs, kept out of the output, used below to build the objects.
      _bodySchema: bodyContent?.schema,
    });
  }
}

// Assign each operation to an object: the entity its 2xx response returns; else the request schema's base name;
// a DELETE without a body takes the object of another operation on the same path.
const entities = new Set(operations.flatMap((o) => Object.values(o.responses).map((r) => r.entity)).filter(Boolean));
for (const op of operations) {
  const fromResponse = Object.entries(op.responses).sort(([a], [b]) => a.localeCompare(b)).map(([, r]) => r.entity).find(Boolean);
  const requestName = op.requestBody?.schema;
  const fromRequest = requestName && entities.has(requestName.replace(REQUEST_SUFFIXES, ''))
    ? requestName.replace(REQUEST_SUFFIXES, '') : requestName;
  op.object = fromResponse ?? fromRequest ?? null;
  op.objectSource = fromResponse ? 'response' : fromRequest ? 'request body' : null;
}
// timeline-items -> timelineItem, people -> person: the path noun as an object name.
function singularCamel(noun) {
  const camel = noun.replace(/-([a-z])/g, (_, c) => c.toUpperCase());
  if (/people$/i.test(camel)) return camel.replace(/eople$/, 'erson');
  if (/ies$/.test(camel)) return camel.replace(/ies$/, 'y');
  if (/sses$/.test(camel)) return camel.replace(/es$/, '');
  return camel.replace(/s$/, '');
}
for (const op of operations.filter((o) => !o.object)) {
  const sibling = operations.find((o) => o.path === op.path && o.object && o.objectSource === 'response');
  const noun = op.path.split('/').filter((s) => s && !s.startsWith('{')).pop() ?? op.path;
  const named = [...entities].find((e) => e.toLowerCase() === singularCamel(noun).toLowerCase());
  op.object = sibling?.object ?? named ?? `path:${noun}`;
  op.objectSource = sibling ? `same path as ${sibling.operationId}` : named ? 'path segment matches the object' : 'path segment';
}

const objects = {};
function objectFor(name) {
  return (objects[name] ??= { schema: name.startsWith('path:') ? null : name, deletable: false,
    operations: { read: [], create: [], update: [], delete: [], action: [] }, fields: {} });
}
function markFields(obj, fields, flag, prefix = '') {
  for (const [rawPath, field] of Object.entries(fields)) {
    const path = prefix ? `${prefix}.${rawPath}` : rawPath;
    if (flag === 'read' && field.writeOnly) continue;
    if (flag !== 'read' && field.readOnly) continue;
    const entry = (obj.fields[path] ??= { read: false, create: false, update: false });
    const { required, ...shape } = field;
    if (!entry.type || flag === 'read') Object.assign(entry, shape);
    else if (entry.type !== field.type) entry.typeConflict = `${flag} body says ${field.type}`;
    entry[flag] = true;
    if (flag === 'create') entry.requiredOnCreate = required;
    if (flag === 'update') entry.requiredOnUpdate = required;
    if (flag === 'read') entry.alwaysPresent = required;
  }
}
for (const op of operations) objectFor(op.object);
for (const [name, obj] of Object.entries(objects)) {
  if (obj.schema && spec.components?.schemas?.[name]) markFields(obj, flatten({ $ref: `#/components/schemas/${name}` }), 'read');
}
for (const op of operations) {
  const obj = objects[op.object];
  obj.operations[op.crud].push(`${op.method} ${op.path} (${op.operationId})`);
  if (op.crud === 'delete') obj.deletable = true;
  if ((op.crud === 'create' || op.crud === 'update') && op._bodySchema) {
    // A sub-resource write (PATCH /organization/billing) changes the nested object the read shape calls `billing`.
    const segment = op.path.split('/').filter((p) => p && !p.startsWith('{')).pop() ?? '';
    const nested = segment.replace(/-([a-z])/g, (_, c) => c.toUpperCase());
    const prefix = obj.fields[nested]?.container && obj.fields[nested].type === 'object' ? nested : '';
    markFields(obj, flatten(op._bodySchema), op.crud, prefix);
  }
}

function sortKeys(value) {
  if (Array.isArray(value)) return value.map(sortKeys);
  if (value && typeof value === 'object') {
    return Object.fromEntries(Object.keys(value).sort().map((k) => [k, sortKeys(value[k])]));
  }
  return value;
}

const out = {
  source: { spec: specPath.replace(/\\/g, '/'), openapi: spec.openapi, title: spec.info?.title ?? null, version: spec.info?.version ?? null },
  summary: {
    operations: operations.length,
    objects: Object.keys(objects).length,
    byCrud: Object.fromEntries(['read', 'create', 'update', 'delete', 'action'].map((c) => [c, operations.filter((o) => o.crud === c).length])),
  },
  operations: operations.map(({ _bodySchema, ...op }) => op),
  objects: sortKeys(objects),
};
process.stdout.write(`${JSON.stringify(out, null, 2)}\n`);
