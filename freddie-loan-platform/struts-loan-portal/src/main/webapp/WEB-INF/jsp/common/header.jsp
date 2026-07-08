<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c"   uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"  %>
<%--  Reusable navigation header — include in every page  --%>
<nav style="background:#0a2342;padding:0 24px;display:flex;align-items:center;justify-content:space-between;height:56px;position:sticky;top:0;z-index:100;box-shadow:0 2px 8px rgba(0,0,0,.4)">
    <a href="${pageContext.request.contextPath}/dashboard.action"
       style="color:#fff;font-size:18px;font-weight:700;text-decoration:none;">
        🏠 Freddie Mac Portal
    </a>
    <div style="display:flex;gap:4px;align-items:center;">
        <a href="${pageContext.request.contextPath}/dashboard.action"      class="nav-link">Dashboard</a>
        <a href="${pageContext.request.contextPath}/loan/list.action"       class="nav-link">Loans</a>
        <a href="${pageContext.request.contextPath}/customer/list.action"   class="nav-link">Customers</a>
        <a href="${pageContext.request.contextPath}/underwriting/list.action" class="nav-link">Underwriting</a>
        <a href="${pageContext.request.contextPath}/report/loanSummary.action" class="nav-link">Reports</a>
        <a href="${pageContext.request.contextPath}/report/auditLog.action" class="nav-link">Audit Log</a>
        <span style="color:#94a3b8;margin:0 12px;font-size:13px;">
            👤 <c:out value="${sessionScope.loggedInUser}" default="Guest"/>
        </span>
        <a href="${pageContext.request.contextPath}/logout.action"
           style="background:#ef4444;color:#fff;padding:6px 14px;border-radius:6px;font-size:13px;text-decoration:none;">
            Logout
        </a>
    </div>
</nav>
<style>
    .nav-link {
        color: #cbd5e1; text-decoration: none; padding: 8px 14px; border-radius: 6px;
        font-size: 14px; font-weight: 500; transition: background .15s, color .15s;
    }
    .nav-link:hover { background: rgba(255,255,255,.1); color: #fff; }
</style>
