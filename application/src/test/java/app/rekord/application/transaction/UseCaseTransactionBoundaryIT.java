package app.rekord.application.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

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
    @Produced
    RecordWriteUseCase produced;

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
        // ArC adds a no-args constructor to a normal-scoped bean for its client proxy; the written one is the other
        assertThat(RecordWriteUseCase.class.getDeclaredConstructors())
                .filteredOn(constructor -> constructor.getParameterCount() > 0)
                .singleElement()
                .satisfies(constructor -> assertThat(constructor.getParameterTypes()).containsExactly(WritePort.class));
    }

    @Test
    void a_failing_second_port_call_reaches_the_caller_and_the_recorded_write_is_rolled_back() {
        // Given: the second port call is armed to fail
        port.failNextConfirm();

        // When
        Throwable thrown = catchThrowable(() -> useCase.save("first"));

        // Then: the exception reaches the caller unwrapped, the transaction rolled back, the write is gone
        assertThat(thrown).isNotNull().isSameAs(port.failure());
        assertThat(port.completions()).containsExactly(Status.STATUS_ROLLEDBACK);
        assertThat(port.committedWrites()).isEmpty();
    }

    @Test
    void a_successful_call_commits_the_recorded_write() {
        // Given: nothing armed

        // When
        useCase.save("first");

        // Then: the write is kept on commit, so the empty list in the failing-call test comes from the rollback
        assertThat(port.completions()).containsExactly(Status.STATUS_COMMITTED);
        assertThat(port.committedWrites()).containsExactly("first");
    }

    @Test
    void the_same_use_case_from_a_producer_method_runs_without_a_transaction() {
        // Given: the same class, produced by an @Produces method (PIN-AC-0448)

        // When
        produced.save("first");

        // Then: ArC did not bind @Transactional to the producer result
        assertThat(port.statusesSeen()).containsExactly(Status.STATUS_NO_TRANSACTION);
        assertThat(port.completions()).isEmpty();
        assertThat(Arc.container()
                        .instance(RecordWriteUseCase.class, Produced.Literal.INSTANCE)
                        .getBean()
                        .getKind())
                .isEqualTo(InjectableBean.Kind.PRODUCER_METHOD);
    }
}
