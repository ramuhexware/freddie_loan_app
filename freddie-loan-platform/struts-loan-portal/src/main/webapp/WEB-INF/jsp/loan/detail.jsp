<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c"   uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"  %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <title>Loan Detail — Freddie Mac Portal</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet"/>
    <style>
        *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: 'Inter', sans-serif; background: #f1f5f9; color: #1e293b; }
        .page-body { padding: 28px 32px; }
        .back-link { color: #6b7280; text-decoration: none; font-size: 13px; }
        .back-link:hover { color: #0d6efd; }
        .page-title { font-size: 22px; font-weight: 700; color: #0a2342; margin: 12px 0 24px; }

        .detail-grid { display: grid; grid-template-columns: 2fr 1fr; gap: 20px; }
        .card { background: #fff; border-radius: 12px; padding: 24px; box-shadow: 0 1px 4px rgba(0,0,0,.08); margin-bottom: 20px; }
        .card-title { font-size: 14px; font-weight: 700; color: #0a2342; padding-bottom: 12px;
                      border-bottom: 1px solid #e5e7eb; margin-bottom: 16px; }

        .field-row { display: flex; padding: 8px 0; border-bottom: 1px solid #f3f4f6; }
        .field-row:last-child { border-bottom: none; }
        .field-label { width: 180px; flex-shrink: 0; font-size: 12px; font-weight: 600; color: #6b7280; text-transform: uppercase; padding-top: 2px; }
        .field-value { flex: 1; font-size: 14px; color: #1e293b; }

        .badge { display:inline-block; padding:3px 10px; border-radius:12px; font-size:12px; font-weight:700; text-transform:uppercase; }
        .badge-green  { background:#dcfce7; color:#166534; }
        .badge-yellow { background:#fef9c3; color:#854d0e; }
        .badge-blue   { background:#dbeafe; color:#1e40af; }
        .badge-red    { background:#fee2e2; color:#991b1b; }
        .badge-gray   { background:#f3f4f6; color:#374151; }

        /* Action buttons panel */
        .action-panel { background: #f8fafc; border: 1px solid #e5e7eb; border-radius: 10px; padding: 20px; }
        .action-panel h3 { font-size: 13px; font-weight: 700; color: #0a2342; margin-bottom: 14px; }
        .btn { display: block; width: 100%; padding: 10px 0; border-radius: 7px; border: none;
               font-size: 13px; font-weight: 600; cursor: pointer; text-align: center;
               text-decoration: none; margin-bottom: 10px; transition: opacity .15s; }
        .btn:hover { opacity: .85; }
        .btn-success { background: #22c55e; color: #fff; }
        .btn-danger  { background: #ef4444; color: #fff; }
        .btn-primary { background: #0d6efd; color: #fff; }
        .btn-warning { background: #f59e0b; color: #fff; }
        .btn-disabled { background: #e5e7eb; color: #9ca3af; cursor: not-allowed; }

        /* UW decisions table */
        table { width: 100%; border-collapse: collapse; font-size: 13px; }
        th { text-align: left; padding: 8px 10px; font-size: 11px; font-weight: 600; color: #6b7280;
             text-transform: uppercase; border-bottom: 1px solid #e5e7eb; }
        td { padding: 10px 10px; border-bottom: 1px solid #f3f4f6; }
        tr:last-child td { border-bottom: none; }

        /* Inline approve/reject forms */
        .inline-form { margin-top: 16px; padding-top: 16px; border-top: 1px solid #e5e7eb; }
        .inline-form label { display: block; font-size: 12px; font-weight: 600; color: #374151; margin-bottom: 4px; }
        .inline-form input[type=number], .inline-form textarea {
            width: 100%; padding: 8px 10px; border: 1.5px solid #d1d5db; border-radius: 6px;
            font-size: 13px; font-family: inherit; margin-bottom: 10px;
        }
    </style>
</head>
<body>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<div class="page-body">
    <a href="${pageContext.request.contextPath}/loan/list.action" class="back-link">← Back to Loan List</a>

    <c:if test="${not empty loan}">
        <h1 class="page-title">Loan Detail
            <span style="font-size:14px;font-weight:400;color:#6b7280;">
                #${fn:substring(loan.loanId,0,8)}...
            </span>
        </h1>

        <div class="detail-grid">
            <%-- Left column: loan details --%>
            <div>
                <div class="card">
                    <div class="card-title">Loan Information (from DB2)</div>
                    <div class="field-row">
                        <div class="field-label">Loan ID</div>
                        <div class="field-value" style="font-family:monospace;font-size:12px;">${loan.loanId}</div>
                    </div>
                    <div class="field-row">
                        <div class="field-label">Customer ID</div>
                        <div class="field-value">
                            <a href="${pageContext.request.contextPath}/customer/detail.action?customerId=${loan.customerId}"
                               style="color:#0d6efd;">${loan.customerName} (${loan.customerId})</a>
                        </div>
                    </div>
                    <div class="field-row">
                        <div class="field-label">Loan Type</div>
                        <div class="field-value"><strong>${loan.loanType}</strong></div>
                    </div>
                    <div class="field-row">
                        <div class="field-label">Loan Amount</div>
                        <div class="field-value" style="font-size:18px;font-weight:700;color:#0a2342;">
                            $<fmt:formatNumber value="${loan.loanAmount}" pattern="#,###.##"/>
                        </div>
                    </div>
                    <div class="field-row">
                        <div class="field-label">Property Value</div>
                        <div class="field-value">$<fmt:formatNumber value="${loan.propertyValue}" pattern="#,###.##"/></div>
                    </div>
                    <div class="field-row">
                        <div class="field-label">Property Address</div>
                        <div class="field-value">${loan.propertyAddress}</div>
                    </div>
                    <div class="field-row">
                        <div class="field-label">Interest Rate</div>
                        <div class="field-value">${loan.interestRate}%</div>
                    </div>
                    <div class="field-row">
                        <div class="field-label">Term</div>
                        <div class="field-value">${loan.loanTermMonths} months</div>
                    </div>
                    <div class="field-row">
                        <div class="field-label">Status</div>
                        <div class="field-value">
                            <c:choose>
                                <c:when test="${loan.loanStatus == 'APPROVED'}"><span class="badge badge-green">${loan.loanStatus}</span></c:when>
                                <c:when test="${loan.loanStatus == 'REJECTED'}"><span class="badge badge-red">${loan.loanStatus}</span></c:when>
                                <c:when test="${loan.loanStatus == 'UNDER_REVIEW'}"><span class="badge badge-yellow">${loan.loanStatus}</span></c:when>
                                <c:when test="${loan.loanStatus == 'DISBURSED'}"><span class="badge badge-blue">${loan.loanStatus}</span></c:when>
                                <c:otherwise><span class="badge badge-gray">${loan.loanStatus}</span></c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                    <div class="field-row">
                        <div class="field-label">Applied</div>
                        <div class="field-value"><fmt:formatDate value="${loan.applicationDate}" pattern="yyyy-MM-dd HH:mm"/></div>
                    </div>
                    <c:if test="${not empty loan.decisionDate}">
                        <div class="field-row">
                            <div class="field-label">Decision Date</div>
                            <div class="field-value"><fmt:formatDate value="${loan.decisionDate}" pattern="yyyy-MM-dd HH:mm"/></div>
                        </div>
                    </c:if>
                    <c:if test="${not empty loan.approvedAmount}">
                        <div class="field-row">
                            <div class="field-label">Approved Amount</div>
                            <div class="field-value" style="color:#16a34a;font-weight:700;">
                                $<fmt:formatNumber value="${loan.approvedAmount}" pattern="#,###.##"/>
                            </div>
                        </div>
                    </c:if>
                    <c:if test="${not empty loan.rejectionReason}">
                        <div class="field-row">
                            <div class="field-label">Rejection Reason</div>
                            <div class="field-value" style="color:#dc2626;">${loan.rejectionReason}</div>
                        </div>
                    </c:if>
                </div>

                <%-- Underwriting decisions table --%>
                <div class="card">
                    <div class="card-title">Underwriting History (DB2 — UNDERWRITING_DECISIONS)</div>
                    <c:choose>
                        <c:when test="${not empty decisions}">
                            <table>
                                <thead><tr><th>Decision</th><th>Underwriter</th><th>Risk</th><th>Approved Amt</th><th>Date</th></tr></thead>
                                <tbody>
                                <c:forEach var="d" items="${decisions}">
                                    <tr>
                                        <td>
                                            <c:choose>
                                                <c:when test="${d.decision == 'APPROVED'}"><span class="badge badge-green">${d.decision}</span></c:when>
                                                <c:when test="${d.decision == 'REJECTED'}"><span class="badge badge-red">${d.decision}</span></c:when>
                                                <c:otherwise><span class="badge badge-yellow">${d.decision}</span></c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>${d.underwriterName}</td>
                                        <td><span class="badge badge-gray">${d.riskCategory}</span></td>
                                        <td>
                                            <c:if test="${not empty d.approvedAmount}">
                                                $<fmt:formatNumber value="${d.approvedAmount}" pattern="#,###.##"/>
                                            </c:if>
                                        </td>
                                        <td><fmt:formatDate value="${d.decisionDate}" pattern="yyyy-MM-dd"/></td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </c:when>
                        <c:otherwise>
                            <p style="color:#9ca3af;text-align:center;padding:24px;font-size:13px;">
                                No underwriting decisions yet.
                            </p>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>

            <%-- Right column: actions --%>
            <div>
                <div class="action-panel">
                    <h3>⚡ Actions</h3>

                    <%-- Submit to underwriting --%>
                    <c:if test="${loan.loanStatus == 'SUBMITTED'}">
                        <a href="${pageContext.request.contextPath}/loan/submitUnderwriting.action?loanId=${loan.loanId}"
                           class="btn btn-primary" id="btn-submit-uw"
                           onclick="return confirm('Submit this loan to underwriting?')">
                            → Submit to Underwriting
                        </a>
                    </c:if>

                    <%-- Go to UW review --%>
                    <c:if test="${loan.loanStatus == 'UNDER_REVIEW'}">
                        <a href="${pageContext.request.contextPath}/underwriting/review.action?loanId=${loan.loanId}"
                           class="btn btn-warning" id="btn-uw-review">
                            📋 Underwriting Review
                        </a>
                    </c:if>

                    <%-- Approve form --%>
                    <c:if test="${loan.loanStatus == 'UNDER_REVIEW' || loan.loanStatus == 'SUBMITTED'}">
                        <div class="inline-form">
                            <form method="post" action="${pageContext.request.contextPath}/loan/approve.action">
                                <input type="hidden" name="loanId" value="${loan.loanId}"/>
                                <label for="approvedAmount">Approved Amount ($)</label>
                                <input type="number" id="approvedAmount" name="approvedAmount"
                                       step="0.01" min="1" placeholder="e.g. 450000.00"/>
                                <button type="submit" class="btn btn-success" id="btn-approve"
                                        onclick="return confirm('Approve this loan?')">✅ Approve Loan</button>
                            </form>
                        </div>

                        <div class="inline-form">
                            <form method="post" action="${pageContext.request.contextPath}/loan/reject.action">
                                <input type="hidden" name="loanId" value="${loan.loanId}"/>
                                <label for="rejectionReason">Rejection Reason</label>
                                <textarea id="rejectionReason" name="rejectionReason" rows="3"
                                          placeholder="State the reason for rejection..."></textarea>
                                <button type="submit" class="btn btn-danger" id="btn-reject"
                                        onclick="return confirm('Reject this loan?')">❌ Reject Loan</button>
                            </form>
                        </div>
                    </c:if>

                    <c:if test="${loan.loanStatus == 'APPROVED' || loan.loanStatus == 'REJECTED' || loan.loanStatus == 'DISBURSED' || loan.loanStatus == 'CLOSED'}">
                        <div class="btn btn-disabled">No actions available</div>
                    </c:if>
                </div>
            </div>
        </div>
    </c:if>

    <c:if test="${empty loan}">
        <div style="text-align:center;padding:60px;color:#9ca3af;">
            <div style="font-size:40px;">🔍</div>
            <p style="margin-top:12px;">Loan not found.</p>
            <a href="${pageContext.request.contextPath}/loan/list.action"
               style="color:#0d6efd;text-decoration:none;margin-top:12px;display:inline-block;">
                Back to list
            </a>
        </div>
    </c:if>
</div>
</body>
</html>
