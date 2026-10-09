---
runtime: lazy
generated-by: pattern-scanner
source: partner-gateway/src/main/java/com/acme/shop/partner/config/WebServiceConfig.java
serves: [bean-config-di, soap-cxf-gateway]
---
<!-- AI_DISCLAIMER v1.0 -->
# Exemplar — `partner-gateway/.../config/WebServiceConfig.java` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## What this artifact is
One of 67 `@Configuration` classes in the repo; this one wires three generated SOAP port clients
(`RegistryPortType`, `AttributeServicePortType`, `CustomerServicePortType`) via a project-local
`WSFactory` helper (`webservice-config` module) — the same shape used for every CXF/SOAP gateway.

## Named `@Configuration` with `@Bean` factory methods {#bean-config-di}
**Serves:** [`bean-config-di`](../bean-config-di.md)

`@Configuration("partnerWebServiceConfig")` — an explicit bean name (convention here: qualify
config classes with a name when multiple configuration classes could otherwise collide/shadow).
Constructor injection for `@Value`-sourced endpoint URLs plus shared collaborators (`WSFactory`,
CXF `Bus`); each `@Bean` method builds one SOAP port client from a WSDL path plus a security
interceptor supplied as a method parameter.

## Centralized `JaxWsProxyFactoryBean` wiring {#soap-cxf-gateway}
**Serves:** [`soap-cxf-gateway`](../soap-cxf-gateway.md)

This class never calls `JaxWsProxyFactoryBean` itself — the actual CXF proxy construction is
centralized in `webservice-config/src/main/java/com/acme/shop/webservice/config/ws/WSFactory.java`,
the single place in the repo that touches `JaxWsProxyFactoryBean`. Every SOAP-client
`@Configuration` calls `WSFactory` with a WSDL path plus service interface and gets back a
configured port client.

### Source (pseudonymized)
```java
package com.acme.shop.partner.config;

import com.acme.partner.auth.protocol.v1.AttributeServicePortType;
import com.acme.partner.customer.protocol.v1.CustomerServicePortType;
import com.acme.partner.registry.protocol.v2.RegistryPortType;
import com.acme.shop.webservice.config.ws.WSFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("partnerWebServiceConfig")
@Slf4j
public class WebServiceConfig {
    private final String registryBaseUrl;
    private final String partnerAttributeAuthorityUrl;
    private final String customerServiceUrl;
    private final WSFactory wsFactory;
    private final Bus cxfBus;

    public WebServiceConfig(@Value("${properties.endpoint.partner.registry}") String registryBaseUrl,
                            @Value("${properties.endpoint.partner.attribute-authority}") String partnerAttributeAuthorityUrl,
                            @Value("${properties.endpoint.partner.customer-service}") String customerServiceUrl,
                            WSFactory wsFactory,
                            Bus globalWsBus) {
        this.registryBaseUrl = registryBaseUrl;
        this.partnerAttributeAuthorityUrl = partnerAttributeAuthorityUrl;
        this.customerServiceUrl = customerServiceUrl;
        this.wsFactory = wsFactory;
        this.cxfBus = globalWsBus;
    }

    @Bean
    public RegistryPortType registryPortType(Interceptor<SoapMessage> sessionTokenSecurityOutInterceptor) {
        return (RegistryPortType) wsFactory.createProxyFactoryBean(registryBaseUrl,
                "/wsdls/RegistryWebService-v2.wsdl",
                "{urn:com:acme:shop:partner:registry:protocol:v2}RegistryService",
                RegistryPortType.class,
                "{urn:com:acme:shop:partner:registry:protocol:v2}RegistryPort",
                sessionTokenSecurityOutInterceptor
        ).create();
    }

    @Bean
    public AttributeServicePortType partnerAttributeAuthorityPortType(WSS4JOutInterceptor keyStoreSecurityOutInterceptor) {
        return (AttributeServicePortType) wsFactory.createProxyFactoryBean(partnerAttributeAuthorityUrl,
                "/wsdls/PartnerAttributeAuthority-v1.wsdl",
                "{urn:com:acme:shop:partner:auth:protocol:v1}AttributeService",
                AttributeServicePortType.class,
                "{urn:com:acme:shop:partner:auth:protocol:v1}AttributeServicePort",
                keyStoreSecurityOutInterceptor
        ).create();
    }

    @Bean
    public CustomerServicePortType customerServicePortType(Interceptor<SoapMessage> sessionTokenSecurityOutInterceptor) {
        return (CustomerServicePortType) wsFactory.createProxyFactoryBean(customerServiceUrl,
                "/wsdls/partner-customer-service-v1.wsdl",
                "{urn:com:acme:shop:partner:customer:protocol:v1}CustomerService",
                CustomerServicePortType.class,
                "{urn:com:acme:shop:partner:customer:protocol:v1}CustomerServicePort",
                sessionTokenSecurityOutInterceptor
        ).create();
    }
}
```

### Edge cases
Two modules define a `WebServiceConfig`:
```java
@Configuration("partnerWebServiceConfig")
class WebServiceConfig { ... }
```
Expected: the explicit bean name avoids collisions in the application context.

Absent interceptor bean:
```java
contextRunner.run(context -> context.getBean("sessionTokenSecurityOutInterceptor"));
```
Expected: startup fails immediately instead of creating an unsecured SOAP proxy.

Adding a new SOAP integration:
```java
@Bean
public SupplierServicePortType supplierServicePortType(Interceptor<SoapMessage> sessionTokenSecurityOutInterceptor) {
    return (SupplierServicePortType) wsFactory.createProxyFactoryBean(...).create();
}
```
Expected: the new client still goes through `WSFactory`; no local `JaxWsProxyFactoryBean` logic is duplicated.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg '@Configuration'` (70 matches / 67 files / 24 modules), `rg '@Bean'` (90 matches / 43 files / 24 modules)
- Scanned at: `abc1234` · tool/query: `rg 'JaxWsProxyFactoryBean'` (2 files: `WSFactory.java` + `WSFactoryTest.java`, 1 module — re-verified)
