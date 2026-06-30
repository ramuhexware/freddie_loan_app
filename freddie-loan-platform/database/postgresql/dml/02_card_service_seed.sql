-- =============================================================================
-- Freddie Mac-Style Home Loan Platform
-- Database  : PostgreSQL 16
-- Script    : DML – Seed / Reference data for card-service
-- =============================================================================

SET search_path TO freddie_cards;

-- =============================================================================
-- SEED: CARDS — Sample credit and debit cards
-- NOTE: CUSTOMER_ID values must match UUIDs in freddie_customer.customers
-- =============================================================================
INSERT INTO freddie_cards."CARDS" (
    "ID", "CUSTOMER_ID", "CARD_NUMBER", "CARD_HOLDER_NAME",
    "CARD_TYPE", "EXPIRY_DATE", "CVV",
    "CREDIT_LIMIT", "AVAILABLE_BALANCE", "CARD_STATUS"
) VALUES
(
    gen_random_uuid(),
    '00000000-0000-0000-0000-000000000001',   -- placeholder; replace with actual customer UUID
    '4111111111111001', 'JAMES HARRISON',
    'CREDIT', '2028-06-30', '123',
    50000.00, 47250.00, 'ACTIVE'
),
(
    gen_random_uuid(),
    '00000000-0000-0000-0000-000000000001',
    '4111111111112001', 'JAMES HARRISON',
    'DEBIT', '2027-12-31', '456',
    NULL, 12800.00, 'ACTIVE'
),
(
    gen_random_uuid(),
    '00000000-0000-0000-0000-000000000002',   -- sofia.martinez
    '4111111111111002', 'SOFIA MARTINEZ',
    'CREDIT', '2027-03-31', '789',
    25000.00, 18000.00, 'ACTIVE'
),
(
    gen_random_uuid(),
    '00000000-0000-0000-0000-000000000003',   -- michael.chen
    '4111111111111003', 'MICHAEL CHEN',
    'CREDIT', '2029-09-30', '321',
    75000.00, 70000.00, 'ACTIVE'
),
(
    gen_random_uuid(),
    '00000000-0000-0000-0000-000000000005',   -- robert.williams
    '4111111111111005', 'ROBERT WILLIAMS',
    'DEBIT', '2026-11-30', '654',
    NULL, 8500.00, 'INACTIVE'
);
