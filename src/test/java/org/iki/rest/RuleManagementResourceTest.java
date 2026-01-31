package org.iki.rest;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class RuleManagementResourceTest {

    @Test
    void getCachedRulesReturnsList() {
        given()
            .when()
            .get("/rules")
            .then()
            .statusCode(200)
            .body("size()", greaterThan(0));
    }

    @Test
    void getCachedRulesContainExpectedFields() {
        given()
            .when()
            .get("/rules")
            .then()
            .statusCode(200)
            .body("[0].id", notNullValue())
            .body("[0].expression", notNullValue());
    }

    @Test
    void getStatsReturnsCorrectStructure() {
        given()
            .when()
            .get("/rules/stats")
            .then()
            .statusCode(200)
            .body("cachedRules", greaterThan(0))
            .body("compiledRules", greaterThan(0));
    }

    @Test
    void statsReflectsSameCountAsRulesList() {
        int ruleCount = given()
            .when()
            .get("/rules")
            .then()
            .extract().jsonPath().getList("$").size();

        given()
            .when()
            .get("/rules/stats")
            .then()
            .body("cachedRules", is(ruleCount));
    }

    @Test
    void refreshRulesReturns202() {
        given()
            .when()
            .post("/rules/refresh")
            .then()
            .statusCode(202)
            .body("message", is("Rule refresh initiated"));
    }

    @Test
    void rulesStillAvailableAfterRefresh() {
        given().when().post("/rules/refresh").then().statusCode(202);

        // Allow refresh to complete
        given()
            .when()
            .get("/rules/stats")
            .then()
            .statusCode(200)
            .body("cachedRules", greaterThan(0));
    }
}