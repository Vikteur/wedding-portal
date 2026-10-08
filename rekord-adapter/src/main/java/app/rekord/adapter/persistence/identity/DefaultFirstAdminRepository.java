package app.rekord.adapter.persistence.identity;

import app.rekord.domain.shared.error.ErrorCode;
import app.rekord.domain.shared.error.RejectedException;
import app.rekord.usecase.identity.port.FirstAdminRepository;
import app.rekord.usecase.identity.port.NewFirstAdmin;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.hibernate.exception.ConstraintViolationException;

/**
 * Storage of the first-admin bootstrap.
 *
 * <p>Refusals: a unique violation is refused as a {@link RejectedException} that has no cause and no stored value, so
 * the refused address, name or hash cannot reach a message or a log line through it. Which refusal depends on what is
 * known of the violated constraint: {@code ux_users_email} is a taken address ({@code DUPLICATE_USERNAME}),
 * {@code ux_organizations_slug} a taken slug ({@code DUPLICATE_NAME}), and a unique violation (SQLState 23505) whose
 * constraint cannot be named is {@code VALIDATION_FAILED}. Hibernate cuts the name out of the server's message text,
 * and PostgreSQL writes that text in its {@code lc_messages} language, so on a server that does not write English the
 * name is {@code null}; the SQLState does not depend on the language. Any other failure (another constraint, a
 * database that is down) is rethrown unchanged, with its cause chain, and the transaction rolls back.
 *
 * <p>The check for an existing admin and the save are two steps, not one lock: two instances that start at the same
 * moment can both pass the check. The bootstrap is for the first start of one instance (see docs/memory.md, TASK-7.3).
 */
@ApplicationScoped
public class DefaultFirstAdminRepository implements FirstAdminRepository {

    private static final String ADDRESS_INDEX = "ux_users_email";
    private static final String SLUG_INDEX = "ux_organizations_slug";
    private static final String UNIQUE_VIOLATION = "23505";

    // An Instance, so that a start without a datasource (the resource tests) does not resolve the inactive session.
    private final Instance<EntityManager> entityManager;

    public DefaultFirstAdminRepository(Instance<EntityManager> entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public boolean hasActiveAdmin() {
        Long count = entityManager.get().createQuery("""
                select count(m) from MembershipEntity m, UserEntity u
                where m.userId = u.id and m.role = 'ADMIN' and m.status = 'ACTIVE'
                  and u.status = 'ACTIVE' and u.deletedAt is null""", Long.class).getSingleResult();
        return count > 0;
    }

    @Override
    public void saveFirstAdmin(NewFirstAdmin admin) {
        OrganizationEntity organization = new OrganizationEntity();
        organization.setId(admin.businessId());
        organization.setName(admin.businessName());
        organization.setSlug(admin.businessSlug());
        organization.setTimezone(admin.timezone());
        organization.setCreatedAt(admin.now());
        organization.setUpdatedAt(admin.now());

        UserEntity user = new UserEntity();
        user.setId(admin.accountId());
        user.setEmail(admin.email());
        user.setPasswordHash(admin.passwordHash());
        user.setDisplayName(admin.displayName());
        user.setStatus(admin.accountStatus());
        user.setPasswordChangedAt(admin.now());
        user.setCreatedAt(admin.now());
        user.setUpdatedAt(admin.now());

        MembershipEntity membership = new MembershipEntity();
        membership.setId(admin.membershipId());
        membership.setOrgId(admin.businessId());
        membership.setUserId(admin.accountId());
        membership.setRole(admin.role());
        membership.setStatus(admin.membershipStatus());
        membership.setCreatedAt(admin.now());
        membership.setUpdatedAt(admin.now());

        try {
            entityManager.get().persist(organization);
            entityManager.get().persist(user);
            entityManager.get().persist(membership);
            entityManager.get().flush();
        } catch (PersistenceException e) {
            throw refusalOf(e);
        }
    }

    private static RuntimeException refusalOf(PersistenceException e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof ConstraintViolationException violation) {
                String constraint = violation.getConstraintName();
                if (ADDRESS_INDEX.equals(constraint)) {
                    return new RejectedException(RejectedException.Kind.CONFLICT, ErrorCode.DUPLICATE_USERNAME,
                            "That address already belongs to an account.");
                }
                if (SLUG_INDEX.equals(constraint)) {
                    return new RejectedException(RejectedException.Kind.CONFLICT, ErrorCode.DUPLICATE_NAME,
                            "That business name is already taken.");
                }
                if (constraint == null && UNIQUE_VIOLATION.equals(violation.getSQLState())) {
                    // The name is unknown (a server that does not write English): still a taken value, still no
                    // cause and no value in the refusal. The reason is the generic one; nothing says which value.
                    return new RejectedException(RejectedException.Kind.CONFLICT, ErrorCode.VALIDATION_FAILED,
                            "A stored value is already taken.");
                }
                break;
            }
        }
        return e;
    }
}
