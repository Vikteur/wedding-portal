-- Runs after V2 only if versions sort numerically: it needs seating_tables.
alter table guests add column table_id uuid references seating_tables (id);
