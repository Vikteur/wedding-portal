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
                @GET @Path("/plain") void plain();
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
    void a_permit_all_on_a_class_a_method_or_a_generated_interface_is_named() {
        // Given a generated interface with a @PermitAll route, a class with @PermitAll, and a method with @PermitAll
        String openApi = """
                package app.rekord.api;

                import jakarta.ws.rs.GET;
                import jakarta.ws.rs.Path;

                public interface OpenApi {
                    @jakarta.annotation.security.PermitAll @GET @Path("/open") void open();
                    @GET @Path("/closed") void closed();
                }
                """;
        String classLevel = """
                package app.rekord.fixture;

                @jakarta.annotation.security.PermitAll
                public class OpenClassResource implements app.rekord.api.OpenApi {
                    @Override public void open() {}
                    @Override public void closed() {}
                }
                """;
        String methodLevel = """
                package app.rekord.fixture;

                public class OpenMethodResource implements app.rekord.api.OpenApi {
                    @jakarta.annotation.security.PermitAll @Override public void open() {}
                    @io.quarkus.security.Authenticated @Override public void closed() {}
                }
                """;
        JavaClasses fixtures = FixtureCompiler.compile(openApi, classLevel, methodLevel);

        // When the uses of @PermitAll are listed
        Set<String> uses = RouteGuard.permitAllUses(fixtures);

        // Then each use is named, and the @Authenticated method is not
        assertThat(uses)
                .containsExactlyInAnyOrder(
                        "app.rekord.api.OpenApi#open",
                        "app.rekord.fixture.OpenClassResource",
                        "app.rekord.fixture.OpenMethodResource#open");
    }

    @Test
    void a_class_level_access_annotation_on_an_allow_listed_route_is_reported() {
        // Given a class-level @Authenticated implementation, with only open on the allow-list
        JavaClasses fixtures = FixtureCompiler.compile(
                FIXTURE_API, implementation("@io.quarkus.security.Authenticated", ""));

        // When the allow-listed routes are checked for an access annotation
        Set<String> overGuarded = RouteGuard.overGuarded(fixtures, Set.of("FixtureApi#open"));

        // Then open is named, and closed is not, because it is not on the allow-list
        assertThat(overGuarded).containsExactly("FixtureApi#open  (UnguardedResource)");
    }

    @Test
    void a_method_level_access_annotation_on_an_allow_listed_route_is_reported() {
        JavaClasses annotated = FixtureCompiler.compile(
                FIXTURE_API, implementation("", "@io.quarkus.security.Authenticated"));
        JavaClasses bare = FixtureCompiler.compile(FIXTURE_API, implementation("", ""));

        assertThat(RouteGuard.overGuarded(annotated, Set.of("FixtureApi#open")))
                .containsExactly("FixtureApi#open  (UnguardedResource)");
        assertThat(RouteGuard.overGuarded(bare, Set.of("FixtureApi#open"))).isEmpty();
    }

    @Test
    void an_allow_listed_route_is_over_guarded_by_the_class_that_declares_its_implementing_method() {
        // Given an @Authenticated base that declares both methods, and an @Authenticated subclass of a plain base
        String guardedBase = """
                package app.rekord.fixture;

                @io.quarkus.security.Authenticated
                public abstract class GuardedBaseResource implements app.rekord.api.FixtureApi {
                    @Override public void open() {}
                    @Override public void closed() {}
                }
                """;
        String plainSub = """
                package app.rekord.fixture;

                public class PlainSubResource extends GuardedBaseResource {}
                """;
        String plainBase = """
                package app.rekord.fixture;

                public abstract class PlainBaseResource implements app.rekord.api.FixtureApi {
                    @Override public void open() {}
                    @Override public void closed() {}
                }
                """;
        String annotatedSub = """
                package app.rekord.fixture;

                @io.quarkus.security.Authenticated
                public class AnnotatedSubResource extends PlainBaseResource {}
                """;
        JavaClasses inherited = FixtureCompiler.compile(FIXTURE_API, guardedBase, plainSub);
        JavaClasses subclassed = FixtureCompiler.compile(FIXTURE_API, plainBase, annotatedSub);

        // When the allow-listed route open is checked in both
        // Then the annotation of the declaring base counts, and the annotation of the subclass does not
        assertThat(RouteGuard.overGuarded(inherited, Set.of("FixtureApi#open")))
                .containsExactly("FixtureApi#open  (PlainSubResource)");
        assertThat(RouteGuard.overGuarded(subclassed, Set.of("FixtureApi#open"))).isEmpty();
    }

    @Test
    void only_concrete_classes_implementing_a_generated_interface_are_scanned() {
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
    void overloads_of_a_route_share_one_name_and_one_allow_list_entry_covers_them_all() {
        // Given a generated interface with two routed overloads of same, and an unannotated implementation
        String overloadApi = """
                package app.rekord.api;

                import jakarta.ws.rs.GET;
                import jakarta.ws.rs.Path;
                import jakarta.ws.rs.PathParam;

                public interface OverloadApi {
                    @GET @Path("/a") void same();
                    @GET @Path("/b/{id}") void same(@PathParam("id") String id);
                }
                """;
        String overloadResource = """
                package app.rekord.fixture;

                public class OverloadResource implements app.rekord.api.OverloadApi {
                    @Override public void same() {}
                    @Override public void same(String id) {}
                }
                """;
        JavaClasses fixtures = FixtureCompiler.compile(overloadApi, overloadResource);

        // When the routes are listed and the implementation is checked, without and with the allow-list entry
        Set<String> routes = RouteGuard.routes(fixtures);
        Set<String> unlisted = RouteGuard.unguarded(fixtures, Set.of());
        Set<String> listed = RouteGuard.unguarded(fixtures, Set.of("OverloadApi#same"));

        // Then both overloads count once, and the one entry exempts both
        assertThat(routes).containsExactly("OverloadApi#same");
        assertThat(unlisted).containsExactly("OverloadApi#same  (OverloadResource)");
        assertThat(listed).isEmpty();
    }

    @Test
    void a_hand_written_resource_that_implements_no_generated_interface_is_named() {
        // Given a loose @Path resource, one that gets its routes from a foreign interface, and three classes to skip
        String loose = """
                package app.rekord.application.debug;

                import jakarta.ws.rs.GET;
                import jakarta.ws.rs.Path;

                @Path("/debug")
                public class LooseResource {
                    @GET public String dump() { return ""; }
                }
                """;
        String outsideApi = """
                package app.rekord.elsewhere;

                import jakarta.ws.rs.GET;
                import jakarta.ws.rs.Path;

                public interface OutsideApi {
                    @GET @Path("/x") void x();
                }
                """;
        String viaInterface = """
                package app.rekord.fixture;

                public class ViaInterfaceResource implements app.rekord.elsewhere.OutsideApi {
                    @Override public void x() {}
                }
                """;
        String abstractImpl = """
                package app.rekord.fixture;

                public abstract class AbstractResource implements app.rekord.api.FixtureApi {}
                """;
        String plainBean = """
                package app.rekord.fixture;

                public class PlainBean {
                    public void run() {}
                }
                """;
        JavaClasses fixtures = FixtureCompiler.compile(
                FIXTURE_API, implementation("", ""), loose, outsideApi, viaInterface, abstractImpl, plainBean);

        // When the resources the guard does not see are listed
        Set<String> outside = RouteGuard.resourcesOutsideTheGuard(fixtures);

        // Then only the two classes that serve routes without a generated interface are named
        assertThat(outside)
                .containsExactlyInAnyOrder(
                        "app.rekord.application.debug.LooseResource", "app.rekord.fixture.ViaInterfaceResource");
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
    void a_class_annotation_on_a_subclass_does_not_guard_a_route_implemented_in_its_unannotated_superclass() {
        // Given an unannotated abstract base that implements FixtureApi, and a subclass annotated at class level
        String base = """
                package app.rekord.fixture;

                public abstract class PlainBaseResource implements app.rekord.api.FixtureApi {
                    @Override public void open() {}
                    @Override public void closed() {}
                }
                """;
        String subclass = """
                package app.rekord.fixture;

                @io.quarkus.security.Authenticated
                public class AnnotatedSubResource extends PlainBaseResource {}
                """;
        JavaClasses fixtures = FixtureCompiler.compile(FIXTURE_API, base, subclass);

        // When the implementations are checked
        Set<String> unguarded = RouteGuard.unguarded(fixtures, Set.of());

        // Then both inherited routes are named: the annotation of the subclass does not reach the methods of the base
        assertThat(unguarded)
                .containsExactlyInAnyOrder(
                        "FixtureApi#closed  (AnnotatedSubResource)", "FixtureApi#open  (AnnotatedSubResource)");
    }

    @Test
    void a_class_annotation_on_the_superclass_that_declares_the_route_guards_it() {
        // Given an @Authenticated abstract base that declares both methods, and a plain subclass
        String base = """
                package app.rekord.fixture;

                @io.quarkus.security.Authenticated
                public abstract class GuardedBaseResource implements app.rekord.api.FixtureApi {
                    @Override public void open() {}
                    @Override public void closed() {}
                }
                """;
        String subclass = """
                package app.rekord.fixture;

                public class PlainSubResource extends GuardedBaseResource {}
                """;
        JavaClasses fixtures = FixtureCompiler.compile(FIXTURE_API, base, subclass);

        // When the implementations are checked
        Set<String> unguarded = RouteGuard.unguarded(fixtures, Set.of());

        // Then the base guards its own declared methods
        assertThat(unguarded).isEmpty();
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

        assertThatCode(() -> RouteGuard.requireFloor(RouteGuard.routes(production))).doesNotThrowAnyException();
        assertThat(RouteGuard.implementations(production))
                .contains("app.rekord.adapter.web.health.HealthResource");
        assertThat(RouteGuard.unguarded(production, RouteGuard.PUBLIC.keySet())).isEmpty();
    }

    @Test
    void no_production_class_or_method_carries_permit_all() {
        assertThat(RouteGuard.permitAllUses(ProductionClasses.importAll()))
                .as("D2: public routes go through RouteGuard.PUBLIC, never @PermitAll")
                .isEmpty();
    }

    @Test
    void every_production_resource_is_seen_by_the_guard() {
        assertThat(RouteGuard.resourcesOutsideTheGuard(ProductionClasses.importAll()))
                .as("a production class with routes that implements no generated interface")
                .isEmpty();
    }

    @Test
    void no_allow_listed_production_route_carries_an_access_annotation() {
        JavaClasses production = ProductionClasses.importAll();

        assertThat(RouteGuard.implementations(production))
                .contains("app.rekord.adapter.web.health.HealthResource");
        assertThat(RouteGuard.overGuarded(production, RouteGuard.PUBLIC.keySet()))
                .as("a public route that an access annotation would refuse")
                .isEmpty();
    }

    private static Set<String> names(int count) {
        Set<String> names = new LinkedHashSet<>();
        IntStream.range(0, count).forEach(i -> names.add("FooApi#route" + i));
        return names;
    }
}
