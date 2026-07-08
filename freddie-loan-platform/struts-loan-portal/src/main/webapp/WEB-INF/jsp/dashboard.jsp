<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c"   uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"  %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title>Dashboard — Freddie Mac Loan Portal</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet"/>
    <style>
        *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: 'Inter', sans-serif; background: #f1f5f9; color: #1e293b; }
        .page-body { padding: 28px 32px; }
        .page-title { font-size: 24px; font-weight: 700; color: #0a2342; margin-bottom: 24px; }

        /* KPI Cards */
        .kpi-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 18px; margin-bottom: 32px; }
        .kpi-card {
            background: #fff; border-radius: 12px; padding: 22px 24px;
            box-shadow: 0 1px 4px rgba(0,0,0,.08);
            border-left: 4px solid var(--accent);
            transition: transform .15s, box-shadow .15s;
        }
        .kpi-card:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(0,0,0,.1); }
        .kpi-label { font-size: 12px; font-weight: 600; color: #6b7280; text-transform: uppercase; letter-spacing: .06em; }
        .kpi-value { font-size: 32px; font-weight: 700; color: #0a2342; margin-top: 6px; }
        .kpi-sub   { font-size: 12px; color: #9ca3af; margin-top: 4px; }

        /* Two-column layout */
        .grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 24px; }
        .card { background: #fff; border-radius: 12px; padding: 24px; box-shadow: 0 1px 4px rgba(0,0,0,.08); }
        .card-title { font-size: 15px; font-weight: 700; color: #0a2342; margin-bottom: 16px;
                      padding-bottom: 10px; border-bottom: 1px solid #e5e7eb; }

        /* Table */
        table { width: 100%; border-collapse: collapse; font-size: 13px; }
        th { text-align: left; padding: 8px 10px; font-size: 11px; font-weight: 600;
             color: #6b7280; text-transform: uppercase; border-bottom: 1px solid #e5e7eb; }
        td { padding: 10px 10px; border-bottom: 1px solid #f3f4f6; color: #374151; }
        tr:last-child td { border-bottom: none; }
        tr:hover td { background: #f8fafc; }

        /* Badge */
        .badge {
            display: inline-block; padding: 2px 8px; border-radius: 12px;
            font-size: 11px; font-weight: 600; text-transform: uppercase;
        }
        .badge-green   { background: #dcfce7; color: #166534; }
        .badge-yellow  { background: #fef9c3; color: #854d0e; }
        .badge-blue    { background: #dbeafe; color: #1e40af; }
        .badge-red     { background: #fee2e2; color: #991b1b; }
        .badge-gray    { background: #f3f4f6; color: #374151; }
    </style>
</head>
<body>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<div class="page-body">
    <h1 class="page-title">📊 Operations Dashboard</h1>

    <%-- KPI Row --%>
    <div class="kpi-grid">
        <div class="kpi-card" style="--accent:#0d6efd;">
            <div class="kpi-label">Total Loans</div>
            <div class="kpi-value"><fmt:formatNumber value="${totalLoans}" pattern="#,###"/></div>
            <div class="kpi-sub">All time</div>
        </div>
        <div class="kpi-card" style="--accent:#10b981;">
            <div class="kpi-label">Total Customers</div>
            <div class="kpi-value"><fmt:formatNumber value="${totalCustomers}" pattern="#,###"/></div>
            <div class="kpi-sub">Active + Inactive</div>
        </div>
        <div class="kpi-card" style="--accent:#f59e0b;">
            <div class="kpi-label">Pending Underwriting</div>
            <div class="kpi-value"><fmt:formatNumber value="${pendingUnderwritingCount}" pattern="#,###"/></div>
            <div class="kpi-sub">Under review</div>
        </div>
        <div class="kpi-card" style="--accent:#8b5cf6;">
            <div class="kpi-label">Total Disbursed</div>
            <div class="kpi-value">$<fmt:formatNumber value="${totalDisbursedAmount}" pattern="#,###"/></div>
            <div class="kpi-sub">Approved & funded</div>
        </div>
    </div>

    <div class="grid-2">
        <%-- Loans by Type --%>
        <div class="card">
            <div class="card-title">Loans by Type (DB2 GROUP BY)</div>
            <table>
                <thead><tr><th>Loan Type</th><th>Count</th></tr></thead>
                <tbody>
                <c:forEach var="row" items="${loanCountByType}">
                    <tr>
                        <td><strong>${row.LOAN_TYPE}</strong></td>
                        <td><fmt:formatNumber value="${row.LOAN_COUNT}" pattern="#,###"/></td>
                    </tr>
                </c:forEach>
                <c:if test="${empty loanCountByType}">
                    <tr><td colspan="2" style="color:#9ca3af;text-align:center;padding:24px;">No data available</td></tr>
                </c:if>
                </tbody>
            </table>
        </div>

        <%-- Loans by Status --%>
        <div class="card">
            <div class="card-title">Loan Portfolio by Status (DB2 SUM)</div>
            <table>
                <thead><tr><th>Status</th><th>Count</th><th>Total Amount</th></tr></thead>
                <tbody>
                <c:forEach var="row" items="${loanAmountByStatus}">
                    <tr>
                        <td>
                            <c:choose>
                                <c:when test="${row.LOAN_STATUS == 'APPROVED'}"><span class="badge badge-green">${row.LOAN_STATUS}</span></c:when>
                                <c:when test="${row.LOAN_STATUS == 'REJECTED'}"><span class="badge badge-red">${row.LOAN_STATUS}</span></c:when>
                                <c:when test="${row.LOAN_STATUS == 'UNDER_REVIEW'}"><span class="badge badge-yellow">${row.LOAN_STATUS}</span></c:when>
                                <c:when test="${row.LOAN_STATUS == 'DISBURSED'}"><span class="badge badge-blue">${row.LOAN_STATUS}</span></c:when>
                                <c:otherwise><span class="badge badge-gray">${row.LOAN_STATUS}</span></c:otherwise>
                            </c:choose>
                        </td>
                        <td><fmt:formatNumber value="${row.LOAN_COUNT}" pattern="#,###"/></td>
                        <td>$<fmt:formatNumber value="${row.TOTAL_AMOUNT}" pattern="#,###.##"/></td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </div>

    <%-- Recent Loans --%>
    <div class="card">
        <div class="card-title">Recent Loan Applications (Last 10 — DB2)</div>
        <table>
            <thead>
                <tr>
                    <th>Loan ID</th>
                    <th>Customer</th>
                    <th>Type</th>
                    <th>Amount</th>
                    <th>Status</th>
                    <th>Date</th>
                    <th>Action</th>
                </tr>
            </thead>
            <tbody>
            <c:forEach var="loan" items="${recentLoans}">
                <tr>
                    <td style="font-family:monospace;font-size:11px;">${loan.loanId}</td>
                    <td>${loan.customerName}</td>
                    <td>${loan.loanType}</td>
                    <td>$<fmt:formatNumber value="${loan.loanAmount}" pattern="#,###.##"/></td>
                    <td>
                        <c:choose>
                            <c:when test="${loan.loanStatus == 'APPROVED'}"><span class="badge badge-green">${loan.loanStatus}</span></c:when>
                            <c:when test="${loan.loanStatus == 'REJECTED'}"><span class="badge badge-red">${loan.loanStatus}</span></c:when>
                            <c:when test="${loan.loanStatus == 'UNDER_REVIEW'}"><span class="badge badge-yellow">${loan.loanStatus}</span></c:when>
                            <c:when test="${loan.loanStatus == 'DISBURSED'}"><span class="badge badge-blue">${loan.loanStatus}</span></c:when>
                            <c:otherwise><span class="badge badge-gray">${loan.loanStatus}</span></c:otherwise>
                        </c:choose>
                    </td>
                    <td><fmt:formatDate value="${loan.applicationDate}" pattern="yyyy-MM-dd"/></td>
                    <td>
                        <a href="${pageContext.request.contextPath}/loan/detail.action?loanId=${loan.loanId}"
                           style="color:#0d6efd;text-decoration:none;font-size:12px;font-weight:600;">View →</a>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty recentLoans}">
                <tr><td colspan="7" style="color:#9ca3af;text-align:center;padding:32px;">No loans found in database</td></tr>
            </c:if>
            </tbody>
        </table>
    </div>
</div>
</body>
</html>
