-- =============================================================================
-- Database initialization script for EDIOS
-- Creates the rules table and populates with sample rules
-- =============================================================================

-- Create the rules table
CREATE TABLE IF NOT EXISTS rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    expression VARCHAR(1000) NOT NULL,
    description VARCHAR(500),
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_rules_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert sample rules for transaction monitoring
-- These are CEL expressions that evaluate to true/false

-- Rule 1: High-value transaction detection
INSERT INTO rules (expression, description, active) VALUES
('amount > 10000.0', 'Flag transactions over $10,000', true);

-- Rule 2: Very high-value transaction
INSERT INTO rules (expression, description, active) VALUES
('amount > 50000.0', 'Critical alert for transactions over $50,000', true);

-- Rule 3: Suspicious account pattern (same debit and credit)
INSERT INTO rules (expression, description, active) VALUES
('debitAccount == creditAccount', 'Self-transfer detection', true);

-- Rule 4: Specific account monitoring
INSERT INTO rules (expression, description, active) VALUES
('debitAccount.startsWith("SUSP-") || creditAccount.startsWith("SUSP-")', 'Suspicious account prefix detection', true);

-- Rule 5: Round number detection (potential structuring)
INSERT INTO rules (expression, description, active) VALUES
('amount == double(int(amount)) && amount >= 1000.0', 'Round amount detection for potential structuring', true);

-- Rule 6: Time-based rule (iki: transactions at unusual hours would need more context)
INSERT INTO rules (expression, description, active) VALUES
('amount > 5000.0 && (debitAccount.contains("OFF") || creditAccount.contains("OFF"))', 'Offshore account high-value transfer', true);

-- Rule 7: Specific CIN pattern
INSERT INTO rules (expression, description, active) VALUES
('cin.startsWith("VIP-")', 'VIP customer transaction', true);

-- Rule 8: Combined conditions
INSERT INTO rules (expression, description, active) VALUES
('amount > 25000.0 && debitAccount != creditAccount && !cin.startsWith("VIP-")', 'Large non-VIP inter-account transfer', true);

-- Inactive rule iki
INSERT INTO rules (expression, description, active) VALUES
('amount < 0', 'Negative amount detection (disabled)', false);
