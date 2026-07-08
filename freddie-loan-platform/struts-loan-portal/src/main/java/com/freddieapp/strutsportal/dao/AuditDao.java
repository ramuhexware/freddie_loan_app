package com.freddieapp.strutsportal.dao;

import com.freddieapp.strutsportal.model.AuditLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

/**
 * Data Access Object for Audit Logs.
 * Called by AuditInterceptor on EVERY Struts action.
 * Uses PROPAGATION_REQUIRES_NEW so audit always commits even if
 * the main action transaction rolls back.
 *
 * DB2 Calls:
 *  1. insertAuditLog()         — INSERT every action invocation
 *  2. findRecentAuditLogs()    — SELECT most recent audit entries
 *  3. findAuditLogsByAction()  — SELECT filtered by action name
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class AuditDao {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<AuditLog> AUDIT_ROW_MAPPER = (rs, rowNum) -> {
        AuditLog a = new AuditLog();
        a.setAuditId(rs.getLong("AUDIT_ID"));
        a.setActionNamespace(rs.getString("ACTION_NAMESPACE"));
        a.setActionName(rs.getString("ACTION_NAME"));
        a.setActionMethod(rs.getString("ACTION_METHOD"));
        a.setSessionUser(rs.getString("SESSION_USER_ID"));
        a.setRemoteIpAddress(rs.getString("REMOTE_IP"));
        a.setRequestParameters(rs.getString("REQUEST_PARAMS"));
        a.setResultCode(rs.getString("RESULT_CODE"));
        a.setExecutionTimeMs(rs.getLong("EXEC_TIME_MS"));
        a.setErrorMessage(rs.getString("ERROR_MESSAGE"));
        Timestamp ts = rs.getTimestamp("AUDIT_TIMESTAMP");
        if (ts != null) a.setTimestamp(ts.toLocalDateTime());
        return a;
    };

    // ================================================================== //
    //  DB2 CALL #1 — INSERT an audit log entry (new transaction)         //
    // ================================================================== //
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insertAuditLog(AuditLog auditLog) {
        log.debug("[DB2-CALL-1-AUDIT] INSERT audit log for action={}/{} result={}",
                auditLog.getActionNamespace(), auditLog.getActionName(), auditLog.getResultCode());
        String sql = """
                INSERT INTO FREDDIE_LOANS.AUDIT_LOG
                    (ACTION_NAMESPACE, ACTION_NAME, ACTION_METHOD, SESSION_USER_ID,
                     REMOTE_IP, REQUEST_PARAMS, RESULT_CODE, EXEC_TIME_MS,
                     ERROR_MESSAGE, AUDIT_TIMESTAMP)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """;
        jdbcTemplate.update(sql,
                auditLog.getActionNamespace(),
                auditLog.getActionName(),
                auditLog.getActionMethod(),
                auditLog.getSessionUser(),
                auditLog.getRemoteIpAddress(),
                auditLog.getRequestParameters(),
                auditLog.getResultCode(),
                auditLog.getExecutionTimeMs(),
                auditLog.getErrorMessage()
        );
    }

    // ================================================================== //
    //  DB2 CALL #2 — Fetch most recent N audit log rows                  //
    // ================================================================== //
    public List<AuditLog> findRecentAuditLogs(int limit) {
        log.debug("[DB2-CALL-2-AUDIT] SELECT recent {} audit log entries", limit);
        String sql = """
                SELECT AUDIT_ID, ACTION_NAMESPACE, ACTION_NAME, ACTION_METHOD,
                       SESSION_USER_ID, REMOTE_IP, REQUEST_PARAMS, RESULT_CODE,
                       EXEC_TIME_MS, ERROR_MESSAGE, AUDIT_TIMESTAMP
                FROM   FREDDIE_LOANS.AUDIT_LOG
                ORDER BY AUDIT_TIMESTAMP DESC
                FETCH FIRST ? ROWS ONLY
                """;
        return jdbcTemplate.query(sql, AUDIT_ROW_MAPPER, limit);
    }

    // ================================================================== //
    //  DB2 CALL #3 — Fetch audit logs filtered by action name            //
    // ================================================================== //
    public List<AuditLog> findAuditLogsByAction(String actionName) {
        log.debug("[DB2-CALL-3-AUDIT] SELECT audit logs for action={}", actionName);
        String sql = """
                SELECT AUDIT_ID, ACTION_NAMESPACE, ACTION_NAME, ACTION_METHOD,
                       SESSION_USER_ID, REMOTE_IP, REQUEST_PARAMS, RESULT_CODE,
                       EXEC_TIME_MS, ERROR_MESSAGE, AUDIT_TIMESTAMP
                FROM   FREDDIE_LOANS.AUDIT_LOG
                WHERE  ACTION_NAME = ?
                ORDER BY AUDIT_TIMESTAMP DESC
                FETCH FIRST 200 ROWS ONLY
                """;
        return jdbcTemplate.query(sql, AUDIT_ROW_MAPPER, actionName);
    }
}
