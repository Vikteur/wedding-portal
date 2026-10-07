package app.rekord.gateway.probe;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.io.File;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ProbeStatusGatewayIT {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private ProbeStatusGateway gateway;

    @BeforeEach
    void gatewayPointsAtWireMock() {
        gateway = new ProbeStatusGateway(URI.create(wireMock.baseUrl()), Duration.ofSeconds(5));
    }

    @Test
    void wiremock_listens_on_a_dynamic_port_and_the_gateway_points_at_it() {
        // Given
        wireMock.stubFor(get(urlEqualTo("/probe/status"))
                .willReturn(aResponse().withStatus(200).withBody("{\"status\":\"up\"}")));

        // When
        gateway.status();

        // Then
        assertThat(wireMock.getPort()).isPositive().isNotEqualTo(8080);
        assertThat(gateway.baseUrl()).isEqualTo(URI.create(wireMock.baseUrl()));
        assertThat(gateway.baseUrl().getPort()).isEqualTo(wireMock.getPort());
        wireMock.verify(exactly(1), getRequestedFor(urlEqualTo("/probe/status")));
    }

    @Test
    void the_gateway_calls_the_stubbed_endpoint_once() {
        // Given
        wireMock.stubFor(get(urlEqualTo("/probe/status"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"up\"}")));

        // When
        String status = gateway.status();

        // Then
        assertThat(status).isEqualTo("up");
        wireMock.verify(
                exactly(1),
                getRequestedFor(urlEqualTo("/probe/status")).withHeader("Accept", equalTo("application/json")));
        assertThat(wireMock.getAllServeEvents()).hasSize(1);
    }

    @Test
    void a_5xx_becomes_the_gateways_typed_failure() {
        // Given
        wireMock.stubFor(get(urlEqualTo("/probe/status")).willReturn(aResponse().withStatus(503)));

        // When / Then
        assertThatThrownBy(gateway::status).isInstanceOf(ProbeUnavailableException.class);
        assertThat(wireMock.getAllServeEvents()).hasSize(1);
    }

    @Test
    void wiremock_comes_from_org_wiremock_3_and_no_legacy_jar_is_on_the_classpath() throws Exception {
        // Given
        String location = WireMockExtension.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toString();

        // When
        String[] classPath = System.getProperty("java.class.path").split(File.pathSeparator);

        // Then
        assertThat(location).contains("org.wiremock").contains("wiremock-standalone-3.");
        assertThat(classPath).noneMatch(entry -> entry.contains("com.github.tomakehurst"));
    }
}
