---
runtime: lazy
generated-by: pattern-scanner
source: partner-gateway/src/main/java/com/acme/shop/partner/gateway/token/TokenExchangeGateway.java
serves: [identifier-pseudonymization]
kind: worked-example
---
> **Worked example, not project facts.** The structure is the pattern to follow: the layers, the
> classes and their roles, the call order, the tests. Every name is a pseudonymized placeholder: the project, the
> packages, the classes, the methods, the parameters and the fields. Map each one to this repo's own name, and never
> copy a placeholder into code.

<!-- AI_DISCLAIMER v1.0 -->
# Exemplar — `partner-gateway/.../token/TokenExchangeGateway.java` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## What this artifact is
The OAuth2 token-exchange step that runs *before* pseudonymization: it swaps the customer's own access
token for a client-scoped token (`urn:ietf:params:oauth:grant-type:token-exchange`) whose `audience`
depends on which downstream client is asking (`CATALOG_API`, `WAREHOUSE_API`, `FULFILMENT_FHIR`,
`FULFILMENT_XML` — see `TokenExchangeClient`). The resulting client-scoped token is what
[`PseudonymGateway`](pseudonym-gateway.md#identifier-pseudonymization) then presents as `pseudoAccessToken`
when converting a NID to a pseudonym.

## Token exchange feeding pseudonymization {#identifier-pseudonymization}
**Serves:** [`identifier-pseudonymization`](../identifier-pseudonymization.md)

Local convention: `extractClientIdFromToken` manually decodes the JWT's Base64 payload to read the
`azp` claim rather than pulling in a full JWT library — deliberately minimal since only one claim is
needed. Each downstream audience's client id is injected via its own `@Value` and selected with a
`switch` on the `TokenExchangeClient` enum.

### Source (pseudonymized)
```java
package com.acme.shop.partner.gateway.token;

import com.acme.shop.authentication.domain.AuthenticatedCustomer;
import com.acme.shop.authentication.domain.token.TokenExchangeClient;
import com.acme.shop.partner.exception.PartnerException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.Map;

@Component
public class TokenExchangeGateway {
    private static final ParameterizedTypeReference<Map<String, String>> TOKEN_RESPONSE_TYPE = new ParameterizedTypeReference<>() {
    };

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final RestClient restClient;
    private final String pseudoTokenUrl;
    private final String catalogClientId;
    private final String warehouseClientId;
    private final String audienceFulfilmentXml;
    private final String audienceFulfilment;

    public TokenExchangeGateway(
            @Value("${properties.endpoint.partner.pseudo-token}") final String pseudoTokenUrl,
            @Value("${properties.oauth2.catalog.client-id}") final String catalogClientId,
            @Value("${properties.oauth2.warehouse.client-id}") final String warehouseClientId,
            @Value("${properties.oauth2.fulfilment.xml.audience}") final String audienceFulfilmentXml,
            @Value("${properties.fhir.fulfilment.pseudo.oauth2.audience}") final String audienceFulfilment,
            @Qualifier("proxyAwareRestClient") final RestClient restClient
    ) {
        this.pseudoTokenUrl = pseudoTokenUrl;
        this.catalogClientId = catalogClientId;
        this.warehouseClientId = warehouseClientId;
        this.audienceFulfilmentXml = audienceFulfilmentXml;
        this.audienceFulfilment = audienceFulfilment;
        this.restClient = restClient;
    }

    public String exchangeToken(TokenExchangeClient client, AuthenticatedCustomer authenticatedCustomer) {
        var token = authenticatedCustomer.getAuthenticationToken();

        MultiValueMap<String, String> data = new LinkedMultiValueMap<>();
        data.add("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange");
        data.add("requested_token_type", "urn:ietf:params:oauth:token-type:access_token");
        data.add("subject_token_type", "urn:ietf:params:oauth:token-type:access_token");
        data.add("subject_token", token);
        data.add("audience", getClientIdForAudience(client));
        data.add("client_id", extractClientIdFromToken(token));

        Map<String, String> response =
                restClient.post()
                        .uri(pseudoTokenUrl)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(data)
                        .retrieve()
                        .body(TOKEN_RESPONSE_TYPE);

        if (response == null || !response.containsKey("access_token")) {
            throw new PartnerException("The response from the token exchange endpoint was invalid");
        }

        return response.get("access_token");
    }

    private String extractClientIdFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                throw new PartnerException("The authentication token is not a valid JWT");
            }

            var payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            JsonNode claims = objectMapper.readTree(payload);
            JsonNode azpNode = claims.get("azp");

            if (azpNode == null || azpNode.asText().isBlank()) {
                throw new PartnerException("The authentication token does not contain the required 'azp' claim");
            }

            return azpNode.asText();
        } catch (PartnerException e) {
            throw e;
        } catch (Exception e) {
            throw new PartnerException("Failed to extract client_id from authentication token", e);
        }
    }

    private String getClientIdForAudience(TokenExchangeClient client) {
        return switch (client) {
            case CATALOG_API -> catalogClientId;
            case WAREHOUSE_API -> warehouseClientId;
            case FULFILMENT_FHIR -> audienceFulfilment;
            case FULFILMENT_XML -> audienceFulfilmentXml;
        };
    }
}
```

### Edge cases
Malformed JWT:
```java
assertThatThrownBy(() -> gateway.exchangeToken(CATALOG_API, customerWithToken("not-a-jwt")))
        .isInstanceOf(PartnerException.class)
        .hasMessage("The authentication token is not a valid JWT");
```
Expected: the gateway fails before posting to the token endpoint.

Absent `azp` claim:
```java
var token = "header." + base64("{\"sub\":\"jane.doe\"}") + ".signature";
```
Expected: the gateway rejects the token because it cannot derive `client_id`.

Endpoint omits `access_token`:
```java
when(restClient.post().retrieve().body(TOKEN_RESPONSE_TYPE)).thenReturn(Map.of("token_type", "Bearer"));
```
Expected: the gateway throws `PartnerException` instead of returning `null`.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg -li 'pseudo' --glob '*.java'` (161 matches / 29 files incl. tests; 16 files / 8 modules main-only — see [pseudonym-gateway])

[pseudonym-gateway]: pseudonym-gateway.md
