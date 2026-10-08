package app.rekord.application.persistence.identity;

import static org.assertj.core.api.Assertions.assertThat;

import app.rekord.adapter.persistence.identity.OrganizationEntity;
import app.rekord.application.persistence.AbstractRepositoryTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class OrganizationTableIT extends AbstractRepositoryTest {

    @Test
    void a_second_live_business_whose_slug_differs_only_in_case_is_refused_on_ux_organizations_slug() {
        // Given a live business
        inNewTransaction(() -> em.persist(IdentityRows.organization(1, "Example Weddings", "example-weddings")));

        // When a second live business has the same slug in other case
        Refusal refusal = refusal(
                () -> em.persist(IdentityRows.organization(2, "Example Weddings Two", "Example-Weddings")));

        // Then it is refused on the live-slug index (BR-DM-09)
        assertThat(refusal).isEqualTo(new Refusal("23505", "ux_organizations_slug"));
    }

    @Test
    void after_the_first_business_is_deleted_its_slug_in_other_case_is_accepted() {
        // Given a business that is deleted
        OrganizationEntity first = IdentityRows.organization(1, "Example Weddings", "example-weddings");
        inNewTransaction(() -> em.persist(first));
        inNewTransaction(() -> em.find(OrganizationEntity.class, first.getId()).setDeletedAt(IdentityRows.T0));

        // When a second business takes the slug in other case, then it is accepted: the index is partial
        inNewTransaction(() -> em.persist(IdentityRows.organization(2, "Example Weddings Two", "Example-Weddings")));
        assertThat(inNewTransactionReturning(
                        () -> em.createQuery("select count(o) from OrganizationEntity o", Long.class)
                                .getSingleResult()))
                .isEqualTo(2L);
    }
}
