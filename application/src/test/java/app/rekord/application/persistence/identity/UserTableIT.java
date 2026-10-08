package app.rekord.application.persistence.identity;

import static org.assertj.core.api.Assertions.assertThat;

import app.rekord.adapter.persistence.identity.UserEntity;
import app.rekord.application.persistence.AbstractRepositoryTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class UserTableIT extends AbstractRepositoryTest {

    @Test
    void a_second_live_user_whose_address_differs_only_in_case_is_refused_on_ux_users_email_until_the_first_is_deleted() {
        // Given a live user
        UserEntity first = IdentityRows.user(1, "planner@example.com", "Pat Planner");
        inNewTransaction(() -> em.persist(first));

        // When a second live user has the same address in other case
        UserEntity second = IdentityRows.user(2, "Planner@example.com", "Pat Planner");
        Refusal refusal = refusal(() -> em.persist(second));

        // Then it is refused on the live-address index (BR-DM-08)
        assertThat(refusal).isEqualTo(new Refusal("23505", "ux_users_email"));

        // And once the first is deleted, the address is free again
        inNewTransaction(() -> em.find(UserEntity.class, first.getId()).setDeletedAt(IdentityRows.T0));
        inNewTransaction(() -> em.persist(second));
        assertThat(inNewTransactionReturning(
                        () -> em.createQuery("select count(u) from UserEntity u", Long.class).getSingleResult()))
                .isEqualTo(2L);
    }
}
