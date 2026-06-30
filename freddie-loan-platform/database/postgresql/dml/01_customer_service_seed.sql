-- =============================================================================
-- Freddie Mac-Style Home Loan Platform
-- Database  : PostgreSQL 16
-- Script    : DML – Seed / Reference data for customer-service
-- =============================================================================

SET search_path TO freddie_customer;

-- =============================================================================
-- SEED: customers — 5 sample mortgage applicants
-- =============================================================================
INSERT INTO freddie_customer.customers (
    id, first_name, last_name, email, phone,
    ssn_encrypted, date_of_birth, nationality,
    address_line1, city, state, zip_code, country,
    customer_status, kyc_status, created_by, version
) VALUES
(
    gen_random_uuid(), 'James', 'Harrison', 'james.harrison@example.com', '+1-555-0101',
    'ENC:AES256:c3d1f8a2b7e94c6d', '1982-04-15', 'USA',
    '123 Maple Street', 'Austin', 'TX', '78701', 'USA',
    'ACTIVE', 'VERIFIED', 'system', 0
),
(
    gen_random_uuid(), 'Sofia', 'Martinez', 'sofia.martinez@example.com', '+1-555-0102',
    'ENC:AES256:a1b2c3d4e5f67890', '1990-08-22', 'USA',
    '456 Oak Avenue', 'Phoenix', 'AZ', '85001', 'USA',
    'ACTIVE', 'PENDING', 'system', 0
),
(
    gen_random_uuid(), 'Michael', 'Chen', 'michael.chen@example.com', '+1-555-0103',
    'ENC:AES256:f9e8d7c6b5a43210', '1975-11-30', 'USA',
    '789 Pine Road', 'Seattle', 'WA', '98101', 'USA',
    'ACTIVE', 'VERIFIED', 'system', 0
),
(
    gen_random_uuid(), 'Priya', 'Patel', 'priya.patel@example.com', '+1-555-0104',
    'ENC:AES256:1a2b3c4d5e6f7a8b', '1988-02-14', 'USA',
    '321 Elm Boulevard', 'Chicago', 'IL', '60601', 'USA',
    'ACTIVE', 'IN_PROGRESS', 'system', 0
),
(
    gen_random_uuid(), 'Robert', 'Williams', 'robert.williams@example.com', '+1-555-0105',
    'ENC:AES256:9b8a7c6d5e4f3210', '1965-07-04', 'USA',
    '654 Cedar Lane', 'Miami', 'FL', '33101', 'USA',
    'ACTIVE', 'VERIFIED', 'system', 0
);

-- =============================================================================
-- SEED: kyc_records — KYC entries for verified customers
-- =============================================================================
INSERT INTO freddie_customer.kyc_records (
    id, customer_id, kyc_provider, kyc_reference, kyc_status,
    risk_level, verified_at, expiry_date, remarks
)
SELECT
    gen_random_uuid(),
    c.id,
    'Equifax-KYC',
    'EQF-' || UPPER(SUBSTR(c.id::TEXT, 1, 8)),
    'PASSED',
    'LOW',
    NOW() - INTERVAL '30 days',
    (NOW() + INTERVAL '1 year')::DATE,
    'Initial KYC verification passed successfully.'
FROM freddie_customer.customers c
WHERE c.kyc_status = 'VERIFIED';
