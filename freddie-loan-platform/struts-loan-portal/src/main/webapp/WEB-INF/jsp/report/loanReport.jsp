<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c"   uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"  %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <title>Loan Reports — Freddie Mac Portal</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet"/>
    <style>
        *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: 'Inter', sans-serif; background: #f1f5f9; color: #1e293b; }
        .page-body { padding: 28px 32px; }
        .page-title { font-size: 22px; font-weight: 700; color: #0a2342; margin-bottom: 24px; }
        .report-nav { display: flex; gap: 10px; margin-bottom: 24px; }
        .report-nav a { padding: 8px 18px; border-radius: 7px; font-size: 13px; font-weight: 600;
                        text-decoration: none; background: #fff; color: #374151; border: 1px solid #d1d5db;
                        transition: background .15s; }
        .report-nav a:hover { background: #0d6efd; color: #fff; border-color: #0d6efd; }

        .grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 24px; }
        .card { background: #fff; border-radius: 12px; padding: 24px; box-shadow: 0 1px 4px rgba(0,0,0,.08); }
        .card-title { font-size: 14px; font-weight: 700; color: #0a2342; margin-bottom: 16px;
                      padding-bottom: 10px; border-bottom: 1px solid #e5e7eb; }
        table { width: 100%; border-collapse: collapse; font-size: 13px; }
        th { text-align: left; padding: 8px 10px; font-size: 11px; font-weight: 600; color: #6b7280;
             text-transform: uppercase; border-bottom: 1px solid #e5e7eb; }
        td { padding: 10px 10px; border-bottom: 1px solid #f3f4f6; }
        tr:last-child td { border-bottom: none; }
        tr:hover td { background: #f8fafc; }

        /* Progress bar */
        .progress-bar { height: 8px; background: #e5e7eb; border-radius: 4px; overflow: hidden; margin-top: 4px; }
        .progress-fill { height: 100%; border-radius: 4px; transition: width .4s ease; }

        /* KPI row */
        .kpi-mini { display: flex; gap: 12px; margin-bottom: 20px; }
        .kpi-mini-card { flex: 1; background: linear-gradient(135deg, #0a2342, #1a4a7a);
                         color: #fff; border-radius: 10px; padding: 14px 16px; }
        .kpi-mini-label { font-size: 11px; font-weight: 600; opacity: .7; text-transform: uppercase; }
        .kpi-mini-value { font-size: 22px; font-weight: 700; margin-top: 4px; }
    </style>
</head>
<body>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<div class="page-body">
    <h1 class="page-title">📈 Loan Summary Report (DB2 Aggregations)</h1>

    <div class="report-nav">
        <a href="${pageContext.request.contextPath}/report/loanSummary.action"    id="nav-loan-summary">Loan Summary</a>
        <a href="${pageContext.request.contextPath}/report/monthlyStats.action"   id="nav-monthly-stats">Monthly Stats</a>
        <a href="${pageContext.request.contextPath}/report/auditLog.action"       id="nav-audit-log">Audit Log</a>
        <a href="${pageContext.request.contextPath}/customer/statsSummary.action" id="nav-customer-stats">Customer Stats</a>
    </div>

    <%-- KPI mini cards --%>
    <div class="kpi-mini">
        <div class="kpi-mini-card">
            <div class="kpi-mini-label">Total Loan Types</div>
            <div class="kpi-mini-value">${loansByType.size()}</div>
        </div>
        <div class="kpi-mini-card">
            <div class="kpi-mini-label">Loan Statuses Tracked</div>
            <div class="kpi-mini-value">${loansByStatus.size()}</div>
        </div>
        <div class="kpi-mini-card">
            <div class="kpi-mini-label">Top 10 Large Loans</div>
            <div class="kpi-mini-value">${topLoans.size()}</div>
        </div>
    </div>

    <div class="grid-2">
        <%-- Loans by type --%>
        <div class="card">
            <div class="card-title">Loans by Type — SUM/AVG/COUNT (DB2 GROUP BY LOAN_TYPE)</div>
            <table>
                <thead><tr><th>Type</th><th>Count</th><th>Total $</th><th>Avg $</th></tr></thead>
                <tbody>
                <c:forEach var="r" items="${loansByType}">
                    <tr>
                        <td><strong>${r.category}</strong></td>
                        <td><fmt:formatNumber value="${r.count}" pattern="#,###"/></td>
                        <td>$<fmt:formatNumber value="${r.totalAmount}" pattern="#,###"/></td>
                        <td>$<fmt:formatNumber value="${r.avgAmount}" pattern="#,###"/></td>
                    </tr>
                </c:forEach>
                <c:if test="${empty loansByType}">
                    <tr><td colspan="4" style="text-align:center;color:#9ca3af;padding:24px;">No data</td></tr>
                </c:if>
                </tbody>
            </table>
        </div>

        <%-- Loans by status --%>
        <div class="card">
            <div class="card-title">Portfolio by Status (DB2 GROUP BY LOAN_STATUS)</div>
            <table>
                <thead><tr><th>Status</th><th>Count</th><th>Min $</th><th>Max $</th></tr></thead>
                <tbody>
                <c:forEach var="r" items="${loansByStatus}">
                    <tr>
                        <td><strong>${r.category}</strong></td>
                        <td><fmt:formatNumber value="${r.count}" pattern="#,###"/></td>
                        <td>$<fmt:formatNumber value="${r.minAmount}" pattern="#,###"/></td>
                        <td>$<fmt:formatNumber value="${r.maxAmount}" pattern="#,###"/></td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </div>

    <%-- Approval rates --%>
    <div class="card" style="margin-bottom:20px;">
        <div class="card-title">Approval Rate by Loan Type (DB2 Subquery)</div>
        <table>
            <thead><tr><th>Loan Type</th><th>Total</th><th>Approved</th><th>Rate (%)</th><th>Visual</th></tr></thead>
            <tbody>
            <c:forEach var="r" items="${approvalRates}">
                <tr>
                    <td><strong>${r.LOAN_TYPE}</strong></td>
                    <td><fmt:formatNumber value="${r.TOTAL_COUNT}" pattern="#,###"/></td>
                    <td><fmt:formatNumber value="${r.APPROVED_COUNT}" pattern="#,###"/></td>
                    <td><fmt:formatNumber value="${r.APPROVAL_RATE_PCT}" pattern="0.0"/>%</td>
                    <td style="width:120px;">
                        <div class="progress-bar">
                            <div class="progress-fill" style="width:${r.APPROVAL_RATE_PCT}%;background:#0d6efd;"></div>
                        </div>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty approvalRates}">
                <tr><td colspan="5" style="text-align:center;color:#9ca3af;padding:24px;">No data</td></tr>
            </c:if>
            </tbody>
        </table>
    </div>

    <%-- Top loans --%>
    <div class="card">
        <div class="card-title">Top 10 Loans by Amount (DB2 ORDER BY LOAN_AMOUNT DESC)</div>
        <table>
            <thead><tr><th>Loan ID</th><th>Customer</th><th>Type</th><th>Amount</th><th>Status</th></tr></thead>
            <tbody>
            <c:forEach var="r" items="${topLoans}">
                <tr>
                    <td style="font-family:monospace;font-size:11px;">${r.LOAN_ID}</td>
                    <td>${r.CUSTOMER_NAME}</td>
                    <td>${r.LOAN_TYPE}</td>
                    <td><strong>$<fmt:formatNumber value="${r.LOAN_AMOUNT}" pattern="#,###.##"/></strong></td>
                    <td>${r.LOAN_STATUS}</td>
                </tr>
            </c:forEach>
            <c:if test="${empty topLoans}">
                <tr><td colspan="5" style="text-align:center;color:#9ca3af;padding:24px;">No data</td></tr>
            </c:if>
            </tbody>
        </table>
    </div>
</div>
</body>
</html>
