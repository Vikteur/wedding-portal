package app.rekord.gateway.probe;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WireMockResetBetweenTestsIT {

    // One static server shared by both methods: this is where stubs and requests could bleed over.
    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .resetOnEachTest(true)
            .build();

    private static boolean firstRan;

    @Test
    @Order(1)
    void first_test_leaves_a_stub_and_a_request_behind() {
        // Given
        wireMock.stubFor(get(urlEqualTo("/probe/status"))
                .willReturn(aResponse().withStatus(200).withBody("{\"status\":\"up\"}")));
        ProbeStatusGateway gateway = new ProbeStatusGateway(URI.create(wireMock.baseUrl()), Duration.ofSeconds(5));

        // When
        String status = gateway.status();

        // Then
        assertThat(status).isEqualTo("up");
        assertThat(wireMock.getStubMappings()).hasSize(1);
        assertThat(wireMock.getAllServeEvents()).hasSize(1);
        firstRan = true;
    }

    @Test
    @Order(2)
    void second_test_starts_with_no_stub_and_no_recorded_request() {
        // Given
        assertThat(firstRan).as("the first test must have run before this one").isTrue();
        ProbeStatusGateway gateway = new ProbeStatusGateway(URI.create(wireMock.baseUrl()), Duration.ofSeconds(5));

        // When / Then
        assertThat(wireMock.getStubMappings()).isEmpty();
        assertThat(wireMock.getAllServeEvents()).isEmpty();
        // The unmatched request gets WireMock's 404, which the gateway maps to its typed failure.
        assertThatThrownBy(gateway::status).isInstanceOf(ProbeUnavailableException.class);
    }
}
