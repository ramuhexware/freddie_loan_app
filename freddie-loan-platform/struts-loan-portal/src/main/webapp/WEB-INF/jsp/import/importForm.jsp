<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c"   uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"  %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <title>Import Loans — Freddie Mac Portal</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet"/>
    <style>
        *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: 'Inter', sans-serif; background: #f1f5f9; color: #1e293b; }
        .page-body { padding: 28px 32px; max-width: 820px; }
        .page-title { font-size: 22px; font-weight: 700; color: #0a2342; margin-bottom: 24px; }
        .card { background: #fff; border-radius: 12px; padding: 28px; box-shadow: 0 1px 4px rgba(0,0,0,.08); margin-bottom: 20px; }
        .card-title { font-size: 14px; font-weight: 700; color: #0a2342; border-bottom: 1px solid #e5e7eb;
                      padding-bottom: 10px; margin-bottom: 18px; }
        .error-box { background:#fee2e2; border:1px solid #fca5a5; border-radius:8px;
                     padding:12px; margin-bottom:16px; font-size:13px; color:#991b1b; }
        .info-box  { background:#eff6ff; border:1px solid #93c5fd; border-radius:8px;
                     padding:14px 16px; font-size:12px; color:#1e40af; margin-bottom:20px; }
        .drop-zone { border:2px dashed #93c5fd; border-radius:10px; padding:40px;
                     text-align:center; background:#eff6ff; cursor:pointer; position:relative;
                     transition:border-color .2s, background .2s; }
        .drop-zone:hover { border-color:#0d6efd; background:#dbeafe; }
        .drop-zone input[type=file] { position:absolute; inset:0; opacity:0; cursor:pointer;
                                       width:100%; height:100%; }
        .drop-icon { font-size:36px; }
        .drop-text { font-size:15px; font-weight:600; color:#1e40af; margin-top:10px; }
        .drop-sub  { font-size:12px; color:#6b7280; margin-top:4px; }
        .file-preview { display:none; margin-top:10px; background:#f0fdf4; border:1px solid #86efac;
                         border-radius:8px; padding:10px 14px; font-size:13px; color:#166534; }
        .btn { padding:11px 24px; border-radius:8px; border:none; font-size:13px; font-weight:600;
               cursor:pointer; text-decoration:none; display:inline-block; }
        .btn-primary { background:#0d6efd; color:#fff; margin-top:18px; }
        .btn-primary:hover { background:#0a58ca; }

        /* CSV template box */
        pre { background:#0f172a; color:#e2e8f0; padding:16px; border-radius:8px;
              font-size:12px; overflow-x:auto; margin-top:10px; }

        /* File class badge */
        .file-class-badge { display:inline-block; background:#fef3c7; color:#92400e; border:1px solid #fcd34d;
                             border-radius:6px; padding:2px 8px; font-size:11px; font-weight:700; margin:2px; }
    </style>
</head>
<body>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<div class="page-body">
    <h1 class="page-title">⬆ Batch Import — Loan Applications from CSV</h1>

    <c:if test="${not empty actionErrors}">
        <div class="error-box">
            <c:forEach var="e" items="${actionErrors}"><div>⚠️ ${e}</div></c:forEach>
        </div>
    </c:if>

    <div class="info-box">
        <strong>📁 java.io.File operations used during import:</strong><br/>
        <span class="file-class-badge">File.exists()</span>
        <span class="file-class-badge">File.isFile()</span>
        <span class="file-class-badge">File.canRead()</span>
        <span class="file-class-badge">File.length()</span>
        <span class="file-class-badge">File.getName()</span>
        <span class="file-class-badge">File.getAbsolutePath()</span>
        <span class="file-class-badge">FileInputStream(File)</span>
        <br/>
        The Struts temp {@code File} is validated using these methods, then opened
        via {@code FileInputStream} for line-by-line CSV parsing with {@code BufferedReader}.
        Valid rows are batch-inserted into DB2.
    </div>

    <div class="card">
        <div class="card-title">Upload CSV File</div>

        <form method="post" enctype="multipart/form-data"
              action="${pageContext.request.contextPath}/import/importCsv.action"
              id="import-form">

            <div class="drop-zone" id="importDropZone">
                <input type="file" name="upload" id="csvFileInput"
                       accept=".csv,.txt" required/>
                <div class="drop-icon">📊</div>
                <div class="drop-text">Click to select a CSV file</div>
                <div class="drop-sub">.csv or .txt — max 10 MB</div>
            </div>

            <div class="file-preview" id="importFilePreview">
                <strong>Selected:</strong>
                <span id="importFileName"></span> —
                <span id="importFileSize"></span>
            </div>

            <button type="submit" class="btn btn-primary" id="btn-import">
                🚀 Import Loans → DB2
            </button>
        </form>
    </div>

    <div class="card">
        <div class="card-title">Expected CSV Format</div>
        <p style="font-size:13px;color:#374151;margin-bottom:8px;">
            First row must be a header (skipped automatically). Columns:
        </p>
        <pre>CUSTOMER_ID,LOAN_TYPE,LOAN_AMOUNT,PROPERTY_VALUE,PROPERTY_ADDRESS,LOAN_TERM_MONTHS
c001,PURCHASE,450000.00,560000.00,"123 Main St, Austin TX",360
c002,REFINANCE,320000.00,400000.00,"200 Pine Ave, Houston TX",180
c003,HELOC,80000.00,560000.00,,120</pre>
        <ul style="margin-top:14px;font-size:13px;color:#374151;padding-left:20px;">
            <li><strong>CUSTOMER_ID</strong> — required, must exist in DB2 CUSTOMERS table</li>
            <li><strong>LOAN_TYPE</strong> — required: PURCHASE | REFINANCE | HELOC | HOME_EQUITY</li>
            <li><strong>LOAN_AMOUNT</strong> — required, positive decimal</li>
            <li><strong>PROPERTY_VALUE</strong> — optional</li>
            <li><strong>PROPERTY_ADDRESS</strong> — optional</li>
            <li><strong>LOAN_TERM_MONTHS</strong> — optional (default: 360)</li>
        </ul>
        <div style="margin-top:16px;">
            <a href="${pageContext.request.contextPath}/export/loans.action"
               style="color:#0d6efd;font-size:13px;font-weight:600;text-decoration:none;">
                📥 Export existing loans as CSV template →
            </a>
        </div>
    </div>
</div>
<script>
    const csvInput    = document.getElementById('csvFileInput');
    const preview     = document.getElementById('importFilePreview');

    csvInput.addEventListener('change', function() {
        if (this.files && this.files[0]) {
            const f = this.files[0];
            document.getElementById('importFileName').textContent = f.name;
            document.getElementById('importFileSize').textContent =
                (f.size / 1024).toFixed(1) + ' KB';
            preview.style.display = 'block';
        }
    });
</script>
</body>
</html>
