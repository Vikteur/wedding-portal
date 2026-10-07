package app.rekord.application.web;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import app.rekord.application.ResourceTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.response.Response;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(ResourceTestProfile.class)
@Tag("resource-test")
class SuccessStatusIT {

    private static final String BASE = "/api/test-only/success-status";

    @Test
    void a_method_that_names_201_answers_201_with_its_body() {
        Response response = given().when().post(BASE + "/created");

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.then().extract().asByteArray()).isEqualTo("{\"ok\":true}".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void a_method_that_names_204_answers_204_with_no_body_although_it_returns_a_health() {
        Response response = given().when().post(BASE + "/no-content");

        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(response.then().extract().asByteArray()).isEmpty();
    }

    @Test
    void a_refusal_after_the_helper_named_201_keeps_its_error_status() {
        Response response = given().when().post(BASE + "/created-then-refused");

        assertThat(response.statusCode()).isEqualTo(409);
    }

    @Test
    void a_method_that_does_not_call_the_helper_answers_200_with_its_body() {
        Response response = given().when().get(BASE + "/untouched");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.then().extract().asByteArray()).isEqualTo("{\"ok\":true}".getBytes(StandardCharsets.UTF_8));
    }
}
