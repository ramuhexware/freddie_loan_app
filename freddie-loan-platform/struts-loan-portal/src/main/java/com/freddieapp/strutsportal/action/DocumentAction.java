package com.freddieapp.strutsportal.action;

import com.freddieapp.strutsportal.dao.DocumentDao;
import com.freddieapp.strutsportal.dao.LoanDao;
import com.freddieapp.strutsportal.model.LoanApplication;
import com.freddieapp.strutsportal.model.LoanDocument;
import com.freddieapp.strutsportal.service.FileService;
import com.opensymphony.xwork2.ActionSupport;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Struts 2 Action for Document Upload and Download.
 *
 * Methods:
 *  - uploadForm()     — Show the upload form
 *  - upload()         — Multipart file upload → write bytes to disk → INSERT to DB2
 *  - download()       — Read file bytes from disk → stream to HTTP response
 *  - listByLoan()     — List all documents for a loan (from DB2)
 *  - pendingReview()  — List documents awaiting review (from DB2)
 *  - approve()        — DB2 UPDATE document status to APPROVED
 *  - reject()         — DB2 UPDATE document status to REJECTED
 *  - delete()         — Delete DB2 record + physical file from disk
 *  - downloadZip()    — Generate ZIP of all loan documents → stream to HTTP
 *  - generateReport() — Write text report to disk → stream to browser
 */
@Slf4j
@Getter
@Setter
public class DocumentAction extends ActionSupport {

    @Autowired private DocumentDao documentDao;
    @Autowired private LoanDao     loanDao;
    @Autowired private FileService fileService;

    // ---- Struts 2 file upload binding ----
    private File   upload;           // Struts binds the actual File object here
    private String uploadContentType;
    private String uploadFileName;

    // ---- Form fields ----
    private String loanId;
    private String documentId;
    private String documentType;
    private String reviewNotes;

    // ---- View data ----
    private List<LoanDocument>       documents    = new ArrayList<>();
    private LoanDocument             document;
    private LoanApplication          loan;
    private List<Map<String, Object>> directoryListing = new ArrayList<>();

    // ================================================================== //
    //  uploadForm() — Show the upload form                                 //
    // ================================================================== //
    public String uploadForm() {
        log.info("[DocumentAction.uploadForm] loanId={}", loanId);
        loan      = loanDao.findLoanById(loanId).orElse(null);
        documents = documentDao.findDocumentsByLoan(loanId);          // DB2 CALL
        return SUCCESS;
    }

    // ================================================================== //
    //  upload() — Multipart upload, write to disk, INSERT to DB2          //
    // ================================================================== //
    public String upload() {
        log.info("[DocumentAction.upload] loanId={} fileName={} contentType={}",
                loanId, uploadFileName, uploadContentType);

        // --- Server-side validations ---
        if (upload == null) {
            addActionError("No file selected for upload.");
            return INPUT;
        }
        if (StringUtils.isBlank(loanId)) {
            addActionError("Loan ID is required.");
            return INPUT;
        }
        if (StringUtils.isBlank(documentType)) {
            addFieldError("documentType", "Document type is required.");
            return INPUT;
        }

        // --- Allowed types guard ---
        if (uploadContentType != null && !isAllowedMimeType(uploadContentType)) {
            addActionError("File type not allowed. Permitted: PDF, PNG, JPG, TIFF, DOCX.");
            return INPUT;
        }

        // --- File size guard (max 25 MB) ---
        long fileSizeBytes = upload.length();
        if (fileSizeBytes > 25L * 1024 * 1024) {
            addActionError("File too large. Maximum allowed size is 25 MB.");
            return INPUT;
        }

        try {
            // IO #1: Read uploaded bytes from the Struts temp file
            byte[] fileBytes = Files.readAllBytes(upload.toPath());

            // Build unique stored name (UUID + extension)
            String ext = getFileExtension(uploadFileName);
            String storedName = UUID.randomUUID().toString() + (ext.isEmpty() ? "" : "." + ext);

            // IO #1: Save to disk under <uploadBaseDir>/<loanId>/
            String session = (String) ServletActionContext.getRequest()
                    .getSession().getAttribute("loggedInUser");
            String absolutePath = fileService.saveUploadedFile(fileBytes, storedName, loanId);

            // DB2 CALL: INSERT document metadata record
            LoanDocument doc = LoanDocument.builder()
                    .documentId(UUID.randomUUID().toString())
                    .loanId(loanId)
                    .customerId(resolveCustomerId(loanId))
                    .documentType(documentType)
                    .originalFileName(uploadFileName)
                    .storedFileName(storedName)
                    .filePath(absolutePath)
                    .fileSizeBytes(fileSizeBytes)
                    .mimeType(uploadContentType)
                    .uploadedBy(session != null ? session : "UNKNOWN")
                    .uploadedAt(LocalDateTime.now())
                    .build();

            documentDao.insertDocument(doc);   // DB2 CALL

            // IO #12: Append to ops log
            fileService.appendToOperationsLog(
                    "UPLOAD | loanId=" + loanId + " | file=" + uploadFileName
                    + " | size=" + fileService.getFileSizeFormatted(fileSizeBytes)
                    + " | user=" + (session != null ? session : "UNKNOWN"));

            addActionMessage("Document '" + uploadFileName + "' uploaded successfully ("
                    + fileService.getFileSizeFormatted(fileSizeBytes) + ").");
            return SUCCESS;

        } catch (Exception e) {
            log.error("[DocumentAction.upload] Upload failed: {}", e.getMessage(), e);
            addActionError("Upload failed: " + e.getMessage());
            return ERROR;
        }
    }

    // ================================================================== //
    //  download() — Read bytes from disk, stream to HTTP response         //
    // ================================================================== //
    public String download() {
        log.info("[DocumentAction.download] documentId={}", documentId);

        // DB2 CALL: fetch document metadata
        document = documentDao.findDocumentById(documentId).orElse(null);
        if (document == null) {
            addActionError("Document not found: " + documentId);
            return "notFound";
        }

        try {
            // IO #2: read file bytes
            byte[] fileBytes = fileService.readFileAsBytes(document.getFilePath());

            // Stream directly to HTTP response
            HttpServletResponse response = ServletActionContext.getResponse();
            response.setContentType(document.getMimeType() != null ? document.getMimeType() : "application/octet-stream");
            response.setContentLengthLong(fileBytes.length);
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + document.getOriginalFileName() + "\"");

            try (OutputStream out = response.getOutputStream()) {
                out.write(fileBytes);
                out.flush();
            }

            log.info("[DocumentAction.download] Streamed {} bytes for file '{}'",
                    fileBytes.length, document.getOriginalFileName());
            return null; // null tells Struts: response is already committed

        } catch (Exception e) {
            log.error("[DocumentAction.download] Error: {}", e.getMessage(), e);
            addActionError("Could not download file: " + e.getMessage());
            return ERROR;
        }
    }

    // ================================================================== //
    //  listByLoan() — List documents for a loan                           //
    // ================================================================== //
    public String listByLoan() {
        log.info("[DocumentAction.listByLoan] loanId={}", loanId);
        loan      = loanDao.findLoanById(loanId).orElse(null);           // DB2 CALL
        documents = documentDao.findDocumentsByLoan(loanId);              // DB2 CALL

        // IO #10: list actual files on disk to reconcile with DB
        try {
            directoryListing = fileService.listUploadDirectory(loanId);
        } catch (Exception e) {
            log.warn("[DocumentAction.listByLoan] Could not list directory: {}", e.getMessage());
        }
        return SUCCESS;
    }

    // ================================================================== //
    //  pendingReview() — List documents awaiting review                   //
    // ================================================================== //
    public String pendingReview() {
        log.info("[DocumentAction.pendingReview] Loading pending documents");
        documents = documentDao.findPendingDocuments();                   // DB2 CALL
        return SUCCESS;
    }

    // ================================================================== //
    //  approve() — Update DB2 status to APPROVED                          //
    // ================================================================== //
    public String approve() {
        log.info("[DocumentAction.approve] documentId={}", documentId);
        int rows = documentDao.updateDocumentStatus(documentId, "APPROVED", reviewNotes); // DB2 CALL
        if (rows == 0) { addActionError("Document not found: " + documentId); return ERROR; }
        addActionMessage("Document approved successfully.");
        return SUCCESS;
    }

    // ================================================================== //
    //  reject() — Update DB2 status to REJECTED                           //
    // ================================================================== //
    public String reject() {
        log.info("[DocumentAction.reject] documentId={}", documentId);
        if (StringUtils.isBlank(reviewNotes)) {
            addFieldError("reviewNotes", "Please provide a rejection reason.");
            return INPUT;
        }
        int rows = documentDao.updateDocumentStatus(documentId, "REJECTED", reviewNotes); // DB2 CALL
        if (rows == 0) { addActionError("Document not found: " + documentId); return ERROR; }
        addActionMessage("Document rejected.");
        return SUCCESS;
    }

    // ================================================================== //
    //  delete() — DELETE from DB2 + physical file                         //
    // ================================================================== //
    public String delete() {
        log.info("[DocumentAction.delete] documentId={}", documentId);

        // DB2 CALL: fetch before delete to get filePath
        document = documentDao.findDocumentById(documentId).orElse(null);
        if (document == null) {
            addActionError("Document not found: " + documentId);
            return ERROR;
        }

        // DB2 CALL: DELETE metadata
        documentDao.deleteDocument(documentId);

        // IO #3: Delete physical file from disk
        fileService.deletePhysicalFile(document.getFilePath());

        addActionMessage("Document '" + document.getOriginalFileName() + "' deleted.");
        return SUCCESS;
    }

    // ================================================================== //
    //  downloadZip() — ZIP all docs for a loan → stream to browser       //
    // ================================================================== //
    public String downloadZip() {
        log.info("[DocumentAction.downloadZip] loanId={}", loanId);

        // DB2 CALL: all documents for loan
        documents = documentDao.findDocumentsByLoan(loanId);
        if (documents.isEmpty()) {
            addActionError("No documents found for loan: " + loanId);
            return ERROR;
        }

        try {
            // IO #9: create ZIP
            String zipPath = fileService.generateBatchExportZip(loanId, documents);

            // Stream ZIP to browser
            byte[] zipBytes = fileService.readFileAsBytes(zipPath);             // IO #2
            HttpServletResponse response = ServletActionContext.getResponse();
            response.setContentType("application/zip");
            response.setContentLengthLong(zipBytes.length);
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"loan_docs_" + loanId.substring(0, 8) + ".zip\"");

            try (OutputStream out = response.getOutputStream()) {
                out.write(zipBytes);
                out.flush();
            }

            // IO #3: Clean up the temp ZIP file after streaming
            fileService.deletePhysicalFile(zipPath);
            return null;

        } catch (Exception e) {
            log.error("[DocumentAction.downloadZip] Error: {}", e.getMessage(), e);
            addActionError("Could not generate ZIP: " + e.getMessage());
            return ERROR;
        }
    }

    // ================================================================== //
    //  generateReport() — Write text report to disk + download           //
    // ================================================================== //
    public String generateReport() {
        log.info("[DocumentAction.generateReport] loanId={}", loanId);

        // DB2 CALL: loan details
        loan = loanDao.findLoanById(loanId).orElse(null);
        if (loan == null) {
            addActionError("Loan not found: " + loanId);
            return ERROR;
        }

        // DB2 CALL: attached documents
        documents = documentDao.findDocumentsByLoan(loanId);

        try {
            // IO #8: write formatted text report to disk
            String reportPath = fileService.generateLoanReportTxt(loan, documents);

            // IO #2: read back and stream to browser
            byte[] reportBytes = fileService.readFileAsBytes(reportPath);
            HttpServletResponse response = ServletActionContext.getResponse();
            response.setContentType("text/plain; charset=UTF-8");
            response.setContentLengthLong(reportBytes.length);
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"loan_report_" + loanId.substring(0, 8) + ".txt\"");

            try (OutputStream out = response.getOutputStream()) {
                out.write(reportBytes);
                out.flush();
            }

            // IO #3: clean up temp report file
            fileService.deletePhysicalFile(reportPath);
            return null;

        } catch (Exception e) {
            log.error("[DocumentAction.generateReport] Error: {}", e.getMessage(), e);
            addActionError("Could not generate report: " + e.getMessage());
            return ERROR;
        }
    }

    // ================================================================== //
    //  Private helpers                                                     //
    // ================================================================== //
    private boolean isAllowedMimeType(String mimeType) {
        return mimeType.startsWith("application/pdf")
            || mimeType.startsWith("image/jpeg")
            || mimeType.startsWith("image/png")
            || mimeType.startsWith("image/tiff")
            || mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
            || mimeType.equals("application/msword");
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) return "";
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot + 1).toLowerCase() : "";
    }

    private String resolveCustomerId(String loanId) {
        return loanDao.findLoanById(loanId)
                .map(LoanApplication::getCustomerId)
                .orElse("UNKNOWN");
    }
}
