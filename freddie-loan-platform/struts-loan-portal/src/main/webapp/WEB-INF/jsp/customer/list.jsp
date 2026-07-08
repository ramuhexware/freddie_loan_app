<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c"   uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"  %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <title>Customer List — Freddie Mac Portal</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet"/>
    <style>
        *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: 'Inter', sans-serif; background: #f1f5f9; color: #1e293b; }
        .page-body { padding: 28px 32px; }
        .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
        .page-title  { font-size: 22px; font-weight: 700; color: #0a2342; }
        .search-bar  { background: #fff; padding: 14px 18px; border-radius: 10px;
                       box-shadow: 0 1px 3px rgba(0,0,0,.07); margin-bottom: 20px;
                       display: flex; gap: 10px; align-items: flex-end; }
        .search-bar input { flex: 1; padding: 9px 12px; border: 1.5px solid #d1d5db;
                            border-radius: 7px; font-size: 14px; font-family: inherit; }
        .btn { padding: 9px 18px; border-radius: 7px; border: none; font-size: 13px;
               font-weight: 600; cursor: pointer; text-decoration: none; display: inline-block; }
        .btn-primary { background: #0d6efd; color: #fff; }
        .btn-secondary { background: #e5e7eb; color: #374151; }
        .card { background: #fff; border-radius: 12px; padding: 0; box-shadow: 0 1px 4px rgba(0,0,0,.08); overflow: hidden; }
        table { width: 100%; border-collapse: collapse; font-size: 13px; }
        thead { background: #f8fafc; }
        th { text-align: left; padding: 12px 14px; font-size: 11px; font-weight: 700;
             color: #6b7280; text-transform: uppercase; letter-spacing: .05em;
             border-bottom: 1px solid #e5e7eb; }
        td { padding: 12px 14px; border-bottom: 1px solid #f3f4f6; vertical-align: middle; }
        tr:last-child td { border-bottom: none; }
        tr:hover td { background: #f8fafc; }
        .badge { display:inline-block; padding:2px 9px; border-radius:12px; font-size:11px; font-weight:700; text-transform:uppercase; }
        .badge-green  { background:#dcfce7; color:#166534; }
        .badge-red    { background:#fee2e2; color:#991b1b; }
        .badge-yellow { background:#fef9c3; color:#854d0e; }
        .avatar { width: 32px; height: 32px; border-radius: 50%; background: #0d6efd;
                  color: #fff; font-size: 13px; font-weight: 700; display: inline-flex;
                  align-items: center; justify-content: center; margin-right: 8px; }
    </style>
</head>
<body>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<div class="page-body">
    <div class="page-header">
        <h1 class="page-title">👥 Customers
            <span style="font-size:14px;font-weight:400;color:#9ca3af;margin-left:8px;">
                ${customers.size()} records (from DB2)
            </span>
        </h1>
        <a href="${pageContext.request.contextPath}/customer/statsSummary.action"
           class="btn btn-secondary" id="btn-customer-stats">📊 Stats</a>
    </div>

    <%-- Search bar --%>
    <form method="get" action="${pageContext.request.contextPath}/customer/search.action">
        <div class="search-bar">
            <input type="text" name="searchTerm" id="searchTerm"
                   placeholder="🔍 Search by name, email, or city..."
                   value="${searchTerm}"/>
            <button type="submit" class="btn btn-primary" id="btn-search">Search</button>
            <a href="${pageContext.request.contextPath}/customer/list.action"
               class="btn btn-secondary">Clear</a>
        </div>
    </form>

    <%-- Customers table --%>
    <div class="card">
        <table>
            <thead>
                <tr>
                    <th>Customer</th>
                    <th>Email</th>
                    <th>Phone</th>
                    <th>Location</th>
                    <th>Credit Score</th>
                    <th>Status</th>
                    <th>Since</th>
                    <th></th>
                </tr>
            </thead>
            <tbody>
            <c:forEach var="c" items="${customers}">
                <tr>
                    <td>
                        <div style="display:flex;align-items:center;">
                            <div class="avatar">${fn:toUpperCase(fn:substring(c.firstName,0,1))}</div>
                            <div>
                                <div style="font-weight:600;">${c.fullName}</div>
                                <div style="font-size:11px;color:#9ca3af;font-family:monospace;">${c.customerId}</div>
                            </div>
                        </div>
                    </td>
                    <td>${c.email}</td>
                    <td>${c.phoneNumber}</td>
                    <td>${c.city}<c:if test="${not empty c.state}">, ${c.state}</c:if></td>
                    <td>
                        <span style="${c.creditScore < 620 ? 'color:#dc2626;font-weight:700;' : c.creditScore >= 740 ? 'color:#16a34a;font-weight:700;' : ''}">
                            ${c.creditScore}
                        </span>
                    </td>
                    <td>
                        <c:choose>
                            <c:when test="${c.customerStatus == 'ACTIVE'}"><span class="badge badge-green">${c.customerStatus}</span></c:when>
                            <c:when test="${c.customerStatus == 'INACTIVE'}"><span class="badge badge-yellow">${c.customerStatus}</span></c:when>
                            <c:otherwise><span class="badge badge-red">${c.customerStatus}</span></c:otherwise>
                        </c:choose>
                    </td>
                    <td><fmt:formatDate value="${c.createdAt}" pattern="yyyy-MM-dd"/></td>
                    <td>
                        <a href="${pageContext.request.contextPath}/customer/detail.action?customerId=${c.customerId}"
                           style="color:#0d6efd;text-decoration:none;font-size:12px;font-weight:600;"
                           id="link-customer-${c.customerId}">View →</a>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty customers}">
                <tr>
                    <td colspan="8" style="text-align:center;padding:48px;color:#9ca3af;">
                        No customers found.
                    </td>
                </tr>
            </c:if>
            </tbody>
        </table>
    </div>
</div>
</body>
</html>
