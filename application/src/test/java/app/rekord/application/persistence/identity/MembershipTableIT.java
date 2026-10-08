package app.rekord.application.persistence.identity;

import static org.assertj.core.api.Assertions.assertThat;

import app.rekord.adapter.persistence.identity.OrganizationEntity;
import app.rekord.adapter.persistence.identity.UserEntity;
import app.rekord.application.persistence.AbstractRepositoryTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class MembershipTableIT extends AbstractRepositoryTest {

    @Test
    void a_member_holds_admin_dj_and_planner_once_each_and_no_other_role() {
        // Given an organisation and a user
        OrganizationEntity org = IdentityRows.organization();
        UserEntity user = IdentityRows.planner();
        inNewTransaction(() -> {
            em.persist(org);
            em.persist(user);
        });

        // When the user holds ADMIN, DJ and PLANNER
        inNewTransaction(() -> {
            em.persist(IdentityRows.membership(1, org, user, "ADMIN"));
            em.persist(IdentityRows.membership(2, org, user, "DJ"));
            em.persist(IdentityRows.membership(3, org, user, "PLANNER"));
        });

        // Then a second PLANNER is refused on the key over organisation, user and role (BR-DM-10)
        assertThat(refusal(() -> em.persist(IdentityRows.membership(4, org, user, "PLANNER"))))
                .isEqualTo(new Refusal("23505", "ux_memberships_org_user_role"));

        // And a role outside ADMIN, PLANNER and DJ is refused by the role check (UD-14.a)
        assertThat(refusal(() -> em.persist(IdentityRows.membership(5, org, user, "OWNER"))))
                .isEqualTo(new Refusal("23514", "memberships_role_check"));

        // And the three accepted rows remain
        assertThat(inNewTransactionReturning(
                        () -> em.createQuery("select count(m) from MembershipEntity m", Long.class)
                                .getSingleResult()))
                .isEqualTo(3L);
    }
}
