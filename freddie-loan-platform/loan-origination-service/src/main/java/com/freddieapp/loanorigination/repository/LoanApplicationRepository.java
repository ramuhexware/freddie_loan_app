package com.freddieapp.loanorigination.repository;

import com.freddieapp.loanorigination.entity.LoanApplication;
import com.freddieapp.loanorigination.entity.LoanApplication.LoanStatus;
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
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, String> {

    // ─── Native SELECT Queries (Oracle SQL) ──────────────────────────────────

    /**
     * Fetch all loans for a given customer ordered by application date descending.
     * Uses Oracle FETCH FIRST (equivalent to LIMIT) with Spring Data Pageable
     * by providing a countQuery for pagination metadata.
     */
    @Query(value = """
            SELECT * FROM FREDDIE_LOANS.LOAN_APPLICATIONS
            WHERE CUSTOMER_ID = :customerId
            ORDER BY APPLICATION_DATE DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM FREDDIE_LOANS.LOAN_APPLICATIONS
            WHERE CUSTOMER_ID = :customerId
            """,
            nativeQuery = true)
    Page<LoanApplication> findByCustomerId(
            @Param("customerId") String customerId,
            Pageable pageable);

    /**
     * Looks up a single loan by ID and status.
     * Useful for verifying that a loan is in the expected state before transitions.
     */
    @Query(value = """
            SELECT * FROM FREDDIE_LOANS.LOAN_APPLICATIONS
            WHERE LOAN_ID     = :loanId
              AND LOAN_STATUS = :#{#status.name()}
            FETCH FIRST 1 ROWS ONLY
            """,
            nativeQuery = true)
    Optional<LoanApplication> findByLoanIdAndStatus(
            @Param("loanId") String loanId,
            @Param("status") LoanStatus status);

    /**
     * Returns paginated loans filtered by status, ordered newest first.
     * Used by loan officer dashboard to monitor queue.
     */
    @Query(value = """
            SELECT * FROM FREDDIE_LOANS.LOAN_APPLICATIONS
            WHERE LOAN_STATUS = :#{#status.name()}
            ORDER BY APPLICATION_DATE DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM FREDDIE_LOANS.LOAN_APPLICATIONS
            WHERE LOAN_STATUS = :#{#status.name()}
            """,
            nativeQuery = true)
    Page<LoanApplication> findByLoanStatus(
            @Param("status") LoanStatus status,
            Pageable pageable);

    /**
     * Full loan search by customer ID + optional status + optional loan type filter.
     * Oracle uses NVL2 for nullable parameter branching.
     */
    @Query(value = """
            SELECT * FROM FREDDIE_LOANS.LOAN_APPLICATIONS
            WHERE CUSTOMER_ID = :customerId
              AND (:status   IS NULL OR LOAN_STATUS = :status)
              AND (:loanType IS NULL OR LOAN_TYPE   = :loanType)
            ORDER BY APPLICATION_DATE DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM FREDDIE_LOANS.LOAN_APPLICATIONS
            WHERE CUSTOMER_ID = :customerId
              AND (:status   IS NULL OR LOAN_STATUS = :status)
              AND (:loanType IS NULL OR LOAN_TYPE   = :loanType)
            """,
            nativeQuery = true)
    Page<LoanApplication> searchLoans(
            @Param("customerId") String customerId,
            @Param("status") String status,
            @Param("loanType") String loanType,
            Pageable pageable);

    /**
     * Retrieves total approved loan amount per customer.
     * Used for credit exposure calculations in underwriting.
     * Returns rows of [CUSTOMER_ID, TOTAL_APPROVED_AMOUNT, LOAN_COUNT].
     */
    @Query(value = """
            SELECT CUSTOMER_ID,
                   SUM(APPROVED_AMOUNT) AS TOTAL_APPROVED,
                   COUNT(*)             AS LOAN_COUNT
            FROM FREDDIE_LOANS.LOAN_APPLICATIONS
            WHERE LOAN_STATUS IN ('APPROVED', 'DISBURSED')
              AND CUSTOMER_ID = :customerId
            GROUP BY CUSTOMER_ID
            """,
            nativeQuery = true)
    List<Object[]> findCreditExposureByCustomer(@Param("customerId") String customerId);

    /**
     * Loans submitted for underwriting review but not yet decided within a given
     * number of hours. Used by a scheduled SLA monitoring job.
     */
    @Query(value = """
            SELECT * FROM FREDDIE_LOANS.LOAN_APPLICATIONS
            WHERE LOAN_STATUS = 'UNDER_REVIEW'
              AND APPLICATION_DATE <= SYSTIMESTAMP - NUMTODSINTERVAL(:hours, 'HOUR')
            ORDER BY APPLICATION_DATE ASC
            """,
            nativeQuery = true)
    List<LoanApplication> findOverduePendingReviews(@Param("hours") int hours);

    /**
     * Fetches count of loans grouped by status and loan type.
     * Used for management reporting dashboards.
     * Returns rows of [LOAN_TYPE, LOAN_STATUS, COUNT, TOTAL_AMOUNT].
     */
    @Query(value = """
            SELECT LOAN_TYPE,
                   LOAN_STATUS,
                   COUNT(*)          AS LOAN_COUNT,
                   SUM(LOAN_AMOUNT)  AS TOTAL_AMOUNT
            FROM FREDDIE_LOANS.LOAN_APPLICATIONS
            WHERE APPLICATION_DATE >= :fromDate
              AND APPLICATION_DATE <= :toDate
            GROUP BY LOAN_TYPE, LOAN_STATUS
            ORDER BY LOAN_TYPE, LOAN_STATUS
            """,
            nativeQuery = true)
    List<Object[]> getLoanSummaryReport(
            @Param("fromDate") OffsetDateTime fromDate,
            @Param("toDate") OffsetDateTime toDate);

    // ─── Native UPDATE / DML Queries (Oracle SQL) ─────────────────────────────

    /**
     * Transitions a loan to UNDER_REVIEW status.
     * Called when a loan officer submits the application to underwriting.
     */
    @Modifying
    @Transactional
    @Query(value = """
            UPDATE FREDDIE_LOANS.LOAN_APPLICATIONS
               SET LOAN_STATUS = 'UNDER_REVIEW',
                   UPDATED_AT  = SYSTIMESTAMP
             WHERE LOAN_ID     = :loanId
               AND LOAN_STATUS = 'SUBMITTED'
            """,
            nativeQuery = true)
    int submitForUnderwriting(@Param("loanId") String loanId);

    /**
     * Records underwriting approval — sets APPROVED status, approved amount,
     * interest rate, and decision timestamp in a single atomic UPDATE.
     */
    @Modifying
    @Transactional
    @Query(value = """
            UPDATE FREDDIE_LOANS.LOAN_APPLICATIONS
               SET LOAN_STATUS      = 'APPROVED',
                   APPROVED_AMOUNT  = :approvedAmount,
                   INTEREST_RATE    = :interestRate,
                   DECISION_DATE    = SYSTIMESTAMP,
                   UPDATED_AT       = SYSTIMESTAMP
             WHERE LOAN_ID          = :loanId
               AND LOAN_STATUS      = 'UNDER_REVIEW'
            """,
            nativeQuery = true)
    int approveLoan(
            @Param("loanId") String loanId,
            @Param("approvedAmount") BigDecimal approvedAmount,
            @Param("interestRate") BigDecimal interestRate);

    /**
     * Records underwriting rejection with reason code.
     */
    @Modifying
    @Transactional
    @Query(value = """
            UPDATE FREDDIE_LOANS.LOAN_APPLICATIONS
               SET LOAN_STATUS       = 'REJECTED',
                   REJECTION_REASON  = :reason,
                   DECISION_DATE     = SYSTIMESTAMP,
                   UPDATED_AT        = SYSTIMESTAMP
             WHERE LOAN_ID           = :loanId
               AND LOAN_STATUS       = 'UNDER_REVIEW'
            """,
            nativeQuery = true)
    int rejectLoan(
            @Param("loanId") String loanId,
            @Param("reason") String reason);

    /**
     * Marks a loan as DISBURSED after funds are released.
     */
    @Modifying
    @Transactional
    @Query(value = """
            UPDATE FREDDIE_LOANS.LOAN_APPLICATIONS
               SET LOAN_STATUS        = 'DISBURSED',
                   DISBURSEMENT_DATE  = SYSTIMESTAMP,
                   UPDATED_AT         = SYSTIMESTAMP
             WHERE LOAN_ID            = :loanId
               AND LOAN_STATUS        = 'APPROVED'
            """,
            nativeQuery = true)
    int disburseLoan(@Param("loanId") String loanId);
}
