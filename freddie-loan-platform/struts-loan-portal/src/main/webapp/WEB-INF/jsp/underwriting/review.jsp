<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c"   uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"  %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <title>Underwriting Review — Freddie Mac Portal</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet"/>
    <style>
        *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: 'Inter', sans-serif; background: #f1f5f9; color: #1e293b; }
        .page-body { padding: 28px 32px; max-width: 900px; }
        .page-title { font-size: 22px; font-weight: 700; color: #0a2342; margin-bottom: 24px; }
        .card { background: #fff; border-radius: 12px; padding: 28px; box-shadow: 0 1px 4px rgba(0,0,0,.08); margin-bottom: 20px; }
        .card-title { font-size: 14px; font-weight: 700; color: #0a2342; margin-bottom: 18px;
                      padding-bottom: 12px; border-bottom: 1px solid #e5e7eb; }
        .info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 24px; }
        .info-item label { display: block; font-size: 11px; font-weight: 600; color: #9ca3af; text-transform: uppercase; }
        .info-item span  { font-size: 15px; font-weight: 600; color: #0a2342; }

        .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
        .form-group { margin-bottom: 0; }
        .form-group.full { grid-column: 1 / -1; }
        label { display: block; font-size: 12px; font-weight: 700; color: #374151;
                text-transform: uppercase; margin-bottom: 5px; }
        input[type=text], input[type=number], select, textarea {
            width: 100%; padding: 10px 12px; border: 1.5px solid #d1d5db;
            border-radius: 8px; font-size: 13px; font-family: inherit;
        }
        input:focus, select:focus, textarea:focus {
            outline: none; border-color: #0d6efd; box-shadow: 0 0 0 3px rgba(13,110,253,.12);
        }
        .error-text { color: #dc2626; font-size: 12px; margin-top: 3px; }
        .btn-row { grid-column: 1 / -1; display: flex; gap: 12px; padding-top: 8px; }
        .btn { padding: 12px 24px; border-radius: 8px; border: none; font-size: 13px;
               font-weight: 600; cursor: pointer; }
        .btn-success { background: #22c55e; color: #fff; }
        .btn-success:hover { background: #16a34a; }
        .btn-danger  { background: #ef4444; color: #fff; }
    </style>
</head>
<body>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<div class="page-body">
    <a href="${pageContext.request.contextPath}/underwriting/list.action"
       style="color:#6b7280;text-decoration:none;font-size:13px;">← Pending Reviews</a>
    <h1 class="page-title" style="margin-top:12px;">📋 Underwriting Review — ${loan.loanId}</h1>

    <%-- Loan info summary --%>
    <c:if test="${not empty loan}">
        <div class="card">
            <div class="card-title">Loan Under Review (from DB2)</div>
            <div class="info-grid">
                <div class="info-item">
                    <label>Customer</label>
                    <span>${loan.customerName}</span>
                </div>
                <div class="info-item">
                    <label>Loan Type</label>
                    <span>${loan.loanType}</span>
                </div>
                <div class="info-item">
                    <label>Requested Amount</label>
                    <span>$<fmt:formatNumber value="${loan.loanAmount}" pattern="#,###.##"/></span>
                </div>
                <div class="info-item">
                    <label>Property Value</label>
                    <span>$<fmt:formatNumber value="${loan.propertyValue}" pattern="#,###.##"/></span>
                </div>
                <div class="info-item">
                    <label>Property Address</label>
                    <span>${loan.propertyAddress}</span>
                </div>
                <div class="info-item">
                    <label>Term</label>
                    <span>${loan.loanTermMonths} months</span>
                </div>
            </div>
        </div>
    </c:if>

    <%-- Decision form --%>
    <div class="card">
        <div class="card-title">Submit Underwriting Decision (INSERT → DB2 + UPDATE loan status)</div>

        <c:if test="${not empty actionErrors}">
            <div style="background:#fee2e2;border:1px solid #fca5a5;border-radius:8px;
                        padding:12px;margin-bottom:16px;font-size:13px;color:#991b1b;">
                <c:forEach var="e" items="${actionErrors}"><div>${e}</div></c:forEach>
            </div>
        </c:if>

        <form method="post" action="${pageContext.request.contextPath}/underwriting/saveDecision.action"
              id="uw-decision-form">
            <input type="hidden" name="loanId" value="${loan.loanId}"/>
            <div class="form-grid">
                <div class="form-group">
                    <label for="underwriterId">Underwriter ID</label>
                    <input type="text" id="underwriterId" name="underwriterId"
                           placeholder="e.g. UW-001" value="${sessionScope.loggedInUser}"/>
                </div>
                <div class="form-group">
                    <label for="underwriterName">Underwriter Name</label>
                    <input type="text" id="underwriterName" name="underwriterName"
                           placeholder="Full Name"/>
                </div>
                <div class="form-group">
                    <label for="decision">Decision *</label>
                    <select id="decision" name="decision" required>
                        <option value="">-- Select Decision --</option>
                        <option value="APPROVED">APPROVED</option>
                        <option value="REJECTED">REJECTED</option>
                        <option value="CONDITIONAL">CONDITIONAL</option>
                    </select>
                    <c:if test="${not empty fieldErrors.decision}">
                        <div class="error-text">${fieldErrors.decision[0]}</div>
                    </c:if>
                </div>
                <div class="form-group">
                    <label for="approvedAmount">Approved Amount ($)</label>
                    <input type="number" id="approvedAmount" name="approvedAmount"
                           step="0.01" min="0" placeholder="e.g. 435000.00"
                           value="${approvedAmount}"/>
                    <c:if test="${not empty fieldErrors.approvedAmount}">
                        <div class="error-text">${fieldErrors.approvedAmount[0]}</div>
                    </c:if>
                </div>
                <div class="form-group">
                    <label for="recommendedRate">Recommended Rate (%)</label>
                    <input type="number" id="recommendedRate" name="recommendedRate"
                           step="0.0001" min="0" placeholder="e.g. 6.7500"/>
                </div>
                <div class="form-group">
                    <label for="riskCategory">Risk Category</label>
                    <select id="riskCategory" name="riskCategory">
                        <option value="LOW">LOW</option>
                        <option value="MEDIUM" selected>MEDIUM</option>
                        <option value="HIGH">HIGH</option>
                    </select>
                </div>
                <div class="form-group">
                    <label for="creditScoreUsed">Credit Score Used</label>
                    <input type="number" id="creditScoreUsed" name="creditScoreUsed"
                           min="300" max="850" placeholder="e.g. 720"/>
                </div>
                <div class="form-group">
                    <label for="debtToIncomeRatio">Debt-to-Income Ratio (%)</label>
                    <input type="number" id="debtToIncomeRatio" name="debtToIncomeRatio"
                           min="0" max="100" placeholder="e.g. 36"/>
                </div>
                <div class="form-group">
                    <label for="loanToValueRatio">Loan-to-Value Ratio (%)</label>
                    <input type="number" id="loanToValueRatio" name="loanToValueRatio"
                           min="0" max="100" placeholder="e.g. 80"/>
                </div>
                <div class="form-group full">
                    <label for="conditions">Conditions (for CONDITIONAL decision)</label>
                    <textarea id="conditions" name="conditions" rows="2"
                              placeholder="List any conditions attached to approval..."></textarea>
                </div>
                <div class="form-group full">
                    <label for="rejectionReason">Rejection Reason (required if REJECTED)</label>
                    <textarea id="rejectionReason" name="rejectionReason" rows="2"
                              placeholder="State the specific reason for rejection..."></textarea>
                    <c:if test="${not empty fieldErrors.rejectionReason}">
                        <div class="error-text">${fieldErrors.rejectionReason[0]}</div>
                    </c:if>
                </div>
                <div class="form-group full">
                    <label for="notes">Additional Notes</label>
                    <textarea id="notes" name="notes" rows="3"
                              placeholder="Any additional underwriter notes..."></textarea>
                </div>
                <div class="btn-row">
                    <button type="submit" class="btn btn-success" id="btn-save-decision">
                        💾 Save Decision (DB2 INSERT + UPDATE)
                    </button>
                    <a href="${pageContext.request.contextPath}/underwriting/list.action"
                       class="btn" style="background:#e5e7eb;color:#374151;text-decoration:none;">
                        Cancel
                    </a>
                </div>
            </div>
        </form>
    </div>
</div>
</body>
</html>
