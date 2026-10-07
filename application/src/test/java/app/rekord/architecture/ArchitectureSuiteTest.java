package app.rekord.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ArchitectureSuiteTest {

    private static final Pattern RULE_ID = Pattern.compile("\\bA(\\d+)\\b");

    @Test
    void the_suite_checks_a1_to_a14_exactly_once() {
        List<String> ids = new ArrayList<>();
        for (Field field : suiteFields()) {
            Set<String> idsOfThisRule = new TreeSet<>();
            Matcher matcher = RULE_ID.matcher(rule(field).getDescription());
            while (matcher.find()) {
                idsOfThisRule.add("A" + matcher.group(1));
            }
            assertThat(idsOfThisRule).as("rule ids in %s", field.getName()).hasSize(1);
            ids.addAll(idsOfThisRule);
        }

        assertThat(ids)
                .containsExactlyInAnyOrder("A1", "A2", "A3", "A4", "A5", "A6", "A7", "A8", "A9", "A10", "A11", "A12", "A13", "A14");
    }

    @Test
    void every_suite_rule_is_frozen() {
        assertThat(suiteFields()).isNotEmpty();
        for (Field field : suiteFields()) {
            assertThat(rule(field)).as("%s.%s", field.getDeclaringClass().getSimpleName(), field.getName())
                    .isInstanceOf(FreezingArchRule.class);
        }
    }

    @Test
    void every_suite_class_runs_in_the_test_task() {
        List<Class<?>> suite = suiteClasses();

        assertThat(suite).hasSize(5);
        for (Class<?> suiteClass : suite) {
            assertThat(suiteClass.getSimpleName()).as("name of the suite class").doesNotEndWith("IT");
            assertThat(suiteClass.isAnnotationPresent(Tag.class)).as("%s carries @Tag", suiteClass).isFalse();
            assertThat(suiteClass.isAnnotationPresent(org.junit.jupiter.api.Tags.class))
                    .as("%s carries @Tags", suiteClass)
                    .isFalse();
        }
    }

    @Test
    void every_suite_class_analyses_the_production_classes() {
        List<Class<?>> suite = suiteClasses();

        assertThat(suite).hasSize(5);
        for (Class<?> suiteClass : suite) {
            AnalyzeClasses analyze = suiteClass.getAnnotation(AnalyzeClasses.class);
            assertThat(analyze.locations()).as("locations of %s", suiteClass).containsExactly(ProductionClasses.class);
            assertThat(analyze.packages()).as("packages of %s", suiteClass).isEmpty();
        }
    }

    /** Every class of this package carrying {@code @AnalyzeClasses}, found by reading the compiled test classes. */
    private static List<Class<?>> suiteClasses() {
        try {
            Path directory = Path.of(ArchitectureSuiteTest.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI())
                    .resolve("app/rekord/architecture");
            List<Class<?>> suite = new ArrayList<>();
            try (Stream<Path> files = Files.list(directory)) {
                for (Path file : files.sorted().toList()) {
                    String name = file.getFileName().toString();
                    if (name.endsWith(".class") && !name.contains("$")) {
                        Class<?> candidate = Class.forName("app.rekord.architecture." + name.replace(".class", ""));
                        if (candidate.isAnnotationPresent(AnalyzeClasses.class)) {
                            suite.add(candidate);
                        }
                    }
                }
            }
            return suite;
        } catch (IOException | URISyntaxException | ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<Field> suiteFields() {
        List<Field> fields = new ArrayList<>();
        for (Class<?> suiteClass : suiteClasses()) {
            for (Field field : suiteClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(ArchTest.class)) {
                    fields.add(field);
                }
            }
        }
        return fields;
    }

    private static ArchRule rule(Field field) {
        try {
            field.setAccessible(true);
            return (ArchRule) field.get(null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
