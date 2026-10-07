package app.rekord.application.transaction;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Test-only qualifier of the use case that {@link UseCaseProducer} builds by hand. */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface Produced {

    final class Literal extends AnnotationLiteral<Produced> implements Produced {

        public static final Literal INSTANCE = new Literal();

        private static final long serialVersionUID = 1L;

        private Literal() {}
    }
}
