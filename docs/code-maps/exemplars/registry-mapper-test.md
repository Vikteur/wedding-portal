---
runtime: lazy
generated-by: pattern-scanner
source: partner-gateway/src/test/java/com/acme/shop/partner/gateway/registry/RegistryMapperTest.java
serves: [domain-to-dto-mapper-tests]
---
<!-- AI_DISCLAIMER v1.0 -->
# Exemplar — `partner-gateway/.../registry/RegistryMapperTest.java` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## What this artifact is
The JUnit 5 test for `RegistryMapper` (see [registry-mapper](registry-mapper.md#domain-dto-mapper)).

## Parameterized-by-hand mapper test {#domain-to-dto-mapper-tests}
**Serves:** [`domain-to-dto-mapper-tests`](../domain-to-dto-mapper-tests.md)

`@ExtendWith(MockitoExtension.class)` + `@InjectMocks`, manual `@BeforeEach init()` call to run the
mapper's `@PostConstruct` (Mockito does not invoke it). One `@Test` per edge case named after the
input shape rather than a single `@ParameterizedTest` — local convention here is descriptive method
names over `@MethodSource` or `@CsvSource`, with a private `getPartnerRecords()` helper for fixture
construction.

### Source (pseudonymized)
```java
package com.acme.shop.partner.gateway.registry;

import com.acme.partner.standards.catalog.id.v1.IDPARTNER;
import com.acme.partner.standards.catalog.schema.v1.PartnerRecordType;
import com.acme.shop.partner.dto.Registry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RegistryMapperTest {

    @InjectMocks
    private RegistryMapper registryMapper;

    @BeforeEach
    public void init() {
        registryMapper.init();
    }

    @Test
    void testMapRegistries() {
        final List<Registry> result = registryMapper.mapRegistries(getPartnerRecords());

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo(Registry.LOYALTY);
        assertThat(result.get(1)).isEqualTo(Registry.PAYMENTS);
    }

    @Test
    void testMapRegistriesEmptyId() {
        final List<PartnerRecordType> input = getPartnerRecords();

        input.get(0).getIds().clear();

        final List<Registry> result = registryMapper.mapRegistries(input);
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(Registry.PAYMENTS);
    }

    @Test
    void testMapRegistriesNull() {
        final List<PartnerRecordType> input = getPartnerRecords();

        input.get(0).getIds().remove(0);
        input.get(0).getIds().add(null);

        final List<Registry> result = registryMapper.mapRegistries(input);
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(Registry.PAYMENTS);
    }

    @Test
    void testMapRegistriesValueNull() {
        final List<PartnerRecordType> input = getPartnerRecords();

        input.get(0).getIds().get(0).setValue(null);

        final List<Registry> result = registryMapper.mapRegistries(input);
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(Registry.PAYMENTS);
    }

    @Test
    void testMapRegistriesUnknownId() {
        final List<PartnerRecordType> input = getPartnerRecords();

        final IDPARTNER unknownRegistry = new IDPARTNER();
        unknownRegistry.setValue("unknown-registry");

        input.get(0).getIds().clear();
        input.get(0).getIds().add(unknownRegistry);

        final List<Registry> result = registryMapper.mapRegistries(input);
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(Registry.PAYMENTS);
    }

    private List<PartnerRecordType> getPartnerRecords() {
        final PartnerRecordType registryType1 = new PartnerRecordType();
        final PartnerRecordType registryType2 = new PartnerRecordType();
        final IDPARTNER registryId1 = new IDPARTNER();
        final IDPARTNER registryId2 = new IDPARTNER();

        registryId1.setValue("1000000002");
        registryId2.setValue("1000000003");

        registryType1.getIds().add(registryId1);
        registryType2.getIds().add(registryId2);

        return Arrays.asList(registryType1, registryType2);
    }
}
```

### Edge cases
Happy path with two known ids:
```java
final List<Registry> result = registryMapper.mapRegistries(getPartnerRecords());
assertThat(result.get(0)).isEqualTo(Registry.LOYALTY);
```
Expected: ordering from the input list is preserved in the mapped output.

Empty id list:
```java
input.get(0).getIds().clear();
```
Expected: the mapper drops only the broken entry and still returns the remaining mapped registry.

Unknown id value:
```java
unknownRegistry.setValue("unknown-registry");
```
Expected: the result keeps only the known mapped entries and never throws.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg 'class[[:space:]]+[A-Za-z]*MapperTest' --glob '*.java'`
