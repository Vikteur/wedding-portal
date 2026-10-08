package app.rekord.application.security;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Reads the routes of the generated {@code app.rekord.api} interfaces and checks the public allow-list against them. */
final class RouteGuard {

    static final String API_PACKAGE = "app.rekord.api";
    static final int FLOOR = 60;

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
}
