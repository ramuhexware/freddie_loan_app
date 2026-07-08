<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c"   uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"  %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <title>Loan Applications — Freddie Mac Portal</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet"/>
    <style>
        *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: 'Inter', sans-serif; background: #f1f5f9; color: #1e293b; }
        .page-body { padding: 28px 32px; }
        .page-header { display:flex; justify-content:space-between; align-items:center; margin-bottom:24px; }
        .page-title  { font-size: 22px; font-weight: 700; color: #0a2342; }
        .btn         { padding: 9px 18px; border-radius: 7px; border: none; font-size: 13px;
                       font-weight: 600; cursor: pointer; text-decoration: none; display: inline-block; }
        .btn-primary { background: #0d6efd; color: #fff; }
        .btn-primary:hover { background: #0a58ca; }

        /* Filter bar */
        .filter-bar { background: #fff; padding: 16px 20px; border-radius: 10px;
                      box-shadow: 0 1px 3px rgba(0,0,0,.07); margin-bottom: 20px;
                      display: flex; gap: 12px; align-items: flex-end; flex-wrap: wrap; }
        .filter-group label { display: block; font-size: 11px; font-weight: 600; color: #6b7280;
                              text-transform: uppercase; margin-bottom: 4px; }
        .filter-group select, .filter-group input {
            padding: 8px 10px; border: 1.5px solid #d1d5db; border-radius: 7px;
            font-size: 13px; font-family: inherit;
        }
        .btn-filter { background: #e5e7eb; color: #374151; }
        .btn-filter:hover { background: #d1d5db; }

        /* Table */
        .card { background: #fff; border-radius: 12px; padding: 0; box-shadow: 0 1px 4px rgba(0,0,0,.08); overflow: hidden; }
        table { width: 100%; border-collapse: collapse; font-size: 13px; }
        thead { background: #f8fafc; }
        th { text-align: left; padding: 12px 14px; font-size: 11px; font-weight: 700;
             color: #6b7280; text-transform: uppercase; letter-spacing: .05em;
             border-bottom: 1px solid #e5e7eb; }
        td { padding: 12px 14px; border-bottom: 1px solid #f3f4f6; color: #374151; vertical-align: middle; }
        tr:last-child td { border-bottom: none; }
        tr:hover td { background: #f8fafc; }

        .badge { display:inline-block; padding:2px 9px; border-radius:12px; font-size:11px; font-weight:700; text-transform:uppercase; }
        .badge-green  { background:#dcfce7; color:#166534; }
        .badge-yellow { background:#fef9c3; color:#854d0e; }
        .badge-blue   { background:#dbeafe; color:#1e40af; }
        .badge-red    { background:#fee2e2; color:#991b1b; }
        .badge-gray   { background:#f3f4f6; color:#374151; }

        .action-link { color:#0d6efd; text-decoration:none; font-weight:600; font-size:12px; }
        .action-link:hover { text-decoration:underline; }
        .count-badge { background:#e5e7eb; padding:2px 8px; border-radius:10px;
                       font-size:12px; font-weight:600; color:#374151; }
    </style>
</head>
<body>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<div class="page-body">
    <div class="page-header">
        <h1 class="page-title">📋 Loan Applications
            <span class="count-badge">${loans.size()} results</span>
        </h1>
        <a href="${pageContext.request.contextPath}/loan/applyForm.action" class="btn btn-primary" id="btn-apply-loan">
            + New Application
        </a>
    </div>

    <%-- Filter bar --%>
    <form method="get" action="${pageContext.request.contextPath}/loan/search.action">
        <div class="filter-bar">
            <div class="filter-group">
                <label>Status</label>
                <select name="filterStatus" id="filterStatus">
                    <option value="">All Statuses</option>
                    <c:forEach var="s" items="${['PENDING','SUBMITTED','UNDER_REVIEW','APPROVED','REJECTED','DISBURSED','CLOSED']}">
                        <option value="${s}" ${filterStatus == s ? 'selected' : ''}>${s}</option>
                    </c:forEach>
                </select>
            </div>
            <div class="filter-group">
                <label>Loan Type</label>
                <select name="filterLoanType" id="filterLoanType">
                    <option value="">All Types</option>
                    <c:forEach var="t" items="${['PURCHASE','REFINANCE','HELOC','HOME_EQUITY']}">
                        <option value="${t}" ${filterLoanType == t ? 'selected' : ''}>${t}</option>
                    </c:forEach>
                </select>
            </div>
            <div class="filter-group">
                <label>From Date</label>
                <input type="date" name="filterFromDate" id="filterFromDate" value="${filterFromDate}"/>
            </div>
            <div class="filter-group">
                <label>To Date</label>
                <input type="date" name="filterToDate" id="filterToDate" value="${filterToDate}"/>
            </div>
            <div class="filter-group">
                <button type="submit" class="btn btn-filter" id="btn-filter">🔍 Filter</button>
            </div>
            <div class="filter-group">
                <a href="${pageContext.request.contextPath}/loan/list.action" class="btn btn-filter">Clear</a>
            </div>
        </div>
    </form>

    <%-- Error / message display --%>
    <c:if test="${not empty actionErrors}">
        <div style="background:#fee2e2;border:1px solid #fca5a5;border-radius:8px;padding:12px 16px;margin-bottom:16px;color:#991b1b;font-size:13px;">
            <c:forEach var="e" items="${actionErrors}"><div>${e}</div></c:forEach>
        </div>
    </c:if>

    <%-- Loans table --%>
    <div class="card">
        <table>
            <thead>
                <tr>
                    <th>Loan ID</th>
                    <th>Customer</th>
                    <th>Type</th>
                    <th>Amount</th>
                    <th>Property Value</th>
                    <th>Term</th>
                    <th>Status</th>
                    <th>Applied</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
            <c:forEach var="loan" items="${loans}">
                <tr>
                    <td style="font-family:monospace;font-size:11px;color:#6b7280;">
                        ${fn:substring(loan.loanId,0,8)}...
                    </td>
                    <td><strong>${loan.customerName}</strong><br/>
                        <span style="font-size:11px;color:#9ca3af;">${loan.customerId}</span></td>
                    <td>${loan.loanType}</td>
                    <td><strong>$<fmt:formatNumber value="${loan.loanAmount}" pattern="#,###.##"/></strong></td>
                    <td>$<fmt:formatNumber value="${loan.propertyValue}" pattern="#,###.##"/></td>
                    <td>${loan.loanTermMonths} mo</td>
                    <td>
                        <c:choose>
                            <c:when test="${loan.loanStatus == 'APPROVED'}"><span class="badge badge-green">${loan.loanStatus}</span></c:when>
                            <c:when test="${loan.loanStatus == 'REJECTED'}"><span class="badge badge-red">${loan.loanStatus}</span></c:when>
                            <c:when test="${loan.loanStatus == 'UNDER_REVIEW'}"><span class="badge badge-yellow">${loan.loanStatus}</span></c:when>
                            <c:when test="${loan.loanStatus == 'DISBURSED'}"><span class="badge badge-blue">${loan.loanStatus}</span></c:when>
                            <c:otherwise><span class="badge badge-gray">${loan.loanStatus}</span></c:otherwise>
                        </c:choose>
                    </td>
                    <td>
                        <fmt:formatDate value="${loan.applicationDate}" pattern="yyyy-MM-dd"/>
                    </td>
                    <td>
                        <a href="${pageContext.request.contextPath}/loan/detail.action?loanId=${loan.loanId}"
                           class="action-link" id="link-detail-${loan.loanId}">View</a>
                        <c:if test="${loan.loanStatus == 'SUBMITTED'}">
                            &nbsp;|&nbsp;
                            <a href="${pageContext.request.contextPath}/loan/submitUnderwriting.action?loanId=${loan.loanId}"
                               class="action-link" id="link-uw-${loan.loanId}"
                               onclick="return confirm('Submit to underwriting?')">→ UW</a>
                        </c:if>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty loans}">
                <tr>
                    <td colspan="9" style="text-align:center;padding:48px;color:#9ca3af;">
                        No loan applications found matching your criteria.
                    </td>
                </tr>
            </c:if>
            </tbody>
        </table>
    </div>
</div>
</body>
</html>
