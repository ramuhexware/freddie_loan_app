<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c"   uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"  %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <title>Loan Documents — Freddie Mac Portal</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet"/>
    <style>
        *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: 'Inter', sans-serif; background: #f1f5f9; color: #1e293b; }
        .page-body { padding: 28px 32px; }
        .page-title { font-size: 22px; font-weight: 700; color: #0a2342; margin-bottom: 8px; }
        .page-sub   { font-size: 13px; color: #6b7280; margin-bottom: 24px; }
        .btn-bar { display:flex; gap:10px; margin-bottom:20px; align-items:center; }
        .btn     { padding:9px 18px; border-radius:7px; border:none; font-size:13px; font-weight:600;
                   cursor:pointer; text-decoration:none; display:inline-block; }
        .btn-primary   { background:#0d6efd; color:#fff; }
        .btn-success   { background:#22c55e; color:#fff; }
        .btn-warning   { background:#f59e0b; color:#fff; }
        .btn-secondary { background:#e5e7eb; color:#374151; }
        .btn-danger    { background:#ef4444; color:#fff; font-size:12px; padding:6px 12px; }

        .card { background:#fff; border-radius:12px; padding:0; box-shadow:0 1px 4px rgba(0,0,0,.08);
                overflow:hidden; margin-bottom:20px; }
        .card-header { background:#0a2342; color:#fff; padding:14px 20px; font-size:14px; font-weight:700; }
        table { width:100%; border-collapse:collapse; font-size:13px; }
        th { text-align:left; padding:10px 14px; font-size:11px; font-weight:700; color:#6b7280;
             text-transform:uppercase; border-bottom:1px solid #e5e7eb; background:#f8fafc; }
        td { padding:12px 14px; border-bottom:1px solid #f3f4f6; vertical-align:middle; }
        tr:last-child td { border-bottom:none; }
        tr:hover td { background:#f8fafc; }

        .badge { display:inline-block; padding:2px 8px; border-radius:10px; font-size:11px; font-weight:700; text-transform:uppercase; }
        .badge-green  { background:#dcfce7; color:#166534; }
        .badge-yellow { background:#fef9c3; color:#854d0e; }
        .badge-red    { background:#fee2e2; color:#991b1b; }
        .badge-gray   { background:#f3f4f6; color:#374151; }

        .io-legend { background:#eff6ff; border:1px solid #93c5fd; border-radius:8px;
                     padding:14px 16px; font-size:12px; color:#1e40af; margin-bottom:20px;
                     display:grid; grid-template-columns:1fr 1fr; gap:6px; }
        .io-legend strong { color:#0a2342; }
    </style>
</head>
<body>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<div class="page-body">
    <h1 class="page-title">📂 Loan Documents</h1>
    <p class="page-sub">Loan ID: <strong>${loanId}</strong>
        <c:if test="${not empty loan}"> — <strong>${loan.customerName}</strong></c:if>
    </p>

    <%-- IO operation legend --%>
    <div class="io-legend">
        <div><strong>📤 Upload:</strong> Files saved to disk via <code>Files.write()</code> + metadata to DB2</div>
        <div><strong>📥 Download:</strong> <code>Files.readAllBytes()</code> → streamed to HTTP response</div>
        <div><strong>🗜 ZIP:</strong> <code>ZipOutputStream</code> bundles all files for batch download</div>
        <div><strong>📋 Report:</strong> <code>BufferedWriter</code> writes .txt report, <code>FileOutputStream</code> saves metadata snapshot</div>
        <div><strong>🔍 Directory:</strong> <code>File.listFiles()</code> reconciles DB with filesystem</div>
        <div><strong>📇 Index:</strong> <code>FileWriter(append)</code> maintains per-loan document index</div>
    </div>

    <c:if test="${not empty actionMessages}">
        <div style="background:#f0fdf4;border:1px solid #86efac;border-radius:8px;
                    padding:12px 16px;margin-bottom:16px;font-size:13px;color:#166534;">
            <c:forEach var="m" items="${actionMessages}"><div>✅ ${m}</div></c:forEach>
        </div>
    </c:if>
    <c:if test="${not empty actionErrors}">
        <div style="background:#fee2e2;border:1px solid #fca5a5;border-radius:8px;
                    padding:12px 16px;margin-bottom:16px;font-size:13px;color:#991b1b;">
            <c:forEach var="e" items="${actionErrors}"><div>⚠️ ${e}</div></c:forEach>
        </div>
    </c:if>

    <div class="btn-bar">
        <a href="${pageContext.request.contextPath}/document/uploadForm.action?loanId=${loanId}"
           class="btn btn-primary" id="btn-upload-new">+ Upload Document</a>
        <a href="${pageContext.request.contextPath}/document/downloadZip.action?loanId=${loanId}"
           class="btn btn-success" id="btn-download-zip">🗜 Download All (ZIP)</a>
        <a href="${pageContext.request.contextPath}/document/generateReport.action?loanId=${loanId}"
           class="btn btn-warning" id="btn-generate-report">📋 Download Report (.txt)</a>
        <a href="${pageContext.request.contextPath}/loan/detail.action?loanId=${loanId}"
           class="btn btn-secondary">← Loan Detail</a>
    </div>

    <%-- DB2 Documents table --%>
    <div class="card">
        <div class="card-header">DB2 Document Records — FREDDIE_LOANS.LOAN_DOCUMENTS (${documents.size()} records)</div>
        <table>
            <thead>
                <tr>
                    <th>Original Filename</th>
                    <th>Type</th>
                    <th>Size</th>
                    <th>MIME</th>
                    <th>Uploaded By</th>
                    <th>Status</th>
                    <th>Uploaded At</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
            <c:forEach var="doc" items="${documents}">
                <tr>
                    <td>
                        <a href="${pageContext.request.contextPath}/document/download.action?documentId=${doc.documentId}"
                           style="color:#0d6efd;text-decoration:none;font-weight:600;"
                           id="link-dl-${doc.documentId}" title="Download file from disk">
                            📄 ${doc.originalFileName}
                        </a>
                        <div style="font-size:10px;color:#9ca3af;font-family:monospace;margin-top:2px;">${doc.documentId}</div>
                    </td>
                    <td><span class="badge badge-gray">${doc.documentType}</span></td>
                    <td>${doc.fileSizeBytes} B</td>
                    <td style="font-size:11px;color:#6b7280;">${doc.mimeType}</td>
                    <td>${doc.uploadedBy}</td>
                    <td>
                        <c:choose>
                            <c:when test="${doc.status == 'APPROVED'}"><span class="badge badge-green">APPROVED</span></c:when>
                            <c:when test="${doc.status == 'REJECTED'}"><span class="badge badge-red">REJECTED</span></c:when>
                            <c:otherwise><span class="badge badge-yellow">PENDING</span></c:otherwise>
                        </c:choose>
                    </td>
                    <td style="font-size:11px;"><fmt:formatDate value="${doc.uploadedAt}" pattern="yyyy-MM-dd HH:mm"/></td>
                    <td>
                        <a href="${pageContext.request.contextPath}/document/download.action?documentId=${doc.documentId}"
                           style="color:#0d6efd;font-size:12px;font-weight:600;text-decoration:none;">⬇</a>
                        &nbsp;
                        <a href="${pageContext.request.contextPath}/document/delete.action?documentId=${doc.documentId}&loanId=${loanId}"
                           style="color:#ef4444;font-size:12px;font-weight:600;text-decoration:none;"
                           onclick="return confirm('Delete this document from disk and DB2?')"
                           id="btn-del-${doc.documentId}">🗑</a>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty documents}">
                <tr><td colspan="8" style="text-align:center;padding:40px;color:#9ca3af;">
                    No documents uploaded yet.
                    <a href="${pageContext.request.contextPath}/document/uploadForm.action?loanId=${loanId}"
                       style="color:#0d6efd;">Upload the first one →</a>
                </td></tr>
            </c:if>
            </tbody>
        </table>
    </div>

    <%-- Disk directory listing (from File.listFiles()) --%>
    <c:if test="${not empty directoryListing}">
        <div class="card">
            <div class="card-header">Filesystem Directory Listing — File.listFiles() (${directoryListing.size()} physical files)</div>
            <table>
                <thead>
                    <tr>
                        <th>Stored Filename (UUID)</th>
                        <th>Size on Disk</th>
                        <th>Last Modified</th>
                        <th>Readable</th>
                        <th>Writable</th>
                    </tr>
                </thead>
                <tbody>
                <c:forEach var="f" items="${directoryListing}">
                    <tr>
                        <td style="font-family:monospace;font-size:11px;">${f.fileName}</td>
                        <td>${f.sizeFormatted}</td>
                        <td style="font-size:11px;color:#6b7280;">${f.lastModified}</td>
                        <td>${f.canRead ? '✅' : '❌'}</td>
                        <td>${f.canWrite ? '✅' : '❌'}</td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </c:if>
</div>
</body>
</html>
