package org.iki.model;

/**
 * Result of evaluating a single rule against a transaction event.
 */
public record RuleEvaluationResult(
        Long ruleId,
        String expression,
        boolean matched,
        String error
) {
    public static RuleEvaluationResult success(Long ruleId, String expression, boolean matched) {
        return new RuleEvaluationResult(ruleId, expression, matched, null);
    }

    public static RuleEvaluationResult failure(Long ruleId, String expression, String error) {
        return new RuleEvaluationResult(ruleId, expression, false, error);
    }

    public boolean hasError() {
        return error != null && !error.isBlank();
    }
}