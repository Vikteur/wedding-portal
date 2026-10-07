package app.rekord.application.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableBean;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionSynchronizationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class UseCaseTransactionBoundaryIT {

    @Inject
    RecordWriteUseCase useCase;

    @Inject
    RecordingWritePort port;

    @Inject
    TransactionSynchronizationRegistry registry;

    @BeforeEach
    void resetPort() {
        port.reset();
    }

    @Test
    void a_transaction_is_active_inside_the_transactional_use_case_method() {
        // Given: no transaction in the test thread
        assertThat(registry.getTransactionStatus()).isEqualTo(Status.STATUS_NO_TRANSACTION);

        // When
        useCase.save("first");

        // Then
        assertThat(port.statusesSeen()).containsExactly(Status.STATUS_ACTIVE);
        assertThat(registry.getTransactionStatus()).isEqualTo(Status.STATUS_NO_TRANSACTION);
    }

    @Test
    void the_use_case_is_an_application_scoped_class_bean_with_a_single_constructor() {
        // Given: the container's bean for the use case
        InjectableBean<RecordWriteUseCase> bean =
                Arc.container().instance(RecordWriteUseCase.class).getBean();

        // Then: a managed class bean, not a producer result
        assertThat(bean.getScope()).isEqualTo(ApplicationScoped.class);
        assertThat(bean.getKind()).isEqualTo(InjectableBean.Kind.CLASS);
        assertThat(RecordWriteUseCase.class.getDeclaredConstructors()).hasSize(1);
    }
}
