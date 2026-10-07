package app.rekord.application.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import jakarta.ws.rs.Path;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class GeneratedContractTest {

    private static final java.nio.file.Path REPO_ROOT = java.nio.file.Path.of(System.getProperty("wedding.repoRoot"));
    private static final java.nio.file.Path GENERATED = REPO_ROOT.resolve("rekord-adapter/build/generated/openapi");
    // Copied from weddingapp .claude/hooks/guard-generated.globs:20-21 (bash [[ ]] semantics: * spans /).
    private static final String REPO_GLOB = "*/build/generated/*";
    private static final String MODULE_GLOB = "build/generated/*";

    private static List<java.nio.file.Path> generatedFiles() throws IOException {
        assertThat(GENERATED).as("generated output directory").isDirectory();
        try (Stream<java.nio.file.Path> walk = Files.walk(GENERATED)) {
            return walk.filter(Files::isRegularFile).toList();
        }
    }

    private static List<java.nio.file.Path> generatedJava() throws IOException {
        List<java.nio.file.Path> java = generatedFiles().stream()
                .filter(p -> p.getFileName().toString().endsWith(".java"))
                .toList();
        assertThat(java).as("generated java files").isNotEmpty();
        return java;
    }

    private static boolean bashGlob(String glob, String value) {
        String regex = Pattern.quote(glob).replace("*", "\\E.*\\Q");
        return value.matches(regex);
    }

    private static boolean inApiPackage(java.nio.file.Path file) {
        return file.getParent().endsWith(java.nio.file.Path.of("app", "rekord", "api"));
    }

    private static List<Class<?>> apiInterfaces() throws Exception {
        List<Class<?>> apis = new ArrayList<>();
        for (java.nio.file.Path file : generatedJava()) {
            String name = file.getFileName().toString().replace(".java", "");
            if (inApiPackage(file) && name.endsWith("Api")) {
                apis.add(Class.forName("app.rekord.api." + name));
            }
        }
        assertThat(apis).isNotEmpty();
        return apis;
    }

    @Test
    void every_generated_file_names_the_jaxrs_spec_generator_and_its_version() throws IOException {
        for (java.nio.file.Path file : generatedJava()) {
            assertThat(Files.readString(file))
                    .as(file.toString())
                    .contains("JavaJAXRSSpecServerCodegen")
                    .contains("Generator version: 7.25.0");
        }
    }

    @Test
    void only_the_api_and_model_packages_are_generated() throws Exception {
        assertThat(Class.forName("app.rekord.api.HealthApi")).isNotNull();
        assertThat(Class.forName("app.rekord.api.model.Health")).isNotNull();
        for (java.nio.file.Path file : generatedJava()) {
            assertThat(Files.readString(file))
                    .as(file.toString())
                    .containsPattern("(?m)^package app\\.rekord\\.api(\\.model)?;");
        }
    }

    @Test
    void interface_only_means_every_api_is_an_interface_without_default_bodies() throws Exception {
        for (Class<?> api : apiInterfaces()) {
            assertThat(api.isInterface()).as(api.getName()).isTrue();
            for (Method method : api.getDeclaredMethods()) {
                assertThat(Modifier.isAbstract(method.getModifiers()))
                        .as(api.getName() + "#" + method.getName())
                        .isTrue();
            }
        }
        for (java.nio.file.Path file : generatedJava()) {
            if (inApiPackage(file)) {
                assertThat(Files.readString(file))
                        .as(file.toString())
                        .containsPattern("public interface \\w+Api");
            }
        }
    }

    @Test
    void jakarta_is_used_and_javax_is_not() throws Exception {
        Path path = Class.forName("app.rekord.api.HealthApi").getAnnotation(Path.class);
        assertThat(path).isNotNull();
        assertThat(path.value()).isEqualTo("/health");
        for (java.nio.file.Path file : generatedJava()) {
            assertThat(Files.readString(file)).as(file.toString()).doesNotContain("javax.");
        }
    }

    @Test
    void return_response_false_means_operations_return_their_model() throws Exception {
        Method health = Class.forName("app.rekord.api.HealthApi").getMethod("health");
        assertThat(health.getReturnType().getName()).isEqualTo("app.rekord.api.model.Health");
        for (Class<?> api : apiInterfaces()) {
            for (Method method : api.getDeclaredMethods()) {
                assertThat(method.getReturnType().getName())
                        .as(api.getName() + "#" + method.getName())
                        .isNotEqualTo("jakarta.ws.rs.core.Response");
            }
        }
    }

    @Test
    void no_swagger_annotations() throws IOException {
        for (java.nio.file.Path file : generatedJava()) {
            assertThat(Files.readString(file)).as(file.toString()).doesNotContain("io.swagger");
        }
    }

    @Test
    void no_json_nullable() throws IOException {
        for (java.nio.file.Path file : generatedJava()) {
            assertThat(Files.readString(file))
                    .as(file.toString())
                    .doesNotContain("JsonNullable")
                    .doesNotContain("org.openapitools.jackson.nullable");
        }
    }

    @Test
    void every_spec_tag_has_an_api_interface_and_there_is_no_default_api() throws Exception {
        String spec = System.getProperty("contract.spec");
        assertThat(spec).as("contract.spec system property").isNotBlank();
        JsonNode tags = new YAMLMapper().readTree(java.nio.file.Path.of(spec).toFile()).get("tags");
        assertThat(tags).isNotNull();
        for (JsonNode tag : tags) {
            String name = tag.get("name").asText();
            String api = Character.toUpperCase(name.charAt(0)) + name.substring(1).toLowerCase(Locale.ROOT) + "Api";
            assertThat(Class.forName("app.rekord.api." + api).isInterface()).as(api).isTrue();
        }
        assertThat(generatedJava()).noneMatch(p -> p.getFileName().toString().equals("DefaultApi.java"));
    }

    @Test
    void the_generated_apis_are_exactly_the_spec_tags_with_no_stale_interface_left_over() throws Exception {
        // Given the spec's tags
        String spec = System.getProperty("contract.spec");
        assertThat(spec).as("contract.spec system property").isNotBlank();
        List<String> expected = new ArrayList<>();
        for (JsonNode tag : new YAMLMapper().readTree(java.nio.file.Path.of(spec).toFile()).get("tags")) {
            String name = tag.get("name").asText();
            expected.add(Character.toUpperCase(name.charAt(0)) + name.substring(1).toLowerCase(Locale.ROOT) + "Api");
        }

        // Then the output holds one interface per tag and nothing an earlier spec left behind
        List<String> generated = generatedJava().stream()
                .filter(GeneratedContractTest::inApiPackage)
                .map(p -> p.getFileName().toString().replace(".java", ""))
                .toList();
        assertThat(generated).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    void api_and_model_tests_are_not_generated() throws IOException {
        assertThat(generatedFiles()).isNotEmpty();
        assertThat(GENERATED.resolve("src/test")).doesNotExist();
        assertThat(generatedFiles()).noneMatch(p -> p.getFileName().toString().endsWith("Test.java"));
    }

    @Test
    void dates_use_java_time_not_joda_or_util_date() throws Exception {
        assertThat(Class.forName("app.rekord.api.model.Wedding")
                        .getMethod("getWeddingDate")
                        .getReturnType())
                .isEqualTo(java.time.LocalDate.class);
        assertThat(Class.forName("app.rekord.api.model.PortalLink")
                        .getMethod("getExpiresAt")
                        .getReturnType())
                .isEqualTo(java.time.OffsetDateTime.class);
        for (java.nio.file.Path file : generatedJava()) {
            assertThat(Files.readString(file))
                    .as(file.toString())
                    .doesNotContain("org.joda")
                    .doesNotContain("java.util.Date");
        }
    }

    @Test
    void the_generated_output_is_under_the_edit_guard_and_compiled_into_the_adapter() throws Exception {
        java.nio.file.Path health = generatedJava().stream()
                .filter(p -> p.getFileName().toString().equals("HealthApi.java"))
                .findFirst()
                .orElseThrow();
        String repoRelative = REPO_ROOT.relativize(health).toString().replace('\\', '/');
        String moduleRelative =
                REPO_ROOT.resolve("rekord-adapter").relativize(health).toString().replace('\\', '/');
        assertThat(bashGlob(REPO_GLOB, repoRelative)).as(repoRelative).isTrue();
        assertThat(bashGlob(MODULE_GLOB, moduleRelative)).as(moduleRelative).isTrue();

        String location = Class.forName("app.rekord.api.HealthApi")
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toString()
                .replace('\\', '/');
        assertThat(location).contains("rekord-adapter/build/");
    }
}
