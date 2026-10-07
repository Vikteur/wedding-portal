package app.rekord.gateway.probe;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Test-only gateway used by the WireMock harness tests; it logs nothing and sends no identifiers. */
final class ProbeStatusGateway {

    private static final Pattern STATUS = Pattern.compile("\"status\"\s*:\s*\"([^\"]*)\"");

    private final URI baseUrl;
    private final Duration timeout;
    private final HttpClient client;

    ProbeStatusGateway(URI baseUrl, Duration timeout) {
        this.baseUrl = baseUrl;
        this.timeout = timeout;
        this.client = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    URI baseUrl() {
        return baseUrl;
    }

    String status() {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/probe/status"))
                .timeout(timeout)
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();
            if (code < 200 || code > 299) {
                throw new ProbeUnavailableException("probe answered " + code);
            }
            Matcher matcher = STATUS.matcher(response.body());
            if (!matcher.find()) {
                throw new ProbeUnavailableException("probe answered without a status");
            }
            return matcher.group(1);
        } catch (IOException e) {
            throw new ProbeUnavailableException("probe unreachable", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProbeUnavailableException("probe call interrupted", e);
        }
    }
}
