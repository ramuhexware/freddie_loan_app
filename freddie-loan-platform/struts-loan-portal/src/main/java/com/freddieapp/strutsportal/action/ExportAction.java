package com.freddieapp.strutsportal.action;

import com.freddieapp.strutsportal.dao.LoanDao;
import com.freddieapp.strutsportal.dao.CustomerDao;
import com.freddieapp.strutsportal.dao.AuditDao;
import com.freddieapp.strutsportal.model.Customer;
import com.freddieapp.strutsportal.model.LoanApplication;
import com.freddieapp.strutsportal.model.AuditLog;
import com.freddieapp.strutsportal.service.FileService;
import com.opensymphony.xwork2.ActionSupport;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.struts2.ServletActionContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Struts 2 Action for CSV / data export.
 *
 * Methods:
 *  - exportLoans()    — DB2 SELECT all loans → CSV OutputStream
 *  - exportCustomers()— DB2 SELECT all customers → CSV OutputStream
 *  - exportAuditLog() — DB2 SELECT audit log → CSV OutputStream
 */
@Slf4j
@Getter
@Setter
public class ExportAction extends ActionSupport {

    @Autowired private LoanDao     loanDao;
    @Autowired private CustomerDao customerDao;
    @Autowired private AuditDao    auditDao;
    @Autowired private FileService fileService;

    private static final DateTimeFormatter FILENAME_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    // ================================================================== //
    //  exportLoans() — DB2 SELECT → CSV file download                    //
    // ================================================================== //
    public String exportLoans() throws Exception {
        log.info("[ExportAction.exportLoans] Exporting all loans to CSV");

        // DB2 CALL — fetch all loans
        List<LoanApplication> loans = loanDao.findAllLoans();

        String fileName = "loans_" + LocalDateTime.now().format(FILENAME_FMT) + ".csv";
        HttpServletResponse response = ServletActionContext.getResponse();
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");

        // IO: Write CSV directly to HTTP response output stream
        try (OutputStream out = response.getOutputStream()) {
            fileService.exportLoansAsCsv(loans, out);
        }

        log.info("[ExportAction.exportLoans] Exported {} loan rows", loans.size());
        return null; // Response already committed
    }

    // ================================================================== //
    //  exportCustomers() — DB2 SELECT → CSV file download                 //
    // ================================================================== //
    public String exportCustomers() throws Exception {
        log.info("[ExportAction.exportCustomers] Exporting all customers to CSV");

        // DB2 CALL — fetch all customers
        List<Customer> customers = customerDao.findAllCustomers();

        String fileName = "customers_" + LocalDateTime.now().format(FILENAME_FMT) + ".csv";
        HttpServletResponse response = ServletActionContext.getResponse();
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");

        try (OutputStream out = response.getOutputStream()) {
            // Build a simple CSV for customers inline
            out.write('\uFEFF');
            StringBuilder sb = new StringBuilder();
            sb.append("CUSTOMER_ID,FIRST_NAME,LAST_NAME,EMAIL,PHONE,CREDIT_SCORE,STATUS,CITY,STATE,CREATED_AT\n");
            for (Customer c : customers) {
                sb.append(String.join(",",
                        q(c.getCustomerId()), q(c.getFirstName()), q(c.getLastName()),
                        q(c.getEmail()), q(c.getPhoneNumber()),
                        c.getCreditScore() != null ? c.getCreditScore().toString() : "",
                        q(c.getCustomerStatus()), q(c.getCity()), q(c.getState()),
                        c.getCreatedAt() != null ? c.getCreatedAt().toString() : ""
                )).append("\n");
            }
            out.write(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        log.info("[ExportAction.exportCustomers] Exported {} customer rows", customers.size());
        return null;
    }

    // ================================================================== //
    //  exportAuditLog() — DB2 SELECT → CSV file download                  //
    // ================================================================== //
    public String exportAuditLog() throws Exception {
        log.info("[ExportAction.exportAuditLog] Exporting audit log to CSV");

        // DB2 CALL — recent 1000 audit entries for export
        List<AuditLog> auditLogs = auditDao.findRecentAuditLogs(1000);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (AuditLog al : auditLogs) {
            rows.add(Map.of(
                    "AUDIT_ID",         al.getAuditId() != null ? al.getAuditId() : "",
                    "AUDIT_TIMESTAMP",  al.getTimestamp() != null ? al.getTimestamp().toString() : "",
                    "SESSION_USER_ID",  al.getSessionUser() != null ? al.getSessionUser() : "",
                    "ACTION_NAMESPACE", al.getActionNamespace() != null ? al.getActionNamespace() : "",
                    "ACTION_NAME",      al.getActionName() != null ? al.getActionName() : "",
                    "ACTION_METHOD",    al.getActionMethod() != null ? al.getActionMethod() : "",
                    "RESULT_CODE",      al.getResultCode() != null ? al.getResultCode() : "",
                    "EXEC_TIME_MS",     al.getExecutionTimeMs() != null ? al.getExecutionTimeMs().toString() : "",
                    "REMOTE_IP",        al.getRemoteIpAddress() != null ? al.getRemoteIpAddress() : ""
            ));
        }

        String fileName = "audit_log_" + LocalDateTime.now().format(FILENAME_FMT) + ".csv";
        HttpServletResponse response = ServletActionContext.getResponse();
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");

        try (OutputStream out = response.getOutputStream()) {
            fileService.exportAuditLogAsCsv(rows, out);
        }

        log.info("[ExportAction.exportAuditLog] Exported {} audit rows", rows.size());
        return null;
    }

    private String q(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"")) return "\"" + val.replace("\"", "\"\"") + "\"";
        return val;
    }
}
