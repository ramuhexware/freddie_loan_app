<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c"   uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"  %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <title>Import Results — Freddie Mac Portal</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet"/>
    <style>
        *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: 'Inter', sans-serif; background: #f1f5f9; color: #1e293b; }
        .page-body { padding: 28px 32px; }
        .page-title { font-size: 22px; font-weight: 700; color: #0a2342; margin-bottom: 24px; }
        .stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 24px; }
        .stat-card { background: #fff; border-radius: 10px; padding: 18px 20px;
                     box-shadow: 0 1px 4px rgba(0,0,0,.08); border-top: 4px solid var(--c); }
        .stat-label { font-size: 11px; font-weight: 700; color: #6b7280; text-transform: uppercase; }
        .stat-value { font-size: 28px; font-weight: 700; color: #0a2342; margin-top: 6px; }
        .card { background: #fff; border-radius: 12px; padding: 0; box-shadow: 0 1px 4px rgba(0,0,0,.08);
                overflow: hidden; margin-bottom: 20px; }
        .card-header { padding: 14px 20px; font-size: 14px; font-weight: 700; }
        .success-header { background: #16a34a; color: #fff; }
        .error-header   { background: #dc2626; color: #fff; }
        table { width: 100%; border-collapse: collapse; font-size: 13px; }
        th { text-align: left; padding: 10px 14px; font-size: 11px; font-weight: 700; color: #6b7280;
             text-transform: uppercase; border-bottom: 1px solid #e5e7eb; background: #f8fafc; }
        td { padding: 10px 14px; border-bottom: 1px solid #f3f4f6; }
        .err-row { font-size: 12px; color: #dc2626; }
        .btn { padding: 10px 22px; border-radius: 7px; border: none; font-size: 13px; font-weight: 600;
               cursor: pointer; text-decoration: none; display: inline-block; margin-right: 10px; }
        .btn-primary   { background: #0d6efd; color: #fff; }
        .btn-secondary { background: #e5e7eb; color: #374151; }
    </style>
</head>
<body>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<div class="page-body">
    <h1 class="page-title">📊 Import Results</h1>

    <div class="stats-row">
        <div class="stat-card" style="--c:#0d6efd;">
            <div class="stat-label">Total Rows Read</div>
            <div class="stat-value">${totalRowsRead}</div>
        </div>
        <div class="stat-card" style="--c:#22c55e;">
            <div class="stat-label">Successfully Inserted</div>
            <div class="stat-value">${insertedCount}</div>
        </div>
        <div class="stat-card" style="--c:#ef4444;">
            <div class="stat-label">Errors / Skipped</div>
            <div class="stat-value">${importErrors.size()}</div>
        </div>
        <div class="stat-card" style="--c:#f59e0b;">
            <div class="stat-label">Parsed (Valid)</div>
            <div class="stat-value">${importedLoans.size()}</div>
        </div>
    </div>

    <%-- Action messages --%>
    <c:if test="${not empty actionMessages}">
        <div style="background:#f0fdf4;border:1px solid #86efac;border-radius:8px;
                    padding:12px 16px;margin-bottom:16px;font-size:13px;color:#166534;">
            <c:forEach var="m" items="${actionMessages}"><div>✅ ${m}</div></c:forEach>
        </div>
    </c:if>

    <%-- Successfully imported loans table --%>
    <c:if test="${not empty importedLoans}">
        <div class="card">
            <div class="card-header success-header">
                ✅ Loans Imported into DB2 (${importedLoans.size()} rows)
            </div>
            <table>
                <thead><tr>
                    <th>Loan ID (Generated)</th>
                    <th>Customer ID</th>
                    <th>Type</th>
                    <th>Amount</th>
                    <th>Property Value</th>
                    <th>Term (mo)</th>
                    <th>Status</th>
                </tr></thead>
                <tbody>
                <c:forEach var="loan" items="${importedLoans}">
                    <tr>
                        <td style="font-family:monospace;font-size:10px;color:#6b7280;">${loan.loanId}</td>
                        <td>${loan.customerId}</td>
                        <td><strong>${loan.loanType}</strong></td>
                        <td>$<fmt:formatNumber value="${loan.loanAmount}" pattern="#,###.##"/></td>
                        <td>
                            <c:if test="${not empty loan.propertyValue}">
                                $<fmt:formatNumber value="${loan.propertyValue}" pattern="#,###.##"/>
                            </c:if>
                        </td>
                        <td>${loan.loanTermMonths}</td>
                        <td><span style="background:#fef9c3;color:#854d0e;padding:2px 8px;
                                         border-radius:10px;font-size:11px;font-weight:700;">
                            ${loan.loanStatus}</span>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </c:if>

    <%-- Errors table --%>
    <c:if test="${not empty importErrors}">
        <div class="card">
            <div class="card-header error-header">
                ❌ Parse / Insert Errors (${importErrors.size()} rows skipped)
            </div>
            <table>
                <thead><tr><th>#</th><th>Error Message</th></tr></thead>
                <tbody>
                <c:forEach var="err" items="${importErrors}" varStatus="loop">
                    <tr class="err-row">
                        <td style="width:40px;">${loop.index + 1}</td>
                        <td>${err}</td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </c:if>

    <div style="margin-top:8px;">
        <a href="${pageContext.request.contextPath}/import/importForm.action"
           class="btn btn-primary">⬆ Import Another File</a>
        <a href="${pageContext.request.contextPath}/loan/list.action"
           class="btn btn-secondary">View All Loans</a>
    </div>
</div>
</body>
</html>
