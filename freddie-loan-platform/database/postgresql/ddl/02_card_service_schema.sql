-- =============================================================================
-- Freddie Mac-Style Home Loan Platform
-- Database  : PostgreSQL 16
-- Schema    : freddie_cards  (card-service)
-- Script    : DDL – Schema, Tables, Indexes, Constraints
-- =============================================================================

\c freddie_cards;

-- ─── 1. Schema ───────────────────────────────────────────────────────────────
CREATE SCHEMA IF NOT EXISTS freddie_cards;

SET search_path TO freddie_cards;

-- ─── 2. Extension ────────────────────────────────────────────────────────────
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =============================================================================
-- TABLE: CARDS
-- Mapped to: Card.java  (@Table(name="CARDS", schema="freddie_cards"))
-- =============================================================================
CREATE TABLE IF NOT EXISTS freddie_cards."CARDS" (
    "ID"                UUID          DEFAULT gen_random_uuid() NOT NULL,
    "CUSTOMER_ID"       UUID          NOT NULL,
    "CARD_NUMBER"       VARCHAR(16)   NOT NULL,
    "CARD_HOLDER_NAME"  VARCHAR(100)  NOT NULL,
    "CARD_TYPE"         VARCHAR(20)   NOT NULL
                            CHECK ("CARD_TYPE" IN ('CREDIT','DEBIT')),
    "EXPIRY_DATE"       DATE          NOT NULL,
    "CVV"               VARCHAR(3)    NOT NULL,
    "CREDIT_LIMIT"      NUMERIC(15,2),
    "AVAILABLE_BALANCE" NUMERIC(15,2),
    "CARD_STATUS"       VARCHAR(20)   NOT NULL DEFAULT 'INACTIVE'
                            CHECK ("CARD_STATUS" IN ('ACTIVE','INACTIVE','BLOCKED','EXPIRED')),
    "CREATED_AT"        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    "UPDATED_AT"        TIMESTAMPTZ,

    CONSTRAINT pk_cards PRIMARY KEY ("ID"),
    CONSTRAINT uq_card_number UNIQUE ("CARD_NUMBER")
);

-- ─── Indexes ──────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_cards_customer_id ON freddie_cards."CARDS" ("CUSTOMER_ID");
CREATE INDEX IF NOT EXISTS idx_cards_card_status  ON freddie_cards."CARDS" ("CARD_STATUS");
CREATE INDEX IF NOT EXISTS idx_cards_card_type    ON freddie_cards."CARDS" ("CARD_TYPE");

-- ─── Audit trigger ────────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION freddie_cards.set_updated_at()
    RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW."UPDATED_AT" = NOW();
    RETURN NEW;
END;
$$;

CREATE OR REPLACE TRIGGER trg_cards_updated_at
    BEFORE UPDATE ON freddie_cards."CARDS"
    FOR EACH ROW EXECUTE FUNCTION freddie_cards.set_updated_at();
