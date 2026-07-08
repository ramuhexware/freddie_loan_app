package com.freddieapp.strutsportal.dao;

import com.freddieapp.strutsportal.model.ReportSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * Data Access Object for Reports.
 * Executes heavy aggregation queries against DB2.
 *
 * DB2 Calls:
 *  1. getLoanSummaryByType()         — SUM/COUNT GROUP BY LOAN_TYPE
 *  2. getLoanSummaryByStatus()       — SUM/COUNT GROUP BY LOAN_STATUS
 *  3. getMonthlyDisbursementStats()  — GROUP BY YEAR+MONTH
 *  4. getTopLoanAmounts()            — SELECT ORDER BY LOAN_AMOUNT DESC
 *  5. getApprovalRateByLoanType()    — Subquery: approved vs total
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ReportDao {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<ReportSummary> REPORT_ROW_MAPPER = (rs, rowNum) -> {
        ReportSummary r = new ReportSummary();
        r.setCategory(rs.getString("CATEGORY"));
        r.setCount(rs.getLong("LOAN_COUNT"));
        r.setTotalAmount(rs.getBigDecimal("TOTAL_AMOUNT"));
        r.setAvgAmount(rs.getBigDecimal("AVG_AMOUNT"));
        try { r.setMinAmount(rs.getBigDecimal("MIN_AMOUNT")); } catch (Exception ignored) {}
        try { r.setMaxAmount(rs.getBigDecimal("MAX_AMOUNT")); } catch (Exception ignored) {}
        try { r.setPeriod(rs.getString("PERIOD")); } catch (Exception ignored) {}
        return r;
    };

    // ================================================================== //
    //  DB2 CALL #1 — Loan summary grouped by loan type                   //
    // ================================================================== //
    public List<ReportSummary> getLoanSummaryByType() {
        log.debug("[DB2-CALL-1-REPORT] Loan summary GROUP BY LOAN_TYPE");
        String sql = """
                SELECT LOAN_TYPE                 AS CATEGORY,
                       COUNT(*)                  AS LOAN_COUNT,
                       SUM(LOAN_AMOUNT)          AS TOTAL_AMOUNT,
                       AVG(LOAN_AMOUNT)          AS AVG_AMOUNT,
                       MIN(LOAN_AMOUNT)          AS MIN_AMOUNT,
                       MAX(LOAN_AMOUNT)          AS MAX_AMOUNT
                FROM   FREDDIE_LOANS.LOAN_APPLICATIONS
                GROUP BY LOAN_TYPE
                ORDER BY TOTAL_AMOUNT DESC
                """;
        return jdbcTemplate.query(sql, REPORT_ROW_MAPPER);
    }

    // ================================================================== //
    //  DB2 CALL #2 — Loan summary grouped by status                      //
    // ================================================================== //
    public List<ReportSummary> getLoanSummaryByStatus() {
        log.debug("[DB2-CALL-2-REPORT] Loan summary GROUP BY LOAN_STATUS");
        String sql = """
                SELECT LOAN_STATUS                AS CATEGORY,
                       COUNT(*)                   AS LOAN_COUNT,
                       SUM(LOAN_AMOUNT)           AS TOTAL_AMOUNT,
                       AVG(LOAN_AMOUNT)           AS AVG_AMOUNT,
                       MIN(LOAN_AMOUNT)           AS MIN_AMOUNT,
                       MAX(LOAN_AMOUNT)           AS MAX_AMOUNT
                FROM   FREDDIE_LOANS.LOAN_APPLICATIONS
                GROUP BY LOAN_STATUS
                ORDER BY LOAN_COUNT DESC
                """;
        return jdbcTemplate.query(sql, REPORT_ROW_MAPPER);
    }

    // ================================================================== //
    //  DB2 CALL #3 — Monthly disbursement statistics                     //
    // ================================================================== //
    public List<ReportSummary> getMonthlyDisbursementStats() {
        log.debug("[DB2-CALL-3-REPORT] Monthly disbursement stats GROUP BY YEAR/MONTH");
        String sql = """
                SELECT CHAR(YEAR(DISBURSEMENT_DATE)) || '-'
                       || LPAD(CHAR(MONTH(DISBURSEMENT_DATE)), 2, '0')  AS PERIOD,
                       LOAN_TYPE                                          AS CATEGORY,
                       COUNT(*)                                           AS LOAN_COUNT,
                       SUM(APPROVED_AMOUNT)                               AS TOTAL_AMOUNT,
                       AVG(APPROVED_AMOUNT)                               AS AVG_AMOUNT
                FROM   FREDDIE_LOANS.LOAN_APPLICATIONS
                WHERE  LOAN_STATUS     = 'DISBURSED'
                  AND  DISBURSEMENT_DATE IS NOT NULL
                GROUP BY YEAR(DISBURSEMENT_DATE), MONTH(DISBURSEMENT_DATE), LOAN_TYPE
                ORDER BY PERIOD DESC, TOTAL_AMOUNT DESC
                """;
        return jdbcTemplate.query(sql, REPORT_ROW_MAPPER);
    }

    // ================================================================== //
    //  DB2 CALL #4 — Top 10 loans by amount                              //
    // ================================================================== //
    public List<Map<String, Object>> getTopLoanAmounts(int topN) {
        log.debug("[DB2-CALL-4-REPORT] SELECT top {} loans by LOAN_AMOUNT", topN);
        String sql = """
                SELECT la.LOAN_ID,
                       TRIM(c.FIRST_NAME) || ' ' || TRIM(c.LAST_NAME) AS CUSTOMER_NAME,
                       la.LOAN_TYPE, la.LOAN_AMOUNT, la.LOAN_STATUS,
                       la.APPLICATION_DATE
                FROM   FREDDIE_LOANS.LOAN_APPLICATIONS la
                LEFT JOIN FREDDIE_LOANS.CUSTOMERS c ON la.CUSTOMER_ID = c.CUSTOMER_ID
                ORDER BY la.LOAN_AMOUNT DESC
                FETCH FIRST ? ROWS ONLY
                """;
        return jdbcTemplate.queryForList(sql, topN);
    }

    // ================================================================== //
    //  DB2 CALL #5 — Approval rate per loan type (subquery)              //
    // ================================================================== //
    public List<Map<String, Object>> getApprovalRateByLoanType() {
        log.debug("[DB2-CALL-5-REPORT] Approval rate per loan type (subquery)");
        String sql = """
                SELECT total.LOAN_TYPE,
                       total.TOTAL_COUNT,
                       COALESCE(approved.APPROVED_COUNT, 0) AS APPROVED_COUNT,
                       CASE WHEN total.TOTAL_COUNT > 0
                            THEN DECIMAL(COALESCE(approved.APPROVED_COUNT, 0), 10, 2)
                                 / total.TOTAL_COUNT * 100
                            ELSE 0
                       END AS APPROVAL_RATE_PCT
                FROM (
                    SELECT LOAN_TYPE, COUNT(*) AS TOTAL_COUNT
                    FROM   FREDDIE_LOANS.LOAN_APPLICATIONS
                    GROUP BY LOAN_TYPE
                ) total
                LEFT JOIN (
                    SELECT LOAN_TYPE, COUNT(*) AS APPROVED_COUNT
                    FROM   FREDDIE_LOANS.LOAN_APPLICATIONS
                    WHERE  LOAN_STATUS IN ('APPROVED', 'DISBURSED')
                    GROUP BY LOAN_TYPE
                ) approved ON total.LOAN_TYPE = approved.LOAN_TYPE
                ORDER BY APPROVAL_RATE_PCT DESC
                """;
        return jdbcTemplate.queryForList(sql);
    }
}
