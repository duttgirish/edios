package org.iki.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class RuleTest {

    @Test
    void validRuleCreation() {
        Rule rule = new Rule(1L, "amount > 100.0", "High value check", true);
        assertEquals(1L, rule.id());
        assertEquals("amount > 100.0", rule.expression());
        assertEquals("High value check", rule.description());
        assertTrue(rule.active());
    }

    @Test
    void convenienceConstructorSetsDefaults() {
        Rule rule = new Rule(1L, "amount > 100.0");
        assertNull(rule.description());
        assertTrue(rule.active());
    }

    @Test
    void inactiveRule() {
        Rule rule = new Rule(1L, "amount > 100.0", "desc", false);
        assertFalse(rule.active());
    }

    @Test
    void nullIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new Rule(null, "amount > 100.0"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t"})
    void nullOrBlankExpressionThrows(String expression) {
        assertThrows(IllegalArgumentException.class,
                () -> new Rule(1L, expression));
    }

    @Test
    void nullDescriptionIsAllowed() {
        Rule rule = new Rule(1L, "amount > 0", null, true);
        assertNull(rule.description());
    }

    @Test
    void equalityAndHashCode() {
        Rule r1 = new Rule(1L, "amount > 100.0", "desc", true);
        Rule r2 = new Rule(1L, "amount > 100.0", "desc", true);
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void inequalityOnDifferentId() {
        Rule r1 = new Rule(1L, "amount > 100.0");
        Rule r2 = new Rule(2L, "amount > 100.0");
        assertNotEquals(r1, r2);
    }

    @Test
    void complexExpressionIsValid() {
        Rule rule = new Rule(1L,
                "debitAccount.startsWith(\"SUSP-\") || creditAccount.startsWith(\"SUSP-\")");
        assertNotNull(rule.expression());
    }
}