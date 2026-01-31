package org.iki.engine;

import dev.cel.common.CelAbstractSyntaxTree;
import dev.cel.common.CelValidationException;
import dev.cel.common.types.SimpleType;
import dev.cel.compiler.CelCompiler;
import dev.cel.compiler.CelCompilerFactory;
import dev.cel.runtime.CelEvaluationException;
import dev.cel.runtime.CelRuntime;
import dev.cel.runtime.CelRuntimeFactory;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.iki.model.Rule;
import org.iki.model.RuleEvaluationResult;
import org.iki.model.TransactionEvent;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CEL (Common Expression Language) rule engine for evaluating transaction events.
 * Compiles and caches CEL programs for performance. Thread-safe for concurrent evaluation.
 */
@ApplicationScoped
public class CelRuleEngine {

    private static final Logger LOG = Logger.getLogger(CelRuleEngine.class);

    private CelCompiler compiler;
    private CelRuntime runtime;

    // Volatile reference for atomic swap during recompilation
    private volatile Map<Long, CelRuntime.Program> compiledPrograms = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        compiler = CelCompilerFactory.standardCelCompilerBuilder()
                .addVar("debitAccount", SimpleType.STRING)
                .addVar("creditAccount", SimpleType.STRING)
                .addVar("cin", SimpleType.STRING)
                .addVar("amount", SimpleType.DOUBLE)
                .addVar("transactedTimeEpochSeconds", SimpleType.INT)
                .build();

        runtime = CelRuntimeFactory.standardCelRuntimeBuilder().build();

        LOG.info("CEL Rule Engine initialized");
    }

    /**
     * Compiles and caches rules for efficient evaluation.
     * Uses atomic swap to prevent race conditions during recompilation.
     *
     * @param rules List of rules to compile
     * @return number of successfully compiled rules
     */
    public int compileAndCacheRules(List<Rule> rules) {
        if (rules == null || rules.isEmpty()) {
            compiledPrograms = new ConcurrentHashMap<>();
            LOG.info("Cleared compiled CEL programs (empty rule set)");
            return 0;
        }

        ConcurrentHashMap<Long, CelRuntime.Program> newPrograms = new ConcurrentHashMap<>();

        for (Rule rule : rules) {
            try {
                CelAbstractSyntaxTree ast = compiler.compile(rule.expression()).getAst();
                CelRuntime.Program program = runtime.createProgram(ast);
                newPrograms.put(rule.id(), program);
                LOG.debugf("Compiled rule %d: %s", rule.id(), rule.expression());
            } catch (CelValidationException e) {
                LOG.errorf("Failed to compile rule %d: %s - Error: %s",
                        rule.id(), rule.expression(), e.getMessage());
            } catch (CelEvaluationException e) {
                LOG.errorf("Failed to create program for rule %d: %s",
                        rule.id(), e.getMessage());
            }
        }

        // Atomic swap - readers see either the old or new map, never a partially updated one
        compiledPrograms = newPrograms;
        LOG.infof("Cached %d/%d compiled CEL programs", newPrograms.size(), rules.size());
        return newPrograms.size();
    }

    /**
     * Evaluates a transaction event against all provided rules.
     *
     * @param event The transaction event to evaluate
     * @param rules The rules to evaluate against
     * @return Unmodifiable list of evaluation results for each rule
     */
    public List<RuleEvaluationResult> evaluateEvent(TransactionEvent event, List<Rule> rules) {
        if (event == null) {
            throw new IllegalArgumentException("event cannot be null");
        }
        if (rules == null || rules.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Object> variables = buildVariables(event);
        // Snapshot the reference for consistent reads during evaluation
        Map<Long, CelRuntime.Program> programs = this.compiledPrograms;
        List<RuleEvaluationResult> results = new ArrayList<>(rules.size());

        for (Rule rule : rules) {
            CelRuntime.Program program = programs.get(rule.id());
            if (program == null) {
                results.add(RuleEvaluationResult.failure(
                        rule.id(),
                        rule.expression(),
                        "Rule not compiled"
                ));
                continue;
            }

            try {
                Object result = program.eval(variables);
                boolean matched = Boolean.TRUE.equals(result);
                results.add(RuleEvaluationResult.success(rule.id(), rule.expression(), matched));

                if (matched) {
                    LOG.debugf("Rule %d matched for CIN %s: %s",
                            rule.id(), event.cin(), rule.expression());
                }
            } catch (CelEvaluationException e) {
                LOG.warnf("Rule %d evaluation failed for CIN %s: %s",
                        rule.id(), event.cin(), e.getMessage());
                results.add(RuleEvaluationResult.failure(
                        rule.id(),
                        rule.expression(),
                        e.getMessage()
                ));
            }
        }

        return Collections.unmodifiableList(results);
    }

    /**
     * Builds the variable map for CEL evaluation from a transaction event.
     */
    Map<String, Object> buildVariables(TransactionEvent event) {
        return Map.of(
                "debitAccount", event.debitAccount(),
                "creditAccount", event.creditAccount(),
                "cin", event.cin(),
                "amount", event.amount().doubleValue(),
                "transactedTimeEpochSeconds", event.transactedTime().getEpochSecond()
        );
    }

    /**
     * Returns the number of compiled rules currently cached.
     */
    public int getCachedRuleCount() {
        return compiledPrograms.size();
    }
}