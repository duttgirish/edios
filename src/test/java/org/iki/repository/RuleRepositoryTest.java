package org.iki.repository;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.iki.model.Rule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class RuleRepositoryTest {

    @Inject
    RuleRepository ruleRepository;

    @Test
    void findAllActiveRulesReturnsNonEmptyList() {
        List<Rule> rules = ruleRepository.findAllActiveRules().await().indefinitely();
        assertNotNull(rules);
        assertFalse(rules.isEmpty());
    }

    @Test
    void allReturnedRulesAreActive() {
        List<Rule> rules = ruleRepository.findAllActiveRules().await().indefinitely();
        assertTrue(rules.stream().allMatch(Rule::active));
    }

    @Test
    void rulesHaveUniqueIds() {
        List<Rule> rules = ruleRepository.findAllActiveRules().await().indefinitely();
        long uniqueIds = rules.stream().map(Rule::id).distinct().count();
        assertEquals(rules.size(), uniqueIds);
    }

    @Test
    void rulesHaveNonBlankExpressions() {
        List<Rule> rules = ruleRepository.findAllActiveRules().await().indefinitely();
        assertTrue(rules.stream().allMatch(r -> r.expression() != null && !r.expression().isBlank()));
    }

    @Test
    void rulesHaveDescriptions() {
        List<Rule> rules = ruleRepository.findAllActiveRules().await().indefinitely();
        assertTrue(rules.stream().allMatch(r -> r.description() != null && !r.description().isBlank()));
    }

    @Test
    void expectedRuleCountFromSampleData() {
        List<Rule> rules = ruleRepository.findAllActiveRules().await().indefinitely();
        assertEquals(8, rules.size(), "Should have 8 active sample rules");
    }

    @Test
    void getTotalRuleCountIncludesAll() {
        assertTrue(ruleRepository.getTotalRuleCount() >= 8);
    }

    @Test
    void containsHighValueRule() {
        List<Rule> rules = ruleRepository.findAllActiveRules().await().indefinitely();
        assertTrue(rules.stream().anyMatch(r -> r.expression().contains("amount > 10000")));
    }

    @Test
    void containsSelfTransferRule() {
        List<Rule> rules = ruleRepository.findAllActiveRules().await().indefinitely();
        assertTrue(rules.stream().anyMatch(r -> r.expression().contains("debitAccount == creditAccount")));
    }

    @Test
    void multipleCallsReturnSameData() {
        List<Rule> first = ruleRepository.findAllActiveRules().await().indefinitely();
        List<Rule> second = ruleRepository.findAllActiveRules().await().indefinitely();
        assertEquals(first.size(), second.size());
        assertEquals(first, second);
    }
}