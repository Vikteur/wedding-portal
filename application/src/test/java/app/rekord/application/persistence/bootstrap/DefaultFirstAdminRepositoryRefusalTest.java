package app.rekord.application.persistence.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.rekord.adapter.persistence.identity.DefaultFirstAdminRepository;
import app.rekord.domain.shared.error.ErrorCode;
import app.rekord.domain.shared.error.RejectedException;
import app.rekord.usecase.identity.port.NewFirstAdmin;
import jakarta.enterprise.inject.Instance;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.junit.jupiter.api.Test;

/**
 * How the adapter maps a violation to a refusal, without a database: the violations are built here, so their message
 * can be in any language. Hibernate cuts the constraint name out of the server's message text, and PostgreSQL writes
 * that text in its {@code lc_messages} language; a German server gives no name at all.
 */
class DefaultFirstAdminRepositoryRefusalTest {

    private static final String UNIQUE_VIOLATION = "23505";
    private static final String CHECK_VIOLATION = "23514";
    private static final String GERMAN_MESSAGE = "FEHLER: doppelter Schlüsselwert verletzt "
            + "Unique-Constraint »ux_users_email«\n  Detail: Schlüssel »(email)=(admin@example.com)"
            + "« existiert bereits.";
    private static final NewFirstAdmin ADMIN = new NewFirstAdmin(new UUID(0L, 1), "Rekord Match", "rekord-match",
            "Europe/Amsterdam", new UUID(0L, 2), "admin@example.com", "The planner", "scrypt$hash", new UUID(0L, 3),
            "ACTIVE", "ADMIN", "ACTIVE", Instant.parse("2027-06-12T10:00:00Z"));

    /** A repository whose entity manager fails the first {@code persist} with the given exception. */
    @SuppressWarnings("unchecked")
    private static DefaultFirstAdminRepository repositoryFailingWith(RuntimeException failure) {
        ClassLoader loader = DefaultFirstAdminRepositoryRefusalTest.class.getClassLoader();
        EntityManager entityManager = (EntityManager) Proxy.newProxyInstance(loader,
                new Class<?>[] {EntityManager.class}, (proxy, method, args) -> {
                    if (method.getName().equals("persist")) {
                        throw failure;
                    }
                    return null;
                });
        Instance<EntityManager> instance = (Instance<EntityManager>) Proxy.newProxyInstance(loader,
                new Class<?>[] {Instance.class}, (proxy, method, args) ->
                        method.getName().equals("get") ? entityManager : null);
        return new DefaultFirstAdminRepository(instance);
    }

    private static PersistenceException violation(String sqlState, ConstraintKind kind, String constraintName) {
        SQLException sql = new SQLException(GERMAN_MESSAGE, sqlState);
        return new PersistenceException(
                new ConstraintViolationException("could not execute statement", sql, "insert", kind, constraintName));
    }

    private static void assertRefusalWithoutValues(RejectedException refused, ErrorCode code) {
        assertThat(refused.kind()).isEqualTo(RejectedException.Kind.CONFLICT);
        assertThat(refused.code()).isEqualTo(code);
        assertThat(refused.getCause()).isNull();
        assertThat(refused.getSuppressed()).isEmpty();
        assertThat(refused.getMessage()).doesNotContain("example.com").doesNotContain("ux_").doesNotContain("FEHLER");
    }

    @Test
    void a_unique_violation_without_a_constraint_name_is_a_refusal_whatever_language_the_server_wrote() {
        // The name is null because the server's message is not English.
        DefaultFirstAdminRepository repository =
                repositoryFailingWith(violation(UNIQUE_VIOLATION, ConstraintKind.UNIQUE, null));

        assertThatThrownBy(() -> repository.saveFirstAdmin(ADMIN))
                .isInstanceOfSatisfying(RejectedException.class,
                        refused -> assertRefusalWithoutValues(refused, ErrorCode.VALIDATION_FAILED));
    }

    @Test
    void a_unique_violation_is_known_by_its_sql_state_even_when_hibernate_names_another_kind() {
        DefaultFirstAdminRepository repository =
                repositoryFailingWith(violation(UNIQUE_VIOLATION, ConstraintKind.OTHER, null));

        assertThatThrownBy(() -> repository.saveFirstAdmin(ADMIN))
                .isInstanceOfSatisfying(RejectedException.class,
                        refused -> assertRefusalWithoutValues(refused, ErrorCode.VALIDATION_FAILED));
    }

    @Test
    void a_taken_address_keeps_its_reason_when_the_name_is_known() {
        DefaultFirstAdminRepository repository =
                repositoryFailingWith(violation(UNIQUE_VIOLATION, ConstraintKind.UNIQUE, "ux_users_email"));

        assertThatThrownBy(() -> repository.saveFirstAdmin(ADMIN))
                .isInstanceOfSatisfying(RejectedException.class,
                        refused -> assertRefusalWithoutValues(refused, ErrorCode.DUPLICATE_USERNAME));
    }

    @Test
    void a_taken_slug_keeps_its_reason_when_the_name_is_known() {
        DefaultFirstAdminRepository repository =
                repositoryFailingWith(violation(UNIQUE_VIOLATION, ConstraintKind.UNIQUE, "ux_organizations_slug"));

        assertThatThrownBy(() -> repository.saveFirstAdmin(ADMIN))
                .isInstanceOfSatisfying(RejectedException.class,
                        refused -> assertRefusalWithoutValues(refused, ErrorCode.DUPLICATE_NAME));
    }

    @Test
    void a_unique_violation_of_another_known_constraint_is_no_refusal() {
        PersistenceException failure = violation(UNIQUE_VIOLATION, ConstraintKind.UNIQUE, "users_pkey");

        assertThatThrownBy(() -> repositoryFailingWith(failure).saveFirstAdmin(ADMIN)).isSameAs(failure);
    }

    @Test
    void a_violation_that_is_not_unique_is_no_refusal_even_without_a_name() {
        PersistenceException failure = violation(CHECK_VIOLATION, ConstraintKind.CHECK, null);

        assertThatThrownBy(() -> repositoryFailingWith(failure).saveFirstAdmin(ADMIN)).isSameAs(failure);
    }

    @Test
    void a_failure_that_is_no_constraint_violation_is_no_refusal() {
        PersistenceException failure = new PersistenceException("connection lost");

        assertThatThrownBy(() -> repositoryFailingWith(failure).saveFirstAdmin(ADMIN)).isSameAs(failure);
    }
}
