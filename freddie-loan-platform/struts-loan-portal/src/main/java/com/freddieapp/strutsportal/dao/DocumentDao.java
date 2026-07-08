package com.freddieapp.strutsportal.dao;

import com.freddieapp.strutsportal.model.LoanDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Loan Documents.
 * Tracks file metadata (path, size, type) stored in DB2
 * while actual file bytes live on the filesystem.
 *
 * DB2 Calls:
 *  1. insertDocument()            — INSERT document metadata record
 *  2. findDocumentsByLoan()       — SELECT all docs for a loan
 *  3. findDocumentById()          — SELECT single doc by PK
 *  4. updateDocumentStatus()      — UPDATE review status
 *  5. deleteDocument()            — DELETE metadata record
 *  6. findDocumentsByType()       — SELECT filtered by document type
 *  7. countDocumentsByLoan()      — COUNT docs per loan
 *  8. findPendingDocuments()      — SELECT WHERE status=PENDING_REVIEW
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class DocumentDao {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<LoanDocument> DOC_ROW_MAPPER = (rs, rowNum) -> {
        LoanDocument d = new LoanDocument();
        d.setDocumentId(rs.getString("DOCUMENT_ID"));
        d.setLoanId(rs.getString("LOAN_ID"));
        d.setCustomerId(rs.getString("CUSTOMER_ID"));
        d.setDocumentType(rs.getString("DOCUMENT_TYPE"));
        d.setOriginalFileName(rs.getString("ORIGINAL_FILE_NAME"));
        d.setStoredFileName(rs.getString("STORED_FILE_NAME"));
        d.setFilePath(rs.getString("FILE_PATH"));
        d.setFileSizeBytes(rs.getLong("FILE_SIZE_BYTES"));
        d.setMimeType(rs.getString("MIME_TYPE"));
        d.setUploadedBy(rs.getString("UPLOADED_BY"));
        d.setStatus(rs.getString("STATUS"));
        d.setReviewNotes(rs.getString("REVIEW_NOTES"));
        Timestamp uploadedAt = rs.getTimestamp("UPLOADED_AT");
        if (uploadedAt != null) d.setUploadedAt(uploadedAt.toLocalDateTime());
        Timestamp reviewedAt = rs.getTimestamp("REVIEWED_AT");
        if (reviewedAt != null) d.setReviewedAt(reviewedAt.toLocalDateTime());
        return d;
    };

    // ================================================================== //
    //  DB2 CALL #1 — INSERT document metadata                             //
    // ================================================================== //
    @Transactional
    public int insertDocument(LoanDocument doc) {
        log.info("[DB2-DOC-1] INSERT document record DOCUMENT_ID={} LOAN_ID={} FILE={}",
                doc.getDocumentId(), doc.getLoanId(), doc.getOriginalFileName());
        String sql = """
                INSERT INTO FREDDIE_LOANS.LOAN_DOCUMENTS
                    (DOCUMENT_ID, LOAN_ID, CUSTOMER_ID, DOCUMENT_TYPE,
                     ORIGINAL_FILE_NAME, STORED_FILE_NAME, FILE_PATH,
                     FILE_SIZE_BYTES, MIME_TYPE, UPLOADED_BY, STATUS, UPLOADED_AT)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING_REVIEW', CURRENT_TIMESTAMP)
                """;
        return jdbcTemplate.update(sql,
                doc.getDocumentId(),
                doc.getLoanId(),
                doc.getCustomerId(),
                doc.getDocumentType(),
                doc.getOriginalFileName(),
                doc.getStoredFileName(),
                doc.getFilePath(),
                doc.getFileSizeBytes(),
                doc.getMimeType(),
                doc.getUploadedBy()
        );
    }

    // ================================================================== //
    //  DB2 CALL #2 — Find all documents for a loan                        //
    // ================================================================== //
    public List<LoanDocument> findDocumentsByLoan(String loanId) {
        log.debug("[DB2-DOC-2] SELECT documents for LOAN_ID={}", loanId);
        String sql = """
                SELECT DOCUMENT_ID, LOAN_ID, CUSTOMER_ID, DOCUMENT_TYPE,
                       ORIGINAL_FILE_NAME, STORED_FILE_NAME, FILE_PATH,
                       FILE_SIZE_BYTES, MIME_TYPE, UPLOADED_BY, STATUS,
                       REVIEW_NOTES, UPLOADED_AT, REVIEWED_AT
                FROM   FREDDIE_LOANS.LOAN_DOCUMENTS
                WHERE  LOAN_ID = ?
                ORDER BY UPLOADED_AT DESC
                """;
        return jdbcTemplate.query(sql, DOC_ROW_MAPPER, loanId);
    }

    // ================================================================== //
    //  DB2 CALL #3 — Find document by primary key                         //
    // ================================================================== //
    public Optional<LoanDocument> findDocumentById(String documentId) {
        log.debug("[DB2-DOC-3] SELECT document DOCUMENT_ID={}", documentId);
        String sql = """
                SELECT DOCUMENT_ID, LOAN_ID, CUSTOMER_ID, DOCUMENT_TYPE,
                       ORIGINAL_FILE_NAME, STORED_FILE_NAME, FILE_PATH,
                       FILE_SIZE_BYTES, MIME_TYPE, UPLOADED_BY, STATUS,
                       REVIEW_NOTES, UPLOADED_AT, REVIEWED_AT
                FROM   FREDDIE_LOANS.LOAN_DOCUMENTS
                WHERE  DOCUMENT_ID = ?
                """;
        List<LoanDocument> results = jdbcTemplate.query(sql, DOC_ROW_MAPPER, documentId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    // ================================================================== //
    //  DB2 CALL #4 — UPDATE document review status                        //
    // ================================================================== //
    @Transactional
    public int updateDocumentStatus(String documentId, String status, String reviewNotes) {
        log.info("[DB2-DOC-4] UPDATE document status DOCUMENT_ID={} STATUS={}", documentId, status);
        String sql = """
                UPDATE FREDDIE_LOANS.LOAN_DOCUMENTS
                SET    STATUS       = ?,
                       REVIEW_NOTES = ?,
                       REVIEWED_AT  = CURRENT_TIMESTAMP
                WHERE  DOCUMENT_ID  = ?
                """;
        return jdbcTemplate.update(sql, status, reviewNotes, documentId);
    }

    // ================================================================== //
    //  DB2 CALL #5 — DELETE document metadata record                      //
    // ================================================================== //
    @Transactional
    public int deleteDocument(String documentId) {
        log.info("[DB2-DOC-5] DELETE document record DOCUMENT_ID={}", documentId);
        String sql = "DELETE FROM FREDDIE_LOANS.LOAN_DOCUMENTS WHERE DOCUMENT_ID = ?";
        return jdbcTemplate.update(sql, documentId);
    }

    // ================================================================== //
    //  DB2 CALL #6 — Find documents filtered by type                      //
    // ================================================================== //
    public List<LoanDocument> findDocumentsByType(String documentType) {
        log.debug("[DB2-DOC-6] SELECT documents by DOCUMENT_TYPE={}", documentType);
        String sql = """
                SELECT DOCUMENT_ID, LOAN_ID, CUSTOMER_ID, DOCUMENT_TYPE,
                       ORIGINAL_FILE_NAME, STORED_FILE_NAME, FILE_PATH,
                       FILE_SIZE_BYTES, MIME_TYPE, UPLOADED_BY, STATUS,
                       REVIEW_NOTES, UPLOADED_AT, REVIEWED_AT
                FROM   FREDDIE_LOANS.LOAN_DOCUMENTS
                WHERE  DOCUMENT_TYPE = ?
                ORDER BY UPLOADED_AT DESC
                """;
        return jdbcTemplate.query(sql, DOC_ROW_MAPPER, documentType);
    }

    // ================================================================== //
    //  DB2 CALL #7 — COUNT documents per loan                             //
    // ================================================================== //
    public int countDocumentsByLoan(String loanId) {
        log.debug("[DB2-DOC-7] COUNT documents for LOAN_ID={}", loanId);
        String sql = "SELECT COUNT(*) FROM FREDDIE_LOANS.LOAN_DOCUMENTS WHERE LOAN_ID = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, loanId);
        return count != null ? count : 0;
    }

    // ================================================================== //
    //  DB2 CALL #8 — Find all documents awaiting review                   //
    // ================================================================== //
    public List<LoanDocument> findPendingDocuments() {
        log.debug("[DB2-DOC-8] SELECT documents WHERE status=PENDING_REVIEW");
        String sql = """
                SELECT ld.DOCUMENT_ID, ld.LOAN_ID, ld.CUSTOMER_ID, ld.DOCUMENT_TYPE,
                       ld.ORIGINAL_FILE_NAME, ld.STORED_FILE_NAME, ld.FILE_PATH,
                       ld.FILE_SIZE_BYTES, ld.MIME_TYPE, ld.UPLOADED_BY, ld.STATUS,
                       ld.REVIEW_NOTES, ld.UPLOADED_AT, ld.REVIEWED_AT
                FROM   FREDDIE_LOANS.LOAN_DOCUMENTS ld
                WHERE  ld.STATUS = 'PENDING_REVIEW'
                ORDER BY ld.UPLOADED_AT ASC
                """;
        return jdbcTemplate.query(sql, DOC_ROW_MAPPER);
    }
}
