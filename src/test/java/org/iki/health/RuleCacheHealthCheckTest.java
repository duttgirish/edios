package org.iki.health;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;

@QuarkusTest
class RuleCacheHealthCheckTest {

    @Test
    void readinessCheckIsUp() {
        given()
            .when()
            .get("/health/ready")
            .then()
            .statusCode(200)
            .body("status", is("UP"));
    }

    @Test
    void livenessCheckIsUp() {
        given()
            .when()
            .get("/health/live")
            .then()
            .statusCode(200)
            .body("status", is("UP"));
    }

    @Test
    void healthCheckIncludesRuleCacheData() {
        given()
            .when()
            .get("/health/ready")
            .then()
            .statusCode(200)
            .body("checks.find { it.name == 'rule-cache' }.data.cachedRules", greaterThan(0))
            .body("checks.find { it.name == 'rule-cache' }.data.compiledRules", greaterThan(0));
    }

    @Test
    void overallHealthIsUp() {
        given()
            .when()
            .get("/health")
            .then()
            .statusCode(200)
            .body("status", is("UP"));
    }
}