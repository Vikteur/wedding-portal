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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

    private static final String FIXTURE_API = """
            package app.rekord.api;

            import jakarta.ws.rs.GET;
            import jakarta.ws.rs.Path;

            public interface FixtureApi {
                @GET @Path("/open") void open();
                @GET @Path("/closed") void closed();
            }
            """;

    private static String implementation(String classAnnotation, String methodAnnotation) {
        return """
                package app.rekord.fixture;

                import app.rekord.api.FixtureApi;

                %s
                public class UnguardedResource implements FixtureApi {
                    %s
                    @Override public void open() {}
                    %s
                    @Override public void closed() {}
                }
                """.formatted(classAnnotation, methodAnnotation, methodAnnotation);
    }

    @Test
    void an_implementation_method_without_an_access_annotation_fails_and_is_named() {
        JavaClasses fixtures = FixtureCompiler.compile(FIXTURE_API, implementation("", ""));

        assertThat(RouteGuard.unguarded(fixtures, Set.of()))
                .containsExactlyInAnyOrder(
                        "FixtureApi#closed  (UnguardedResource)", "FixtureApi#open  (UnguardedResource)");
        assertThatThrownBy(() -> RouteGuard.requireGuarded(fixtures, Set.of()))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("FixtureApi#closed  (UnguardedResource)");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "io.quarkus.security.Authenticated",
                "jakarta.annotation.security.RolesAllowed(\"ADMIN\")",
                "jakarta.annotation.security.PermitAll",
                "jakarta.annotation.security.DenyAll"
            })
    void each_access_annotation_on_the_implementing_method_guards_it(String annotation) {
        JavaClasses fixtures = FixtureCompiler.compile(FIXTURE_API, implementation("", "@" + annotation));

        assertThat(RouteGuard.unguarded(fixtures, Set.of())).isEmpty();
    }

    @Test
    void an_access_annotation_on_the_implementing_class_guards_its_methods() {
        JavaClasses fixtures = FixtureCompiler.compile(
                FIXTURE_API, implementation("@io.quarkus.security.Authenticated", ""));

        assertThat(RouteGuard.unguarded(fixtures, Set.of())).isEmpty();
    }

    @Test
    void an_access_annotation_on_the_generated_interface_is_not_counted() {
        String annotatedApi = """
                package app.rekord.api;

                import jakarta.ws.rs.GET;
                import jakarta.ws.rs.Path;

                public interface FixtureApi {
                    @io.quarkus.security.Authenticated @GET @Path("/open") void open();
                    @jakarta.annotation.security.PermitAll @GET @Path("/closed") void closed();
                }
                """;
        JavaClasses fixtures = FixtureCompiler.compile(annotatedApi, implementation("", ""));

        assertThat(RouteGuard.unguarded(fixtures, Set.of()))
                .containsExactlyInAnyOrder(
                        "FixtureApi#closed  (UnguardedResource)", "FixtureApi#open  (UnguardedResource)");
    }

    @Test
    void an_allow_listed_route_needs_no_annotation() {
        JavaClasses fixtures = FixtureCompiler.compile(FIXTURE_API, implementation("", ""));

        assertThat(RouteGuard.unguarded(fixtures, Set.of("FixtureApi#open")))
                .containsExactly("FixtureApi#closed  (UnguardedResource)");
    }

    @Test
    void a_class_implementing_no_generated_interface_is_not_scanned() {
        String outsideApi = """
                package app.rekord.elsewhere;

                import jakarta.ws.rs.GET;
                import jakarta.ws.rs.Path;

                public interface OutsideApi {
                    @GET @Path("/x") void x();
                }
                """;
        String outsideImpl = """
                package app.rekord.fixture;

                public class OutsideResource implements app.rekord.elsewhere.OutsideApi {
                    @Override public void x() {}
                }
                """;
        String abstractImpl = """
                package app.rekord.fixture;

                public abstract class AbstractResource implements app.rekord.api.FixtureApi {}
                """;
        String subInterface = """
                package app.rekord.fixture;

                public interface SubApi extends app.rekord.api.FixtureApi {}
                """;
        JavaClasses fixtures =
                FixtureCompiler.compile(FIXTURE_API, outsideApi, outsideImpl, abstractImpl, subInterface);

        assertThat(RouteGuard.implementations(fixtures)).isEmpty();
        assertThat(RouteGuard.unguarded(fixtures, Set.of())).isEmpty();
    }

    @Test
    void every_production_implementation_of_a_generated_interface_is_guarded_or_public() {
        JavaClasses production = ProductionClasses.importAll();

        assertThat(RouteGuard.implementations(production))
                .contains("app.rekord.adapter.web.health.HealthResource");
        assertThat(RouteGuard.unguarded(production, RouteGuard.PUBLIC.keySet())).isEmpty();
    }

    private static Set<String> names(int count) {
        Set<String> names = new LinkedHashSet<>();
        IntStream.range(0, count).forEach(i -> names.add("FooApi#route" + i));
        return names;
    }
}
