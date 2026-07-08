package com.freddieapp.strutsportal.action;

import com.freddieapp.strutsportal.dao.LoanDao;
import com.freddieapp.strutsportal.model.LoanApplication;
import com.freddieapp.strutsportal.service.FileService;
import com.freddieapp.strutsportal.service.FileService.ImportResult;
import com.opensymphony.xwork2.ActionSupport;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Struts 2 Action for CSV Batch Import of Loan Applications.
 *
 * Methods:
 *  - importForm()   — Show the upload form
 *  - importCsv()    — Accept multipart CSV → FileInputStream → parse → DB2 INSERT
 *
 * The CSV is read from the uploaded temp File using FileInputStream
 * (java.io.File + java.io.FileInputStream), then passed to FileService
 * which uses BufferedReader to parse each row.
 */
@Slf4j
@Getter
@Setter
public class ImportAction extends ActionSupport {

    @Autowired private FileService fileService;
    @Autowired private LoanDao     loanDao;

    // ---- Struts 2 multipart bindings ----
    private File   upload;               // temp File created by Struts
    private String uploadContentType;
    private String uploadFileName;

    // ---- View data ----
    private ImportResult         importResult;
    private List<LoanApplication> importedLoans  = new ArrayList<>();
    private List<String>          importErrors   = new ArrayList<>();
    private int                   totalRowsRead  = 0;
    private int                   insertedCount  = 0;

    // ================================================================== //
    //  importForm() — Show the CSV upload form                            //
    // ================================================================== //
    public String importForm() {
        return SUCCESS;
    }

    // ================================================================== //
    //  importCsv() — Parse uploaded CSV + INSERT rows to DB2              //
    //                Uses java.io.File + FileInputStream                  //
    // ================================================================== //
    public String importCsv() {
        log.info("[ImportAction.importCsv] fileName={} contentType={}",
                uploadFileName, uploadContentType);

        // --- Validation ---
        if (upload == null) {
            addActionError("Please select a CSV file to upload.");
            return INPUT;
        }

        // Use File class to validate the uploaded temp file
        if (!upload.exists()) {                           // File.exists()
            addActionError("Uploaded file is not accessible.");
            return INPUT;
        }
        if (!upload.isFile()) {                           // File.isFile()
            addActionError("Uploaded path is not a file.");
            return INPUT;
        }
        if (!upload.canRead()) {                          // File.canRead()
            addActionError("Cannot read the uploaded file.");
            return INPUT;
        }

        long fileSize = upload.length();                  // File.length()
        log.info("[ImportAction.importCsv] Uploaded temp file: {} ({} bytes) at {}",
                upload.getName(), fileSize, upload.getAbsolutePath()); // File.getName(), getAbsolutePath()

        if (fileSize == 0) {
            addActionError("The uploaded CSV file is empty.");
            return INPUT;
        }

        // Only accept CSV content type
        if (uploadContentType != null
                && !uploadContentType.contains("csv")
                && !uploadContentType.contains("text/plain")) {
            addActionError("Only .csv files are accepted. Detected: " + uploadContentType);
            return INPUT;
        }

        // ---- Parse the CSV using java.io.FileInputStream ----
        try (InputStream csvStream = new FileInputStream(upload)) {    // FileInputStream(File)

            importResult = fileService.importLoansFromCsv(csvStream);

            importedLoans = importResult.parsedLoans;
            importErrors  = importResult.errors;
            totalRowsRead = importResult.totalRowsRead;

            log.info("[ImportAction.importCsv] Parsed {} loans, {} errors from {} rows",
                    importedLoans.size(), importErrors.size(), totalRowsRead);

            // ---- Batch INSERT valid rows into DB2 ----
            for (LoanApplication loan : importedLoans) {
                try {
                    loanDao.insertLoan(loan);   // DB2 CALL
                    insertedCount++;
                } catch (Exception e) {
                    importErrors.add("DB INSERT failed for customerId="
                            + loan.getCustomerId() + ": " + e.getMessage());
                    log.warn("[ImportAction.importCsv] Insert failed: {}", e.getMessage());
                }
            }

            if (insertedCount > 0) {
                addActionMessage(insertedCount + " loan application(s) imported successfully from '"
                        + uploadFileName + "'.");
            }
            if (!importErrors.isEmpty()) {
                addActionMessage(importErrors.size() + " row(s) had errors and were skipped. See details below.");
            }

        } catch (Exception e) {
            log.error("[ImportAction.importCsv] Fatal error: {}", e.getMessage(), e);
            addActionError("Import failed: " + e.getMessage());
            return ERROR;
        }

        return SUCCESS;
    }
}
