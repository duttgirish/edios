package org.iki.repository;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.iki.model.Rule;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Repository for loading rules.
 * Currently uses sample in-memory rules for development.
 */
@ApplicationScoped
public class RuleRepository {

    private static final Logger LOG = Logger.getLogger(RuleRepository.class);

    /**
     * Sample rules for development/testing (matching init.sql).
     */
    private static final List<Rule> SAMPLE_RULES = List.of(
            new Rule(1L, "amount > 10000.0", "Flag transactions over $10,000", true),
            new Rule(2L, "amount > 50000.0", "Critical alert for transactions over $50,000", true),
            new Rule(3L, "debitAccount == creditAccount", "Self-transfer detection", true),
            new Rule(4L, "debitAccount.startsWith(\"SUSP-\") || creditAccount.startsWith(\"SUSP-\")",
                    "Suspicious account prefix detection", true),
            new Rule(5L, "amount == double(int(amount)) && amount >= 1000.0",
                    "Round amount detection for potential structuring", true),
            new Rule(6L, "amount > 5000.0 && (debitAccount.contains(\"OFF\") || creditAccount.contains(\"OFF\"))",
                    "Offshore account high-value transfer", true),
            new Rule(7L, "cin.startsWith(\"VIP-\")", "VIP customer transaction", true),
            new Rule(8L, "amount > 25000.0 && debitAccount != creditAccount && !cin.startsWith(\"VIP-\")",
                    "Large non-VIP inter-account transfer", true)
    );

    /**
     * Returns all active rules.
     * Currently returns sample in-memory rules.
     *
     * @return Uni containing list of active rules
     */
    public Uni<List<Rule>> findAllActiveRules() {
        List<Rule> activeRules = SAMPLE_RULES.stream()
                .filter(Rule::active)
                .toList();
        LOG.debugf("Loaded %d active sample rules", activeRules.size());
        return Uni.createFrom().item(activeRules);
    }

    /**
     * Returns the total number of sample rules (including inactive).
     */
    public int getTotalRuleCount() {
        return SAMPLE_RULES.size();
    }
}