<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c"   uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"  %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <title>Upload Document — Freddie Mac Portal</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet"/>
    <style>
        *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: 'Inter', sans-serif; background: #f1f5f9; color: #1e293b; }
        .page-body { padding: 28px 32px; max-width: 780px; }
        .page-title { font-size: 22px; font-weight: 700; color: #0a2342; margin: 12px 0 24px; }
        .card { background: #fff; border-radius: 12px; padding: 28px; box-shadow: 0 1px 4px rgba(0,0,0,.08); margin-bottom: 20px; }
        .card-title { font-size: 14px; font-weight: 700; color: #0a2342; border-bottom: 1px solid #e5e7eb;
                      padding-bottom: 10px; margin-bottom: 18px; }

        /* Drag-drop zone */
        .drop-zone {
            border: 2px dashed #93c5fd; border-radius: 10px; padding: 40px;
            text-align: center; background: #eff6ff; cursor: pointer;
            transition: border-color .2s, background .2s; position: relative;
        }
        .drop-zone:hover, .drop-zone.drag-over {
            border-color: #0d6efd; background: #dbeafe;
        }
        .drop-zone input[type=file] {
            position: absolute; inset: 0; opacity: 0; cursor: pointer; width: 100%; height: 100%;
        }
        .drop-icon  { font-size: 40px; }
        .drop-text  { font-size: 15px; font-weight: 600; color: #1e40af; margin-top: 10px; }
        .drop-sub   { font-size: 12px; color: #6b7280; margin-top: 4px; }
        .file-preview { display:none; margin-top: 12px; background: #f0fdf4;
                        border: 1px solid #86efac; border-radius: 8px; padding: 12px 16px;
                        font-size: 13px; color: #166534; }

        label    { display:block; font-size:12px; font-weight:700; color:#374151;
                   text-transform:uppercase; margin-bottom:5px; }
        select, input[type=text] { width:100%; padding:10px 12px; border:1.5px solid #d1d5db;
                   border-radius:8px; font-size:13px; font-family:inherit; }
        select:focus, input:focus { outline:none; border-color:#0d6efd; }
        .btn-row { display:flex; gap:12px; margin-top:20px; }
        .btn     { padding:11px 24px; border-radius:8px; border:none; font-size:13px;
                   font-weight:600; cursor:pointer; text-decoration:none; display:inline-block; }
        .btn-primary   { background:#0d6efd; color:#fff; }
        .btn-primary:hover { background:#0a58ca; }
        .btn-secondary { background:#e5e7eb; color:#374151; }
        .error-box { background:#fee2e2; border:1px solid #fca5a5; border-radius:8px;
                     padding:12px; margin-bottom:16px; font-size:13px; color:#991b1b; }
        .info-box  { background:#eff6ff; border:1px solid #93c5fd; border-radius:8px;
                     padding:12px; font-size:12px; color:#1e40af; margin-bottom:16px; }
        .form-group { margin-bottom: 16px; }
    </style>
</head>
<body>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<div class="page-body">
    <a href="${pageContext.request.contextPath}/loan/list.action"
       style="color:#6b7280;text-decoration:none;font-size:13px;">← Back to Loans</a>
    <h1 class="page-title">📎 Upload Document
        <c:if test="${not empty loanId}">
            <span style="font-size:14px;font-weight:400;color:#9ca3af;">for Loan ${loanId}</span>
        </c:if>
    </h1>

    <c:if test="${not empty actionErrors}">
        <div class="error-box">
            <c:forEach var="e" items="${actionErrors}"><div>⚠️ ${e}</div></c:forEach>
        </div>
    </c:if>

    <div class="info-box">
        💡 <strong>IO Flow:</strong> File bytes → Saved to disk via <code>java.nio.Files.write()</code>
        → Metadata persisted to DB2 via <code>DocumentDao.insertDocument()</code>
        → Properties snapshot written via <code>FileOutputStream</code>.
        Allowed: PDF, PNG, JPG, TIFF, DOCX · Max 25 MB.
    </div>

    <div class="card">
        <div class="card-title">Select Document to Upload</div>

        <form method="post" enctype="multipart/form-data"
              action="${pageContext.request.contextPath}/document/upload.action"
              id="upload-form">

            <input type="hidden" name="loanId" value="${loanId}"/>

            <!-- Drag & Drop zone -->
            <div class="drop-zone" id="dropZone">
                <input type="file" name="upload" id="fileInput" required
                       accept=".pdf,.png,.jpg,.jpeg,.tiff,.doc,.docx"/>
                <div class="drop-icon">📄</div>
                <div class="drop-text">Click to browse or drag & drop</div>
                <div class="drop-sub">PDF, PNG, JPG, TIFF, DOCX — up to 25 MB</div>
            </div>

            <!-- File preview (shown after selection) -->
            <div class="file-preview" id="filePreview">
                <strong>Selected:</strong> <span id="selectedFileName"></span>
                &nbsp;|&nbsp; <span id="selectedFileSize"></span>
            </div>

            <div class="form-group" style="margin-top:18px;">
                <label for="documentType">Document Type *</label>
                <select id="documentType" name="documentType" required>
                    <option value="">-- Select Document Type --</option>
                    <option value="PAY_STUB">Pay Stub</option>
                    <option value="TAX_RETURN">Tax Return (W-2 / 1040)</option>
                    <option value="BANK_STATEMENT">Bank Statement</option>
                    <option value="APPRAISAL">Property Appraisal</option>
                    <option value="PURCHASE_AGREEMENT">Purchase Agreement</option>
                    <option value="GOVERNMENT_ID">Government Issued ID</option>
                    <option value="INSURANCE_POLICY">Homeowners Insurance</option>
                    <option value="OTHER">Other</option>
                </select>
                <c:if test="${not empty fieldErrors.documentType}">
                    <div style="color:#dc2626;font-size:12px;margin-top:3px;">${fieldErrors.documentType[0]}</div>
                </c:if>
            </div>

            <div class="btn-row">
                <button type="submit" class="btn btn-primary" id="btn-upload">⬆ Upload Document</button>
                <a href="${pageContext.request.contextPath}/loan/detail.action?loanId=${loanId}"
                   class="btn btn-secondary">Cancel</a>
            </div>
        </form>
    </div>

    <%-- Existing documents table --%>
    <c:if test="${not empty documents}">
        <div class="card">
            <div class="card-title">Documents Already Uploaded for This Loan (from DB2)</div>
            <table style="width:100%;border-collapse:collapse;font-size:13px;">
                <thead style="background:#f8fafc;">
                    <tr>
                        <th style="padding:10px 12px;text-align:left;font-size:11px;color:#6b7280;text-transform:uppercase;border-bottom:1px solid #e5e7eb;">File Name</th>
                        <th style="padding:10px 12px;text-align:left;font-size:11px;color:#6b7280;text-transform:uppercase;border-bottom:1px solid #e5e7eb;">Type</th>
                        <th style="padding:10px 12px;text-align:left;font-size:11px;color:#6b7280;text-transform:uppercase;border-bottom:1px solid #e5e7eb;">Size</th>
                        <th style="padding:10px 12px;text-align:left;font-size:11px;color:#6b7280;text-transform:uppercase;border-bottom:1px solid #e5e7eb;">Status</th>
                        <th style="padding:10px 12px;text-align:left;font-size:11px;color:#6b7280;text-transform:uppercase;border-bottom:1px solid #e5e7eb;">Uploaded</th>
                    </tr>
                </thead>
                <tbody>
                <c:forEach var="doc" items="${documents}">
                    <tr>
                        <td style="padding:10px 12px;border-bottom:1px solid #f3f4f6;">
                            <a href="${pageContext.request.contextPath}/document/download.action?documentId=${doc.documentId}"
                               style="color:#0d6efd;text-decoration:none;font-weight:600;">
                                📄 ${doc.originalFileName}
                            </a>
                        </td>
                        <td style="padding:10px 12px;border-bottom:1px solid #f3f4f6;">${doc.documentType}</td>
                        <td style="padding:10px 12px;border-bottom:1px solid #f3f4f6;">${doc.fileSizeBytes} B</td>
                        <td style="padding:10px 12px;border-bottom:1px solid #f3f4f6;">
                            <span style="background:${doc.status == 'APPROVED' ? '#dcfce7' : doc.status == 'REJECTED' ? '#fee2e2' : '#fef9c3'};
                                         color:${doc.status == 'APPROVED' ? '#166534' : doc.status == 'REJECTED' ? '#991b1b' : '#854d0e'};
                                         padding:2px 8px;border-radius:10px;font-size:11px;font-weight:700;">
                                ${doc.status}
                            </span>
                        </td>
                        <td style="padding:10px 12px;border-bottom:1px solid #f3f4f6;font-size:11px;color:#9ca3af;">
                            <fmt:formatDate value="${doc.uploadedAt}" pattern="yyyy-MM-dd HH:mm"/>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </c:if>
</div>

<script>
    const fileInput   = document.getElementById('fileInput');
    const filePreview = document.getElementById('filePreview');
    const dropZone    = document.getElementById('dropZone');

    fileInput.addEventListener('change', function () {
        if (this.files && this.files[0]) {
            const f = this.files[0];
            document.getElementById('selectedFileName').textContent = f.name;
            document.getElementById('selectedFileSize').textContent =
                (f.size / 1024).toFixed(1) + ' KB';
            filePreview.style.display = 'block';
        }
    });

    dropZone.addEventListener('dragover', e => { e.preventDefault(); dropZone.classList.add('drag-over'); });
    dropZone.addEventListener('dragleave', () => dropZone.classList.remove('drag-over'));
    dropZone.addEventListener('drop', e => {
        e.preventDefault();
        dropZone.classList.remove('drag-over');
        fileInput.files = e.dataTransfer.files;
        fileInput.dispatchEvent(new Event('change'));
    });
</script>
</body>
</html>
