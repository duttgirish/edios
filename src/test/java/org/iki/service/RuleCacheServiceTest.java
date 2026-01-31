package org.iki.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.iki.model.Rule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class RuleCacheServiceTest {

    @Inject
    RuleCacheService ruleCacheService;

    @Test
    void cachedRulesAreLoadedOnStartup() {
        List<Rule> rules = ruleCacheService.getCachedRules();
        assertNotNull(rules);
        assertFalse(rules.isEmpty());
    }

    @Test
    void cachedRulesAreUnmodifiable() {
        List<Rule> rules = ruleCacheService.getCachedRules();
        assertThrows(UnsupportedOperationException.class,
                () -> rules.add(new Rule(999L, "test")));
    }

    @Test
    void forceRefreshKeepsRulesAvailable() {
        ruleCacheService.forceRefresh();
        List<Rule> rules = ruleCacheService.getCachedRules();
        assertNotNull(rules);
        assertFalse(rules.isEmpty());
    }

    @Test
    void lastRefreshTimeIsSetAfterStartup() {
        assertNotNull(ruleCacheService.getLastRefreshTime());
    }

    @Test
    void lastRefreshSucceededAfterStartup() {
        assertTrue(ruleCacheService.isLastRefreshSucceeded());
    }

    @Test
    void forceRefreshUpdatesTimestamp() {
        var before = ruleCacheService.getLastRefreshTime();
        ruleCacheService.forceRefresh();
        var after = ruleCacheService.getLastRefreshTime();
        assertNotNull(after);
        assertTrue(after.compareTo(before) >= 0);
    }

    @Test
    void cachedRulesContainExpectedCount() {
        assertEquals(8, ruleCacheService.getCachedRules().size());
    }
}