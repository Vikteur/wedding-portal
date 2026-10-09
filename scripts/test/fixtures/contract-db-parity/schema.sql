-- A designed schema, not a migration: enums are text plus a CHECK.
CREATE TABLE guests (
    id     uuid         NOT NULL DEFAULT gen_random_uuid(),
    name   varchar(100) NOT NULL,
    status text         NOT NULL DEFAULT 'INVITED',
    CONSTRAINT pk_guests PRIMARY KEY (id),
    CONSTRAINT ck_guests_status CHECK (status IN ('INVITED', 'CONFIRMED', 'it''s'))
);
