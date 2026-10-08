package app.rekord.application.error;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.build.compatible.spi.Parameters;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticBeanCreator;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * The classes {@link BindAcyclicCauseToResources} gave {@link AcyclicCause} at build time (TASK-5.7), as the extension
 * chose them. The extension publishes them as a bean so that {@code AcyclicCauseBindingIT} can hold every JAX-RS
 * resource bean of the container against the extension's own choice: an interceptor subclass alone would also be
 * there for a resource that has some other interceptor, such as bean validation. Nothing else uses it.
 */
public final class AcyclicCauseBindings {

    /** The name of the synthetic bean parameter that carries the class names. */
    static final String CLASSES = "classes";

    private final Set<String> classNames;

    private AcyclicCauseBindings(Set<String> classNames) {
        this.classNames = Collections.unmodifiableSet(new TreeSet<>(classNames));
    }

    /** The binary names of the bound classes, in order. */
    public Set<String> classNames() {
        return classNames;
    }

    public boolean binds(String className) {
        return classNames.contains(className);
    }

    /** Builds the bean from the class names the extension recorded. */
    public static final class Creator implements SyntheticBeanCreator<AcyclicCauseBindings> {

        @Override
        public AcyclicCauseBindings create(Instance<Object> lookup, Parameters params) {
            return new AcyclicCauseBindings(new TreeSet<>(Arrays.asList(params.get(CLASSES, String[].class))));
        }
    }
}
