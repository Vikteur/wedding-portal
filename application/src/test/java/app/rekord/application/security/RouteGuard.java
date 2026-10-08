package app.rekord.application.security;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.properties.HasAnnotations;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Reads the routes of the generated {@code app.rekord.api} interfaces, checks the public allow-list against them, and
 * reports routes whose implementing method and declaring class carry no access annotation (BR-OPS-22). It also reports
 * allow-listed routes that carry one, every {@code @PermitAll} (D2), and resources it cannot see.
 */
final class RouteGuard {

    static final String API_PACKAGE = "app.rekord.api";

    /** A floor, not the count: the pinned contract has 84 operations, and a contract bump must not need an edit here. */
    static final int FLOOR = 60;
    static final String PERMIT_ALL = "jakarta.annotation.security.PermitAll";

    private static final String PATH = "jakarta.ws.rs.Path";

    private static final Set<String> ACCESS_ANNOTATIONS = Set.of(
            "io.quarkus.security.Authenticated",
            "jakarta.annotation.security.RolesAllowed",
            "jakarta.annotation.security.PermitAll",
            "jakarta.annotation.security.DenyAll");

    /**
     * The public operations, each with the reason it needs no session. A name is {@code Api#method}, so one entry
     * covers every overload of that method.
     */
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

    /**
     * The {@code Interface#method} names of every routed method of an interface directly in {@code app.rekord.api}.
     * Overloads share one name, so they count once.
     */
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
     * The routes of generated interfaces outside the allow-list whose implementing method and the class that declares
     * that method both lack an access annotation, as {@code Api#method  (Resource)}. For a method inherited from a
     * superclass, only the annotation of that superclass counts, not one on the scanned subclass. The annotations of
     * the interface are never read.
     */
    static Set<String> unguarded(JavaClasses classes, Set<String> allowList) {
        Set<String> unguarded = new TreeSet<>();
        for (ImplementedRoute route : implementedRoutes(classes)) {
            if (!allowList.contains(route.name()) && !route.guarded()) {
                unguarded.add(route.label());
            }
        }
        return unguarded;
    }

    /** Allow-listed routes whose implementation still carries an access annotation, which would refuse a visitor. */
    static Set<String> overGuarded(JavaClasses classes, Set<String> allowList) {
        Set<String> over = new TreeSet<>();
        for (ImplementedRoute route : implementedRoutes(classes)) {
            if (allowList.contains(route.name()) && route.guarded()) {
                over.add(route.label());
            }
        }
        return over;
    }

    /** D2: no {@code @PermitAll} anywhere; a public route is public through {@link #PUBLIC} only. */
    static Set<String> permitAllUses(JavaClasses classes) {
        Set<String> uses = new TreeSet<>();
        for (JavaClass type : classes) {
            if (type.isAnnotatedWith(PERMIT_ALL)) {
                uses.add(type.getName());
            }
            for (JavaMethod method : type.getMethods()) {
                if (method.isAnnotatedWith(PERMIT_ALL)) {
                    uses.add(type.getName() + "#" + method.getName());
                }
            }
        }
        return uses;
    }

    /** Production endpoints that implement no generated interface, which the route guard would otherwise never see. */
    static Set<String> resourcesOutsideTheGuard(JavaClasses classes) {
        Set<String> implementations = implementations(classes);
        Set<String> outside = new TreeSet<>();
        for (JavaClass type : classes) {
            if (type.isInterface()
                    || API_PACKAGE.equals(type.getPackageName())
                    || implementations.contains(type.getName())) {
                continue;
            }
            boolean routesFromInterface = type.getAllRawInterfaces().stream()
                    .filter(api -> !isApi(api))
                    .anyMatch(RouteGuard::declaresRoute);
            if (declaresRoute(type) || routesFromInterface) {
                outside.add(type.getName());
            }
        }
        return outside;
    }

    static void requireGuarded(JavaClasses classes, Set<String> allowList) {
        Set<String> unguarded = unguarded(classes, allowList);
        if (!unguarded.isEmpty()) {
            throw new AssertionError("routes without @Authenticated, @RolesAllowed, @PermitAll or @DenyAll:\n  "
                    + String.join("\n  ", unguarded));
        }
    }

    /** One route of a generated interface, as implemented by one concrete class. */
    private record ImplementedRoute(String name, JavaClass resource, JavaMethod route) {

        /** The implementing method carries an access annotation, or so does the class that declares that method. */
        boolean guarded() {
            return implementing(resource, route)
                    .map(method -> hasAccessAnnotation(method) || hasAccessAnnotation(method.getOwner()))
                    .orElse(false);
        }

        String label() {
            return name + "  (" + resource.getSimpleName() + ")";
        }
    }

    private static List<ImplementedRoute> implementedRoutes(JavaClasses classes) {
        List<ImplementedRoute> routes = new ArrayList<>();
        for (JavaClass type : classes) {
            if (!isImplementation(type)) {
                continue;
            }
            for (JavaClass api : type.getAllRawInterfaces()) {
                if (!isApi(api)) {
                    continue;
                }
                for (JavaMethod route : api.getMethods()) {
                    if (isRoute(route)) {
                        routes.add(new ImplementedRoute(api.getSimpleName() + "#" + route.getName(), type, route));
                    }
                }
            }
        }
        return routes;
    }

    private static boolean declaresRoute(JavaClass type) {
        return type.isAnnotatedWith(PATH)
                || type.getMethods().stream().anyMatch(method -> isRoute(method) || method.isAnnotatedWith(PATH));
    }

    private static boolean isApi(JavaClass type) {
        return type.isInterface() && API_PACKAGE.equals(type.getPackageName());
    }

    private static boolean isImplementation(JavaClass type) {
        return !type.isInterface()
                && !type.getModifiers().contains(JavaModifier.ABSTRACT)
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

    private static boolean hasAccessAnnotation(HasAnnotations<?> target) {
        return ACCESS_ANNOTATIONS.stream().anyMatch(target::isAnnotatedWith);
    }
}
