-- =============================================================================
-- Freddie Mac-Style Home Loan Platform
-- Database  : Oracle Database 21c / 19c
-- Schema    : FREDDIE_LOANS  (loan-origination-service)
-- Script    : DDL – Schema, Tables, Sequences, Indexes, Constraints
-- Run as    : FREDDIE_LOANS schema user (or a DBA who grants the schema)
-- =============================================================================

-- ─── 1. Create schema/user (run as SYSDBA) ───────────────────────────────────
-- CREATE USER FREDDIE_LOANS IDENTIFIED BY "FreddieL0ans#Secure"
--     DEFAULT TABLESPACE USERS
--     TEMPORARY TABLESPACE TEMP
--     QUOTA UNLIMITED ON USERS;
-- GRANT CONNECT, RESOURCE, CREATE VIEW TO FREDDIE_LOANS;

-- ─── Switch context ──────────────────────────────────────────────────────────
ALTER SESSION SET CURRENT_SCHEMA = FREDDIE_LOANS;

-- =============================================================================
-- TABLE: LOAN_APPLICATIONS
-- Mapped to: LoanApplication.java  (@Table(name="LOAN_APPLICATIONS", schema="FREDDIE_LOANS"))
-- =============================================================================
CREATE TABLE FREDDIE_LOANS.LOAN_APPLICATIONS (
    LOAN_ID           VARCHAR2(36)    NOT NULL,
    CUSTOMER_ID       VARCHAR2(36)    NOT NULL,
    LOAN_TYPE         VARCHAR2(50)    NOT NULL
                          CONSTRAINT chk_loan_type
                          CHECK (LOAN_TYPE IN ('PURCHASE','REFINANCE','HELOC','HOME_EQUITY')),
    LOAN_AMOUNT       NUMBER(18,2)    NOT NULL,
    PROPERTY_VALUE    NUMBER(18,2),
    PROPERTY_ADDRESS  VARCHAR2(500),
    INTEREST_RATE     NUMBER(6,4),
    LOAN_TERM_MONTHS  NUMBER(5),
    LOAN_STATUS       VARCHAR2(30)    DEFAULT 'PENDING'  NOT NULL
                          CONSTRAINT chk_loan_status
                          CHECK (LOAN_STATUS IN
                              ('PENDING','SUBMITTED','UNDER_REVIEW','APPROVED',
                               'REJECTED','DISBURSED','CLOSED')),
    APPLICATION_DATE  TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    DECISION_DATE     TIMESTAMP WITH TIME ZONE,
    DISBURSEMENT_DATE TIMESTAMP WITH TIME ZONE,
    APPROVED_AMOUNT   NUMBER(18,2),
    REJECTION_REASON  VARCHAR2(1000),
    CREATED_BY        VARCHAR2(100),
    UPDATED_AT        TIMESTAMP WITH TIME ZONE,
    VERSION           NUMBER(19)      DEFAULT 0  NOT NULL,

    CONSTRAINT PK_LOAN_APPLICATIONS PRIMARY KEY (LOAN_ID)
)
TABLESPACE USERS;

-- ─── Indexes ──────────────────────────────────────────────────────────────────
CREATE INDEX IDX_LOANS_CUSTOMER_ID  ON FREDDIE_LOANS.LOAN_APPLICATIONS (CUSTOMER_ID);
CREATE INDEX IDX_LOANS_STATUS       ON FREDDIE_LOANS.LOAN_APPLICATIONS (LOAN_STATUS);
CREATE INDEX IDX_LOANS_TYPE         ON FREDDIE_LOANS.LOAN_APPLICATIONS (LOAN_TYPE);
CREATE INDEX IDX_LOANS_APP_DATE     ON FREDDIE_LOANS.LOAN_APPLICATIONS (APPLICATION_DATE);

-- ─── Audit trigger ────────────────────────────────────────────────────────────
CREATE OR REPLACE TRIGGER TRG_LOANS_UPDATED_AT
    BEFORE UPDATE ON FREDDIE_LOANS.LOAN_APPLICATIONS
    FOR EACH ROW
BEGIN
    :NEW.UPDATED_AT := SYSTIMESTAMP;
END;
/

-- =============================================================================
-- TABLE: LOAN_STATUS_HISTORY
-- Mapped to: LoanStatusHistory.java  (@Table(name="LOAN_STATUS_HISTORY", schema="FREDDIE_LOANS"))
-- =============================================================================
CREATE TABLE FREDDIE_LOANS.LOAN_STATUS_HISTORY (
    HISTORY_ID   VARCHAR2(36)    NOT NULL,
    LOAN_ID      VARCHAR2(36)    NOT NULL,
    FROM_STATUS  VARCHAR2(30),
    TO_STATUS    VARCHAR2(30)    NOT NULL,
    CHANGED_AT   TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CHANGED_BY   VARCHAR2(100),
    NOTES        VARCHAR2(2000),

    CONSTRAINT PK_LOAN_STATUS_HISTORY PRIMARY KEY (HISTORY_ID),
    CONSTRAINT FK_LSH_LOAN_APPLICATION
        FOREIGN KEY (LOAN_ID)
        REFERENCES FREDDIE_LOANS.LOAN_APPLICATIONS (LOAN_ID)
        ON DELETE CASCADE
)
TABLESPACE USERS;

-- ─── Indexes ──────────────────────────────────────────────────────────────────
CREATE INDEX IDX_LSH_LOAN_ID    ON FREDDIE_LOANS.LOAN_STATUS_HISTORY (LOAN_ID);
CREATE INDEX IDX_LSH_CHANGED_AT ON FREDDIE_LOANS.LOAN_STATUS_HISTORY (CHANGED_AT);

-- =============================================================================
-- VIEW: ACTIVE_LOAN_PIPELINE  — Current loans under review
-- =============================================================================
CREATE OR REPLACE VIEW FREDDIE_LOANS.ACTIVE_LOAN_PIPELINE AS
SELECT
    LA.LOAN_ID,
    LA.CUSTOMER_ID,
    LA.LOAN_TYPE,
    LA.LOAN_AMOUNT,
    LA.LOAN_STATUS,
    LA.APPLICATION_DATE,
    ROUND((SYSDATE - CAST(LA.APPLICATION_DATE AS DATE)), 0) AS DAYS_IN_PIPELINE
FROM
    FREDDIE_LOANS.LOAN_APPLICATIONS LA
WHERE
    LA.LOAN_STATUS IN ('SUBMITTED', 'UNDER_REVIEW');
/

COMMIT;
