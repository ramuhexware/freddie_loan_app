package com.freddieapp.underwriting.repository;

import com.freddieapp.underwriting.entity.UnderwritingAssessment;
import com.freddieapp.underwriting.entity.UnderwritingAssessment.Decision;
import com.freddieapp.underwriting.entity.UnderwritingAssessment.RiskLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UnderwritingAssessmentRepository extends JpaRepository<UnderwritingAssessment, String> {

    // ─── Native SELECT Queries (IBM DB2 SQL) ─────────────────────────────────

    /**
     * Retrieves the most recent underwriting assessment for a given loan.
     * DB2 uses FETCH FIRST n ROWS ONLY for result limiting.
     */
    @Query(value = """
            SELECT * FROM FREDDIE_UW.UNDERWRITING_ASSESSMENTS
            WHERE LOAN_ID = :loanId
            ORDER BY ASSESSED_AT DESC
            FETCH FIRST 1 ROWS ONLY
            """,
            nativeQuery = true)
    Optional<UnderwritingAssessment> findLatestByLoanId(@Param("loanId") String loanId);

    /**
     * Retrieves all assessments for a customer, ordered newest first.
     * Supports paginated display in underwriter dashboard.
     */
    @Query(value = """
            SELECT * FROM FREDDIE_UW.UNDERWRITING_ASSESSMENTS
            WHERE CUSTOMER_ID = :customerId
            ORDER BY ASSESSED_AT DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM FREDDIE_UW.UNDERWRITING_ASSESSMENTS
            WHERE CUSTOMER_ID = :customerId
            """,
            nativeQuery = true)
    Page<UnderwritingAssessment> findByCustomerId(
            @Param("customerId") String customerId,
            Pageable pageable);

    /**
     * Finds all HIGH or CRITICAL risk assessments pending manual review.
     * Used by compliance officer queue management.
     */
    @Query(value = """
            SELECT * FROM FREDDIE_UW.UNDERWRITING_ASSESSMENTS
            WHERE RISK_LEVEL IN ('HIGH', 'CRITICAL')
              AND DECISION  = 'REFERRED'
            ORDER BY ASSESSED_AT ASC
            FETCH FIRST :limit ROWS ONLY
            """,
            nativeQuery = true)
    List<UnderwritingAssessment> findHighRiskReferrals(@Param("limit") int limit);

    /**
     * Calculates average DTI and LTV ratios grouped by decision type.
     * Returns rows of [DECISION, AVG_DTI, AVG_LTV, ASSESSMENT_COUNT].
     * Used for underwriting policy analytics.
     */
    @Query(value = """
            SELECT DECISION,
                   AVG(DTI_RATIO)        AS AVG_DTI,
                   AVG(LTV_RATIO)        AS AVG_LTV,
                   COUNT(*)              AS ASSESSMENT_COUNT
            FROM FREDDIE_UW.UNDERWRITING_ASSESSMENTS
            WHERE ASSESSED_AT BETWEEN :fromDate AND :toDate
            GROUP BY DECISION
            ORDER BY DECISION
            """,
            nativeQuery = true)
    List<Object[]> getRiskAnalyticsSummary(
            @Param("fromDate") OffsetDateTime fromDate,
            @Param("toDate") OffsetDateTime toDate);

    /**
     * Checks whether the customer already has an assessment in the current
     * underwriting cycle (within the last 30 days) to avoid duplicate reviews.
     * DB2 uses CURRENT TIMESTAMP and DAYS function for date arithmetic.
     */
    @Query(value = """
            SELECT COUNT(*) FROM FREDDIE_UW.UNDERWRITING_ASSESSMENTS
            WHERE CUSTOMER_ID  = :customerId
              AND ASSESSED_AT >= CURRENT TIMESTAMP - 30 DAYS
            """,
            nativeQuery = true)
    int countRecentAssessmentsByCustomer(@Param("customerId") String customerId);

    /**
     * Retrieves assessments where credit score falls below the minimum
     * threshold — used for risk reporting to senior management.
     * Returns rows of [LOAN_ID, CUSTOMER_ID, CREDIT_SCORE, RISK_LEVEL, DECISION].
     */
    @Query(value = """
            SELECT LOAN_ID, CUSTOMER_ID, CREDIT_SCORE, RISK_LEVEL, DECISION
            FROM FREDDIE_UW.UNDERWRITING_ASSESSMENTS
            WHERE CREDIT_SCORE < :minScore
              AND ASSESSED_AT  >= :fromDate
            ORDER BY CREDIT_SCORE ASC
            FETCH FIRST :limit ROWS ONLY
            """,
            nativeQuery = true)
    List<Object[]> findLowCreditScoreAssessments(
            @Param("minScore") int minScore,
            @Param("fromDate") OffsetDateTime fromDate,
            @Param("limit") int limit);

    /**
     * Fetches the average credit score, DTI, and LTV across all loans
     * approved in a given period. Used for portfolio-level risk monitoring.
     */
    @Query(value = """
            SELECT AVG(CREDIT_SCORE) AS AVG_SCORE,
                   AVG(DTI_RATIO)    AS AVG_DTI,
                   AVG(LTV_RATIO)    AS AVG_LTV,
                   MIN(CREDIT_SCORE) AS MIN_SCORE,
                   MAX(CREDIT_SCORE) AS MAX_SCORE
            FROM FREDDIE_UW.UNDERWRITING_ASSESSMENTS
            WHERE DECISION    = 'APPROVED'
              AND ASSESSED_AT BETWEEN :fromDate AND :toDate
            """,
            nativeQuery = true)
    Object[] getPortfolioRiskMetrics(
            @Param("fromDate") OffsetDateTime fromDate,
            @Param("toDate") OffsetDateTime toDate);

    // ─── Native UPDATE / DML Queries (IBM DB2 SQL) ───────────────────────────

    /**
     * Records the final underwriting decision and risk level for a given assessment.
     * DB2 uses CURRENT TIMESTAMP (no parentheses) for the current time.
     */
    @Modifying
    @Transactional
    @Query(value = """
            UPDATE FREDDIE_UW.UNDERWRITING_ASSESSMENTS
               SET DECISION          = :#{#decision.name()},
                   RISK_LEVEL        = :#{#riskLevel.name()},
                   DECISION_REASON   = :reason,
                   ASSESSED_BY       = :assessedBy,
                   ASSESSED_AT       = CURRENT TIMESTAMP
             WHERE ASSESSMENT_ID     = :assessmentId
            """,
            nativeQuery = true)
    int recordDecision(
            @Param("assessmentId") String assessmentId,
            @Param("decision") Decision decision,
            @Param("riskLevel") RiskLevel riskLevel,
            @Param("reason") String reason,
            @Param("assessedBy") String assessedBy);

    /**
     * Updates credit bureau reference after an external credit pull completes.
     */
    @Modifying
    @Transactional
    @Query(value = """
            UPDATE FREDDIE_UW.UNDERWRITING_ASSESSMENTS
               SET BUREAU_REF  = :bureauRef,
                   CREDIT_SCORE = :creditScore
             WHERE ASSESSMENT_ID = :assessmentId
            """,
            nativeQuery = true)
    int updateCreditBureauData(
            @Param("assessmentId") String assessmentId,
            @Param("bureauRef") String bureauRef,
            @Param("creditScore") int creditScore);

    /**
     * Updates the DTI and LTV ratios after income/property verification is complete.
     */
    @Modifying
    @Transactional
    @Query(value = """
            UPDATE FREDDIE_UW.UNDERWRITING_ASSESSMENTS
               SET DTI_RATIO    = :dtiRatio,
                   LTV_RATIO    = :ltvRatio,
                   ANNUAL_INCOME = :annualIncome,
                   MONTHLY_DEBT  = :monthlyDebt
             WHERE ASSESSMENT_ID = :assessmentId
            """,
            nativeQuery = true)
    int updateFinancialRatios(
            @Param("assessmentId") String assessmentId,
            @Param("dtiRatio") BigDecimal dtiRatio,
            @Param("ltvRatio") BigDecimal ltvRatio,
            @Param("annualIncome") BigDecimal annualIncome,
            @Param("monthlyDebt") BigDecimal monthlyDebt);
}
