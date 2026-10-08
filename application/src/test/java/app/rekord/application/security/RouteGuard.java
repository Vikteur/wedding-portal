package app.rekord.application.security;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import java.util.Collections;
import java.util.Optional;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Reads the routes of the generated {@code app.rekord.api} interfaces and checks the public allow-list against them. */
final class RouteGuard {

    static final String API_PACKAGE = "app.rekord.api";
    static final int FLOOR = 60;

    private static final Set<String> ACCESS_ANNOTATIONS = Set.of(
            "io.quarkus.security.Authenticated",
            "jakarta.annotation.security.RolesAllowed",
            "jakarta.annotation.security.PermitAll",
            "jakarta.annotation.security.DenyAll");

    /** The public operations, each with the reason it needs no session. */
    static final Map<String, String> PUBLIC = publicOperations();

    private RouteGuard() {}

    private static Map<String, String> publicOperations() {
        Map<String, String> operations = new LinkedHashMap<>();
        operations.put("HealthApi#health", "liveness probe, answers before any session exists");
        operations.put("AuthApi#login", "signing in happens before a session exists");
        operations.put("AuthApi#logout", "signing out has to work from an expired session");
        operations.put("AuthApi#closePortalSession", "signing out of the portal has to work from an expired session");
        operations.put("AuthApi#previewInvite", "an invitee looks at an invite before having an account");
        operations.put("AuthApi#acceptInvite", "an invitee accepts an invite before having an account");
        operations.put("AuthApi#openPortalSession", "a guest opens the portal with a link, before any session");
        return Collections.unmodifiableMap(operations);
    }

    /** The {@code Interface#method} names of every routed method of an interface directly in {@code app.rekord.api}. */
    static Set<String> routes(JavaClasses classes) {
        Set<String> routes = new TreeSet<>();
        for (JavaClass type : classes) {
            if (!type.isInterface() || !API_PACKAGE.equals(type.getPackageName())) {
                continue;
            }
            for (JavaMethod method : type.getMethods()) {
                if (isRoute(method)) {
                    routes.add(type.getSimpleName() + "#" + method.getName());
                }
            }
        }
        return routes;
    }

    private static boolean isRoute(JavaMethod method) {
        return method.getAnnotations().stream()
                .anyMatch(a -> a.getRawType().isAnnotatedWith("jakarta.ws.rs.HttpMethod"));
    }

    static void requireFloor(Set<String> routes) {
        if (routes.size() < FLOOR) {
            throw new AssertionError("expected at least " + FLOOR + " routes in the generated interfaces, found "
                    + routes.size());
        }
    }

    static Set<String> staleAllowListEntries(Set<String> routes, Set<String> allowList) {
        Set<String> stale = new TreeSet<>(allowList);
        stale.removeAll(routes);
        return stale;
    }

    /** The fully qualified names of the concrete classes that implement an interface of {@code app.rekord.api}. */
    static Set<String> implementations(JavaClasses classes) {
        Set<String> names = new TreeSet<>();
        for (JavaClass type : classes) {
            if (isImplementation(type)) {
                names.add(type.getName());
            }
        }
        return names;
    }

    /**
     * The routes of generated interfaces outside the allow-list whose implementing method and implementing class both
     * lack an access annotation, as {@code Api#method  (Resource)}. The annotations of the interface are never read.
     */
    static Set<String> unguarded(JavaClasses classes, Set<String> allowList) {
        Set<String> unguarded = new TreeSet<>();
        for (JavaClass type : classes) {
            if (!isImplementation(type)) {
                continue;
            }
            for (JavaClass api : type.getAllRawInterfaces()) {
                if (!isApi(api)) {
                    continue;
                }
                for (JavaMethod route : api.getMethods()) {
                    String name = api.getSimpleName() + "#" + route.getName();
                    if (!isRoute(route) || allowList.contains(name)) {
                        continue;
                    }
                    boolean guarded = hasAccessAnnotation(type)
                            || implementing(type, route).map(RouteGuard::hasAccessAnnotation).orElse(false);
                    if (!guarded) {
                        unguarded.add(name + "  (" + type.getSimpleName() + ")");
                    }
                }
            }
        }
        return unguarded;
    }

    static void requireGuarded(JavaClasses classes, Set<String> allowList) {
        Set<String> unguarded = unguarded(classes, allowList);
        if (!unguarded.isEmpty()) {
            throw new AssertionError("routes without @Authenticated, @RolesAllowed, @PermitAll or @DenyAll:\n  "
                    + String.join("\n  ", unguarded));
        }
    }

    private static boolean isApi(JavaClass type) {
        return type.isInterface() && API_PACKAGE.equals(type.getPackageName());
    }

    private static boolean isImplementation(JavaClass type) {
        return !type.isInterface()
                && !type.getModifiers().contains(com.tngtech.archunit.core.domain.JavaModifier.ABSTRACT)
                && type.getAllRawInterfaces().stream().anyMatch(RouteGuard::isApi);
    }

    private static Optional<JavaMethod> implementing(JavaClass type, JavaMethod route) {
        for (Optional<JavaClass> c = Optional.of(type); c.isPresent(); c = c.get().getRawSuperclass()) {
            for (JavaMethod method : c.get().getMethods()) {
                if (method.getName().equals(route.getName())
                        && method.getRawParameterTypes().stream().map(JavaClass::getName).toList()
                                .equals(route.getRawParameterTypes().stream().map(JavaClass::getName).toList())) {
                    return Optional.of(method);
                }
            }
        }
        return Optional.empty();
    }

    private static boolean hasAccessAnnotation(com.tngtech.archunit.core.domain.properties.HasAnnotations<?> target) {
        return ACCESS_ANNOTATIONS.stream().anyMatch(target::isAnnotatedWith);
    }
}
