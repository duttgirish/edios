package org.iki.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class TransactionEventResourceTest {

    @Test
    void ingestSingleEvent() {
        String payload = """
            [
                {
                    "debitAccount": "ACC-001",
                    "creditAccount": "ACC-002",
                    "cin": "CIN-12345",
                    "amount": 15000.00,
                    "transactedTime": "2024-01-15T10:30:00Z"
                }
            ]
            """;

        given()
            .contentType(ContentType.JSON)
            .body(payload)
            .when()
            .post("/events")
            .then()
            .statusCode(202)
            .body("dispatched", is(1))
            .body("total", is(1));
    }

    @Test
    void ingestEmptyListReturns400() {
        given()
            .contentType(ContentType.JSON)
            .body("[]")
            .when()
            .post("/events")
            .then()
            .statusCode(400)
            .body("message", containsString("empty"));
    }

    @Test
    void ingestMultipleEvents() {
        String payload = """
            [
                {
                    "debitAccount": "ACC-001",
                    "creditAccount": "ACC-002",
                    "cin": "CIN-001",
                    "amount": 5000.00,
                    "transactedTime": "2024-01-15T10:30:00Z"
                },
                {
                    "debitAccount": "ACC-003",
                    "creditAccount": "ACC-004",
                    "cin": "CIN-002",
                    "amount": 25000.00,
                    "transactedTime": "2024-01-15T11:00:00Z"
                },
                {
                    "debitAccount": "SUSP-001",
                    "creditAccount": "ACC-005",
                    "cin": "CIN-003",
                    "amount": 1000.00,
                    "transactedTime": "2024-01-15T11:30:00Z"
                }
            ]
            """;

        given()
            .contentType(ContentType.JSON)
            .body(payload)
            .when()
            .post("/events")
            .then()
            .statusCode(202)
            .body("dispatched", is(3))
            .body("total", is(3));
    }

    @Test
    void ingestNullBodyReturns400() {
        given()
            .contentType(ContentType.JSON)
            .when()
            .post("/events")
            .then()
            .statusCode(400);
    }

    @Test
    void ingestMalformedJsonReturns400() {
        given()
            .contentType(ContentType.JSON)
            .body("{not valid json")
            .when()
            .post("/events")
            .then()
            .statusCode(400);
    }

    @Test
    void ingestEventWithMissingFieldReturnsError() {
        String payload = """
            [
                {
                    "debitAccount": "ACC-001",
                    "creditAccount": "ACC-002",
                    "amount": 100.00,
                    "transactedTime": "2024-01-15T10:30:00Z"
                }
            ]
            """;

        int status = given()
            .contentType(ContentType.JSON)
            .body(payload)
            .when()
            .post("/events")
            .then()
            .extract().statusCode();

        assertTrue(status >= 400, "Missing cin field should cause a client or server error, got " + status);
    }

    @Test
    void ingestEventWithBlankDebitAccountReturnsError() {
        String payload = """
            [
                {
                    "debitAccount": "  ",
                    "creditAccount": "ACC-002",
                    "cin": "CIN-123",
                    "amount": 100.00,
                    "transactedTime": "2024-01-15T10:30:00Z"
                }
            ]
            """;

        int status = given()
            .contentType(ContentType.JSON)
            .body(payload)
            .when()
            .post("/events")
            .then()
            .extract().statusCode();

        assertTrue(status >= 400, "Blank debitAccount should cause a client or server error, got " + status);
    }

    @Test
    void ingestHighValueEvent() {
        String payload = """
            [
                {
                    "debitAccount": "ACC-001",
                    "creditAccount": "ACC-002",
                    "cin": "CIN-123",
                    "amount": 999999.99,
                    "transactedTime": "2024-06-15T12:00:00Z"
                }
            ]
            """;

        given()
            .contentType(ContentType.JSON)
            .body(payload)
            .when()
            .post("/events")
            .then()
            .statusCode(202)
            .body("dispatched", is(1));
    }

    @Test
    void ingestZeroAmountEvent() {
        String payload = """
            [
                {
                    "debitAccount": "ACC-001",
                    "creditAccount": "ACC-002",
                    "cin": "CIN-123",
                    "amount": 0,
                    "transactedTime": "2024-06-15T12:00:00Z"
                }
            ]
            """;

        given()
            .contentType(ContentType.JSON)
            .body(payload)
            .when()
            .post("/events")
            .then()
            .statusCode(202)
            .body("dispatched", is(1));
    }

    @Test
    void ingestSelfTransferEvent() {
        String payload = """
            [
                {
                    "debitAccount": "ACC-001",
                    "creditAccount": "ACC-001",
                    "cin": "CIN-123",
                    "amount": 500.00,
                    "transactedTime": "2024-06-15T12:00:00Z"
                }
            ]
            """;

        given()
            .contentType(ContentType.JSON)
            .body(payload)
            .when()
            .post("/events")
            .then()
            .statusCode(202)
            .body("dispatched", is(1));
    }

    @Test
    void wrongContentTypeReturnsError() {
        int status = given()
            .contentType(ContentType.TEXT)
            .body("some text")
            .when()
            .post("/events")
            .then()
            .extract().statusCode();

        assertTrue(status >= 400, "Wrong content type should cause an error, got " + status);
    }
}