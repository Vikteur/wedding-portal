package app.rekord.application.error;

import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.ClassConfig;
import jakarta.enterprise.inject.build.compatible.spi.Enhancement;
import jakarta.enterprise.lang.model.declarations.ClassInfo;
import jakarta.ws.rs.Path;

/**
 * Puts {@link AcyclicCause} on every JAX-RS resource class at build time (TASK-5.7), so no resource has to remember it
 * and a resource added later is guarded without anyone touching this. A resource is a concrete class that carries
 * {@code @Path}, or inherits it from a superclass or an interface: every real resource is the second kind, a class
 * with no path of its own implementing the generated {@code *Api} interface of the contract.
 *
 * <p>It lives here and not next to the resources because the resources are in other modules, which must not depend on
 * the application. Registered through {@code META-INF/services}; {@code AcyclicCauseBindingIT} fails if a resource
 * is missed.
 */
public class BindAcyclicCauseToResources implements BuildCompatibleExtension {

    @Enhancement(types = Object.class, withSubtypes = true)
    public void bind(ClassConfig candidate) {
        ClassInfo type = candidate.info();
        if (type.isPlainClass() && !type.isAbstract() && !type.hasAnnotation(AcyclicCause.class) && isResource(type)) {
            candidate.addAnnotation(AcyclicCause.class);
        }
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
