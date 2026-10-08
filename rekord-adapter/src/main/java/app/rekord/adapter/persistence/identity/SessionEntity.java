package app.rekord.adapter.persistence.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** A live sign-in; only the SHA-256 hash of the token is stored (BR-ID-09). The ip column is not mapped. Mapped by name; Flyway owns the table (V2__identity.sql). No behaviour, no association. */
@Entity
@Table(name = "sessions")
public class SessionEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "subject_kind", nullable = false)
    private String subjectKind;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "portal_id")
    private UUID portalId;

    @Column(name = "wedding_id")
    private UUID weddingId;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "roles", nullable = false, columnDefinition = "text[]")
    private String[] roles;

    @Column(name = "token_hash", nullable = false)
    private byte[] tokenHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "idle_expires_at", nullable = false)
    private Instant idleExpiresAt;

    @Column(name = "absolute_expires_at", nullable = false)
    private Instant absoluteExpiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "user_agent")
    private String userAgent;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSubjectKind() {
        return subjectKind;
    }

    public void setSubjectKind(String subjectKind) {
        this.subjectKind = subjectKind;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getPortalId() {
        return portalId;
    }

    public void setPortalId(UUID portalId) {
        this.portalId = portalId;
    }

    public UUID getWeddingId() {
        return weddingId;
    }

    public void setWeddingId(UUID weddingId) {
        this.weddingId = weddingId;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public void setOrgId(UUID orgId) {
        this.orgId = orgId;
    }

    public String[] getRoles() {
        return roles;
    }

    public void setRoles(String[] roles) {
        this.roles = roles;
    }

    public byte[] getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(byte[] tokenHash) {
        this.tokenHash = tokenHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public Instant getIdleExpiresAt() {
        return idleExpiresAt;
    }

    public void setIdleExpiresAt(Instant idleExpiresAt) {
        this.idleExpiresAt = idleExpiresAt;
    }

    public Instant getAbsoluteExpiresAt() {
        return absoluteExpiresAt;
    }

    public void setAbsoluteExpiresAt(Instant absoluteExpiresAt) {
        this.absoluteExpiresAt = absoluteExpiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof SessionEntity that && id != null && id.equals(that.id));
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hashCode(id);
    }
}
