package com.freddieapp.strutsportal.action;

import com.freddieapp.strutsportal.client.LoanServiceClient;
import com.freddieapp.strutsportal.dao.LoanDao;
import com.freddieapp.strutsportal.dao.UnderwritingDao;
import com.freddieapp.strutsportal.model.LoanApplication;
import com.freddieapp.strutsportal.model.UnderwritingDecision;
import com.opensymphony.xwork2.ActionSupport;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Struts 2 Action for all Loan Application screens.
 *
 * Methods:
 *  - list()              — load all loans from DB2 (with optional filters)
 *  - detail()            — load single loan + underwriting decisions from DB2
 *  - applyForm()         — show application form
 *  - save()              — submit new loan via REST to loan-origination-service
 *  - submitUnderwriting()— POST to loan-origination-service REST to start UW
 *  - approve()           — UPDATE DB2 status to APPROVED
 *  - reject()            — UPDATE DB2 status to REJECTED with reason
 *  - search()            — filtered search via DB2
 */
@Slf4j
@Getter
@Setter
public class LoanAction extends ActionSupport {

    @Autowired private LoanDao            loanDao;
    @Autowired private UnderwritingDao    underwritingDao;
    @Autowired private LoanServiceClient  loanServiceClient;

    // ---- List / search form parameters ----
    private String filterStatus;
    private String filterLoanType;
    private String filterFromDate;
    private String filterToDate;

    // ---- Detail ----
    private String loanId;

    // ---- Apply form fields ----
    private String     customerId;
    private String     loanType;
    private BigDecimal loanAmount;
    private BigDecimal propertyValue;
    private String     propertyAddress;
    private Integer    loanTermMonths;

    // ---- Approve/Reject ----
    private BigDecimal approvedAmount;
    private String     rejectionReason;

    // ---- View data ----
    private List<LoanApplication>   loans         = new ArrayList<>();
    private LoanApplication         loan;
    private List<UnderwritingDecision> decisions   = new ArrayList<>();
    private Map<String, Object>     serviceResponse;

    // ================================================================== //
    //  list() — DB2 SELECT all loans (with optional status/type filter)  //
    // ================================================================== //
    public String list() {
        log.info("[LoanAction.list] filterStatus={} filterType={}", filterStatus, filterLoanType);

        if (StringUtils.isNotBlank(filterStatus) && StringUtils.isNotBlank(filterLoanType)) {
            // DB2 CALL — compound filter
            loans = loanDao.findLoansByTypeAndStatus(filterLoanType, filterStatus);
        } else if (StringUtils.isNotBlank(filterStatus)) {
            // DB2 CALL — status filter
            loans = loanDao.findLoansByStatus(filterStatus);
        } else if (StringUtils.isNotBlank(filterFromDate) && StringUtils.isNotBlank(filterToDate)) {
            // DB2 CALL — date range filter
            loans = loanDao.findLoansByDateRange(
                    LocalDateTime.parse(filterFromDate + "T00:00:00"),
                    LocalDateTime.parse(filterToDate + "T23:59:59"));
        } else {
            // DB2 CALL — all loans
            loans = loanDao.findAllLoans();
        }

        log.info("[LoanAction.list] Loaded {} loans", loans.size());
        return SUCCESS;
    }

    // ================================================================== //
    //  detail() — DB2 SELECT single loan + underwriting history          //
    // ================================================================== //
    public String detail() {
        log.info("[LoanAction.detail] loanId={}", loanId);

        // DB2 CALL — loan by ID
        loan = loanDao.findLoanById(loanId)
                .orElse(null);

        if (loan == null) {
            addActionError("Loan not found: " + loanId);
            return "notFound";
        }

        // DB2 CALL — underwriting decisions for this loan
        decisions = underwritingDao.findDecisionsByLoan(loanId);

        return SUCCESS;
    }

    // ================================================================== //
    //  applyForm() — Just display the empty form                        //
    // ================================================================== //
    public String applyForm() {
        return SUCCESS;
    }

    // ================================================================== //
    //  save() — Validate form, POST to loan-origination-service REST    //
    // ================================================================== //
    public String save() {
        log.info("[LoanAction.save] Submitting loan for customerId={} amount={}", customerId, loanAmount);

        // Basic server-side validation
        if (StringUtils.isBlank(customerId)) {
            addFieldError("customerId", "Customer ID is required");
        }
        if (StringUtils.isBlank(loanType)) {
            addFieldError("loanType", "Loan type is required");
        }
        if (loanAmount == null || loanAmount.compareTo(BigDecimal.ZERO) <= 0) {
            addFieldError("loanAmount", "Loan amount must be a positive number");
        }
        if (hasFieldErrors()) return INPUT;

        // Build REST payload
        Map<String, Object> payload = Map.of(
                "customerId",      customerId,
                "loanType",        loanType,
                "loanAmount",      loanAmount,
                "propertyValue",   propertyValue != null ? propertyValue : BigDecimal.ZERO,
                "propertyAddress", StringUtils.defaultString(propertyAddress),
                "loanTermMonths",  loanTermMonths != null ? loanTermMonths : 360
        );

        // REST CALL → loan-origination-service
        serviceResponse = loanServiceClient.submitLoanApplication(payload);

        if (serviceResponse.containsKey("error")) {
            addActionError("Failed to submit loan: " + serviceResponse.get("error"));
            return ERROR;
        }

        addActionMessage("Loan application submitted successfully!");
        return SUCCESS;
    }

    // ================================================================== //
    //  submitUnderwriting() — POST via REST to send loan to UW           //
    // ================================================================== //
    public String submitUnderwriting() {
        log.info("[LoanAction.submitUnderwriting] loanId={}", loanId);

        // REST CALL → loan-origination-service
        serviceResponse = loanServiceClient.submitForUnderwriting(loanId);

        if (serviceResponse.containsKey("error")) {
            addActionError("Failed to submit for underwriting: " + serviceResponse.get("error"));
            // Reload loan detail for the error view
            loan = loanDao.findLoanById(loanId).orElse(null);
            return ERROR;
        }

        addActionMessage("Loan " + loanId + " submitted for underwriting!");
        return SUCCESS;
    }

    // ================================================================== //
    //  approve() — DB2 UPDATE loan to APPROVED                           //
    // ================================================================== //
    public String approve() {
        log.info("[LoanAction.approve] Approving loanId={} approvedAmount={}", loanId, approvedAmount);

        if (approvedAmount == null || approvedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            addFieldError("approvedAmount", "Approved amount must be specified");
            loan = loanDao.findLoanById(loanId).orElse(null);
            return ERROR;
        }

        // DB2 CALL — update loan decision
        int rows = loanDao.updateLoanDecision(loanId, "APPROVED", approvedAmount, null);
        if (rows == 0) {
            addActionError("Loan not found or update failed: " + loanId);
            return ERROR;
        }

        addActionMessage("Loan " + loanId + " approved for $" + approvedAmount);
        return SUCCESS;
    }

    // ================================================================== //
    //  reject() — DB2 UPDATE loan to REJECTED with reason                //
    // ================================================================== //
    public String reject() {
        log.info("[LoanAction.reject] Rejecting loanId={} reason={}", loanId, rejectionReason);

        if (StringUtils.isBlank(rejectionReason)) {
            addFieldError("rejectionReason", "Rejection reason is required");
            loan = loanDao.findLoanById(loanId).orElse(null);
            return ERROR;
        }

        // DB2 CALL — update loan decision
        int rows = loanDao.updateLoanDecision(loanId, "REJECTED", null, rejectionReason);
        if (rows == 0) {
            addActionError("Loan not found or update failed: " + loanId);
            return ERROR;
        }

        addActionMessage("Loan " + loanId + " rejected.");
        return SUCCESS;
    }

    // ================================================================== //
    //  search() — DB2 search with date range                             //
    // ================================================================== //
    public String search() {
        log.info("[LoanAction.search] status={} type={} from={} to={}",
                filterStatus, filterLoanType, filterFromDate, filterToDate);
        return list(); // delegates to list() with filters populated
    }
}
