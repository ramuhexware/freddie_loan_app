<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c"   uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"  %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title>Freddie Mac Loan Portal — Login</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;600;700&display=swap" rel="stylesheet"/>
    <style>
        *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            font-family: 'Inter', sans-serif;
            background: linear-gradient(135deg, #0a2342 0%, #1a4a7a 60%, #0d6efd 100%);
            min-height: 100vh;
            display: flex; align-items: center; justify-content: center;
        }
        .login-card {
            background: rgba(255,255,255,0.97);
            border-radius: 16px;
            padding: 48px 40px;
            width: 420px;
            box-shadow: 0 24px 64px rgba(0,0,0,0.35);
        }
        .logo-row { text-align: center; margin-bottom: 32px; }
        .logo-row .brand { font-size: 28px; font-weight: 700; color: #0a2342; }
        .logo-row .sub   { font-size: 13px; color: #6b7280; margin-top: 4px; }
        .form-group { margin-bottom: 20px; }
        label { display: block; font-size: 13px; font-weight: 600; color: #374151; margin-bottom: 6px; }
        input[type=text], input[type=password] {
            width: 100%; padding: 12px 14px; border: 1.5px solid #d1d5db;
            border-radius: 8px; font-size: 14px; transition: border-color .2s;
        }
        input:focus { outline: none; border-color: #0d6efd; box-shadow: 0 0 0 3px rgba(13,110,253,.12); }
        .btn-primary {
            width: 100%; padding: 14px; background: #0d6efd; color: #fff;
            border: none; border-radius: 8px; font-size: 15px; font-weight: 600;
            cursor: pointer; transition: background .2s, transform .1s;
        }
        .btn-primary:hover { background: #0a58ca; transform: translateY(-1px); }
        .error-box {
            background: #fef2f2; border: 1px solid #fca5a5; border-radius: 8px;
            padding: 12px 14px; font-size: 13px; color: #b91c1c; margin-bottom: 20px;
        }
        .hint { font-size: 12px; color: #9ca3af; text-align: center; margin-top: 24px; }
    </style>
</head>
<body>
<div class="login-card">
    <div class="logo-row">
        <div class="brand">🏠 Freddie Mac</div>
        <div class="sub">Loan Administration Portal</div>
    </div>

    <c:if test="${not empty actionErrors}">
        <div class="error-box">
            <c:forEach var="err" items="${actionErrors}"><span>${err}</span></c:forEach>
        </div>
    </c:if>

    <form method="post" action="${pageContext.request.contextPath}/login.action">
        <div class="form-group">
            <label for="username">Username</label>
            <input type="text" id="username" name="username"
                   placeholder="Enter your username" value="${username}" required/>
        </div>
        <div class="form-group">
            <label for="password">Password</label>
            <input type="password" id="password" name="password"
                   placeholder="Enter your password" required/>
        </div>
        <button type="submit" class="btn-primary" id="btn-login">Sign In</button>
    </form>
    <div class="hint">Demo: admin / freddie123 &nbsp;|&nbsp; officer / freddie123</div>
</div>
</body>
</html>
