<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c"   uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"  %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <title>Audit Log — Freddie Mac Portal</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet"/>
    <style>
        *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: 'Inter', sans-serif; background: #f1f5f9; color: #1e293b; }
        .page-body { padding: 28px 32px; }
        .page-title { font-size: 22px; font-weight: 700; color: #0a2342; margin-bottom: 24px; }
        .card { background: #fff; border-radius: 12px; padding: 0; box-shadow: 0 1px 4px rgba(0,0,0,.08); overflow: hidden; }
        table { width: 100%; border-collapse: collapse; font-size: 12px; }
        thead { background: #1e293b; color: #e2e8f0; }
        th { text-align: left; padding: 12px 12px; font-size: 11px; font-weight: 600;
             text-transform: uppercase; letter-spacing: .05em; }
        td { padding: 10px 12px; border-bottom: 1px solid #f3f4f6; vertical-align: middle; }
        tr:last-child td { border-bottom: none; }
        tr:nth-child(even) td { background: #f8fafc; }
        tr:hover td { background: #eff6ff; }
        .badge { display:inline-block; padding:2px 7px; border-radius:10px; font-size:10px; font-weight:700; text-transform:uppercase; }
        .badge-green  { background:#dcfce7; color:#166534; }
        .badge-red    { background:#fee2e2; color:#991b1b; }
        .badge-gray   { background:#f3f4f6; color:#374151; }
        .mono { font-family: monospace; font-size: 11px; }
    </style>
</head>
<body>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<div class="page-body">
    <h1 class="page-title">🔍 Audit Log (Last 100 Entries — DB2 AUDIT_LOG)</h1>
    <p style="font-size:13px;color:#6b7280;margin-bottom:20px;">
        Every portal action is recorded to DB2 by AuditInterceptor.
        Entries are written in a separate transaction (PROPAGATION_REQUIRES_NEW).
    </p>

    <div class="card">
        <table>
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Timestamp</th>
                    <th>User</th>
                    <th>Namespace</th>
                    <th>Action</th>
                    <th>Method</th>
                    <th>Result</th>
                    <th>Time (ms)</th>
                    <th>IP</th>
                </tr>
            </thead>
            <tbody>
            <c:forEach var="log" items="${recentAuditLogs}">
                <tr>
                    <td class="mono">${log.auditId}</td>
                    <td class="mono">
                        <fmt:formatDate value="${log.timestamp}" pattern="yyyy-MM-dd HH:mm:ss"/>
                    </td>
                    <td><strong>${log.sessionUser}</strong></td>
                    <td class="mono">${log.actionNamespace}</td>
                    <td class="mono">${log.actionName}</td>
                    <td class="mono">${log.actionMethod}</td>
                    <td>
                        <c:choose>
                            <c:when test="${log.resultCode == 'success'}"><span class="badge badge-green">SUCCESS</span></c:when>
                            <c:when test="${log.resultCode == 'error' || log.resultCode == 'ERROR'}"><span class="badge badge-red">ERROR</span></c:when>
                            <c:otherwise><span class="badge badge-gray">${log.resultCode}</span></c:otherwise>
                        </c:choose>
                    </td>
                    <td>
                        <span style="${log.executionTimeMs > 1000 ? 'color:#dc2626;font-weight:700;' : ''}">
                            ${log.executionTimeMs}ms
                        </span>
                    </td>
                    <td class="mono">${log.remoteIpAddress}</td>
                </tr>
            </c:forEach>
            <c:if test="${empty recentAuditLogs}">
                <tr>
                    <td colspan="9" style="text-align:center;padding:48px;color:#9ca3af;">
                        No audit log entries found. Entries appear after you navigate the portal.
                    </td>
                </tr>
            </c:if>
            </tbody>
        </table>
    </div>
</div>
</body>
</html>
