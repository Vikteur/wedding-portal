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
        assertThatCode(() -> RouteGuard.requireFloor(routes)).doesNotThrowAnyException();
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
    void only_methods_with_an_http_method_annotation_are_routes() {
        // Given an api interface with every JAX-RS verb, a custom verb and a plain method, and an unannotated implementation
        String propfind = """
                package app.rekord.fixture;

                import jakarta.ws.rs.HttpMethod;
                import java.lang.annotation.Retention;
                import java.lang.annotation.RetentionPolicy;

                @HttpMethod("PROPFIND")
                @Retention(RetentionPolicy.RUNTIME)
                public @interface Propfind {}
                """;
        String verbsApi = """
                package app.rekord.api;

                import jakarta.ws.rs.*;

                public interface VerbsApi {
                    @GET void get();
                    @POST void post();
                    @PUT void put();
                    @DELETE void delete();
                    @PATCH void patch();
                    @HEAD void head();
                    @OPTIONS void options();
                    @app.rekord.fixture.Propfind void propfind();
                    void plain();
                }
                """;
        String verbsResource = """
                package app.rekord.fixture;

                public class VerbsResource implements app.rekord.api.VerbsApi {
                    public void get() {}
                    public void post() {}
                    public void put() {}
                    public void delete() {}
                    public void patch() {}
                    public void head() {}
                    public void options() {}
                    public void propfind() {}
                    public void plain() {}
                }
                """;
        JavaClasses fixtures = FixtureCompiler.compile(propfind, verbsApi, verbsResource);
        Set<String> verbs = Set.of("get", "post", "put", "delete", "patch", "head", "options", "propfind");

        // When the routes are listed and the implementation is checked
        Set<String> routes = RouteGuard.routes(fixtures);
        Set<String> unguarded = RouteGuard.unguarded(fixtures, Set.of());

        // Then every verb is a route and the plain method is neither listed nor reported
        assertThat(routes)
                .containsExactlyInAnyOrderElementsOf(
                        verbs.stream().map(verb -> "VerbsApi#" + verb).toList());
        assertThat(unguarded)
                .containsExactlyInAnyOrderElementsOf(
                        verbs.stream().map(verb -> "VerbsApi#" + verb + "  (VerbsResource)").toList());
    }

    @Test
    void a_route_implemented_in_a_superclass_is_checked_on_that_method() {
        // Given an abstract base that implements FixtureApi with only open guarded, and a concrete subclass
        String base = """
                package app.rekord.fixture;

                public abstract class BaseResource implements app.rekord.api.FixtureApi {
                    @io.quarkus.security.Authenticated @Override public void open() {}
                    @Override public void closed() {}
                }
                """;
        String subclass = """
                package app.rekord.fixture;

                public class InheritingResource extends BaseResource {}
                """;
        JavaClasses fixtures = FixtureCompiler.compile(FIXTURE_API, base, subclass);

        // When the implementations are checked
        Set<String> unguarded = RouteGuard.unguarded(fixtures, Set.of());

        // Then the subclass is scanned and only the route its superclass leaves unguarded is named
        assertThat(RouteGuard.implementations(fixtures)).containsExactly("app.rekord.fixture.InheritingResource");
        assertThat(unguarded).containsExactly("FixtureApi#closed  (InheritingResource)");
    }

    @Test
    void a_route_is_matched_to_the_implementing_method_with_the_same_parameter_types() {
        // Given a route find(String) and two implementations, each with a find(Integer) overload
        String paramApi = """
                package app.rekord.api;

                import jakarta.ws.rs.GET;
                import jakarta.ws.rs.Path;
                import jakarta.ws.rs.PathParam;

                public interface ParamApi {
                    @GET @Path("/{id}") void find(@PathParam("id") String id);
                }
                """;
        String overloadGuarded = """
                package app.rekord.fixture;

                public class OverloadGuardedResource implements app.rekord.api.ParamApi {
                    @io.quarkus.security.Authenticated public void find(Integer id) {}
                    @Override public void find(String id) {}
                }
                """;
        String routeGuarded = """
                package app.rekord.fixture;

                public class RouteGuardedResource implements app.rekord.api.ParamApi {
                    public void find(Integer id) {}
                    @io.quarkus.security.Authenticated @Override public void find(String id) {}
                }
                """;
        JavaClasses fixtures = FixtureCompiler.compile(paramApi, overloadGuarded, routeGuarded);

        // When the implementations are checked
        Set<String> unguarded = RouteGuard.unguarded(fixtures, Set.of());

        // Then only the class whose find(String) lacks an annotation is named
        assertThat(unguarded).containsExactly("ParamApi#find  (OverloadGuardedResource)");
    }

    @Test
    void a_route_without_an_implementing_method_is_unguarded() {
        // Given a route served by a default method of the interface, and an implementation that guards only the other
        String defaultApi = """
                package app.rekord.api;

                import jakarta.ws.rs.GET;
                import jakarta.ws.rs.Path;

                public interface DefaultApi {
                    @GET @Path("/open") default void open() {}
                    @GET @Path("/closed") void closed();
                }
                """;
        String defaultResource = """
                package app.rekord.fixture;

                public class DefaultResource implements app.rekord.api.DefaultApi {
                    @io.quarkus.security.Authenticated @Override public void closed() {}
                }
                """;
        JavaClasses fixtures = FixtureCompiler.compile(defaultApi, defaultResource);

        // When the implementation is checked
        Set<String> unguarded = RouteGuard.unguarded(fixtures, Set.of());

        // Then the route with no implementing method is named
        assertThat(unguarded).containsExactly("DefaultApi#open  (DefaultResource)");
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
