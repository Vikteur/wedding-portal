package app.rekord.application.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.rekord.domain.shared.error.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Binds the domain {@link ErrorCode} to the ErrorCode enum of the pinned contract (PIN-20-0205). */
class ErrorCodeContractTest {

    private static final List<String> PYTHON_ONLY = List.of("BAD_DATE", "BAD_KIND", "BAD_POSITION",
            "BAD_START_PREF", "EMPTY_NAMES", "NOTHING_MATCHED", "NOTHING_MISSING");

    private static Set<String> contractEnum() throws IOException {
        String spec = System.getProperty("contract.spec");
        assertThat(spec).as("contract.spec system property").isNotBlank();
        JsonNode values = new YAMLMapper().readTree(java.nio.file.Path.of(spec).toFile())
                .path("components").path("schemas").path("ErrorCode").path("enum");
        Set<String> names = new LinkedHashSet<>();
        values.forEach(v -> names.add(v.asText()));
        assertThat(names).as("components.schemas.ErrorCode.enum").isNotEmpty();
        return names;
    }

    static void assertEveryCodeInContract(Set<String> contractEnum) {
        Set<String> missing = Arrays.stream(ErrorCode.values()).map(Enum::name)
                .filter(name -> !contractEnum.contains(name))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        assertThat(missing).as("ErrorCode constants missing from the contract enum").isEmpty();
    }

    @Test
    void every_domain_code_exists_in_the_contract_enum() throws IOException {
        assertEveryCodeInContract(contractEnum());
    }

    @Test
    void a_code_missing_from_the_contract_enum_fails_the_check() throws IOException {
        Set<String> withoutBadLink = new LinkedHashSet<>(contractEnum());
        assertThat(withoutBadLink.remove("BAD_LINK")).isTrue();
        assertThatThrownBy(() -> assertEveryCodeInContract(withoutBadLink))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("BAD_LINK");
    }

    @Test
    void the_python_only_codes_are_not_in_the_contract_either() throws IOException {
        assertThat(contractEnum()).doesNotContainAnyElementsOf(PYTHON_ONLY);
    }
}
