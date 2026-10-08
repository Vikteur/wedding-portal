package app.rekord.application.persistence.identity;

import static org.assertj.core.api.Assertions.assertThat;

import app.rekord.adapter.persistence.identity.MembershipEntity;
import app.rekord.adapter.persistence.identity.OrganizationEntity;
import app.rekord.adapter.persistence.identity.SessionEntity;
import app.rekord.adapter.persistence.identity.UserEntity;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

class IdentityEntitiesTest {

    @Test
    void no_entity_shows_an_address_a_password_hash_a_token_hash_or_a_name_in_to_string() {
        // Given one filled entity of each kind
        OrganizationEntity org = IdentityRows.organization();
        UserEntity user = IdentityRows.planner();
        List<Object> entities = List.of(
                org, user, IdentityRows.membership(1, org, user, "PLANNER"), IdentityRows.userSession(1, org, user));
        String hex = HexFormat.of().formatHex(IdentityRows.tokenHash(1));

        // Then no toString shows a personal or secret value
        for (Object entity : entities) {
            assertThat(entity.toString())
                    .doesNotContain("example.com")
                    .doesNotContain(IdentityRows.PASSWORD_HASH)
                    .doesNotContain(hex)
                    .doesNotContain("Pat Planner")
                    .doesNotContain("Example Weddings");
        }
    }

    @Test
    void two_entities_are_equal_exactly_when_their_ids_are() {
        // Given two users with one id and different content
        UserEntity a = IdentityRows.user(1, "planner@example.com", "Pat Planner");
        UserEntity sameId = IdentityRows.user(1, "dj@example.com", "Dee Jay");
        UserEntity otherId = IdentityRows.user(2, "planner@example.com", "Pat Planner");

        // Then they are equal by id alone
        assertThat(a).isEqualTo(sameId).hasSameHashCodeAs(sameId).isNotEqualTo(otherId);

        // And the same holds for the other three kinds
        OrganizationEntity org = IdentityRows.organization(1, "Example Weddings", "example-weddings");
        assertThat(org)
                .isEqualTo(IdentityRows.organization(1, "Example Weddings Two", "example-two"))
                .isNotEqualTo(IdentityRows.organization(2, "Example Weddings", "example-weddings"));
        MembershipEntity membership = IdentityRows.membership(1, org, a, "DJ");
        assertThat(membership)
                .isEqualTo(IdentityRows.membership(1, org, a, "ADMIN"))
                .isNotEqualTo(IdentityRows.membership(2, org, a, "DJ"));
        SessionEntity session = IdentityRows.userSession(1, org, a);
        assertThat(session)
                .isEqualTo(IdentityRows.userSession(1, org, otherId))
                .isNotEqualTo(IdentityRows.userSession(2, org, a));
    }
}
