package com.freddieapp.strutsportal.dao;

import com.freddieapp.strutsportal.model.LoanApplication;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Data Access Object for Loan Applications.
 * Executes all DB2 queries against FREDDIE_LOANS.LOAN_APPLICATIONS.
 *
 * DB2 Calls in this DAO:
 *  1.  findAllLoans()              — SELECT all with customer name join
 *  2.  findLoanById()             — SELECT by primary key
 *  3.  findLoansByCustomer()      — SELECT filtered by CUSTOMER_ID
 *  4.  findLoansByStatus()        — SELECT filtered by LOAN_STATUS
 *  5.  findLoansByDateRange()     — SELECT filtered by APPLICATION_DATE range
 *  6.  findLoansByTypeAndStatus() — SELECT filtered by type + status (compound)
 *  7.  insertLoan()               — INSERT new loan application row
 *  8.  updateLoanStatus()         — UPDATE LOAN_STATUS + DECISION_DATE
 *  9.  updateApprovedAmount()     — UPDATE APPROVED_AMOUNT + REJECTION_REASON
 * 10.  countLoansByType()         — SELECT COUNT(*) GROUP BY LOAN_TYPE
 * 11.  sumLoanAmountByStatus()    — SELECT SUM(LOAN_AMOUNT) GROUP BY LOAN_STATUS
 * 12.  findRecentLoans()          — SELECT ORDER BY APPLICATION_DATE DESC, FETCH FIRST
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class LoanDao {

    private final JdbcTemplate jdbcTemplate;

    // ------------------------------------------------------------------ //
    //  RowMapper
    // ------------------------------------------------------------------ //
    private static final RowMapper<LoanApplication> LOAN_ROW_MAPPER = (rs, rowNum) -> {
        LoanApplication loan = new LoanApplication();
        loan.setLoanId(rs.getString("LOAN_ID"));
        loan.setCustomerId(rs.getString("CUSTOMER_ID"));
        loan.setLoanType(rs.getString("LOAN_TYPE"));
        loan.setLoanStatus(rs.getString("LOAN_STATUS"));
        loan.setLoanAmount(rs.getBigDecimal("LOAN_AMOUNT"));
        loan.setPropertyValue(rs.getBigDecimal("PROPERTY_VALUE"));
        loan.setPropertyAddress(rs.getString("PROPERTY_ADDRESS"));
        loan.setInterestRate(rs.getBigDecimal("INTEREST_RATE"));
        loan.setLoanTermMonths(rs.getInt("LOAN_TERM_MONTHS") == 0 ? null : rs.getInt("LOAN_TERM_MONTHS"));
        loan.setApprovedAmount(rs.getBigDecimal("APPROVED_AMOUNT"));
        loan.setRejectionReason(rs.getString("REJECTION_REASON"));
        loan.setCreatedBy(rs.getString("CREATED_BY"));

        Timestamp appDate = rs.getTimestamp("APPLICATION_DATE");
        if (appDate != null) loan.setApplicationDate(appDate.toLocalDateTime());

        Timestamp decDate = rs.getTimestamp("DECISION_DATE");
        if (decDate != null) loan.setDecisionDate(decDate.toLocalDateTime());

        Timestamp disbDate = rs.getTimestamp("DISBURSEMENT_DATE");
        if (disbDate != null) loan.setDisbursementDate(disbDate.toLocalDateTime());

        Timestamp updDate = rs.getTimestamp("UPDATED_AT");
        if (updDate != null) loan.setUpdatedAt(updDate.toLocalDateTime());

        // Optional: customer name from JOIN
        try {
            loan.setCustomerName(rs.getString("CUSTOMER_NAME"));
        } catch (SQLException ignored) { /* column not selected in all queries */ }

        return loan;
    };

    // ================================================================== //
    //  DB2 CALL #1 — Find ALL loans with customer name (JOIN)            //
    // ================================================================== //
    public List<LoanApplication> findAllLoans() {
        log.debug("[DB2-CALL-1] SELECT all loans with customer JOIN");
        String sql = """
                SELECT la.LOAN_ID, la.CUSTOMER_ID, la.LOAN_TYPE, la.LOAN_STATUS,
                       la.LOAN_AMOUNT, la.PROPERTY_VALUE, la.PROPERTY_ADDRESS,
                       la.INTEREST_RATE, la.LOAN_TERM_MONTHS, la.APPLICATION_DATE,
                       la.DECISION_DATE, la.DISBURSEMENT_DATE, la.APPROVED_AMOUNT,
                       la.REJECTION_REASON, la.CREATED_BY, la.UPDATED_AT,
                       TRIM(c.FIRST_NAME) || ' ' || TRIM(c.LAST_NAME) AS CUSTOMER_NAME
                FROM   FREDDIE_LOANS.LOAN_APPLICATIONS la
                LEFT JOIN FREDDIE_LOANS.CUSTOMERS c ON la.CUSTOMER_ID = c.CUSTOMER_ID
                ORDER BY la.APPLICATION_DATE DESC
                """;
        return jdbcTemplate.query(sql, LOAN_ROW_MAPPER);
    }

    // ================================================================== //
    //  DB2 CALL #2 — Find loan by primary key                            //
    // ================================================================== //
    public Optional<LoanApplication> findLoanById(String loanId) {
        log.debug("[DB2-CALL-2] SELECT loan by LOAN_ID={}", loanId);
        String sql = """
                SELECT la.LOAN_ID, la.CUSTOMER_ID, la.LOAN_TYPE, la.LOAN_STATUS,
                       la.LOAN_AMOUNT, la.PROPERTY_VALUE, la.PROPERTY_ADDRESS,
                       la.INTEREST_RATE, la.LOAN_TERM_MONTHS, la.APPLICATION_DATE,
                       la.DECISION_DATE, la.DISBURSEMENT_DATE, la.APPROVED_AMOUNT,
                       la.REJECTION_REASON, la.CREATED_BY, la.UPDATED_AT,
                       TRIM(c.FIRST_NAME) || ' ' || TRIM(c.LAST_NAME) AS CUSTOMER_NAME
                FROM   FREDDIE_LOANS.LOAN_APPLICATIONS la
                LEFT JOIN FREDDIE_LOANS.CUSTOMERS c ON la.CUSTOMER_ID = c.CUSTOMER_ID
                WHERE  la.LOAN_ID = ?
                """;
        List<LoanApplication> results = jdbcTemplate.query(sql, LOAN_ROW_MAPPER, loanId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    // ================================================================== //
    //  DB2 CALL #3 — Find all loans for a customer                       //
    // ================================================================== //
    public List<LoanApplication> findLoansByCustomer(String customerId) {
        log.debug("[DB2-CALL-3] SELECT loans by CUSTOMER_ID={}", customerId);
        String sql = """
                SELECT la.*, TRIM(c.FIRST_NAME) || ' ' || TRIM(c.LAST_NAME) AS CUSTOMER_NAME
                FROM   FREDDIE_LOANS.LOAN_APPLICATIONS la
                LEFT JOIN FREDDIE_LOANS.CUSTOMERS c ON la.CUSTOMER_ID = c.CUSTOMER_ID
                WHERE  la.CUSTOMER_ID = ?
                ORDER BY la.APPLICATION_DATE DESC
                """;
        return jdbcTemplate.query(sql, LOAN_ROW_MAPPER, customerId);
    }

    // ================================================================== //
    //  DB2 CALL #4 — Find loans filtered by status                       //
    // ================================================================== //
    public List<LoanApplication> findLoansByStatus(String status) {
        log.debug("[DB2-CALL-4] SELECT loans by LOAN_STATUS={}", status);
        String sql = """
                SELECT la.LOAN_ID, la.CUSTOMER_ID, la.LOAN_TYPE, la.LOAN_STATUS,
                       la.LOAN_AMOUNT, la.PROPERTY_VALUE, la.PROPERTY_ADDRESS,
                       la.INTEREST_RATE, la.LOAN_TERM_MONTHS, la.APPLICATION_DATE,
                       la.DECISION_DATE, la.DISBURSEMENT_DATE, la.APPROVED_AMOUNT,
                       la.REJECTION_REASON, la.CREATED_BY, la.UPDATED_AT,
                       TRIM(c.FIRST_NAME) || ' ' || TRIM(c.LAST_NAME) AS CUSTOMER_NAME
                FROM   FREDDIE_LOANS.LOAN_APPLICATIONS la
                LEFT JOIN FREDDIE_LOANS.CUSTOMERS c ON la.CUSTOMER_ID = c.CUSTOMER_ID
                WHERE  la.LOAN_STATUS = ?
                ORDER BY la.APPLICATION_DATE DESC
                """;
        return jdbcTemplate.query(sql, LOAN_ROW_MAPPER, status);
    }

    // ================================================================== //
    //  DB2 CALL #5 — Find loans within an application date range         //
    // ================================================================== //
    public List<LoanApplication> findLoansByDateRange(LocalDateTime fromDate, LocalDateTime toDate) {
        log.debug("[DB2-CALL-5] SELECT loans between {} and {}", fromDate, toDate);
        String sql = """
                SELECT la.LOAN_ID, la.CUSTOMER_ID, la.LOAN_TYPE, la.LOAN_STATUS,
                       la.LOAN_AMOUNT, la.PROPERTY_VALUE, la.PROPERTY_ADDRESS,
                       la.INTEREST_RATE, la.LOAN_TERM_MONTHS, la.APPLICATION_DATE,
                       la.DECISION_DATE, la.DISBURSEMENT_DATE, la.APPROVED_AMOUNT,
                       la.REJECTION_REASON, la.CREATED_BY, la.UPDATED_AT,
                       TRIM(c.FIRST_NAME) || ' ' || TRIM(c.LAST_NAME) AS CUSTOMER_NAME
                FROM   FREDDIE_LOANS.LOAN_APPLICATIONS la
                LEFT JOIN FREDDIE_LOANS.CUSTOMERS c ON la.CUSTOMER_ID = c.CUSTOMER_ID
                WHERE  la.APPLICATION_DATE BETWEEN ? AND ?
                ORDER BY la.APPLICATION_DATE DESC
                """;
        return jdbcTemplate.query(sql, LOAN_ROW_MAPPER,
                Timestamp.valueOf(fromDate), Timestamp.valueOf(toDate));
    }

    // ================================================================== //
    //  DB2 CALL #6 — Compound filter: type AND status                    //
    // ================================================================== //
    public List<LoanApplication> findLoansByTypeAndStatus(String loanType, String status) {
        log.debug("[DB2-CALL-6] SELECT loans by LOAN_TYPE={} AND LOAN_STATUS={}", loanType, status);
        String sql = """
                SELECT la.LOAN_ID, la.CUSTOMER_ID, la.LOAN_TYPE, la.LOAN_STATUS,
                       la.LOAN_AMOUNT, la.PROPERTY_VALUE, la.PROPERTY_ADDRESS,
                       la.INTEREST_RATE, la.LOAN_TERM_MONTHS, la.APPLICATION_DATE,
                       la.DECISION_DATE, la.DISBURSEMENT_DATE, la.APPROVED_AMOUNT,
                       la.REJECTION_REASON, la.CREATED_BY, la.UPDATED_AT,
                       TRIM(c.FIRST_NAME) || ' ' || TRIM(c.LAST_NAME) AS CUSTOMER_NAME
                FROM   FREDDIE_LOANS.LOAN_APPLICATIONS la
                LEFT JOIN FREDDIE_LOANS.CUSTOMERS c ON la.CUSTOMER_ID = c.CUSTOMER_ID
                WHERE  la.LOAN_TYPE = ? AND la.LOAN_STATUS = ?
                ORDER BY la.APPLICATION_DATE DESC
                """;
        return jdbcTemplate.query(sql, LOAN_ROW_MAPPER, loanType, status);
    }

    // ================================================================== //
    //  DB2 CALL #7 — INSERT a new loan application row                   //
    // ================================================================== //
    @Transactional
    public int insertLoan(LoanApplication loan) {
        log.info("[DB2-CALL-7] INSERT loan LOAN_ID={}", loan.getLoanId());
        String sql = """
                INSERT INTO FREDDIE_LOANS.LOAN_APPLICATIONS
                    (LOAN_ID, CUSTOMER_ID, LOAN_TYPE, LOAN_AMOUNT, PROPERTY_VALUE,
                     PROPERTY_ADDRESS, LOAN_TERM_MONTHS, LOAN_STATUS, CREATED_BY,
                     APPLICATION_DATE, UPDATED_AT)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """;
        return jdbcTemplate.update(sql,
                loan.getLoanId(),
                loan.getCustomerId(),
                loan.getLoanType(),
                loan.getLoanAmount(),
                loan.getPropertyValue(),
                loan.getPropertyAddress(),
                loan.getLoanTermMonths(),
                loan.getLoanStatus(),
                loan.getCreatedBy()
        );
    }

    // ================================================================== //
    //  DB2 CALL #8 — UPDATE loan status                                  //
    // ================================================================== //
    @Transactional
    public int updateLoanStatus(String loanId, String newStatus) {
        log.info("[DB2-CALL-8] UPDATE LOAN_STATUS={} for LOAN_ID={}", newStatus, loanId);
        String sql = """
                UPDATE FREDDIE_LOANS.LOAN_APPLICATIONS
                SET    LOAN_STATUS = ?,
                       DECISION_DATE = CURRENT_TIMESTAMP,
                       UPDATED_AT = CURRENT_TIMESTAMP
                WHERE  LOAN_ID = ?
                """;
        return jdbcTemplate.update(sql, newStatus, loanId);
    }

    // ================================================================== //
    //  DB2 CALL #9 — UPDATE approved amount or rejection reason          //
    // ================================================================== //
    @Transactional
    public int updateLoanDecision(String loanId, String status,
                                  java.math.BigDecimal approvedAmount, String rejectionReason) {
        log.info("[DB2-CALL-9] UPDATE loan decision LOAN_ID={} STATUS={}", loanId, status);
        String sql = """
                UPDATE FREDDIE_LOANS.LOAN_APPLICATIONS
                SET    LOAN_STATUS      = ?,
                       APPROVED_AMOUNT  = ?,
                       REJECTION_REASON = ?,
                       DECISION_DATE    = CURRENT_TIMESTAMP,
                       UPDATED_AT       = CURRENT_TIMESTAMP
                WHERE  LOAN_ID = ?
                """;
        return jdbcTemplate.update(sql, status, approvedAmount, rejectionReason, loanId);
    }

    // ================================================================== //
    //  DB2 CALL #10 — COUNT loans grouped by type (for dashboard KPIs)   //
    // ================================================================== //
    public List<Map<String, Object>> countLoansByType() {
        log.debug("[DB2-CALL-10] SELECT COUNT GROUP BY LOAN_TYPE");
        String sql = """
                SELECT LOAN_TYPE, COUNT(*) AS LOAN_COUNT
                FROM   FREDDIE_LOANS.LOAN_APPLICATIONS
                GROUP BY LOAN_TYPE
                ORDER BY LOAN_COUNT DESC
                """;
        return jdbcTemplate.queryForList(sql);
    }

    // ================================================================== //
    //  DB2 CALL #11 — SUM loan amounts grouped by status                 //
    // ================================================================== //
    public List<Map<String, Object>> sumLoanAmountByStatus() {
        log.debug("[DB2-CALL-11] SELECT SUM GROUP BY LOAN_STATUS");
        String sql = """
                SELECT LOAN_STATUS,
                       COUNT(*)            AS LOAN_COUNT,
                       SUM(LOAN_AMOUNT)    AS TOTAL_AMOUNT,
                       AVG(LOAN_AMOUNT)    AS AVG_AMOUNT
                FROM   FREDDIE_LOANS.LOAN_APPLICATIONS
                GROUP BY LOAN_STATUS
                ORDER BY TOTAL_AMOUNT DESC
                """;
        return jdbcTemplate.queryForList(sql);
    }

    // ================================================================== //
    //  DB2 CALL #12 — Most recent N loans (dashboard feed)               //
    // ================================================================== //
    public List<LoanApplication> findRecentLoans(int limit) {
        log.debug("[DB2-CALL-12] SELECT recent {} loans", limit);
        String sql = """
                SELECT la.LOAN_ID, la.CUSTOMER_ID, la.LOAN_TYPE, la.LOAN_STATUS,
                       la.LOAN_AMOUNT, la.PROPERTY_VALUE, la.PROPERTY_ADDRESS,
                       la.INTEREST_RATE, la.LOAN_TERM_MONTHS, la.APPLICATION_DATE,
                       la.DECISION_DATE, la.DISBURSEMENT_DATE, la.APPROVED_AMOUNT,
                       la.REJECTION_REASON, la.CREATED_BY, la.UPDATED_AT,
                       TRIM(c.FIRST_NAME) || ' ' || TRIM(c.LAST_NAME) AS CUSTOMER_NAME
                FROM   FREDDIE_LOANS.LOAN_APPLICATIONS la
                LEFT JOIN FREDDIE_LOANS.CUSTOMERS c ON la.CUSTOMER_ID = c.CUSTOMER_ID
                ORDER BY la.APPLICATION_DATE DESC
                FETCH FIRST ? ROWS ONLY
                """;
        return jdbcTemplate.query(sql, LOAN_ROW_MAPPER, limit);
    }
}
