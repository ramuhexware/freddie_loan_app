-- =============================================================================
-- Freddie Mac-Style Home Loan Platform
-- Database  : PostgreSQL 16
-- Schema    : freddie_customer  (customer-service)
-- Script    : DDL – Schema, Tables, Indexes, Constraints
-- =============================================================================

-- ─── 1. Create database (run as superuser) ───────────────────────────────────
-- Already handled by deployment/init-scripts/postgres/init.sql
-- CREATE DATABASE freddie_customer;

\c freddie_customer;

-- ─── 2. Schema ───────────────────────────────────────────────────────────────
CREATE SCHEMA IF NOT EXISTS freddie_customer;

SET search_path TO freddie_customer;

-- ─── 3. Extension (UUID generation) ──────────────────────────────────────────
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =============================================================================
-- TABLE: customers
-- Mapped to: Customer.java  (@Table(name="customers", schema="freddie_customer"))
-- =============================================================================
CREATE TABLE IF NOT EXISTS freddie_customer.customers (
    id               UUID          DEFAULT gen_random_uuid() NOT NULL,
    first_name       VARCHAR(100)  NOT NULL,
    last_name        VARCHAR(100)  NOT NULL,
    email            VARCHAR(255)  NOT NULL,
    phone            VARCHAR(20),
    ssn_encrypted    TEXT          NOT NULL,          -- AES-256 at application layer
    date_of_birth    DATE          NOT NULL,
    nationality      CHAR(3),
    address_line1    VARCHAR(255),
    address_line2    VARCHAR(255),
    city             VARCHAR(100),
    state            VARCHAR(50),
    zip_code         VARCHAR(10),
    country          VARCHAR(3)    NOT NULL DEFAULT 'USA',
    customer_status  VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE'
                         CHECK (customer_status IN ('ACTIVE','INACTIVE','SUSPENDED','DECEASED')),
    kyc_status       VARCHAR(20)   NOT NULL DEFAULT 'PENDING'
                         CHECK (kyc_status IN ('PENDING','IN_PROGRESS','VERIFIED','FAILED','EXPIRED')),
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ,
    created_by       VARCHAR(100),
    version          BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT pk_customers PRIMARY KEY (id),
    CONSTRAINT uq_customers_email UNIQUE (email)
);

-- ─── Indexes ──────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_customers_email          ON freddie_customer.customers (email);
CREATE INDEX IF NOT EXISTS idx_customers_kyc_status     ON freddie_customer.customers (kyc_status);
CREATE INDEX IF NOT EXISTS idx_customers_customer_status ON freddie_customer.customers (customer_status);
CREATE INDEX IF NOT EXISTS idx_customers_last_name      ON freddie_customer.customers (last_name);

-- ─── Audit trigger ────────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION freddie_customer.set_updated_at()
    RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

CREATE OR REPLACE TRIGGER trg_customers_updated_at
    BEFORE UPDATE ON freddie_customer.customers
    FOR EACH ROW EXECUTE FUNCTION freddie_customer.set_updated_at();

-- =============================================================================
-- TABLE: kyc_records
-- Mapped to: KycRecord.java  (@Table(name="kyc_records", schema="freddie_customer"))
-- =============================================================================
CREATE TABLE IF NOT EXISTS freddie_customer.kyc_records (
    id             UUID         DEFAULT gen_random_uuid() NOT NULL,
    customer_id    UUID         NOT NULL,
    kyc_provider   VARCHAR(100),
    kyc_reference  VARCHAR(255),
    kyc_status     VARCHAR(20)  NOT NULL
                       CHECK (kyc_status IN ('SUBMITTED','IN_REVIEW','PASSED','FAILED','EXPIRED')),
    risk_level     VARCHAR(20)
                       CHECK (risk_level IN ('LOW','MEDIUM','HIGH','PROHIBITED')),
    verified_at    TIMESTAMPTZ,
    expiry_date    DATE,
    remarks        TEXT,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_kyc_records PRIMARY KEY (id),
    CONSTRAINT fk_kyc_customer
        FOREIGN KEY (customer_id) REFERENCES freddie_customer.customers (id)
        ON DELETE CASCADE
);

-- ─── Indexes ──────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_kyc_customer_id  ON freddie_customer.kyc_records (customer_id);
CREATE INDEX IF NOT EXISTS idx_kyc_status       ON freddie_customer.kyc_records (kyc_status);
CREATE INDEX IF NOT EXISTS idx_kyc_risk_level   ON freddie_customer.kyc_records (risk_level);
