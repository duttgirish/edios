package org.iki.model;

/**
 * Represents a CEL rule loaded from the rules source.
 */
public record Rule(
        Long id,
        String expression,
        String description,
        boolean active
) {
    public Rule {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("expression cannot be null or blank");
        }
    }

    /**
     * Convenience constructor for rules without description (backwards compatible).
     */
    public Rule(Long id, String expression) {
        this(id, expression, null, true);
    }
}