-- TASK-7.2 (P1-E01-T02): organisations, accounts, memberships and sessions, wedding-portal's first business migration.
--
-- Shape: the final one of the rekord-api oracle (read only): V1__identity.sql for organizations, users and memberships,
-- V9__membership_roles.sql for the memberships keys, V2__sessions.sql for sessions. Identity is global and a
-- membership is a join row, so one person can work for two organisations.
--
-- Deviations from the oracle (UD-13: the database is empty, nothing is imported):
--   * memberships.role also accepts ADMIN (UD-14.a): ADMIN, PLANNER, DJ.
--   * users has no failed_login_count and no locked_until (S20 UX-13).
--   * no dj_invites (TASK-8.1), no auth_attempts and no audit_log: auth_attempts and the sign-in throttle come with
--     TASK-7.5 (P1-E01-T05), audit_log with TASK-15.10 (P1-E09-T10).
--
-- BR-ID-09: a session stores only sha256(token) as token_hash; the token itself is never stored.
-- BR-DM-03: every instant is timestamptz.
-- BR-DM-08, BR-DM-09: a live address and a live slug are unique, case-insensitively.
-- BR-DM-10: a member holds each role once per organisation.
-- BR-DM-12: a session belongs to an account or to a portal, never both and never neither; a portal session is
-- always scoped to one wedding.

create table organizations (
    id          uuid primary key,
    name        text        not null,
    slug        text        not null,
    -- Where the business is; the default a new wedding inherits.
    timezone    text        not null default 'Europe/Amsterdam',
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now(),
    deleted_at  timestamptz
);

create unique index ux_organizations_slug
    on organizations (lower(slug))
    where deleted_at is null;

create table users (
    id                   uuid primary key,
    email                text not null,
    -- Null until an invitation is accepted.
    password_hash        text,
    display_name         text not null,
    phone                text,
    status               text not null default 'INVITED'
                         check (status in ('INVITED', 'ACTIVE', 'DISABLED')),
    last_login_at        timestamptz,
    password_changed_at  timestamptz,
    created_at           timestamptz not null default now(),
    updated_at           timestamptz not null default now(),
    deleted_at           timestamptz
);

-- Addresses are case-insensitive in practice, so uniqueness has to be too. A functional index, not citext.
create unique index ux_users_email
    on users (lower(email))
    where deleted_at is null;

create table memberships (
    id         uuid primary key,
    org_id     uuid not null references organizations (id) on delete cascade,
    user_id    uuid not null references users (id) on delete cascade,
    role       text not null check (role in ('ADMIN', 'PLANNER', 'DJ')),
    status     text not null default 'ACTIVE' check (status in ('ACTIVE', 'DISABLED')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

-- A person may hold several roles in an organisation, but not the same one twice.
create unique index ux_memberships_org_user_role on memberships (org_id, user_id, role);
-- Resolving a session lists a member's roles on every request; that read gets its own index (oracle V9).
create index ix_memberships_org_user on memberships (org_id, user_id);
create index ix_memberships_user on memberships (user_id) where status = 'ACTIVE';

-- One table for every credential type: an account signing in and a couple entering their access code.
-- Not tenant-filtered: a session is read before the organisation is known.
create table sessions (
    id                  uuid primary key,
    subject_kind        text not null check (subject_kind in ('USER', 'PORTAL')),
    user_id             uuid references users (id) on delete cascade,
    -- Foreign keys for these arrive with their tables (P1-E04-T01).
    portal_id           uuid,
    wedding_id          uuid,
    org_id              uuid not null references organizations (id) on delete cascade,
    -- Copied from the membership at sign-in, so a role change does not re-scope an open session.
    roles               text[] not null,
    -- Only sha256(token). The raw value exists in the cookie and nowhere else.
    token_hash          bytea not null,
    created_at          timestamptz not null default now(),
    last_seen_at        timestamptz not null default now(),
    -- Idle expiry slides forward on use; absolute expiry does not.
    idle_expires_at     timestamptz not null,
    absolute_expires_at timestamptz not null,
    revoked_at          timestamptz,
    ip                  inet,
    user_agent          text,
    constraint ck_sessions_subject check (
        (subject_kind = 'USER'
             and user_id is not null
             and portal_id is null)
        or (subject_kind = 'PORTAL'
             and portal_id is not null
             and user_id is null
             and wedding_id is not null)
    )
);

create unique index ux_sessions_token on sessions (token_hash);
create index ix_sessions_live on sessions (absolute_expires_at) where revoked_at is null;
create index ix_sessions_user on sessions (user_id) where revoked_at is null;
create index ix_sessions_portal on sessions (portal_id) where revoked_at is null;
