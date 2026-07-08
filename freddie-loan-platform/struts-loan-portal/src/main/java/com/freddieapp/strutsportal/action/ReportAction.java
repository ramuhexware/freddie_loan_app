package com.freddieapp.strutsportal.action;

import com.freddieapp.strutsportal.dao.AuditDao;
import com.freddieapp.strutsportal.dao.ReportDao;
import com.freddieapp.strutsportal.model.AuditLog;
import com.freddieapp.strutsportal.model.ReportSummary;
import com.opensymphony.xwork2.ActionSupport;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Struts 2 Action for Reports.
 *
 * Methods:
 *  - loanSummary()   — loan summary by type and by status from DB2
 *  - monthlyStats()  — monthly disbursement stats from DB2
 *  - auditLog()      — recent audit log entries from DB2
 */
@Slf4j
@Getter
@Setter
public class ReportAction extends ActionSupport {

    @Autowired private ReportDao reportDao;
    @Autowired private AuditDao  auditDao;

    // ---- View data ----
    private List<ReportSummary>      loansByType           = new ArrayList<>();
    private List<ReportSummary>      loansByStatus         = new ArrayList<>();
    private List<ReportSummary>      monthlyDisbursements  = new ArrayList<>();
    private List<Map<String, Object>> topLoans             = new ArrayList<>();
    private List<Map<String, Object>> approvalRates        = new ArrayList<>();
    private List<AuditLog>           recentAuditLogs       = new ArrayList<>();

    // ================================================================== //
    //  loanSummary() — DB2 aggregations by type and status               //
    // ================================================================== //
    public String loanSummary() {
        log.info("[ReportAction.loanSummary] Loading loan summary report from DB2");

        // DB2 CALL — summary by type
        loansByType = reportDao.getLoanSummaryByType();

        // DB2 CALL — summary by status
        loansByStatus = reportDao.getLoanSummaryByStatus();

        // DB2 CALL — top 10 loans by amount
        topLoans = reportDao.getTopLoanAmounts(10);

        // DB2 CALL — approval rates per loan type
        approvalRates = reportDao.getApprovalRateByLoanType();

        log.info("[ReportAction.loanSummary] Report loaded — {} types, {} statuses",
                loansByType.size(), loansByStatus.size());
        return SUCCESS;
    }

    // ================================================================== //
    //  monthlyStats() — DB2 GROUP BY month/year disbursement stats       //
    // ================================================================== //
    public String monthlyStats() {
        log.info("[ReportAction.monthlyStats] Loading monthly disbursement stats from DB2");

        // DB2 CALL — monthly disbursement groupings
        monthlyDisbursements = reportDao.getMonthlyDisbursementStats();

        log.info("[ReportAction.monthlyStats] Loaded {} monthly stat rows", monthlyDisbursements.size());
        return SUCCESS;
    }

    // ================================================================== //
    //  auditLog() — DB2 SELECT recent 100 audit rows                     //
    // ================================================================== //
    public String auditLog() {
        log.info("[ReportAction.auditLog] Loading recent audit log entries from DB2");

        // DB2 CALL — recent 100 audit entries
        recentAuditLogs = auditDao.findRecentAuditLogs(100);

        log.info("[ReportAction.auditLog] Loaded {} audit entries", recentAuditLogs.size());
        return SUCCESS;
    }
}
