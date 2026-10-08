package app.rekord.adapter.persistence.identity;

import app.rekord.domain.shared.error.ErrorCode;
import app.rekord.domain.shared.error.RejectedException;
import app.rekord.usecase.identity.port.FirstAdminRepository;
import app.rekord.usecase.identity.port.NewFirstAdmin;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.hibernate.exception.ConstraintViolationException;

/**
 * Storage of the first-admin bootstrap. A taken address or slug is refused from the name of the violated constraint
 * alone: the refusal has no cause, so no stored address, name or hash can reach a message or a log line.
 */
@ApplicationScoped
public class DefaultFirstAdminRepository implements FirstAdminRepository {

    private static final String ADDRESS_INDEX = "ux_users_email";
    private static final String SLUG_INDEX = "ux_organizations_slug";

    private final EntityManager em;

    public DefaultFirstAdminRepository(EntityManager em) {
        this.em = em;
    }

    @Override
    public boolean hasActiveAdmin() {
        Long count = em.createQuery("""
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
            em.persist(organization);
            em.persist(user);
            em.persist(membership);
            em.flush();
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
                break;
            }
        }
        return e;
    }
}
