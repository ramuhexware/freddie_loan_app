package com.freddieapp.strutsportal.action;

import com.freddieapp.strutsportal.dao.LoanDao;
import com.freddieapp.strutsportal.dao.UnderwritingDao;
import com.freddieapp.strutsportal.model.LoanApplication;
import com.freddieapp.strutsportal.model.UnderwritingDecision;
import com.opensymphony.xwork2.ActionSupport;
import jakarta.servlet.http.HttpSession;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Struts 2 Action for Underwriting screens.
 *
 * Methods:
 *  - list()          — DB2 SELECT all underwriting decisions
 *  - review()        — DB2 SELECT loan + latest decision for review form
 *  - saveDecision()  — DB2 INSERT new underwriting decision + DB2 UPDATE loan status
 *  - pendingReviews()— DB2 SELECT all loans in UNDER_REVIEW state
 */
@Slf4j
@Getter
@Setter
public class UnderwritingAction extends ActionSupport {

    @Autowired private UnderwritingDao underwritingDao;
    @Autowired private LoanDao         loanDao;

    // ---- Form parameters ----
    private String     loanId;
    private String     underwriterId;
    private String     underwriterName;
    private String     decision;         // APPROVED | REJECTED | CONDITIONAL
    private BigDecimal approvedAmount;
    private BigDecimal recommendedRate;
    private String     conditions;
    private String     rejectionReason;
    private Integer    debtToIncomeRatio;
    private Integer    loanToValueRatio;
    private Integer    creditScoreUsed;
    private String     riskCategory;
    private String     notes;

    // ---- View data ----
    private List<UnderwritingDecision> decisions        = new ArrayList<>();
    private List<UnderwritingDecision> pendingDecisions = new ArrayList<>();
    private UnderwritingDecision       currentDecision;
    private LoanApplication            loan;

    // ================================================================== //
    //  list() — DB2 SELECT all decisions (for a loan or all)             //
    // ================================================================== //
    public String list() {
        log.info("[UnderwritingAction.list] loanId={}", loanId);

        if (StringUtils.isNotBlank(loanId)) {
            // DB2 CALL — decisions for one loan
            decisions = underwritingDao.findDecisionsByLoan(loanId);
        } else {
            // DB2 CALL — pending reviews (loans with UNDER_REVIEW status)
            decisions = underwritingDao.findPendingReviews();
        }

        log.info("[UnderwritingAction.list] Loaded {} decisions", decisions.size());
        return SUCCESS;
    }

    // ================================================================== //
    //  review() — Load loan + latest decision for the review form        //
    // ================================================================== //
    public String review() {
        log.info("[UnderwritingAction.review] loanId={}", loanId);

        // DB2 CALL — load the loan being reviewed
        loan = loanDao.findLoanById(loanId).orElse(null);
        if (loan == null) {
            addActionError("Loan not found: " + loanId);
            return ERROR;
        }

        // DB2 CALL — load latest previous decision (if any)
        currentDecision = underwritingDao.findLatestDecisionByLoanId(loanId).orElse(null);

        return SUCCESS;
    }

    // ================================================================== //
    //  saveDecision() — DB2 INSERT decision + DB2 UPDATE loan status     //
    // ================================================================== //
    public String saveDecision() {
        log.info("[UnderwritingAction.saveDecision] loanId={} decision={}", loanId, decision);

        // Server-side validation
        if (StringUtils.isBlank(loanId)) {
            addFieldError("loanId", "Loan ID is required");
        }
        if (StringUtils.isBlank(decision)) {
            addFieldError("decision", "Decision (APPROVED/REJECTED/CONDITIONAL) is required");
        }
        if ("REJECTED".equals(decision) && StringUtils.isBlank(rejectionReason)) {
            addFieldError("rejectionReason", "Rejection reason is required when rejecting");
        }
        if ("APPROVED".equals(decision) && approvedAmount == null) {
            addFieldError("approvedAmount", "Approved amount is required when approving");
        }
        if (hasFieldErrors()) {
            // Reload the loan for the form re-display
            loan = loanDao.findLoanById(loanId).orElse(null);
            return INPUT;
        }

        // Build decision model
        UnderwritingDecision decisionModel = UnderwritingDecision.builder()
                .decisionId(UUID.randomUUID().toString())
                .loanId(loanId)
                .underwriterId(underwriterId)
                .underwriterName(underwriterName)
                .decision(decision)
                .approvedAmount(approvedAmount)
                .recommendedRate(recommendedRate)
                .conditions(conditions)
                .rejectionReason(rejectionReason)
                .debtToIncomeRatio(debtToIncomeRatio)
                .loanToValueRatio(loanToValueRatio)
                .creditScoreUsed(creditScoreUsed)
                .riskCategory(riskCategory)
                .notes(notes)
                .build();

        // DB2 CALL — insert underwriting decision
        underwritingDao.insertDecision(decisionModel);

        // DB2 CALL — update loan status based on decision
        String newLoanStatus = switch (decision) {
            case "APPROVED"    -> "APPROVED";
            case "REJECTED"    -> "REJECTED";
            case "CONDITIONAL" -> "UNDER_REVIEW"; // remains for re-evaluation
            default            -> "UNDER_REVIEW";
        };
        loanDao.updateLoanDecision(loanId, newLoanStatus, approvedAmount, rejectionReason);

        addActionMessage("Underwriting decision '" + decision + "' saved for loan " + loanId);
        return SUCCESS;
    }

    // ================================================================== //
    //  pendingReviews() — DB2 SELECT loans awaiting underwriting         //
    // ================================================================== //
    public String pendingReviews() {
        log.info("[UnderwritingAction.pendingReviews] Loading UNDER_REVIEW loans");

        // DB2 CALL — loans pending underwriting review
        pendingDecisions = underwritingDao.findPendingReviews();

        log.info("[UnderwritingAction.pendingReviews] {} loans pending review", pendingDecisions.size());
        return SUCCESS;
    }
}
