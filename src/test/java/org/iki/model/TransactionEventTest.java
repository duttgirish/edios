package org.iki.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class TransactionEventTest {

    private static final Instant NOW = Instant.parse("2024-06-15T10:30:00Z");

    @Test
    void validEventCreation() {
        TransactionEvent event = new TransactionEvent("ACC-001", "ACC-002", "CIN-123",
                new BigDecimal("15000.00"), NOW);

        assertEquals("ACC-001", event.debitAccount());
        assertEquals("ACC-002", event.creditAccount());
        assertEquals("CIN-123", event.cin());
        assertEquals(new BigDecimal("15000.00"), event.amount());
        assertEquals(NOW, event.transactedTime());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    void nullOrBlankDebitAccountThrows(String debitAccount) {
        assertThrows(IllegalArgumentException.class,
                () -> new TransactionEvent(debitAccount, "ACC-002", "CIN-123",
                        BigDecimal.ONE, NOW));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t"})
    void nullOrBlankCreditAccountThrows(String creditAccount) {
        assertThrows(IllegalArgumentException.class,
                () -> new TransactionEvent("ACC-001", creditAccount, "CIN-123",
                        BigDecimal.ONE, NOW));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void nullOrBlankCinThrows(String cin) {
        assertThrows(IllegalArgumentException.class,
                () -> new TransactionEvent("ACC-001", "ACC-002", cin,
                        BigDecimal.ONE, NOW));
    }

    @Test
    void nullAmountThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new TransactionEvent("ACC-001", "ACC-002", "CIN-123",
                        null, NOW));
    }

    @Test
    void nullTransactedTimeThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new TransactionEvent("ACC-001", "ACC-002", "CIN-123",
                        BigDecimal.ONE, null));
    }

    @Test
    void zeroAmountIsValid() {
        TransactionEvent event = new TransactionEvent("ACC-001", "ACC-002", "CIN-123",
                BigDecimal.ZERO, NOW);
        assertEquals(BigDecimal.ZERO, event.amount());
    }

    @Test
    void negativeAmountIsValid() {
        TransactionEvent event = new TransactionEvent("ACC-001", "ACC-002", "CIN-123",
                new BigDecimal("-100.50"), NOW);
        assertEquals(new BigDecimal("-100.50"), event.amount());
    }

    @Test
    void veryLargeAmountIsValid() {
        BigDecimal largeAmount = new BigDecimal("99999999999999.99");
        TransactionEvent event = new TransactionEvent("ACC-001", "ACC-002", "CIN-123",
                largeAmount, NOW);
        assertEquals(largeAmount, event.amount());
    }

    @Test
    void equalityAndHashCode() {
        TransactionEvent e1 = new TransactionEvent("A", "B", "C", BigDecimal.TEN, NOW);
        TransactionEvent e2 = new TransactionEvent("A", "B", "C", BigDecimal.TEN, NOW);
        assertEquals(e1, e2);
        assertEquals(e1.hashCode(), e2.hashCode());
    }

    @Test
    void toStringContainsFields() {
        TransactionEvent event = new TransactionEvent("ACC-001", "ACC-002", "CIN-123",
                BigDecimal.ONE, NOW);
        String str = event.toString();
        assertTrue(str.contains("ACC-001"));
        assertTrue(str.contains("ACC-002"));
        assertTrue(str.contains("CIN-123"));
    }

    @Test
    void selfTransferIsValid() {
        TransactionEvent event = new TransactionEvent("ACC-001", "ACC-001", "CIN-123",
                BigDecimal.ONE, NOW);
        assertEquals(event.debitAccount(), event.creditAccount());
    }

    @Test
    void specialCharactersInAccounts() {
        TransactionEvent event = new TransactionEvent("ACC-001/SUB", "ACC-002.MAIN", "CIN/123",
                BigDecimal.ONE, NOW);
        assertEquals("ACC-001/SUB", event.debitAccount());
    }

    @Test
    void epochInstantIsValid() {
        TransactionEvent event = new TransactionEvent("ACC-001", "ACC-002", "CIN-123",
                BigDecimal.ONE, Instant.EPOCH);
        assertEquals(Instant.EPOCH, event.transactedTime());
    }
}