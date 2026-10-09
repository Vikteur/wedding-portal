---
runtime: lazy
generated-by: pattern-scanner
source: fulfilment-gateway/src/test/java/com/acme/shop/fulfilment/gateway/FulfilmentFhirGatewayTest.java
serves: [wiremock-gateway-stubs]
---
<!-- AI_DISCLAIMER v1.0 -->
# Exemplar — `fulfilment-gateway/.../FulfilmentFhirGatewayTest.java` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## What this artifact is
An integration test for [`FulfilmentFhirGateway`](fulfilment-fhir-gateway.md#fhir-hapi-gateway) that stubs
the upstream FHIR server with WireMock rather than mocking the HAPI client.

## WireMock-stubbed gateway test {#wiremock-gateway-stubs}
**Serves:** [`wiremock-gateway-stubs`](../wiremock-gateway-stubs.md)

The test extends a shared `AbstractFulfilmentGatewayTest` base, keeps one shared `WireMockServer`
because nested tests share the Spring context, wires the dynamic port with `@DynamicPropertySource`,
and attaches a Logback `ListAppender` when it needs to assert that transport logging redacts
sensitive headers.

### Source (pseudonymized)
> Original file is >300 lines; the source below keeps the pattern-relevant members: shared server
> wiring, log assertions, a paginated shipment test, and the reusable WireMock helper methods.

```java
package com.acme.shop.fulfilment.gateway;

import com.acme.shop.fulfilment.exception.FulfilmentException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import org.junit.jupiter.api.*;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FulfilmentFhirGatewayTest extends AbstractFulfilmentGatewayTest {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String STOREFRONT = "storefront";
    private static final String AUTHENTICATION_TOKEN = "authentication_token";
    private static final String NID = "00000000000"; // deliberately invalid for edge-case coverage
    private static final String CUSTOMER_ORDER_RECORD = "CUSTOMER_ORDER_RECORD";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String RESTRICTION_SEARCH_ENDPOINT = "/Restriction/_search";
    private static final String SHIPMENT_SEARCH_ENDPOINT = "/Shipment/_search";

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;
    private Level originalLogLevel;

    static WireMockServer wiremock = new WireMockServer(wireMockConfig().dynamicPort());

    @BeforeAll
    static void startWireMock() {
        wiremock.start();
    }

    @BeforeEach
    void setUpLogging() {
        logger = (Logger) LoggerFactory.getLogger(FhirClientErrorLoggingInterceptor.class);
        originalLogLevel = logger.getLevel();
        logger.setAdditive(false);

        appender = new ListAppender<>();
        appender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        appender.start();

        logger.addAppender(appender);
        wiremock.resetAll();
    }

    @AfterEach
    void tearDownLogging() {
        if (logger != null && appender != null) {
            logger.detachAppender(appender);
            appender.stop();
            logger.setLevel(originalLogLevel);
            logger.setAdditive(true);
        }
    }

    @AfterAll
    static void stopWireMock() {
        wiremock.stop();
    }

    @DynamicPropertySource
    static void registerEndpointFulfilment(DynamicPropertyRegistry registry) {
        registry.add("properties.endpoint.fulfilment", () -> "host:port");
        registry.add("properties.fhir.fulfilment.endpoint",
                () -> "http://localhost:%s".formatted(wiremock.port()));
    }

    @Autowired
    private FulfilmentFhirGateway fulfilmentFhirGateway;

    @TestConfiguration
    static class FhirContextTestConfiguration {
        @Bean
        @Primary
        public FhirContext fulfilmentFhirContext() {
            var ctx = FhirContext.forR4();
            ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
            return ctx;
        }
    }

    @Nested
    class FhirLoggingTests {
        private static final String ERROR_FHIR_FAILURE_BODY = "{\"error\":\"FHIR failure\"}";

        @Test
        void givenFhirServerError_whenRequestFails_thenLogsRequestAndResponseWithoutSensitiveData() {
            logger.setLevel(Level.ERROR);

            wiremock.stubFor(baseSearchStub(RESTRICTION_SEARCH_ENDPOINT, CUSTOMER_ORDER_RECORD)
                    .willReturn(jsonResponse(500).withBody(ERROR_FHIR_FAILURE_BODY)));

            assertThatThrownBy(() -> fulfilmentFhirGateway.getRestrictions(
                    AUTHENTICATION_TOKEN,
                    NID,
                    STOREFRONT)).isInstanceOf(FulfilmentException.class);

            final String messages = getFormattedLoggingMessagesStream().collect(joining("\n"));

            assertThat(messages)
                    .contains("HTTP request failed")
                    .contains("Request headers:")
                    .contains("Response body")
                    .contains(ERROR_FHIR_FAILURE_BODY)
                    .doesNotContain(authBearer());
        }

        @Test
        void givenLargeFhirErrorResponse_whenRequestFails_thenResponseBodyIsTruncated() {
            logger.setLevel(Level.ERROR);

            final String hugeBody = "x".repeat(50_000);

            wiremock.stubFor(baseSearchStub(RESTRICTION_SEARCH_ENDPOINT, CUSTOMER_ORDER_RECORD)
                    .willReturn(jsonResponse(500).withBody(hugeBody)));

            assertThatThrownBy(() -> fulfilmentFhirGateway.getRestrictions(
                    AUTHENTICATION_TOKEN,
                    NID,
                    STOREFRONT)).isInstanceOf(FulfilmentException.class);

            final String responseLog = getFormattedLoggingMessagesStream()
                    .filter(message -> message.contains("Response body"))
                    .findFirst()
                    .orElseThrow();

            assertThat(responseLog).doesNotContain(hugeBody);
        }

        private Stream<String> getFormattedLoggingMessagesStream() {
            return appender.list.stream().map(ILoggingEvent::getFormattedMessage);
        }
    }

    @Nested
    class ShipmentTests {

        @Test
        void givenPaginatedResponse_whenSearchShipments_thenReturnAllShipmentsFromAllPages() {
            final String page1Response = withWiremockPort(resourceContent("post_search_shipment_page1_response.json"));
            final String page2Response = withWiremockPort(resourceContent("post_search_shipment_page2_response.json"));

            wiremock.stubFor(baseSearchStub(SHIPMENT_SEARCH_ENDPOINT, CUSTOMER_ORDER_RECORD)
                    .willReturn(jsonOkResponse(page1Response)));
            wiremock.stubFor(get(urlMatching(".*_getpages=page-1-bundle.*"))
                    .willReturn(jsonOkResponse(page2Response)));

            var result = fulfilmentFhirGateway.getShipments(AUTHENTICATION_TOKEN, NID, STOREFRONT);

            assertThat(result).hasSize(4);
            wiremock.verify(1, postRequestedFor(urlEqualTo(SHIPMENT_SEARCH_ENDPOINT)));
            wiremock.verify(1, getRequestedFor(urlMatching(".*_getpages=page-1-bundle.*")));
        }

        private String withWiremockPort(final String content) {
            return content.replace(
                    "http://localhost",
                    "http://localhost:%s".formatted(wiremock.port()));
        }
    }

    private MappingBuilder baseSearchStub(final String url, final String securityRecord) {
        return post(url)
                .withHeader(AUTHORIZATION_HEADER, equalTo(authBearer()))
                .withFormParam("customer.identifier", equalTo(customerIdentifier()))
                .withFormParam("_security", equalTo(fulfilmentLink(securityRecord)));
    }

    private static String customerIdentifier() {
        return "https://example.invalid/fhir/core/NamingSystem/nid|urn:com:acme:shop:pseudo:v1:" + NID;
    }

    private ResponseDefinitionBuilder jsonResponse(final int status) {
        return aResponse()
                .withStatus(status)
                .withHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    }

    private ResponseDefinitionBuilder jsonOkResponse(final String bodyString) {
        return ok()
                .withHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody(bodyString);
    }

    private static String fulfilmentLink(final String code) {
        return "https://security.example.invalid|" + code;
    }

    private String resourceContent(final String name) {
        return new String(resourceAsBytes(name), StandardCharsets.UTF_8);
    }

    private byte[] resourceAsBytes(final String name) {
        try (final InputStream is = Objects.requireNonNull(
                getClass().getResourceAsStream(name),
                "Absent test resource: " + name)) {
            return is.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read test resource: " + name, e);
        }
    }

    private static String authBearer() {
        return "******";
    }
}
```

### Edge cases
404 shipment search:
```java
wiremock.stubFor(baseSearchStub("/Shipment/_search", CUSTOMER_ORDER_RECORD)
        .willReturn(jsonResponse(404)));
```
Expected: the gateway test expects an empty list rather than a thrown exception.

Large upstream failure body:
```java
wiremock.stubFor(baseSearchStub("/Restriction/_search", CUSTOMER_ORDER_RECORD)
        .willReturn(jsonResponse(500).withBody("x".repeat(50_000))));
```
Expected: the logging assertion proves the response body is truncated and the token is redacted.

Second page unavailable:
```java
wiremock.stubFor(get(urlMatching(".*_getpages=page-1-bundle.*"))
        .willReturn(jsonResponse(500)));
```
Expected: the test fails on the pagination seam instead of silently returning page one only.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg 'WireMock' --glob '*.java'` (77 matches / 14 files / 4 modules)
