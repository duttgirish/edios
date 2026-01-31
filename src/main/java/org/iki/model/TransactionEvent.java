package org.iki.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Immutable record representing a financial transaction event.
 * Used for high-throughput event ingestion and rule evaluation.
 */
public record TransactionEvent(
        String debitAccount,
        String creditAccount,
        String cin,
        BigDecimal amount,
        Instant transactedTime
) {
    public TransactionEvent {
        if (debitAccount == null || debitAccount.isBlank()) {
            throw new IllegalArgumentException("debitAccount cannot be null or blank");
        }
        if (creditAccount == null || creditAccount.isBlank()) {
            throw new IllegalArgumentException("creditAccount cannot be null or blank");
        }
        if (cin == null || cin.isBlank()) {
            throw new IllegalArgumentException("cin cannot be null or blank");
        }
        if (amount == null) {
            throw new IllegalArgumentException("amount cannot be null");
        }
        if (transactedTime == null) {
            throw new IllegalArgumentException("transactedTime cannot be null");
        }
    }
}