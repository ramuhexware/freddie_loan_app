package com.freddieapp.strutsportal.dao;

import com.freddieapp.strutsportal.model.UnderwritingDecision;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Underwriting Decisions.
 * Executes DB2 queries against FREDDIE_LOANS.UNDERWRITING_DECISIONS.
 *
 * DB2 Calls:
 *  1. findDecisionsByLoan()    — SELECT all decisions for a loan
 *  2. findPendingReviews()     — SELECT loans awaiting underwriting
 *  3. insertDecision()         — INSERT a new underwriting decision
 *  4. findDecisionByLoanId()   — SELECT latest decision for a loan
 *  5. findDecisionsByRisk()    — SELECT filtered by risk category
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class UnderwritingDao {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<UnderwritingDecision> DECISION_ROW_MAPPER = (rs, rowNum) -> {
        UnderwritingDecision d = new UnderwritingDecision();
        d.setDecisionId(rs.getString("DECISION_ID"));
        d.setLoanId(rs.getString("LOAN_ID"));
        d.setUnderwriterId(rs.getString("UNDERWRITER_ID"));
        d.setUnderwriterName(rs.getString("UNDERWRITER_NAME"));
        d.setDecision(rs.getString("DECISION"));
        d.setApprovedAmount(rs.getBigDecimal("APPROVED_AMOUNT"));
        d.setRecommendedRate(rs.getBigDecimal("RECOMMENDED_RATE"));
        d.setConditions(rs.getString("CONDITIONS"));
        d.setRejectionReason(rs.getString("REJECTION_REASON"));
        d.setDebtToIncomeRatio(rs.getInt("DEBT_TO_INCOME_RATIO") == 0 ? null : rs.getInt("DEBT_TO_INCOME_RATIO"));
        d.setLoanToValueRatio(rs.getInt("LOAN_TO_VALUE_RATIO") == 0 ? null : rs.getInt("LOAN_TO_VALUE_RATIO"));
        d.setCreditScoreUsed(rs.getInt("CREDIT_SCORE_USED") == 0 ? null : rs.getInt("CREDIT_SCORE_USED"));
        d.setRiskCategory(rs.getString("RISK_CATEGORY"));
        d.setNotes(rs.getString("NOTES"));
        Timestamp dt = rs.getTimestamp("DECISION_DATE");
        if (dt != null) d.setDecisionDate(dt.toLocalDateTime());
        Timestamp ct = rs.getTimestamp("CREATED_AT");
        if (ct != null) d.setCreatedAt(ct.toLocalDateTime());
        return d;
    };

    // ================================================================== //
    //  DB2 CALL #1 — Find all decisions for a loan (history)             //
    // ================================================================== //
    public List<UnderwritingDecision> findDecisionsByLoan(String loanId) {
        log.debug("[DB2-CALL-1] SELECT underwriting decisions for LOAN_ID={}", loanId);
        String sql = """
                SELECT DECISION_ID, LOAN_ID, UNDERWRITER_ID, UNDERWRITER_NAME,
                       DECISION, APPROVED_AMOUNT, RECOMMENDED_RATE, CONDITIONS,
                       REJECTION_REASON, DEBT_TO_INCOME_RATIO, LOAN_TO_VALUE_RATIO,
                       CREDIT_SCORE_USED, RISK_CATEGORY, NOTES, DECISION_DATE, CREATED_AT
                FROM   FREDDIE_LOANS.UNDERWRITING_DECISIONS
                WHERE  LOAN_ID = ?
                ORDER BY CREATED_AT DESC
                """;
        return jdbcTemplate.query(sql, DECISION_ROW_MAPPER, loanId);
    }

    // ================================================================== //
    //  DB2 CALL #2 — Find all loans currently pending underwriting review //
    // ================================================================== //
    public List<UnderwritingDecision> findPendingReviews() {
        log.debug("[DB2-CALL-2] SELECT loans pending underwriting (LOAN_STATUS=UNDER_REVIEW)");
        String sql = """
                SELECT ud.DECISION_ID, ud.LOAN_ID, ud.UNDERWRITER_ID, ud.UNDERWRITER_NAME,
                       ud.DECISION, ud.APPROVED_AMOUNT, ud.RECOMMENDED_RATE, ud.CONDITIONS,
                       ud.REJECTION_REASON, ud.DEBT_TO_INCOME_RATIO, ud.LOAN_TO_VALUE_RATIO,
                       ud.CREDIT_SCORE_USED, ud.RISK_CATEGORY, ud.NOTES,
                       ud.DECISION_DATE, ud.CREATED_AT
                FROM   FREDDIE_LOANS.UNDERWRITING_DECISIONS ud
                JOIN   FREDDIE_LOANS.LOAN_APPLICATIONS la ON ud.LOAN_ID = la.LOAN_ID
                WHERE  la.LOAN_STATUS = 'UNDER_REVIEW'
                ORDER BY ud.CREATED_AT ASC
                """;
        return jdbcTemplate.query(sql, DECISION_ROW_MAPPER);
    }

    // ================================================================== //
    //  DB2 CALL #3 — INSERT a new underwriting decision                  //
    // ================================================================== //
    @Transactional
    public int insertDecision(UnderwritingDecision decision) {
        log.info("[DB2-CALL-3] INSERT underwriting decision for LOAN_ID={}", decision.getLoanId());
        String sql = """
                INSERT INTO FREDDIE_LOANS.UNDERWRITING_DECISIONS
                    (DECISION_ID, LOAN_ID, UNDERWRITER_ID, UNDERWRITER_NAME,
                     DECISION, APPROVED_AMOUNT, RECOMMENDED_RATE, CONDITIONS,
                     REJECTION_REASON, DEBT_TO_INCOME_RATIO, LOAN_TO_VALUE_RATIO,
                     CREDIT_SCORE_USED, RISK_CATEGORY, NOTES,
                     DECISION_DATE, CREATED_AT)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """;
        return jdbcTemplate.update(sql,
                decision.getDecisionId(),
                decision.getLoanId(),
                decision.getUnderwriterId(),
                decision.getUnderwriterName(),
                decision.getDecision(),
                decision.getApprovedAmount(),
                decision.getRecommendedRate(),
                decision.getConditions(),
                decision.getRejectionReason(),
                decision.getDebtToIncomeRatio(),
                decision.getLoanToValueRatio(),
                decision.getCreditScoreUsed(),
                decision.getRiskCategory(),
                decision.getNotes()
        );
    }

    // ================================================================== //
    //  DB2 CALL #4 — Find latest decision for a specific loan            //
    // ================================================================== //
    public Optional<UnderwritingDecision> findLatestDecisionByLoanId(String loanId) {
        log.debug("[DB2-CALL-4] SELECT latest underwriting decision for LOAN_ID={}", loanId);
        String sql = """
                SELECT DECISION_ID, LOAN_ID, UNDERWRITER_ID, UNDERWRITER_NAME,
                       DECISION, APPROVED_AMOUNT, RECOMMENDED_RATE, CONDITIONS,
                       REJECTION_REASON, DEBT_TO_INCOME_RATIO, LOAN_TO_VALUE_RATIO,
                       CREDIT_SCORE_USED, RISK_CATEGORY, NOTES, DECISION_DATE, CREATED_AT
                FROM   FREDDIE_LOANS.UNDERWRITING_DECISIONS
                WHERE  LOAN_ID = ?
                ORDER BY CREATED_AT DESC
                FETCH FIRST 1 ROWS ONLY
                """;
        List<UnderwritingDecision> results = jdbcTemplate.query(sql, DECISION_ROW_MAPPER, loanId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    // ================================================================== //
    //  DB2 CALL #5 — Find all decisions by risk category                 //
    // ================================================================== //
    public List<UnderwritingDecision> findDecisionsByRiskCategory(String riskCategory) {
        log.debug("[DB2-CALL-5] SELECT underwriting decisions by RISK_CATEGORY={}", riskCategory);
        String sql = """
                SELECT DECISION_ID, LOAN_ID, UNDERWRITER_ID, UNDERWRITER_NAME,
                       DECISION, APPROVED_AMOUNT, RECOMMENDED_RATE, CONDITIONS,
                       REJECTION_REASON, DEBT_TO_INCOME_RATIO, LOAN_TO_VALUE_RATIO,
                       CREDIT_SCORE_USED, RISK_CATEGORY, NOTES, DECISION_DATE, CREATED_AT
                FROM   FREDDIE_LOANS.UNDERWRITING_DECISIONS
                WHERE  RISK_CATEGORY = ?
                ORDER BY CREATED_AT DESC
                """;
        return jdbcTemplate.query(sql, DECISION_ROW_MAPPER, riskCategory);
    }
}
