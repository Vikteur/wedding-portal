---
runtime: lazy
source: partner-gateway/src/main/java/com/acme/shop/partner/config/WebServiceConfig.java
serves: [bean-config-di]
kind: worked-example
---

<!-- AI_DISCLAIMER v1.0 -->
# Exemplar — `partner-gateway/.../config/WebServiceConfig.java` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## What this artifact is
One of 67 CDI config classes in the repo; this one supplies the WS-Security interceptors for three
generated SOAP port clients (`RegistryPortType`, `AttributeServicePortType`, `CustomerServicePortType`)
via a project-local `WSFactory` helper (`webservice-config` module). The port clients themselves are
declared in `application.properties` and built by the Quarkus CXF extension (`io.quarkiverse.cxf:quarkus-cxf`,
which also manages the CXF `Bus`) — the same shape used for every CXF/SOAP gateway.

## `@ApplicationScoped` config class with `@Produces` methods {#bean-config-di}
**Serves:** [`bean-config-di`](../bean-config-di.md)

An `@ApplicationScoped` config class with constructor injection of the shared `WSFactory` collaborator;
each `@Produces` method returns one security interceptor, disambiguated with a module-prefixed
`@Named("partner...")` (convention here: CDI resolves beans by type + qualifier, so two `WebServiceConfig`
classes in different packages never clash, but two producers with the same `@Named` value would).
The clients reference those beans by name (`#partnerSessionTokenOutInterceptor`) in config and are
injected into gateways with `@CXFClient("<client>")`; no producer builds a proxy by hand.

### Source (pseudonymized)
```java
package com.acme.shop.partner.config;

import com.acme.shop.webservice.config.ws.WSFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;

@ApplicationScoped
@Slf4j
public class WebServiceConfig {
    private final WSFactory wsFactory;

    public WebServiceConfig(WSFactory wsFactory) {
        this.wsFactory = wsFactory;
    }

    @Produces
    @Singleton
    @Named("partnerSessionTokenOutInterceptor")
    Interceptor<SoapMessage> partnerSessionTokenOutInterceptor() {
        return wsFactory.createSessionTokenOutInterceptor();
    }

    @Produces
    @Singleton
    @Named("partnerKeyStoreOutInterceptor")
    WSS4JOutInterceptor partnerKeyStoreOutInterceptor() {
        return wsFactory.createKeyStoreOutInterceptor();
    }
}
```

```properties
# partner-gateway/src/main/resources/application.properties
quarkus.cxf.client."registry".wsdl=wsdls/RegistryWebService-v2.wsdl
quarkus.cxf.client."registry".client-endpoint-url=${properties.endpoint.partner.registry}
quarkus.cxf.client."registry".service-interface=com.acme.partner.registry.protocol.v2.RegistryPortType
quarkus.cxf.client."registry".endpoint-namespace=urn:com:acme:shop:partner:registry:protocol:v2
quarkus.cxf.client."registry".endpoint-name=RegistryPort
quarkus.cxf.client."registry".out-interceptors=#partnerSessionTokenOutInterceptor

quarkus.cxf.client."attribute-authority".wsdl=wsdls/PartnerAttributeAuthority-v1.wsdl
quarkus.cxf.client."attribute-authority".client-endpoint-url=${properties.endpoint.partner.attribute-authority}
quarkus.cxf.client."attribute-authority".service-interface=com.acme.partner.auth.protocol.v1.AttributeServicePortType
quarkus.cxf.client."attribute-authority".endpoint-namespace=urn:com:acme:shop:partner:auth:protocol:v1
quarkus.cxf.client."attribute-authority".endpoint-name=AttributeServicePort
quarkus.cxf.client."attribute-authority".out-interceptors=#partnerKeyStoreOutInterceptor

quarkus.cxf.client."customer-service".wsdl=wsdls/partner-customer-service-v1.wsdl
quarkus.cxf.client."customer-service".client-endpoint-url=${properties.endpoint.partner.customer-service}
quarkus.cxf.client."customer-service".service-interface=com.acme.partner.customer.protocol.v1.CustomerServicePortType
quarkus.cxf.client."customer-service".endpoint-namespace=urn:com:acme:shop:partner:customer:protocol:v1
quarkus.cxf.client."customer-service".endpoint-name=CustomerServicePort
quarkus.cxf.client."customer-service".out-interceptors=#partnerSessionTokenOutInterceptor
```

### Edge cases
Two modules define a `WebServiceConfig`:
```java
@ApplicationScoped
class WebServiceConfig { ... }   // com.acme.shop.partner.config and com.acme.shop.fulfilment.config
```
Expected: no collision — CDI identifies beans by type and qualifier, not class simple name; only the
`@Named` producer values must stay unique, hence the module prefix.

Absent interceptor bean:
```properties
quarkus.cxf.client."registry".out-interceptors=#partnerSessionTokenOutInterceptor
# ...but no @Produces @Named("partnerSessionTokenOutInterceptor") exists
```
Expected: startup fails immediately instead of creating an unsecured SOAP client.

Adding a new SOAP integration:
```properties
quarkus.cxf.client."supplier-service".wsdl=wsdls/SupplierService-v1.wsdl
quarkus.cxf.client."supplier-service".client-endpoint-url=${properties.endpoint.partner.supplier-service}
quarkus.cxf.client."supplier-service".service-interface=com.acme.partner.supplier.protocol.v1.SupplierServicePortType
quarkus.cxf.client."supplier-service".out-interceptors=#partnerSessionTokenOutInterceptor
```
```java
@CXFClient("supplier-service") SupplierServicePortType supplierServicePortType
```
Expected: the new client is declared in config and injected with `@CXFClient`; no `@Produces` method
hand-builds a `JaxWsProxyFactoryBean`, and interceptor construction stays in `WSFactory`.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg '@ApplicationScoped' --glob '*Config.java'` (70 matches / 67 files / 24 modules), `rg '@Produces'` (90 matches / 43 files / 24 modules)
