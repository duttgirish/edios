package org.iki.verticle;

import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.iki.engine.CelRuleEngine;
import org.iki.model.Rule;
import org.iki.model.RuleEvaluationResult;
import org.iki.model.TransactionEvent;
import org.iki.service.RuleCacheService;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Vert.x event consumer that processes transaction events against cached CEL rules.
 * Uses virtual threads for efficient blocking operation handling.
 */
@ApplicationScoped
public class RuleEvaluatorVerticle {

    private static final Logger LOG = Logger.getLogger(RuleEvaluatorVerticle.class);

    @Inject
    CelRuleEngine celRuleEngine;

    @Inject
    RuleCacheService ruleCacheService;

    /**
     * Consumes transaction events from the event bus and evaluates them against all rules.
     *
     * @param event The transaction event to process
     */
    @ConsumeEvent(value = "transaction.process", blocking = true)
    @RunOnVirtualThread
    public void processTransaction(TransactionEvent event) {
        long startTime = System.nanoTime();

        try {
            LOG.debugf("Processing transaction for CIN: %s, Amount: %s",
                    event.cin(), event.amount());

            List<Rule> rules = ruleCacheService.getCachedRules();

            if (rules.isEmpty()) {
                LOG.warn("No rules available for evaluation");
                return;
            }

            List<RuleEvaluationResult> results = celRuleEngine.evaluateEvent(event, rules);

            // Single-pass counting
            long matchedCount = 0;
            long errorCount = 0;
            for (RuleEvaluationResult result : results) {
                if (result.hasError()) {
                    errorCount++;
                } else if (result.matched()) {
                    matchedCount++;
                }
            }

            long durationMs = (System.nanoTime() - startTime) / 1_000_000;

            LOG.infof("Processed CIN %s: %d/%d rules matched, %d errors, took %d ms",
                    event.cin(), matchedCount, rules.size(), errorCount, durationMs);

            handleMatchedRules(event, results);

        } catch (Exception e) {
            LOG.errorf(e, "Error processing transaction for CIN: %s", event.cin());
        }
    }

    /**
     * Handle rules that matched the transaction event.
     * Logs alerts for each matched rule.
     */
    private void handleMatchedRules(TransactionEvent event, List<RuleEvaluationResult> results) {
        for (RuleEvaluationResult result : results) {
            if (result.matched()) {
                LOG.infof("ALERT: Rule %d matched for transaction - CIN: %s, Debit: %s, Credit: %s, Amount: %s",
                        result.ruleId(),
                        event.cin(),
                        event.debitAccount(),
                        event.creditAccount(),
                        event.amount());
            }
        }
    }
}