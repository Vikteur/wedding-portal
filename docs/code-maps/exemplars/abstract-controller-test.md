---
runtime: lazy
source: account-adapter/src/test/java/com/acme/shop/account/adapter/controller/AbstractControllerTest.java
serves: [spring-boot-slice-tests]
kind: worked-example
---
> **Worked example, not project facts.** The structure is the pattern to follow: the layers, the
> classes and their roles, the call order, the tests. Every name is a pseudonymized placeholder: the project, the
> packages, the classes, the methods, the parameters and the fields. Map each one to this repo's own name, and never
> copy a placeholder into code.

<!-- AI_DISCLAIMER v1.0 -->
# Exemplar — `account-adapter/.../controller/AbstractControllerTest.java` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## What this artifact is
A shared abstract base for the `@WebMvcTest` slice tests of every controller in `account-adapter`,
so each concrete `*ControllerTest` only adds its own request setup and `MockMvc` assertions.

## `@WebMvcTest` base with scoped component scan {#spring-boot-slice-tests}
**Serves:** [`spring-boot-slice-tests`](../spring-boot-slice-tests.md)

`@WebMvcTest` plus a narrowed `@ComponentScan` loads only the controller layer, not the whole
adapter package, keeping the slice fast. `@ContextConfiguration(classes =
AbstractControllerTest.TestConfig.class)` supplies a minimal test config. Every collaborator the
controllers need (gateways, mappers, use cases, repositories) is declared once here as
`@MockitoBean` and inherited, instead of being re-declared per concrete test class.

### Source (pseudonymized)
```java
package com.acme.shop.account.adapter.controller;

import com.acme.shop.account.adapter.gateway.DelegationMapper;
import com.acme.shop.account.adapter.gateway.DelegationPartnerGateway;
import com.acme.shop.account.adapter.repository.TopicDelegationJpaRepository;
import com.acme.shop.account.domain.TopicDelegationFactory;
import com.acme.shop.account.usecase.CreateCustomerWithMockedSalesRegionUseCase;
import com.acme.shop.account.usecase.CreateCustomerWithMockedSegmentUseCase;
import com.acme.shop.account.usecase.DeleteMockedSalesRegionUseCase;
import com.acme.shop.account.usecase.DeleteMockedSegmentUseCase;
import com.acme.shop.account.usecase.GetCustomerInformationUseCase;
import com.acme.shop.account.usecase.GetDelegationsUseCase;
import com.acme.shop.account.usecase.GetMockedCustomersUseCase;
import com.acme.shop.account.usecase.GetTopicDelegationUseCase;
import com.acme.shop.account.usecase.RecordLastVisitCustomerUseCase;
import com.acme.shop.account.usecase.UpdateTopicDelegationUseCase;
import com.acme.shop.account.usecase.VerifyDelegationForTopicAccessUseCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@ComponentScan(
        basePackages = "com.acme.shop.account.adapter.controller",
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = ".*Controller"
        )
)
@ContextConfiguration(classes = AbstractControllerTest.TestConfig.class)
public abstract class AbstractControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @MockitoBean
    protected DelegationPartnerGateway delegationPartnerGateway;

    @MockitoBean
    protected DelegationMapper delegationMapper;

    @MockitoBean
    protected GetDelegationsUseCase getDelegationsUseCase;

    @MockitoBean
    protected UpdateTopicDelegationUseCase updateTopicDelegationUseCase;

    @MockitoBean
    protected TopicDelegationJpaRepository topicDelegationJpaRepository;

    @MockitoBean
    protected TopicDelegationFactory topicDelegationFactory;

    @MockitoBean
    protected GetTopicDelegationUseCase getTopicDelegationUseCase;

    @MockitoBean
    protected VerifyDelegationForTopicAccessUseCase verifyDelegationForTopicAccessUseCase;

    @MockitoBean
    protected GetCustomerInformationUseCase getCustomerInformationUseCase;

    @MockitoBean
    protected RecordLastVisitCustomerUseCase recordLastVisitCustomerUseCase;

    @MockitoBean
    protected CreateCustomerWithMockedSalesRegionUseCase createCustomerWithMockedSalesRegionUseCase;

    @MockitoBean
    protected DeleteMockedSalesRegionUseCase deleteMockedSalesRegionUseCase;

    @MockitoBean
    protected GetMockedCustomersUseCase getMockedCustomersUseCase;

    @MockitoBean
    protected CreateCustomerWithMockedSegmentUseCase createCustomerWithMockedSegmentUseCase;

    @MockitoBean
    protected DeleteMockedSegmentUseCase deleteMockedSegmentUseCase;

    @Configuration
    @ComponentScan(basePackages = "com.acme.shop.account.adapter.controller")
    static class TestConfig {
    }
}
```

### Edge cases
Controller package contains helpers next to controllers:
```java
includeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*Controller")
```
Expected: only controller classes are loaded into the slice; helpers stay outside unless imported explicitly.

Concrete test forgets to mock a shared collaborator:
```java
@MockitoBean
protected VerifyDelegationForTopicAccessUseCase verifyDelegationForTopicAccessUseCase;
```
Expected: the abstract base already provides the shared collaborator graph, so subclasses stay focused on endpoint behavior.

Subclass only needs request/response assertions:
```java
@Autowired
protected MockMvc mockMvc;
```
Expected: inherited `MockMvc` and common mocks keep controller tests concise.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg '@WebMvcTest'`, `rg '@DataJpaTest'`, `rg '@SpringBootTest'`
