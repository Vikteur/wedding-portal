package app.rekord.application.persistence.migration;

import app.rekord.application.persistence.migration.SchemaSnapshot.Column;
import app.rekord.application.persistence.migration.SchemaSnapshot.Constraint;
import app.rekord.application.persistence.migration.SchemaSnapshot.Default;
import app.rekord.application.persistence.migration.SchemaSnapshot.Index;
import java.util.List;

/**
 * V2 creates the identity tables in the final shape of the rekord-api oracle (V1 + V9 for memberships, V2 for
 * sessions), with the role ADMIN and without {@code failed_login_count} and {@code locked_until}. The literals are
 * PostgreSQL's rendering of the DDL. The ids have no default and no identity column: the caller assigns them.
 */
class V2MigrationIT extends MigrationSchemaCheck {

    private static final String TZ = "timestamp with time zone";
    private static final String NOW = "now()";

    @Override
    protected String version() {
        return "2";
    }

    @Override
    protected SchemaSnapshot expected() {
        return new SchemaSnapshot(
                List.of(),
                List.of("memberships", "organizations", "sessions", "users"),
                List.of(
                        new Column("memberships", "id", "uuid", false),
                        new Column("memberships", "org_id", "uuid", false),
                        new Column("memberships", "user_id", "uuid", false),
                        new Column("memberships", "role", "text", false),
                        new Column("memberships", "status", "text", false),
                        new Column("memberships", "created_at", TZ, false),
                        new Column("memberships", "updated_at", TZ, false),
                        new Column("organizations", "id", "uuid", false),
                        new Column("organizations", "name", "text", false),
                        new Column("organizations", "slug", "text", false),
                        new Column("organizations", "timezone", "text", false),
                        new Column("organizations", "created_at", TZ, false),
                        new Column("organizations", "updated_at", TZ, false),
                        new Column("organizations", "deleted_at", TZ, true),
                        new Column("sessions", "id", "uuid", false),
                        new Column("sessions", "subject_kind", "text", false),
                        new Column("sessions", "user_id", "uuid", true),
                        new Column("sessions", "portal_id", "uuid", true),
                        new Column("sessions", "wedding_id", "uuid", true),
                        new Column("sessions", "org_id", "uuid", false),
                        new Column("sessions", "roles", "_text", false),
                        new Column("sessions", "token_hash", "bytea", false),
                        new Column("sessions", "created_at", TZ, false),
                        new Column("sessions", "last_seen_at", TZ, false),
                        new Column("sessions", "idle_expires_at", TZ, false),
                        new Column("sessions", "absolute_expires_at", TZ, false),
                        new Column("sessions", "revoked_at", TZ, true),
                        new Column("sessions", "ip", "inet", true),
                        new Column("sessions", "user_agent", "text", true),
                        new Column("users", "id", "uuid", false),
                        new Column("users", "email", "text", false),
                        new Column("users", "password_hash", "text", true),
                        new Column("users", "display_name", "text", false),
                        new Column("users", "phone", "text", true),
                        new Column("users", "status", "text", false),
                        new Column("users", "last_login_at", TZ, true),
                        new Column("users", "password_changed_at", TZ, true),
                        new Column("users", "created_at", TZ, false),
                        new Column("users", "updated_at", TZ, false),
                        new Column("users", "deleted_at", TZ, true)),
                List.of(
                        new Constraint("memberships", "memberships_org_id_fkey", "FOREIGN KEY", List.of("org_id"),
                                "references organizations(id) on delete CASCADE"),
                        new Constraint("memberships", "memberships_pkey", "PRIMARY KEY", List.of("id"), ""),
                        new Constraint("memberships", "memberships_role_check", "CHECK", List.of(),
                                "(role = ANY (ARRAY['ADMIN'::text, 'PLANNER'::text, 'DJ'::text]))"),
                        new Constraint("memberships", "memberships_status_check", "CHECK", List.of(),
                                "(status = ANY (ARRAY['ACTIVE'::text, 'DISABLED'::text]))"),
                        new Constraint("memberships", "memberships_user_id_fkey", "FOREIGN KEY", List.of("user_id"),
                                "references users(id) on delete CASCADE"),
                        new Constraint("organizations", "organizations_pkey", "PRIMARY KEY", List.of("id"), ""),
                        new Constraint("sessions", "ck_sessions_subject", "CHECK", List.of(),
                                "(((subject_kind = 'USER'::text) AND (user_id IS NOT NULL) AND (portal_id IS NULL))"
                                        + " OR ((subject_kind = 'PORTAL'::text) AND (portal_id IS NOT NULL)"
                                        + " AND (user_id IS NULL) AND (wedding_id IS NOT NULL)))"),
                        new Constraint("sessions", "sessions_org_id_fkey", "FOREIGN KEY", List.of("org_id"),
                                "references organizations(id) on delete CASCADE"),
                        new Constraint("sessions", "sessions_pkey", "PRIMARY KEY", List.of("id"), ""),
                        new Constraint("sessions", "sessions_subject_kind_check", "CHECK", List.of(),
                                "(subject_kind = ANY (ARRAY['USER'::text, 'PORTAL'::text]))"),
                        new Constraint("sessions", "sessions_user_id_fkey", "FOREIGN KEY", List.of("user_id"),
                                "references users(id) on delete CASCADE"),
                        new Constraint("users", "users_pkey", "PRIMARY KEY", List.of("id"), ""),
                        new Constraint("users", "users_status_check", "CHECK", List.of(),
                                "(status = ANY (ARRAY['INVITED'::text, 'ACTIVE'::text, 'DISABLED'::text]))")),
                List.of(
                        new Index("memberships", "ix_memberships_org_user",
                                "CREATE INDEX ix_memberships_org_user ON public.memberships USING btree (org_id, user_id)"),
                        new Index("memberships", "ix_memberships_user",
                                "CREATE INDEX ix_memberships_user ON public.memberships USING btree (user_id)"
                                        + " WHERE (status = 'ACTIVE'::text)"),
                        new Index("memberships", "ux_memberships_org_user_role",
                                "CREATE UNIQUE INDEX ux_memberships_org_user_role ON public.memberships"
                                        + " USING btree (org_id, user_id, role)"),
                        new Index("organizations", "ux_organizations_slug",
                                "CREATE UNIQUE INDEX ux_organizations_slug ON public.organizations"
                                        + " USING btree (lower(slug)) WHERE (deleted_at IS NULL)"),
                        new Index("sessions", "ix_sessions_live",
                                "CREATE INDEX ix_sessions_live ON public.sessions USING btree (absolute_expires_at)"
                                        + " WHERE (revoked_at IS NULL)"),
                        new Index("sessions", "ix_sessions_portal",
                                "CREATE INDEX ix_sessions_portal ON public.sessions USING btree (portal_id)"
                                        + " WHERE (revoked_at IS NULL)"),
                        new Index("sessions", "ix_sessions_user",
                                "CREATE INDEX ix_sessions_user ON public.sessions USING btree (user_id)"
                                        + " WHERE (revoked_at IS NULL)"),
                        new Index("sessions", "ux_sessions_token",
                                "CREATE UNIQUE INDEX ux_sessions_token ON public.sessions USING btree (token_hash)"),
                        new Index("users", "ux_users_email",
                                "CREATE UNIQUE INDEX ux_users_email ON public.users USING btree (lower(email))"
                                        + " WHERE (deleted_at IS NULL)")),
                List.of(
                        new Default("memberships", "status", "'ACTIVE'::text", null),
                        new Default("memberships", "created_at", NOW, null),
                        new Default("memberships", "updated_at", NOW, null),
                        new Default("organizations", "timezone", "'Europe/Amsterdam'::text", null),
                        new Default("organizations", "created_at", NOW, null),
                        new Default("organizations", "updated_at", NOW, null),
                        new Default("sessions", "created_at", NOW, null),
                        new Default("sessions", "last_seen_at", NOW, null),
                        new Default("users", "status", "'INVITED'::text", null),
                        new Default("users", "created_at", NOW, null),
                        new Default("users", "updated_at", NOW, null)));
    }
}
