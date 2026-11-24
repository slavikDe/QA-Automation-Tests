import io.restassured.http.ContentType;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.equalTo;


public class APITest {
    private final String BASE_URL = "https://restful-booker.herokuapp.com";

    private final String RANDOM_FIRSTNAME = "Jim";
    private final String RANDOM_LASTNAME = "Brown";
    private final Integer RANDOM_PRICE = 111;
    private final Boolean IS_DEPOSIT_PAID_TRUE = true;
    private final String RANDOM_ADDITIONAL_NEEDS = "Breakfast";
    private final LocalDate RANDOM_DATE = LocalDate.of(2026, 1, 20);

    @Test
    public void createNewBookWithStatus201() {
        Map<String, Object> body = Map.of(
                "firstname", RANDOM_FIRSTNAME,
                "lastname", RANDOM_LASTNAME,
                "totalprice", RANDOM_PRICE,
                "depositpaid", IS_DEPOSIT_PAID_TRUE,
                "bookingdates", Map.of("checkin", RANDOM_DATE.toString(), "checkout", RANDOM_DATE.plusDays(5).toString()),
                "additionalneeds", RANDOM_ADDITIONAL_NEEDS
            );

        int bookingId = given()
                .baseUri(BASE_URL)
                .contentType(ContentType.JSON)
                .body(body)
            .when()
                .post("/booking")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().path("bookingid");

        given()
                .baseUri(BASE_URL)
                .contentType(ContentType.JSON)
            .when()
                .get("/booking/{id}", bookingId)
            .then()
                .statusCode(HttpStatus.SC_OK)
                .body("firstname", equalTo(RANDOM_FIRSTNAME))
                .body("lastname", equalTo(RANDOM_LASTNAME))
                .body("totalprice", equalTo(RANDOM_PRICE))
                .body("depositpaid", equalTo(IS_DEPOSIT_PAID_TRUE))
                .body("bookingdates.checkin", equalTo(RANDOM_DATE.toString()))
                .body("bookingdates.checkout", equalTo(RANDOM_DATE.plusDays(5).toString()))
                .body("additionalneeds", equalTo(RANDOM_ADDITIONAL_NEEDS))
                .log().all();
    }
}
