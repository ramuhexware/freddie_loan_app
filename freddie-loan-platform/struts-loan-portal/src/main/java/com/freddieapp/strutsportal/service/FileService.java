package com.freddieapp.strutsportal.service;

import com.freddieapp.strutsportal.dao.DocumentDao;
import com.freddieapp.strutsportal.dao.LoanDao;
import com.freddieapp.strutsportal.model.LoanApplication;
import com.freddieapp.strutsportal.model.LoanDocument;
import com.freddieapp.strutsportal.model.ReportSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Central service for ALL file I/O operations in the Struts portal.
 *
 * Responsibilities:
 *  1.  saveUploadedFile()           — persist uploaded bytes to disk (NIO Path)
 *  2.  readFileAsBytes()            — read a stored document file into byte[]
 *  3.  deletePhysicalFile()         — delete file from disk after DB record removed
 *  4.  exportLoansAsCsv()           — write loan list to a CSV OutputStream
 *  5.  exportCustomersAsCsv()       — write customer list to CSV
 *  6.  exportAuditLogAsCsv()        — write audit log to CSV
 *  7.  importLoansFromCsv()         — parse uploaded CSV → List<LoanApplication>
 *  8.  generateLoanReportTxt()      — write a formatted text report to disk
 *  9.  generateBatchExportZip()     — ZIP all documents for a loan into one archive
 * 10.  listUploadDirectory()         — list all files in a directory
 * 11.  getFileSizeFormatted()        — utility: human-readable file size string
 * 12.  readClasspathTemplate()       — read a .txt template from classpath resources
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final DocumentDao documentDao;
    private final LoanDao     loanDao;

    /** Base directory for all uploaded documents — configurable */
    @Value("${portal.upload.dir:${java.io.tmpdir}/freddie-docs}")
    private String uploadBaseDir;

    /** Base directory for generated report files */
    @Value("${portal.reports.dir:${java.io.tmpdir}/freddie-reports}")
    private String reportsBaseDir;

    private static final DateTimeFormatter FILE_TS_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter DISPLAY_FMT  =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ================================================================== //
    //  IO #1 — Save uploaded file bytes to disk (NIO)                     //
    // ================================================================== //
    /**
     * Writes the uploaded byte array to the upload directory.
     * Creates directories if they don't exist.
     *
     * @return the absolute path where the file was saved
     */
    public String saveUploadedFile(byte[] fileBytes, String storedFileName,
                                   String loanId) throws IOException {
        // Build target directory: <uploadBaseDir>/<loanId>/
        Path targetDir = Paths.get(uploadBaseDir, loanId);
        Files.createDirectories(targetDir);

        Path targetPath = targetDir.resolve(storedFileName);
        Files.write(targetPath, fileBytes, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);

        log.info("[IO#1] Saved file {} ({} bytes) → {}", storedFileName,
                fileBytes.length, targetPath.toAbsolutePath());
        return targetPath.toAbsolutePath().toString();
    }

    // ================================================================== //
    //  IO #2 — Read stored document bytes from disk (NIO)                 //
    // ================================================================== //
    /**
     * Reads a document file into a byte array for HTTP download streaming.
     */
    public byte[] readFileAsBytes(String absoluteFilePath) throws IOException {
        Path filePath = Paths.get(absoluteFilePath);
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("Document file not found: " + absoluteFilePath);
        }
        byte[] bytes = Files.readAllBytes(filePath);
        log.info("[IO#2] Read {} bytes from {}", bytes.length, absoluteFilePath);
        return bytes;
    }

    // ================================================================== //
    //  IO #3 — Delete physical file from disk                             //
    // ================================================================== //
    /**
     * Removes the file from the filesystem. Logs a warning if it doesn't exist.
     */
    public void deletePhysicalFile(String absoluteFilePath) {
        try {
            Path filePath = Paths.get(absoluteFilePath);
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("[IO#3] Deleted file: {}", absoluteFilePath);
            } else {
                log.warn("[IO#3] File not found for deletion: {}", absoluteFilePath);
            }
        } catch (IOException e) {
            log.error("[IO#3] Error deleting file {}: {}", absoluteFilePath, e.getMessage(), e);
        }
    }

    // ================================================================== //
    //  IO #4 — Export loan list to CSV (write to OutputStream)            //
    // ================================================================== //
    /**
     * Streams the full loan list as RFC-4180 CSV to the provided OutputStream.
     * Used by ExportAction to push CSV directly to HTTP response.
     */
    public void exportLoansAsCsv(List<LoanApplication> loans,
                                  OutputStream outputStream) throws IOException {
        log.info("[IO#4] Exporting {} loans to CSV", loans.size());

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {

            // Write UTF-8 BOM for Excel compatibility
            writer.write('\uFEFF');

            // Header row
            writer.write("LOAN_ID,CUSTOMER_ID,CUSTOMER_NAME,LOAN_TYPE,LOAN_AMOUNT," +
                         "PROPERTY_VALUE,PROPERTY_ADDRESS,INTEREST_RATE," +
                         "LOAN_TERM_MONTHS,LOAN_STATUS,APPLICATION_DATE," +
                         "DECISION_DATE,APPROVED_AMOUNT,REJECTION_REASON,CREATED_BY");
            writer.newLine();

            // Data rows
            for (LoanApplication loan : loans) {
                writer.write(String.join(",",
                        csvEscape(loan.getLoanId()),
                        csvEscape(loan.getCustomerId()),
                        csvEscape(loan.getCustomerName()),
                        csvEscape(loan.getLoanType()),
                        nullSafe(loan.getLoanAmount()),
                        nullSafe(loan.getPropertyValue()),
                        csvEscape(loan.getPropertyAddress()),
                        nullSafe(loan.getInterestRate()),
                        loan.getLoanTermMonths() != null ? loan.getLoanTermMonths().toString() : "",
                        csvEscape(loan.getLoanStatus()),
                        formatDate(loan.getApplicationDate()),
                        formatDate(loan.getDecisionDate()),
                        nullSafe(loan.getApprovedAmount()),
                        csvEscape(loan.getRejectionReason()),
                        csvEscape(loan.getCreatedBy())
                ));
                writer.newLine();
            }
            writer.flush();
        }
        log.info("[IO#4] CSV export complete — {} rows written", loans.size());
    }

    // ================================================================== //
    //  IO #5 — Export loan documents list to CSV                          //
    // ================================================================== //
    /**
     * Writes document metadata table to CSV (not the file bytes — just metadata).
     */
    public void exportDocumentsAsCsv(List<LoanDocument> documents,
                                      OutputStream outputStream) throws IOException {
        log.info("[IO#5] Exporting {} document records to CSV", documents.size());

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {

            writer.write('\uFEFF');
            writer.write("DOCUMENT_ID,LOAN_ID,CUSTOMER_ID,DOCUMENT_TYPE," +
                         "ORIGINAL_FILE_NAME,FILE_SIZE_BYTES,MIME_TYPE," +
                         "UPLOADED_BY,STATUS,UPLOADED_AT");
            writer.newLine();

            for (LoanDocument doc : documents) {
                writer.write(String.join(",",
                        csvEscape(doc.getDocumentId()),
                        csvEscape(doc.getLoanId()),
                        csvEscape(doc.getCustomerId()),
                        csvEscape(doc.getDocumentType()),
                        csvEscape(doc.getOriginalFileName()),
                        doc.getFileSizeBytes() != null ? doc.getFileSizeBytes().toString() : "0",
                        csvEscape(doc.getMimeType()),
                        csvEscape(doc.getUploadedBy()),
                        csvEscape(doc.getStatus()),
                        formatDate(doc.getUploadedAt())
                ));
                writer.newLine();
            }
            writer.flush();
        }
    }

    // ================================================================== //
    //  IO #6 — Export audit log to CSV                                    //
    // ================================================================== //
    /**
     * Writes audit log rows to CSV for compliance reporting.
     * Accepts a generic list of Map rows from AuditDao.
     */
    public void exportAuditLogAsCsv(List<Map<String, Object>> auditRows,
                                     OutputStream outputStream) throws IOException {
        log.info("[IO#6] Exporting {} audit rows to CSV", auditRows.size());

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {

            writer.write('\uFEFF');
            writer.write("AUDIT_ID,TIMESTAMP,SESSION_USER,NAMESPACE,ACTION,METHOD,RESULT,EXEC_MS,IP");
            writer.newLine();

            for (Map<String, Object> row : auditRows) {
                writer.write(String.join(",",
                        csvEscape(String.valueOf(row.getOrDefault("AUDIT_ID", ""))),
                        csvEscape(String.valueOf(row.getOrDefault("AUDIT_TIMESTAMP", ""))),
                        csvEscape(String.valueOf(row.getOrDefault("SESSION_USER_ID", ""))),
                        csvEscape(String.valueOf(row.getOrDefault("ACTION_NAMESPACE", ""))),
                        csvEscape(String.valueOf(row.getOrDefault("ACTION_NAME", ""))),
                        csvEscape(String.valueOf(row.getOrDefault("ACTION_METHOD", ""))),
                        csvEscape(String.valueOf(row.getOrDefault("RESULT_CODE", ""))),
                        csvEscape(String.valueOf(row.getOrDefault("EXEC_TIME_MS", ""))),
                        csvEscape(String.valueOf(row.getOrDefault("REMOTE_IP", "")))
                ));
                writer.newLine();
            }
            writer.flush();
        }
    }

    // ================================================================== //
    //  IO #7 — Parse a CSV file of loan applications (batch import)       //
    // ================================================================== //
    /**
     * Reads an uploaded CSV file (from multipart upload) and parses it into
     * a list of LoanApplication objects for batch processing.
     *
     * Expected CSV format (with header):
     *   CUSTOMER_ID,LOAN_TYPE,LOAN_AMOUNT,PROPERTY_VALUE,PROPERTY_ADDRESS,LOAN_TERM_MONTHS
     *
     * @return ImportResult containing parsed loans and any row-level errors
     */
    public ImportResult importLoansFromCsv(InputStream csvInputStream) throws IOException {
        log.info("[IO#7] Parsing loan applications from CSV input stream");

        List<LoanApplication> parsed = new ArrayList<>();
        List<String> errors          = new ArrayList<>();
        int lineNumber               = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(csvInputStream, StandardCharsets.UTF_8))) {

            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();

                // Skip empty lines
                if (line.isEmpty()) continue;

                // Skip header
                if (isHeader) { isHeader = false; continue; }

                String[] cols = line.split(",", -1);
                if (cols.length < 4) {
                    errors.add("Line " + lineNumber + ": insufficient columns (need at least 4, got " + cols.length + ")");
                    continue;
                }

                try {
                    LoanApplication loan = LoanApplication.builder()
                            .loanId(UUID.randomUUID().toString())
                            .customerId(cols[0].trim().replace("\"", ""))
                            .loanType(cols[1].trim().replace("\"", "").toUpperCase())
                            .loanAmount(new BigDecimal(cols[2].trim().replace("\"", "")))
                            .propertyValue(cols.length > 3 && !cols[3].trim().isEmpty()
                                    ? new BigDecimal(cols[3].trim().replace("\"", "")) : null)
                            .propertyAddress(cols.length > 4 ? cols[4].trim().replace("\"", "") : "")
                            .loanTermMonths(cols.length > 5 && !cols[5].trim().isEmpty()
                                    ? Integer.parseInt(cols[5].trim()) : 360)
                            .loanStatus("PENDING")
                            .createdBy("CSV_IMPORT")
                            .applicationDate(LocalDateTime.now())
                            .build();

                    // Validate required fields
                    if (StringUtils.isBlank(loan.getCustomerId())) {
                        errors.add("Line " + lineNumber + ": CUSTOMER_ID is blank");
                        continue;
                    }
                    if (StringUtils.isBlank(loan.getLoanType())) {
                        errors.add("Line " + lineNumber + ": LOAN_TYPE is blank");
                        continue;
                    }
                    parsed.add(loan);

                } catch (NumberFormatException e) {
                    errors.add("Line " + lineNumber + ": invalid number format — " + e.getMessage());
                } catch (Exception e) {
                    errors.add("Line " + lineNumber + ": parse error — " + e.getMessage());
                }
            }
        }

        log.info("[IO#7] CSV import complete — {} parsed, {} errors", parsed.size(), errors.size());
        return new ImportResult(parsed, errors, lineNumber - 1);
    }

    // ================================================================== //
    //  IO #8 — Generate a formatted text loan report and save to disk     //
    // ================================================================== //
    /**
     * Creates a human-readable .txt summary report for a specific loan and
     * writes it to the reports directory on disk.
     *
     * @return the absolute path of the generated report file
     */
    public String generateLoanReportTxt(LoanApplication loan,
                                         List<LoanDocument> documents) throws IOException {
        Path reportsDir = Paths.get(reportsBaseDir);
        Files.createDirectories(reportsDir);

        String fileName = "loan_report_" + loan.getLoanId().substring(0, 8)
                + "_" + LocalDateTime.now().format(FILE_TS_FMT) + ".txt";
        Path reportPath = reportsDir.resolve(fileName);

        log.info("[IO#8] Generating text report for loanId={} → {}", loan.getLoanId(), reportPath);

        try (BufferedWriter writer = Files.newBufferedWriter(reportPath, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            writer.write("=".repeat(72)); writer.newLine();
            writer.write("  FREDDIE MAC LOAN ADMINISTRATION PORTAL"); writer.newLine();
            writer.write("  LOAN APPLICATION REPORT"); writer.newLine();
            writer.write("  Generated: " + LocalDateTime.now().format(DISPLAY_FMT)); writer.newLine();
            writer.write("=".repeat(72)); writer.newLine();
            writer.newLine();

            writer.write("LOAN IDENTIFICATION"); writer.newLine();
            writer.write("-".repeat(40)); writer.newLine();
            writer.write(String.format("  Loan ID          : %s%n", loan.getLoanId()));
            writer.write(String.format("  Customer ID      : %s%n", loan.getCustomerId()));
            writer.write(String.format("  Customer Name    : %s%n", loan.getCustomerName()));
            writer.newLine();

            writer.write("LOAN DETAILS"); writer.newLine();
            writer.write("-".repeat(40)); writer.newLine();
            writer.write(String.format("  Loan Type        : %s%n", loan.getLoanType()));
            writer.write(String.format("  Requested Amount : $%,.2f%n",
                    loan.getLoanAmount() != null ? loan.getLoanAmount() : BigDecimal.ZERO));
            writer.write(String.format("  Property Value   : $%,.2f%n",
                    loan.getPropertyValue() != null ? loan.getPropertyValue() : BigDecimal.ZERO));
            writer.write(String.format("  Property Address : %s%n", loan.getPropertyAddress()));
            writer.write(String.format("  Interest Rate    : %s%%%n",
                    loan.getInterestRate() != null ? loan.getInterestRate() : "TBD"));
            writer.write(String.format("  Term             : %d months%n",
                    loan.getLoanTermMonths() != null ? loan.getLoanTermMonths() : 0));
            writer.newLine();

            writer.write("CURRENT STATUS"); writer.newLine();
            writer.write("-".repeat(40)); writer.newLine();
            writer.write(String.format("  Status           : %s%n", loan.getLoanStatus()));
            writer.write(String.format("  Application Date : %s%n", formatDate(loan.getApplicationDate())));
            writer.write(String.format("  Decision Date    : %s%n", formatDate(loan.getDecisionDate())));
            if (loan.getApprovedAmount() != null) {
                writer.write(String.format("  Approved Amount  : $%,.2f%n", loan.getApprovedAmount()));
            }
            if (StringUtils.isNotBlank(loan.getRejectionReason())) {
                writer.write(String.format("  Rejection Reason : %s%n", loan.getRejectionReason()));
            }
            writer.newLine();

            writer.write("ATTACHED DOCUMENTS (" + documents.size() + ")"); writer.newLine();
            writer.write("-".repeat(40)); writer.newLine();
            if (documents.isEmpty()) {
                writer.write("  No documents attached."); writer.newLine();
            } else {
                for (LoanDocument doc : documents) {
                    writer.write(String.format("  [%s] %-30s  %s  (%s bytes)%n",
                            doc.getStatus(),
                            doc.getOriginalFileName(),
                            doc.getDocumentType(),
                            doc.getFileSizeBytes()));
                }
            }

            writer.newLine();
            writer.write("=".repeat(72)); writer.newLine();
            writer.write("  END OF REPORT — FREDDIE MAC LOAN PORTAL"); writer.newLine();
            writer.write("=".repeat(72)); writer.newLine();
            writer.flush();
        }

        log.info("[IO#8] Report written: {} ({} bytes)", reportPath,
                Files.size(reportPath));
        return reportPath.toAbsolutePath().toString();
    }

    // ================================================================== //
    //  IO #9 — ZIP all documents for a loan into one downloadable archive  //
    // ================================================================== //
    /**
     * Creates a ZIP archive containing all physical files attached to a loan.
     * Writes the ZIP to a temp file and returns its path.
     */
    public String generateBatchExportZip(String loanId,
                                          List<LoanDocument> documents) throws IOException {
        Path reportsDir = Paths.get(reportsBaseDir);
        Files.createDirectories(reportsDir);

        String zipName = "loan_docs_" + loanId.substring(0, 8)
                + "_" + LocalDateTime.now().format(FILE_TS_FMT) + ".zip";
        Path zipPath = reportsDir.resolve(zipName);

        log.info("[IO#9] Creating ZIP for loanId={} with {} documents → {}",
                loanId, documents.size(), zipPath);

        try (ZipOutputStream zos = new ZipOutputStream(
                new BufferedOutputStream(Files.newOutputStream(zipPath)))) {

            for (LoanDocument doc : documents) {
                Path docPath = Paths.get(doc.getFilePath());
                if (!Files.exists(docPath)) {
                    log.warn("[IO#9] Skipping missing file: {}", doc.getFilePath());
                    continue;
                }
                ZipEntry entry = new ZipEntry(doc.getDocumentType() + "/" + doc.getOriginalFileName());
                entry.setSize(Files.size(docPath));
                zos.putNextEntry(entry);
                Files.copy(docPath, zos);
                zos.closeEntry();
                log.debug("[IO#9] Zipped: {}", doc.getOriginalFileName());
            }
            zos.flush();
        }

        log.info("[IO#9] ZIP created: {} ({} bytes)", zipPath, Files.size(zipPath));
        return zipPath.toAbsolutePath().toString();
    }

    // ================================================================== //
    //  IO #10 — List files in the upload directory for a loan             //
    // ================================================================== //
    /**
     * Returns metadata about every physical file found in a loan's upload folder.
     */
    public List<Map<String, Object>> listUploadDirectory(String loanId) throws IOException {
        Path loanDir = Paths.get(uploadBaseDir, loanId);
        log.debug("[IO#10] Listing directory: {}", loanDir);

        if (!Files.exists(loanDir)) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        try (var stream = Files.list(loanDir)) {
            stream.filter(Files::isRegularFile).forEach(p -> {
                try {
                    BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class);
                    Map<String, Object> info = new LinkedHashMap<>();
                    info.put("fileName",      p.getFileName().toString());
                    info.put("absolutePath",  p.toAbsolutePath().toString());
                    info.put("sizeBytes",     attrs.size());
                    info.put("sizeFormatted", getFileSizeFormatted(attrs.size()));
                    info.put("lastModified",  attrs.lastModifiedTime().toString());
                    result.add(info);
                } catch (IOException ex) {
                    log.warn("[IO#10] Could not read attrs for {}: {}", p, ex.getMessage());
                }
            });
        }
        return result;
    }

    // ================================================================== //
    //  IO #11 — Read a classpath text template file                        //
    // ================================================================== //
    /**
     * Reads a template file from the classpath (e.g. email templates,
     * report header templates stored under src/main/resources/templates/).
     */
    public String readClasspathTemplate(String templateName) throws IOException {
        String resourcePath = "/templates/" + templateName;
        log.debug("[IO#11] Reading classpath template: {}", resourcePath);

        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new FileNotFoundException("Template not found on classpath: " + resourcePath);
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }
    }

    // ================================================================== //
    //  IO #12 — Write application-level operational log snapshot to disk  //
    // ================================================================== //
    /**
     * Appends a line to a daily operations log file.
     * Uses StandardOpenOption.APPEND to never truncate the existing file.
     */
    public void appendToOperationsLog(String message) {
        try {
            Path logDir  = Paths.get(reportsBaseDir, "ops-logs");
            Files.createDirectories(logDir);

            String today   = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            Path logFile   = logDir.resolve("portal-ops-" + today + ".log");
            String logLine = LocalDateTime.now().format(DISPLAY_FMT) + " | " + message + System.lineSeparator();

            Files.write(logFile, logLine.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            log.debug("[IO#12] Appended to ops log: {}", logFile);
        } catch (IOException e) {
            log.warn("[IO#12] Failed to write ops log: {}", e.getMessage());
        }
    }

    // ================================================================== //
    //  Utility — Human-readable file size                                  //
    // ================================================================== //
    public String getFileSizeFormatted(long bytes) {
        if (bytes < 1024)             return bytes + " B";
        if (bytes < 1024 * 1024)      return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    // ================================================================== //
    //  Private helpers                                                     //
    // ================================================================== //
    private String csvEscape(String value) {
        if (value == null) return "";
        // Wrap in quotes if contains comma, quote, or newline
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String nullSafe(BigDecimal value) {
        return value != null ? value.toPlainString() : "";
    }

    private String formatDate(LocalDateTime dt) {
        return dt != null ? dt.format(DISPLAY_FMT) : "";
    }

    // ================================================================== //
    //  Inner class: ImportResult                                            //
    // ================================================================== //
    public static class ImportResult {
        public final List<LoanApplication> parsedLoans;
        public final List<String>          errors;
        public final int                   totalRowsRead;

        public ImportResult(List<LoanApplication> parsedLoans,
                            List<String> errors, int totalRowsRead) {
            this.parsedLoans   = parsedLoans;
            this.errors        = errors;
            this.totalRowsRead = totalRowsRead;
        }

        public boolean hasErrors()  { return !errors.isEmpty(); }
        public int successCount()   { return parsedLoans.size(); }
    }
}
