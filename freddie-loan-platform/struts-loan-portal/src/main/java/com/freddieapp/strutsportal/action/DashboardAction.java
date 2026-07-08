package com.freddieapp.strutsportal.action;

import com.freddieapp.strutsportal.dao.LoanDao;
import com.freddieapp.strutsportal.dao.CustomerDao;
import com.freddieapp.strutsportal.dao.AuditDao;
import com.freddieapp.strutsportal.model.LoanApplication;
import com.opensymphony.xwork2.ActionSupport;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Struts 2 Action for the Dashboard screen.
 * Aggregates KPI data from multiple DB2 queries and recent loans.
 */
@Slf4j
@Getter
@Setter
public class DashboardAction extends ActionSupport {

    @Autowired private LoanDao     loanDao;
    @Autowired private CustomerDao customerDao;
    @Autowired private AuditDao    auditDao;

    // ---- view-layer data ----
    private List<LoanApplication>    recentLoans;
    private List<Map<String, Object>> loanCountByType;
    private List<Map<String, Object>> loanAmountByStatus;
    private List<Map<String, Object>> customerStatusSummary;

    private long     totalLoans;
    private long     totalCustomers;
    private BigDecimal totalDisbursedAmount;
    private long     pendingUnderwritingCount;

    @Override
    public String execute() {
        log.info("[Dashboard] Loading KPI data from DB2");

        // DB2 CALL — recent 10 loans
        recentLoans = loanDao.findRecentLoans(10);

        // DB2 CALL — loan counts per type
        loanCountByType = loanDao.countLoansByType();

        // DB2 CALL — loan amounts per status
        loanAmountByStatus = loanDao.sumLoanAmountByStatus();

        // DB2 CALL — customer status breakdown
        customerStatusSummary = customerDao.countCustomersByStatus();

        // Derive KPI totals from aggregated data
        totalLoans = loanCountByType.stream()
                .mapToLong(m -> ((Number) m.get("LOAN_COUNT")).longValue())
                .sum();

        totalCustomers = customerStatusSummary.stream()
                .mapToLong(m -> ((Number) m.get("CUSTOMER_COUNT")).longValue())
                .sum();

        totalDisbursedAmount = loanAmountByStatus.stream()
                .filter(m -> "DISBURSED".equals(m.get("LOAN_STATUS")))
                .map(m -> (BigDecimal) m.get("TOTAL_AMOUNT"))
                .findFirst().orElse(BigDecimal.ZERO);

        pendingUnderwritingCount = loanAmountByStatus.stream()
                .filter(m -> "UNDER_REVIEW".equals(m.get("LOAN_STATUS")))
                .mapToLong(m -> ((Number) m.get("LOAN_COUNT")).longValue())
                .sum();

        log.info("[Dashboard] KPIs loaded — totalLoans={} totalCustomers={} pendingUW={}",
                totalLoans, totalCustomers, pendingUnderwritingCount);
        return SUCCESS;
    }
}
