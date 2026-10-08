package app.rekord.application.error;

import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.ClassConfig;
import jakarta.enterprise.inject.build.compatible.spi.Enhancement;
import jakarta.enterprise.inject.build.compatible.spi.Synthesis;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticComponents;
import jakarta.enterprise.lang.model.declarations.ClassInfo;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Path;
import java.util.Set;
import java.util.TreeSet;

/**
 * Puts {@link AcyclicCause} on every JAX-RS resource class at build time (TASK-5.7), so no resource has to remember it
 * and a resource added later is guarded without anyone touching this. A resource is a concrete class that carries
 * {@code @Path}, or inherits it from a superclass or an interface: every real resource is the second kind, a class
 * with no path of its own implementing the generated {@code *Api} interface of the contract.
 *
 * <p>It lives here and not next to the resources because the resources are in other modules, which must not depend on
 * the application. Registered through {@code META-INF/services}.
 *
 * <p>The classes it binds are published as the {@link AcyclicCauseBindings} bean, and {@code AcyclicCauseBindingIT}
 * fails, listing them, if a resource bean of the container is not among them.
 */
public class BindAcyclicCauseToResources implements BuildCompatibleExtension {

    /** Filled in the enhancement phase and read in the synthesis phase, which the container runs on this instance. */
    private final Set<String> bound = new TreeSet<>();

    @Enhancement(types = Object.class, withSubtypes = true)
    public void bind(ClassConfig candidate) {
        ClassInfo type = candidate.info();
        if (type.isPlainClass() && !type.isAbstract() && !type.hasAnnotation(AcyclicCause.class) && isResource(type)) {
            candidate.addAnnotation(AcyclicCause.class);
            bound.add(type.name());
        }
    }

    /** Hands the bound class names to the running application, so that a test can hold the container against them. */
    @Synthesis
    public void publish(SyntheticComponents components) {
        components.addBean(AcyclicCauseBindings.class)
                .type(AcyclicCauseBindings.class)
                .scope(Singleton.class)
                .withParam(AcyclicCauseBindings.CLASSES, bound.toArray(String[]::new))
                .createWith(AcyclicCauseBindings.Creator.class);
    }

    /**
     * ArC hands over a super type that is missing from the build classpath as a model that cannot be read, so reading
     * it throws a bare {@code NullPointerException}. The build still has to fail, because skipping the class could
     * leave a resource unguarded, but the failure now names the class.
     */
    private static boolean isResource(ClassInfo type) {
        try {
            return carriesPath(type);
        } catch (RuntimeException unreadable) {
            throw new IllegalStateException("Cannot read the super types of " + type.name()
                    + " to bind the cause-cycle guard (TASK-5.7): one of them is not on the build classpath", unreadable);
        }
    }

    private static boolean carriesPath(ClassInfo type) {
        if (type == null || type.name().equals(Object.class.getName())) {
            return false;
        }
        if (type.hasAnnotation(Path.class)) {
            return true;
        }
        for (ClassInfo implemented : type.superInterfacesDeclarations()) {
            if (carriesPath(implemented)) {
                return true;
            }
        }
        return carriesPath(type.superClassDeclaration());
    }
}
