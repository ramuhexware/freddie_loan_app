-- =============================================================================
-- Freddie Mac-Style Home Loan Platform
-- Database  : IBM DB2 LUW 11.5
-- Schema    : FREDDIE_UW  (underwriting-service)
-- Script    : DML – Seed / Reference data
-- =============================================================================

SET CURRENT SCHEMA = FREDDIE_UW;

-- =============================================================================
-- SEED: UNDERWRITING_ASSESSMENTS — 4 sample risk assessments
-- =============================================================================

-- Assessment 1: APPROVED — strong profile
INSERT INTO FREDDIE_UW.UNDERWRITING_ASSESSMENTS (
    ASSESSMENT_ID, LOAN_ID, CUSTOMER_ID,
    CREDIT_SCORE, DTI_RATIO, LTV_RATIO,
    ANNUAL_INCOME, MONTHLY_DEBT,
    RISK_LEVEL, DECISION,
    DECISION_REASON, ASSESSED_BY, BUREAU_REF
) VALUES (
    '11111111-1111-1111-1111-111111111111',
    'LOAN-00000000-0000-0001',
    '00000000-0000-0000-0000-000000000001',
    730, 32.50, 81.82,
    120000.00, 3250.00,
    'LOW', 'APPROVED',
    'DTI 32.5% within 43% guideline. LTV 81.8% within 97% limit. Credit score 730 exceeds 620 floor. Approved with standard PMI.',
    'underwriter-sys', 'EQF-TU-20240601-730A'
);

-- Assessment 2: REFERRED — borderline DTI
INSERT INTO FREDDIE_UW.UNDERWRITING_ASSESSMENTS (
    ASSESSMENT_ID, LOAN_ID, CUSTOMER_ID,
    CREDIT_SCORE, DTI_RATIO, LTV_RATIO,
    ANNUAL_INCOME, MONTHLY_DEBT,
    RISK_LEVEL, DECISION,
    DECISION_REASON, ASSESSED_BY, BUREAU_REF
) VALUES (
    '22222222-2222-2222-2222-222222222222',
    'LOAN-00000000-0000-0002',
    '00000000-0000-0000-0000-000000000002',
    668, 41.20, 71.79,
    85000.00, 2917.00,
    'MEDIUM', 'REFERRED',
    'DTI 41.2% approaching 43% ceiling. Credit score 668 above minimum but in moderate range. Referred for manual senior review.',
    'underwriter-sys', 'EXP-EQF-20240602-668B'
);

-- Assessment 3: DECLINED — high risk
INSERT INTO FREDDIE_UW.UNDERWRITING_ASSESSMENTS (
    ASSESSMENT_ID, LOAN_ID, CUSTOMER_ID,
    CREDIT_SCORE, DTI_RATIO, LTV_RATIO,
    ANNUAL_INCOME, MONTHLY_DEBT,
    RISK_LEVEL, DECISION,
    DECISION_REASON, ASSESSED_BY, BUREAU_REF
) VALUES (
    '33333333-3333-3333-3333-333333333333',
    'LOAN-00000000-0000-0004',
    '00000000-0000-0000-0000-000000000004',
    580, 52.80, 88.57,
    72000.00, 3168.00,
    'HIGH', 'DECLINED',
    'Credit score 580 below minimum pre-qualification gate (620). DTI 52.8% exceeds 43% guideline. Auto-declined by BPEL pre-qualification gate.',
    'underwriter-sys', 'TU-EXP-20240603-580C'
);

-- Assessment 4: APPROVED — excellent profile, HELOC
INSERT INTO FREDDIE_UW.UNDERWRITING_ASSESSMENTS (
    ASSESSMENT_ID, LOAN_ID, CUSTOMER_ID,
    CREDIT_SCORE, DTI_RATIO, LTV_RATIO,
    ANNUAL_INCOME, MONTHLY_DEBT,
    RISK_LEVEL, DECISION,
    DECISION_REASON, ASSESSED_BY, BUREAU_REF
) VALUES (
    '44444444-4444-4444-4444-444444444444',
    'LOAN-00000000-0000-0003',
    '00000000-0000-0000-0000-000000000003',
    795, 18.40, 13.33,
    220000.00, 3374.00,
    'LOW', 'APPROVED',
    'Excellent credit profile. HELOC approved at 80% combined LTV. DTI 18.4% well within limits.',
    'underwriter-sys', 'EQF-TU-20240601-795D'
);

COMMIT WORK;
