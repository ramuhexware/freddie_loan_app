<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="jakarta.servlet.jsp.jstl/core" prefix="c"%>
<%@ taglib uri="jakarta.servlet.jsp.jstl/fmt" prefix="fmt"%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Freddie Mac-Style Legacy Admin Dashboard</title>
    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <style>
        :root {
            --primary-color: #0d2c54;
            --accent-color: #00b4d8;
            --background-color: #f8f9fa;
            --card-bg: #ffffff;
            --text-main: #2b2d42;
            --text-secondary: #6c757d;
            --border-color: #e9ecef;
            --success-color: #2ec4b6;
            --warning-color: #ff9f1c;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }

        body {
            font-family: 'Outfit', sans-serif;
            background-color: var(--background-color);
            color: var(--text-main);
            line-height: 1.6;
            padding: 40px 20px;
        }

        .container {
            max-width: 1200px;
            margin: 0 auto;
        }

        header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 40px;
            border-bottom: 2px solid var(--border-color);
            padding-bottom: 20px;
        }

        .logo-section h1 {
            font-size: 28px;
            color: var(--primary-color);
            font-weight: 700;
        }

        .logo-section p {
            color: var(--text-secondary);
            font-size: 14px;
            margin-top: 4px;
        }

        .status-badge {
            background-color: rgba(46, 196, 182, 0.15);
            color: var(--success-color);
            padding: 8px 16px;
            border-radius: 30px;
            font-weight: 600;
            font-size: 14px;
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .status-dot {
            width: 10px;
            height: 10px;
            background-color: var(--success-color);
            border-radius: 50%;
            display: inline-block;
        }

        .stats-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 20px;
            margin-bottom: 40px;
        }

        .stat-card {
            background-color: var(--card-bg);
            border-radius: 12px;
            padding: 24px;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.02);
            border: 1px solid var(--border-color);
            transition: transform 0.2s ease, box-shadow 0.2s ease;
        }

        .stat-card:hover {
            transform: translateY(-5px);
            box-shadow: 0 10px 15px rgba(0, 0, 0, 0.05);
        }

        .stat-title {
            font-size: 14px;
            color: var(--text-secondary);
            text-transform: uppercase;
            letter-spacing: 1px;
            margin-bottom: 8px;
            font-weight: 500;
        }

        .stat-value {
            font-size: 36px;
            color: var(--primary-color);
            font-weight: 700;
        }

        .content-card {
            background-color: var(--card-bg);
            border-radius: 16px;
            padding: 30px;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.02);
            border: 1px solid var(--border-color);
        }

        .content-card h2 {
            font-size: 20px;
            color: var(--primary-color);
            margin-bottom: 24px;
            font-weight: 600;
        }

        table {
            width: 100%;
            border-collapse: collapse;
            text-align: left;
        }

        th {
            background-color: #f1f5f9;
            color: var(--primary-color);
            padding: 16px;
            font-weight: 600;
            font-size: 14px;
            border-bottom: 2px solid var(--border-color);
        }

        td {
            padding: 16px;
            border-bottom: 1px solid var(--border-color);
            color: var(--text-main);
            font-size: 15px;
        }

        tr:hover td {
            background-color: #f8fafc;
        }

        .badge {
            padding: 6px 12px;
            border-radius: 20px;
            font-size: 12px;
            font-weight: 600;
            display: inline-block;
        }

        .badge-approved {
            background-color: rgba(46, 196, 182, 0.12);
            color: var(--success-color);
        }

        .badge-review {
            background-color: rgba(255, 159, 28, 0.12);
            color: var(--warning-color);
        }

        .badge-submitted {
            background-color: rgba(0, 180, 216, 0.12);
            color: var(--accent-color);
        }

        .action-btn {
            background-color: var(--primary-color);
            color: white;
            border: none;
            padding: 8px 16px;
            border-radius: 6px;
            font-size: 14px;
            font-weight: 500;
            cursor: pointer;
            transition: background-color 0.2s ease;
        }

        .action-btn:hover {
            background-color: #1a4276;
        }

        .calculator-card {
            background-color: var(--card-bg);
            border-radius: 16px;
            padding: 30px;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.02);
            border: 1px solid var(--border-color);
            margin-top: 40px;
        }

        .calculator-form {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 20px;
            margin-bottom: 24px;
            align-items: flex-end;
        }

        .form-group {
            display: flex;
            flex-direction: column;
            gap: 8px;
        }

        .form-group label {
            font-size: 14px;
            font-weight: 600;
            color: var(--primary-color);
        }

        .form-group input {
            padding: 10px 14px;
            border-radius: 6px;
            border: 1px solid var(--border-color);
            font-family: inherit;
            font-size: 15px;
            outline: none;
            transition: border-color 0.2s;
        }

        .form-group input:focus {
            border-color: var(--accent-color);
        }

        .calc-submit-btn {
            background-color: var(--accent-color);
            color: white;
            border: none;
            padding: 12px 24px;
            border-radius: 6px;
            font-size: 15px;
            font-weight: 600;
            cursor: pointer;
            transition: background-color 0.2s;
        }

        .calc-submit-btn:hover {
            background-color: #0096b4;
        }

        .results-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
            gap: 15px;
            margin-bottom: 30px;
            background-color: #f8fafc;
            padding: 20px;
            border-radius: 8px;
            border: 1px dashed var(--accent-color);
        }

        .result-item {
            display: flex;
            flex-direction: column;
        }

        .result-label {
            font-size: 12px;
            color: var(--text-secondary);
            text-transform: uppercase;
            font-weight: 500;
        }

        .result-value {
            font-size: 18px;
            color: var(--primary-color);
            font-weight: 700;
            margin-top: 4px;
        }

        .table-scrollable {
            max-height: 400px;
            overflow-y: auto;
            margin-top: 20px;
            border: 1px solid var(--border-color);
            border-radius: 8px;
        }

        .table-scrollable th {
            position: sticky;
            top: 0;
            z-index: 10;
        }

        .alert-danger {
            background-color: rgba(230, 57, 70, 0.1);
            color: #e63946;
            padding: 12px;
            border-radius: 6px;
            margin-bottom: 20px;
            font-size: 14px;
            border: 1px solid rgba(230, 57, 70, 0.2);
        }
    </style>
</head>
<body>
    <div class="container">
        <header>
            <div class="logo-section">
                <h1>Freddie Mac-Style Home Loan Platform</h1>
                <p>Legacy Admin Dashboard Console (Servlets &amp; JSP Integration)</p>
            </div>
            <div class="status-badge">
                <span class="status-dot"></span>
                <span>System status: ${stats.systemHealth}</span>
            </div>
        </header>

        <section class="stats-grid">
            <div class="stat-card">
                <div class="stat-title">Total Customers</div>
                <div class="stat-value">${stats.totalCustomers}</div>
            </div>
            <div class="stat-card">
                <div class="stat-title">Active Mortgages</div>
                <div class="stat-value">${stats.activeLoans}</div>
            </div>
            <div class="stat-card">
                <div class="stat-title">Underwritten Queue</div>
                <div class="stat-value">${stats.underwrittenCount}</div>
            </div>
        </section>

        <section class="content-card">
            <h2>Active Loan Origination Pipelines (JSP View)</h2>
            <table>
                <thead>
                    <tr>
                        <th>Loan ID</th>
                        <th>Borrower Name</th>
                        <th>Loan Type</th>
                        <th>Requested Amount</th>
                        <th>Status</th>
                        <th>Operations</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="app" items="${applications}">
                        <tr>
                            <td><strong>${app.loanId}</strong></td>
                            <td>${app.borrowerName}</td>
                            <td>${app.loanType}</td>
                            <td>
                                <fmt:formatNumber value="${app.amount}" type="currency" currencySymbol="$" maxFractionDigits="2"/>
                            </td>
                            <td>
                                <c:choose>
                                    <c:when test="${app.status == 'APPROVED'}">
                                        <span class="badge badge-approved">APPROVED</span>
                                    </c:when>
                                    <c:when test="${app.status == 'UNDER_REVIEW'}">
                                        <span class="badge badge-review">UNDER REVIEW</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="badge badge-submitted">SUBMITTED</span>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td>
                                <button class="action-btn" onclick="alert('Viewing application detailed logs for ${app.loanId}...')">Audit Details</button>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </section>

        <section class="calculator-card">
            <h2>Interactive LLPA Pricing &amp; Amortization Calculator</h2>
            <p style="color: var(--text-secondary); font-size: 14px; margin-top: 4px; margin-bottom: 24px;">
                Calculate Loan-to-Value (LTV), Risk Surcharge (LLPA), Private Mortgage Insurance (PMI), and generate a 30-Year Fixed Amortization Schedule.
            </p>

            <c:if test="${not empty calcError}">
                <div class="alert-danger">${calcError}</div>
            </c:if>

            <form action="dashboard" method="GET" class="calculator-form">
                <div class="form-group">
                    <label for="calcLoanAmount">Loan Amount ($)</label>
                    <input type="number" id="calcLoanAmount" name="calcLoanAmount" value="${empty param.calcLoanAmount ? '300000' : param.calcLoanAmount}" required>
                </div>
                <div class="form-group">
                    <label for="calcPropertyValue">Property Value ($)</label>
                    <input type="number" id="calcPropertyValue" name="calcPropertyValue" value="${empty param.calcPropertyValue ? '375000' : param.calcPropertyValue}" required>
                </div>
                <div class="form-group">
                    <label for="calcFicoScore">FICO Credit Score</label>
                    <input type="number" id="calcFicoScore" name="calcFicoScore" value="${empty param.calcFicoScore ? '720' : param.calcFicoScore}" min="300" max="850" required>
                </div>
                <div>
                    <button type="submit" class="calc-submit-btn">Project Payments</button>
                </div>
            </form>

            <c:if test="${showCalculatorResults}">
                <div class="results-grid">
                    <div class="result-item">
                        <div class="result-label">Loan-to-Value (LTV)</div>
                        <div class="result-value">
                            <fmt:formatNumber value="${calcLtv}" maxFractionDigits="2"/>%
                        </div>
                    </div>
                    <div class="result-item">
                        <div class="result-label">LLPA Risk Surcharge</div>
                        <div class="result-value">
                            <fmt:formatNumber value="${calcLlpa}" maxFractionDigits="2"/>%
                        </div>
                    </div>
                    <div class="result-item">
                        <div class="result-label">PMI Monthly Premium</div>
                        <div class="result-value">
                            <fmt:formatNumber value="${calcPmi}" type="currency" currencySymbol="$" maxFractionDigits="2"/>
                        </div>
                    </div>
                    <div class="result-item">
                        <div class="result-label">Adjusted Interest Rate</div>
                        <div class="result-value">
                            <fmt:formatNumber value="${calcAdjustedRate}" maxFractionDigits="2"/>%
                        </div>
                    </div>
                </div>

                <h3 style="margin-top: 30px; margin-bottom: 10px; color: var(--primary-color);">Amortization Schedule Preview (First 36 Months)</h3>
                <div class="table-scrollable">
                    <table>
                        <thead>
                            <tr>
                                <th>Month</th>
                                <th>Principal Paid</th>
                                <th>Interest Paid</th>
                                <th>PMI Premium</th>
                                <th>Total Payment</th>
                                <th>Remaining Principal</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="payment" items="${calcSchedule}">
                                <tr>
                                    <td><strong>Month ${payment.monthNumber}</strong></td>
                                    <td>
                                        <fmt:formatNumber value="${payment.principalPaid}" type="currency" currencySymbol="$"/>
                                    </td>
                                    <td>
                                        <fmt:formatNumber value="${payment.interestPaid}" type="currency" currencySymbol="$"/>
                                    </td>
                                    <td>
                                        <fmt:formatNumber value="${payment.pmiPaid}" type="currency" currencySymbol="$"/>
                                    </td>
                                    <td>
                                        <strong style="color: var(--primary-color);"><fmt:formatNumber value="${payment.totalPayment}" type="currency" currencySymbol="$"/></strong>
                                    </td>
                                    <td>
                                        <fmt:formatNumber value="${payment.remainingPrincipal}" type="currency" currencySymbol="$"/>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:if>
        </section>
    </div>
</body>
</html>
