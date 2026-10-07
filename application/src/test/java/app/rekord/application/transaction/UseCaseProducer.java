package app.rekord.application.transaction;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;

/**
 * Test-only producer: shows that ArC does not bind {@code @Transactional} to a produced use case (PIN-AC-0448). A3 does
 * not refuse it because it lives in test sources and the use case is outside {@code app.rekord.usecase..}.
 */
@Dependent
public class UseCaseProducer {

    @Produces
    @Produced
    RecordWriteUseCase produced(WritePort port) {
        return new RecordWriteUseCase(port);
    }
}
