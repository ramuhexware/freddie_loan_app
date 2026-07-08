<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c"   uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"  %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <title>Apply for Loan — Freddie Mac Portal</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet"/>
    <style>
        *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: 'Inter', sans-serif; background: #f1f5f9; color: #1e293b; }
        .page-body { padding: 28px 32px; max-width: 760px; }
        .page-title { font-size: 22px; font-weight: 700; color: #0a2342; margin-bottom: 24px; }
        .card { background: #fff; border-radius: 12px; padding: 32px; box-shadow: 0 1px 4px rgba(0,0,0,.08); }
        .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
        .form-group { margin-bottom: 0; }
        .form-group.full { grid-column: 1 / -1; }
        label { display: block; font-size: 12px; font-weight: 700; color: #374151;
                text-transform: uppercase; letter-spacing: .04em; margin-bottom: 6px; }
        input, select, textarea {
            width: 100%; padding: 11px 13px; border: 1.5px solid #d1d5db;
            border-radius: 8px; font-size: 14px; font-family: inherit; transition: border-color .2s;
        }
        input:focus, select:focus { outline: none; border-color: #0d6efd; box-shadow: 0 0 0 3px rgba(13,110,253,.12); }
        .error-text { color: #dc2626; font-size: 12px; margin-top: 4px; }
        .divider { grid-column: 1 / -1; border: none; border-top: 1px solid #e5e7eb; margin: 4px 0; }
        .section-label { grid-column: 1 / -1; font-size: 13px; font-weight: 700; color: #6b7280;
                         text-transform: uppercase; letter-spacing: .05em; padding-top: 8px; }
        .btn-row { grid-column: 1 / -1; display: flex; gap: 12px; padding-top: 8px; }
        .btn { padding: 12px 28px; border-radius: 8px; border: none; font-size: 14px;
               font-weight: 600; cursor: pointer; text-decoration: none; }
        .btn-primary { background: #0d6efd; color: #fff; }
        .btn-primary:hover { background: #0a58ca; }
        .btn-secondary { background: #e5e7eb; color: #374151; }
        .error-box { background: #fee2e2; border: 1px solid #fca5a5; border-radius: 8px;
                     padding: 14px; margin-bottom: 20px; font-size: 13px; color: #991b1b; }
    </style>
</head>
<body>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<div class="page-body">
    <a href="${pageContext.request.contextPath}/loan/list.action"
       style="color:#6b7280;text-decoration:none;font-size:13px;">← Back to Loans</a>
    <h1 class="page-title" style="margin-top:12px;">📝 New Loan Application</h1>

    <c:if test="${not empty actionErrors}">
        <div class="error-box">
            <c:forEach var="e" items="${actionErrors}"><div>${e}</div></c:forEach>
        </div>
    </c:if>

    <div class="card">
        <form method="post" action="${pageContext.request.contextPath}/loan/save.action" id="loan-apply-form">
            <div class="form-grid">
                <div class="section-label">Customer Information</div>

                <div class="form-group full">
                    <label for="customerId">Customer ID *</label>
                    <input type="text" id="customerId" name="customerId"
                           placeholder="e.g. f47ac10b-58cc-4372-a567-0e02b2c3d479"
                           value="${customerId}" required/>
                    <c:if test="${not empty fieldErrors.customerId}">
                        <div class="error-text">${fieldErrors.customerId[0]}</div>
                    </c:if>
                </div>

                <hr class="divider"/>
                <div class="section-label">Loan Details</div>

                <div class="form-group">
                    <label for="loanType">Loan Type *</label>
                    <select id="loanType" name="loanType" required>
                        <option value="">-- Select Type --</option>
                        <option value="PURCHASE"     ${loanType == 'PURCHASE'     ? 'selected' : ''}>Purchase</option>
                        <option value="REFINANCE"    ${loanType == 'REFINANCE'    ? 'selected' : ''}>Refinance</option>
                        <option value="HELOC"        ${loanType == 'HELOC'        ? 'selected' : ''}>HELOC</option>
                        <option value="HOME_EQUITY"  ${loanType == 'HOME_EQUITY'  ? 'selected' : ''}>Home Equity</option>
                    </select>
                    <c:if test="${not empty fieldErrors.loanType}">
                        <div class="error-text">${fieldErrors.loanType[0]}</div>
                    </c:if>
                </div>

                <div class="form-group">
                    <label for="loanAmount">Loan Amount ($) *</label>
                    <input type="number" id="loanAmount" name="loanAmount"
                           step="0.01" min="1" placeholder="e.g. 450000.00"
                           value="${loanAmount}" required/>
                    <c:if test="${not empty fieldErrors.loanAmount}">
                        <div class="error-text">${fieldErrors.loanAmount[0]}</div>
                    </c:if>
                </div>

                <div class="form-group">
                    <label for="propertyValue">Property Value ($)</label>
                    <input type="number" id="propertyValue" name="propertyValue"
                           step="0.01" min="0" placeholder="e.g. 550000.00"
                           value="${propertyValue}"/>
                </div>

                <div class="form-group">
                    <label for="loanTermMonths">Loan Term (Months) *</label>
                    <select id="loanTermMonths" name="loanTermMonths">
                        <option value="60"  ${loanTermMonths == 60  ? 'selected' : ''}>5 Years (60 months)</option>
                        <option value="120" ${loanTermMonths == 120 ? 'selected' : ''}>10 Years (120 months)</option>
                        <option value="180" ${loanTermMonths == 180 ? 'selected' : ''}>15 Years (180 months)</option>
                        <option value="240" ${loanTermMonths == 240 ? 'selected' : ''}>20 Years (240 months)</option>
                        <option value="360" ${loanTermMonths == 360 ? 'selected' : ''} selected>30 Years (360 months)</option>
                    </select>
                </div>

                <div class="form-group full">
                    <label for="propertyAddress">Property Address</label>
                    <input type="text" id="propertyAddress" name="propertyAddress"
                           placeholder="123 Main St, Springfield, IL 62701"
                           value="${propertyAddress}" maxlength="500"/>
                </div>

                <div class="btn-row">
                    <button type="submit" class="btn btn-primary" id="btn-submit-loan">
                        🚀 Submit Application (via REST → loan-origination-service)
                    </button>
                    <a href="${pageContext.request.contextPath}/loan/list.action" class="btn btn-secondary">Cancel</a>
                </div>
            </div>
        </form>
    </div>
</div>
</body>
</html>
