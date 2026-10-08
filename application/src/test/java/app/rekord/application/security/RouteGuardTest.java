package app.rekord.application.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.rekord.architecture.FixtureCompiler;
import app.rekord.architecture.ProductionClasses;
import com.tngtech.archunit.core.domain.JavaClasses;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class RouteGuardTest {

    private static final String AUTH_API = """
            package app.rekord.api;

            import jakarta.ws.rs.GET;
            import jakarta.ws.rs.POST;
            import jakarta.ws.rs.Path;

            @Path("/auth")
            public interface AuthApi {
                @POST @Path("/sign-in") void signIn();
                @POST @Path("/other") void other();
                @GET @Path("/not-a-route-annotation-free") void plain();
                void notARoute();
            }
            """;

    private static String routedInterface(String pkg, String name) {
        return """
                package %s;

                import jakarta.ws.rs.GET;
                import jakarta.ws.rs.Path;

                public interface %s {
                    @GET @Path("/one") void one();
                    @GET @Path("/two") void two();
                }
                """.formatted(pkg, name);
    }

    @Test
    void the_generated_interfaces_of_the_pinned_contract_declare_at_least_60_routes() {
        // Given every production class of every module
        JavaClasses production = ProductionClasses.importAll();

        // When the routes of the generated interfaces are listed
        Set<String> routes = RouteGuard.routes(production);

        // Then the floor holds and the health route is among them
        assertThat(routes).hasSizeGreaterThanOrEqualTo(60).contains("HealthApi#health");
    }

    @Test
    void a_route_count_below_the_floor_fails_and_says_how_many_were_found() {
        Set<String> fiftyNine = names(59);
        Set<String> sixty = names(60);

        assertThatThrownBy(() -> RouteGuard.requireFloor(fiftyNine))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("59");
        assertThatCode(() -> RouteGuard.requireFloor(sixty)).doesNotThrowAnyException();
    }

    @Test
    void only_interfaces_of_the_api_package_are_read() {
        // Given routed interfaces in app.rekord.api.model, in another package and in app.rekord.api
        JavaClasses fixtures = FixtureCompiler.compile(
                routedInterface("app.rekord.api.model", "ModelApi"),
                routedInterface("app.rekord.elsewhere", "ElsewhereApi"),
                routedInterface("app.rekord.api", "InApi"));

        // When the routes are listed
        Set<String> routes = RouteGuard.routes(fixtures);

        // Then only the api package adds routes
        assertThat(routes).containsExactlyInAnyOrder("InApi#one", "InApi#two");
    }

    @Test
    void the_allow_list_is_exactly_the_seven_public_operations() {
        assertThat(RouteGuard.PUBLIC.keySet())
                .containsExactlyInAnyOrder(
                        "HealthApi#health",
                        "AuthApi#login",
                        "AuthApi#logout",
                        "AuthApi#closePortalSession",
                        "AuthApi#previewInvite",
                        "AuthApi#acceptInvite",
                        "AuthApi#openPortalSession");
    }

    @Test
    void every_allow_list_entry_names_a_route_of_the_generated_interfaces() {
        Set<String> routes = RouteGuard.routes(ProductionClasses.importAll());

        assertThat(RouteGuard.staleAllowListEntries(routes, RouteGuard.PUBLIC.keySet()))
                .isEmpty();
    }

    @Test
    void a_renamed_or_removed_allow_list_entry_is_reported_by_name() {
        // Given an AuthApi whose login is renamed signIn and whose logout is missing
        Set<String> routes = RouteGuard.routes(FixtureCompiler.compile(AUTH_API));
        Set<String> allowList = Set.of("AuthApi#login", "AuthApi#logout", "AuthApi#signIn");

        // When the allow-list is checked
        Set<String> stale = RouteGuard.staleAllowListEntries(routes, allowList);

        // Then exactly the two missing entries are named
        assertThat(stale).containsExactlyInAnyOrder("AuthApi#login", "AuthApi#logout");
    }

    private static Set<String> names(int count) {
        Set<String> names = new LinkedHashSet<>();
        IntStream.range(0, count).forEach(i -> names.add("FooApi#route" + i));
        return names;
    }
}
