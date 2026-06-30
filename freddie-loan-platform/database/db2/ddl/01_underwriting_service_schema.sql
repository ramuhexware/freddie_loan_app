-- =============================================================================
-- Freddie Mac-Style Home Loan Platform
-- Database  : IBM DB2 LUW 11.5
-- Schema    : FREDDIE_UW  (underwriting-service)
-- Script    : DDL – Schema, Tables, Indexes, Constraints
-- Run as    : DB2ADMIN or a user with DBADM authority
-- =============================================================================

-- ─── 1. Create schema ────────────────────────────────────────────────────────
CREATE SCHEMA FREDDIE_UW;

SET CURRENT SCHEMA = FREDDIE_UW;

-- =============================================================================
-- TABLE: UNDERWRITING_ASSESSMENTS
-- Mapped to: UnderwritingAssessment.java
--            (@Table(name="UNDERWRITING_ASSESSMENTS", schema="FREDDIE_UW"))
-- =============================================================================
CREATE TABLE FREDDIE_UW.UNDERWRITING_ASSESSMENTS (
    ASSESSMENT_ID    VARCHAR(36)      NOT NULL,
    LOAN_ID          VARCHAR(36)      NOT NULL,
    CUSTOMER_ID      VARCHAR(36)      NOT NULL,
    CREDIT_SCORE     INTEGER,
    DTI_RATIO        DECIMAL(5,2),              -- Debt-to-Income %
    LTV_RATIO        DECIMAL(5,2),              -- Loan-to-Value %
    ANNUAL_INCOME    DECIMAL(18,2),
    MONTHLY_DEBT     DECIMAL(12,2),
    RISK_LEVEL       VARCHAR(20)
                         CONSTRAINT CHK_UW_RISK_LEVEL
                         CHECK (RISK_LEVEL IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    DECISION         VARCHAR(20)
                         CONSTRAINT CHK_UW_DECISION
                         CHECK (DECISION IN ('APPROVED','REFERRED','DECLINED')),
    DECISION_REASON  VARCHAR(2000),
    ASSESSED_AT      TIMESTAMP        NOT NULL WITH DEFAULT CURRENT TIMESTAMP,
    ASSESSED_BY      VARCHAR(100),
    BUREAU_REF       VARCHAR(255),

    CONSTRAINT PK_UW_ASSESSMENTS PRIMARY KEY (ASSESSMENT_ID)
)
IN USERSPACE1;

-- ─── Indexes ──────────────────────────────────────────────────────────────────
CREATE INDEX FREDDIE_UW.IDX_UW_LOAN_ID      ON FREDDIE_UW.UNDERWRITING_ASSESSMENTS (LOAN_ID);
CREATE INDEX FREDDIE_UW.IDX_UW_CUSTOMER_ID  ON FREDDIE_UW.UNDERWRITING_ASSESSMENTS (CUSTOMER_ID);
CREATE INDEX FREDDIE_UW.IDX_UW_DECISION     ON FREDDIE_UW.UNDERWRITING_ASSESSMENTS (DECISION);
CREATE INDEX FREDDIE_UW.IDX_UW_RISK_LEVEL   ON FREDDIE_UW.UNDERWRITING_ASSESSMENTS (RISK_LEVEL);
CREATE INDEX FREDDIE_UW.IDX_UW_ASSESSED_AT  ON FREDDIE_UW.UNDERWRITING_ASSESSMENTS (ASSESSED_AT);

-- =============================================================================
-- VIEW: DECLINED_ASSESSMENTS — Recently declined with reason
-- =============================================================================
CREATE VIEW FREDDIE_UW.DECLINED_ASSESSMENTS AS
SELECT
    ASSESSMENT_ID,
    LOAN_ID,
    CUSTOMER_ID,
    CREDIT_SCORE,
    DTI_RATIO,
    LTV_RATIO,
    RISK_LEVEL,
    DECISION_REASON,
    ASSESSED_AT
FROM
    FREDDIE_UW.UNDERWRITING_ASSESSMENTS
WHERE
    DECISION = 'DECLINED'
ORDER BY
    ASSESSED_AT DESC;

COMMIT WORK;
