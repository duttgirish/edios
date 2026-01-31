package org.iki.engine;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.iki.model.Rule;
import org.iki.model.RuleEvaluationResult;
import org.iki.model.TransactionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class CelRuleEngineTest {

    @Inject
    CelRuleEngine celRuleEngine;

    private List<Rule> testRules;

    @BeforeEach
    void setUp() {
        // Matches RuleRepository sample rules exactly
        testRules = List.of(
                new Rule(1L, "amount > 10000.0"),                                                            // index 0
                new Rule(2L, "amount > 50000.0"),                                                            // index 1
                new Rule(3L, "debitAccount == creditAccount"),                                                // index 2
                new Rule(4L, "debitAccount.startsWith(\"SUSP-\") || creditAccount.startsWith(\"SUSP-\")"),   // index 3
                new Rule(5L, "amount == double(int(amount)) && amount >= 1000.0"),                           // index 4
                new Rule(6L, "amount > 5000.0 && (debitAccount.contains(\"OFF\") || creditAccount.contains(\"OFF\"))"), // index 5
                new Rule(7L, "cin.startsWith(\"VIP-\")"),                                                    // index 6
                new Rule(8L, "amount > 25000.0 && debitAccount != creditAccount && !cin.startsWith(\"VIP-\")") // index 7
        );
        celRuleEngine.compileAndCacheRules(testRules);
    }

    private TransactionEvent event(String debit, String credit, String cin, String amount) {
        return new TransactionEvent(debit, credit, cin, new BigDecimal(amount), Instant.now());
    }

    // --- High-value transaction tests ---

    // --- High-value transaction tests (rule index 0: amount > 10000) ---

    @Test
    void highValueTransactionMatchesRule1() {
        List<RuleEvaluationResult> results = celRuleEngine.evaluateEvent(
                event("ACC-001", "ACC-002", "CIN-123", "15000.00"), testRules);
        assertTrue(results.get(0).matched());
    }

    @Test
    void exactlyAtThresholdDoesNotMatchGreaterThan() {
        List<RuleEvaluationResult> results = celRuleEngine.evaluateEvent(
                event("ACC-001", "ACC-002", "CIN-123", "10000.00"), testRules);
        assertFalse(results.get(0).matched(), "amount > 10000 should not match exactly 10000");
    }

    @Test
    void justAboveThresholdMatches() {
        List<RuleEvaluationResult> results = celRuleEngine.evaluateEvent(
                event("ACC-001", "ACC-002", "CIN-123", "10000.01"), testRules);
        assertTrue(results.get(0).matched());
    }

    @Test
    void belowThresholdDoesNotMatch() {
        List<RuleEvaluationResult> results = celRuleEngine.evaluateEvent(
                event("ACC-001", "ACC-002", "CIN-123", "9999.99"), testRules);
        assertFalse(results.get(0).matched());
    }

    // --- Very high value (rule index 1: amount > 50000) ---

    @Test
    void veryHighValueMatches() {
        List<RuleEvaluationResult> results = celRuleEngine.evaluateEvent(
                event("ACC-001", "ACC-002", "CIN-123", "60000.00"), testRules);
        assertTrue(results.get(1).matched());
    }

    @Test
    void belowVeryHighThresholdDoesNotMatch() {
        List<RuleEvaluationResult> results = celRuleEngine.evaluateEvent(
                event("ACC-001", "ACC-002", "CIN-123", "49999.99"), testRules);
        assertFalse(results.get(1).matched());
    }

    // --- Self-transfer detection (rule index 2: debitAccount == creditAccount) ---

    @Test
    void selfTransferMatches() {
        List<RuleEvaluationResult> results = celRuleEngine.evaluateEvent(
                event("ACC-001", "ACC-001", "CIN-123", "500.00"), testRules);
        assertTrue(results.get(2).matched());
    }

    @Test
    void differentAccountsDoNotMatchSelfTransfer() {
        List<RuleEvaluationResult> results = celRuleEngine.evaluateEvent(
                event("ACC-001", "ACC-002", "CIN-123", "500.00"), testRules);
        assertFalse(results.get(2).matched());
    }

    // --- Suspicious account prefix (rule index 3: SUSP- debit OR credit) ---

    @Test
    void suspiciousDebitAccountMatches() {
        List<RuleEvaluationResult> results = celRuleEngine.evaluateEvent(
                event("SUSP-001", "ACC-002", "CIN-123", "100.00"), testRules);
        assertTrue(results.get(3).matched());
    }

    @Test
    void suspiciousCreditAccountMatches() {
        List<RuleEvaluationResult> results = celRuleEngine.evaluateEvent(
                event("ACC-001", "SUSP-002", "CIN-123", "100.00"), testRules);
        assertTrue(results.get(3).matched());
    }

    @Test
    void nonSuspiciousAccountDoesNotMatch() {
        List<RuleEvaluationResult> results = celRuleEngine.evaluateEvent(
                event("ACC-001", "ACC-002", "CIN-123", "100.00"), testRules);
        assertFalse(results.get(3).matched());
    }

    @Test
    void caseMattersForSuspPrefix() {
        List<RuleEvaluationResult> results = celRuleEngine.evaluateEvent(
                event("susp-001", "ACC-002", "CIN-123", "100.00"), testRules);
        assertFalse(results.get(3).matched(), "SUSP- prefix is case-sensitive");
    }

    // --- VIP customer (rule index 6: cin.startsWith("VIP-")) ---

    @Test
    void vipCustomerMatches() {
        List<RuleEvaluationResult> results = celRuleEngine.evaluateEvent(
                event("ACC-001", "ACC-002", "VIP-123", "100.00"), testRules);
        assertTrue(results.get(6).matched());
    }

    @Test
    void nonVipCustomerDoesNotMatch() {
        List<RuleEvaluationResult> results = celRuleEngine.evaluateEvent(
                event("ACC-001", "ACC-002", "CIN-123", "100.00"), testRules);
        assertFalse(results.get(6).matched());
    }

    // --- Round amount detection ---

    @Test
    void roundAmountAboveThresholdMatches() {
        List<RuleEvaluationResult> results = celRuleEngine.evaluateEvent(
                event("ACC-001", "ACC-002", "CIN-123", "5000.00"), testRules);
        assertTrue(results.get(4).matched());
    }

    @Test
    void nonRoundAmountDoesNotMatch() {
        List<RuleEvaluationResult> results = celRuleEngine.evaluateEvent(
                event("ACC-001", "ACC-002", "CIN-123", "5000.50"), testRules);
        assertFalse(results.get(4).matched());
    }

    @Test
    void roundAmountBelowThresholdDoesNotMatch() {
        List<RuleEvaluationResult> results = celRuleEngine.evaluateEvent(
                event("ACC-001", "ACC-002", "CIN-123", "999.00"), testRules);
        assertFalse(results.get(4).matched());
    }

    // --- Offshore account rule ---

    @Test
    void offshoreDebitHighValueMatches() {
        List<RuleEvaluationResult> results = celRuleEngine.evaluateEvent(
                event("ACC-OFF-001", "ACC-002", "CIN-123", "6000.00"), testRules);
        assertTrue(results.get(5).matched());
    }

    @Test
    void offshoreCreditHighValueMatches() {
        List<RuleEvaluationResult> results = celRuleEngine.evaluateEvent(
                event("ACC-001", "OFF-002", "CIN-123", "6000.00"), testRules);
        assertTrue(results.get(5).matched());
    }

    @Test
    void offshoreLowValueDoesNotMatch() {
        List<RuleEvaluationResult> results = celRuleEngine.evaluateEvent(
                event("ACC-OFF-001", "ACC-002", "CIN-123", "4000.00"), testRules);
        assertFalse(results.get(5).matched());
    }

    // --- Large non-VIP inter-account transfer ---

    @Test
    void largeNonVipInterAccountMatches() {
        List<RuleEvaluationResult> results = celRuleEngine.evaluateEvent(
                event("ACC-001", "ACC-002", "CIN-123", "30000.00"), testRules);
        assertTrue(results.get(7).matched());
    }

    @Test
    void largeVipIsExcluded() {
        List<RuleEvaluationResult> results = celRuleEngine.evaluateEvent(
                event("ACC-001", "ACC-002", "VIP-123", "30000.00"), testRules);
        assertFalse(results.get(7).matched());
    }

    @Test
    void largeSelfTransferIsExcluded() {
        List<RuleEvaluationResult> results = celRuleEngine.evaluateEvent(
                event("ACC-001", "ACC-001", "CIN-123", "30000.00"), testRules);
        assertFalse(results.get(7).matched());
    }

    // --- No rules match ---

    @Test
    void noRulesMatch() {
        List<RuleEvaluationResult> results = celRuleEngine.evaluateEvent(
                event("ACC-001", "ACC-002", "CIN-123", "100.00"), testRules);
        assertTrue(results.stream().noneMatch(RuleEvaluationResult::matched));
    }

    // --- Multiple rules match simultaneously ---

    @Test
    void multipleRulesCanMatch() {
        // high value + round amount + large non-VIP inter-account
        List<RuleEvaluationResult> results = celRuleEngine.evaluateEvent(
                event("ACC-001", "ACC-002", "CIN-123", "30000.00"), testRules);
        long matchCount = results.stream().filter(RuleEvaluationResult::matched).count();
        assertTrue(matchCount >= 3, "Should match at least rule 1, 5, and 8");
    }

    // --- Invalid rule compilation ---

    @Test
    void invalidRuleCompilationReturnsZero() {
        int compiled = celRuleEngine.compileAndCacheRules(List.of(
                new Rule(99L, "invalid syntax !!!")
        ));
        assertEquals(0, compiled);
        assertEquals(0, celRuleEngine.getCachedRuleCount());
    }

    @Test
    void mixOfValidAndInvalidRules() {
        int compiled = celRuleEngine.compileAndCacheRules(List.of(
                new Rule(10L, "amount > 100.0"),
                new Rule(11L, "bad expression !!!"),
                new Rule(12L, "cin == \"test\"")
        ));
        assertEquals(2, compiled);
        assertEquals(2, celRuleEngine.getCachedRuleCount());
    }

    // --- Edge cases for compile and cache ---

    @Test
    void emptyRuleListClearsCache() {
        celRuleEngine.compileAndCacheRules(testRules);
        assertTrue(celRuleEngine.getCachedRuleCount() > 0);

        celRuleEngine.compileAndCacheRules(Collections.emptyList());
        assertEquals(0, celRuleEngine.getCachedRuleCount());
    }

    @Test
    void nullRuleListClearsCache() {
        celRuleEngine.compileAndCacheRules(testRules);
        assertTrue(celRuleEngine.getCachedRuleCount() > 0);

        celRuleEngine.compileAndCacheRules(null);
        assertEquals(0, celRuleEngine.getCachedRuleCount());
    }

    @Test
    void recompilingReplacesOldCache() {
        celRuleEngine.compileAndCacheRules(testRules);
        assertEquals(8, celRuleEngine.getCachedRuleCount());

        celRuleEngine.compileAndCacheRules(List.of(new Rule(100L, "amount > 1.0")));
        assertEquals(1, celRuleEngine.getCachedRuleCount());
    }

    // --- Evaluate with edge case inputs ---

    @Test
    void evaluateWithNullEventThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> celRuleEngine.evaluateEvent(null, testRules));
    }

    @Test
    void evaluateWithNullRulesReturnsEmpty() {
        TransactionEvent event = event("ACC-001", "ACC-002", "CIN-123", "100.00");
        List<RuleEvaluationResult> results = celRuleEngine.evaluateEvent(event, null);
        assertTrue(results.isEmpty());
    }

    @Test
    void evaluateWithEmptyRulesReturnsEmpty() {
        TransactionEvent event = event("ACC-001", "ACC-002", "CIN-123", "100.00");
        List<RuleEvaluationResult> results = celRuleEngine.evaluateEvent(event, Collections.emptyList());
        assertTrue(results.isEmpty());
    }

    @Test
    void evaluateUncompiledRuleReturnsError() {
        List<Rule> uncompiledRules = List.of(new Rule(999L, "amount > 0"));
        // Don't compile rule 999 - it's not in the cache
        celRuleEngine.compileAndCacheRules(testRules);

        List<RuleEvaluationResult> results = celRuleEngine.evaluateEvent(
                event("ACC-001", "ACC-002", "CIN-123", "100.00"), uncompiledRules);

        assertEquals(1, results.size());
        assertTrue(results.get(0).hasError());
        assertEquals("Rule not compiled", results.get(0).error());
    }

    // --- Amount precision edge cases ---

    @Test
    void verySmallAmountAboveZero() {
        List<RuleEvaluationResult> results = celRuleEngine.evaluateEvent(
                event("ACC-001", "ACC-002", "CIN-123", "0.01"), testRules);
        assertFalse(results.get(0).matched());
    }

    @Test
    void zeroAmount() {
        List<RuleEvaluationResult> results = celRuleEngine.evaluateEvent(
                event("ACC-001", "ACC-002", "CIN-123", "0"), testRules);
        assertNotNull(results);
        assertFalse(results.get(0).matched());
    }

    // --- Result list is unmodifiable ---

    @Test
    void resultListIsUnmodifiable() {
        List<RuleEvaluationResult> results = celRuleEngine.evaluateEvent(
                event("ACC-001", "ACC-002", "CIN-123", "100.00"), testRules);
        assertThrows(UnsupportedOperationException.class,
                () -> results.add(RuleEvaluationResult.success(99L, "x", true)));
    }

    // --- getCachedRuleCount ---

    @Test
    void cachedRuleCountAfterCompilation() {
        celRuleEngine.compileAndCacheRules(testRules);
        assertEquals(testRules.size(), celRuleEngine.getCachedRuleCount());
    }

    @Test
    void cachedRuleCountInitiallyZeroAfterClear() {
        celRuleEngine.compileAndCacheRules(null);
        assertEquals(0, celRuleEngine.getCachedRuleCount());
    }
}