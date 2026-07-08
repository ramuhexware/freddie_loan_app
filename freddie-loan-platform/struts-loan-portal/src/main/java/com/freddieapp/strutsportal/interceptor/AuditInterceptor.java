package com.freddieapp.strutsportal.interceptor;

import com.freddieapp.strutsportal.dao.AuditDao;
import com.freddieapp.strutsportal.model.AuditLog;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.struts2.ServletActionContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Struts 2 Interceptor — fires before and after EVERY action.
 *
 * Before:  records start time
 * After:   writes an AUDIT_LOG row to DB2 (PROPAGATION_REQUIRES_NEW)
 *
 * This ensures every click in the portal is permanently audited,
 * independent of whether the business transaction committed or rolled back.
 */
@Slf4j
public class AuditInterceptor extends AbstractInterceptor {

    @Setter
    @Autowired
    private AuditDao auditDao;

    @Override
    public String intercept(ActionInvocation invocation) throws Exception {
        long startTime = System.currentTimeMillis();

        HttpServletRequest request = ServletActionContext.getRequest();
        HttpSession        session = request != null ? request.getSession(false) : null;

        String namespace  = invocation.getProxy().getNamespace();
        String actionName = invocation.getProxy().getActionName();
        String method     = invocation.getProxy().getMethod();
        String remoteIp   = request != null ? request.getRemoteAddr() : "unknown";
        String sessionUser = session != null
                ? (String) session.getAttribute("loggedInUser") : "anonymous";

        // Capture request parameters as simple k=v string
        String params = "";
        if (request != null) {
            params = request.getParameterMap().entrySet().stream()
                    .filter(e -> !e.getKey().equalsIgnoreCase("password"))
                    .map(e -> e.getKey() + "=" + String.join(",", e.getValue()))
                    .collect(Collectors.joining("; "));
        }

        String resultCode  = "UNKNOWN";
        String errorMsg    = null;

        try {
            resultCode = invocation.invoke();  // execute the actual action
            return resultCode;
        } catch (Exception ex) {
            resultCode = "ERROR";
            errorMsg   = ex.getMessage();
            log.error("[AuditInterceptor] Exception in action {}/{}: {}", namespace, actionName, errorMsg);
            throw ex;
        } finally {
            long execTimeMs = System.currentTimeMillis() - startTime;
            try {
                AuditLog auditLog = AuditLog.builder()
                        .actionNamespace(namespace)
                        .actionName(actionName)
                        .actionMethod(method)
                        .sessionUser(sessionUser)
                        .remoteIpAddress(remoteIp)
                        .requestParameters(params.length() > 2000
                                ? params.substring(0, 2000) : params)
                        .resultCode(resultCode)
                        .executionTimeMs(execTimeMs)
                        .errorMessage(errorMsg != null && errorMsg.length() > 500
                                ? errorMsg.substring(0, 500) : errorMsg)
                        .build();

                // DB2 write — PROPAGATION_REQUIRES_NEW in AuditDao
                auditDao.insertAuditLog(auditLog);

                log.debug("[AuditInterceptor] Logged action {}/{} → result={} in {}ms",
                        namespace, actionName, resultCode, execTimeMs);
            } catch (Exception auditEx) {
                // Never let audit failure break the user-facing flow
                log.warn("[AuditInterceptor] Failed to write audit log: {}", auditEx.getMessage());
            }
        }
    }
}
