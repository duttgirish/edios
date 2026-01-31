package org.iki.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RuleEvaluationResultTest {

    @Test
    void successMatched() {
        RuleEvaluationResult result = RuleEvaluationResult.success(1L, "amount > 100", true);
        assertEquals(1L, result.ruleId());
        assertEquals("amount > 100", result.expression());
        assertTrue(result.matched());
        assertNull(result.error());
        assertFalse(result.hasError());
    }

    @Test
    void successNotMatched() {
        RuleEvaluationResult result = RuleEvaluationResult.success(1L, "amount > 100", false);
        assertFalse(result.matched());
        assertFalse(result.hasError());
    }

    @Test
    void failure() {
        RuleEvaluationResult result = RuleEvaluationResult.failure(1L, "bad expr", "Compile error");
        assertFalse(result.matched());
        assertEquals("Compile error", result.error());
        assertTrue(result.hasError());
    }

    @Test
    void failureWithBlankErrorIsNotError() {
        RuleEvaluationResult result = new RuleEvaluationResult(1L, "expr", false, "  ");
        assertFalse(result.hasError());
    }

    @Test
    void failureWithEmptyErrorIsNotError() {
        RuleEvaluationResult result = new RuleEvaluationResult(1L, "expr", false, "");
        assertFalse(result.hasError());
    }

    @Test
    void equalityAndHashCode() {
        RuleEvaluationResult r1 = RuleEvaluationResult.success(1L, "expr", true);
        RuleEvaluationResult r2 = RuleEvaluationResult.success(1L, "expr", true);
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }
}