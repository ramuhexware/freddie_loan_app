package com.freddieapp.documentservice.repository;

import com.freddieapp.documentservice.entity.LoanDocument;
import com.freddieapp.documentservice.entity.LoanDocument.DocumentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Repository
public interface LoanDocumentRepository extends ReactiveMongoRepository<LoanDocument, String> {

    // ─── MongoDB Native @Query (JSON filter syntax) ───────────────────────────

    /**
     * Finds all documents associated with a specific loan ID.
     * MongoDB native query — field names match the @Field mappings on the entity.
     */
    @Query("{ 'loanId': ?0 }")
    Flux<LoanDocument> findByLoanId(String loanId);

    /**
     * Finds documents by loanId AND documentType (e.g., W2, PAY_STUB).
     * Used when the loan processor needs a specific document type.
     */
    @Query("{ 'loanId': ?0, 'documentType': ?1 }")
    Flux<LoanDocument> findByLoanIdAndDocumentType(String loanId, String documentType);

    /**
     * Retrieves a document by its unique documentId (business key, not MongoDB _id).
     */
    @Query("{ 'documentId': ?0 }")
    Mono<LoanDocument> findByDocumentId(String documentId);

    /**
     * Paginated fetch of all documents uploaded by a specific customer,
     * filtered by status (UPLOADED, VERIFIED, REJECTED, etc.).
     */
    @Query("{ 'customerId': ?0, 'status': ?1 }")
    Flux<LoanDocument> findByCustomerIdAndStatus(
            String customerId,
            DocumentStatus status,
            Pageable pageable);

    /**
     * Finds documents whose expiry date has passed and are still VERIFIED.
     * Used by a scheduled document expiry sweep job.
     * MongoDB $lt operator for date comparison.
     */
    @Query("{ 'expiryDate': { $lt: ?0 }, 'status': 'VERIFIED' }")
    Flux<LoanDocument> findExpiredDocuments(java.time.LocalDate cutoffDate);

    /**
     * Full-text style search using MongoDB $regex on fileName and documentType.
     * Case-insensitive match via $options: 'i'.
     */
    @Query("{ $or: [ { 'fileName': { $regex: ?0, $options: 'i' } }, " +
                    "{ 'documentType': { $regex: ?0, $options: 'i' } } ] }")
    Flux<LoanDocument> searchByFileNameOrType(String searchTerm);

    /**
     * Fetches documents uploaded after a given timestamp.
     * Useful for audit trails and incremental processing pipelines.
     */
    @Query("{ 'customerId': ?0, 'uploadedAt': { $gte: ?1 } }")
    Flux<LoanDocument> findByCustomerIdUploadedAfter(String customerId, Instant since);

    /**
     * Finds documents tagged with a specific tag (MongoDB $in on array field).
     */
    @Query("{ 'tags': { $in: [?0] } }")
    Flux<LoanDocument> findByTag(String tag);

    /**
     * Finds documents larger than a given size threshold.
     * Used by storage management jobs to identify oversized uploads.
     */
    @Query("{ 'sizeBytes': { $gt: ?0 } }")
    Flux<LoanDocument> findDocumentsLargerThan(long sizeBytes);

    // ─── MongoDB @Aggregation Pipelines ──────────────────────────────────────

    /**
     * Returns the count and total size of documents grouped by documentType
     * for a specific loan. Useful for document completeness checks.
     * Returns Flux<Object> where each entry has: _id (type), count, totalSize.
     */
    @Aggregation(pipeline = {
            "{ $match: { 'loanId': ?0 } }",
            "{ $group: { _id: '$documentType', count: { $sum: 1 }, totalSize: { $sum: '$sizeBytes' } } }",
            "{ $sort: { '_id': 1 } }"
    })
    Flux<Object> aggregateDocumentSummaryByType(String loanId);

    /**
     * Returns the count of documents grouped by status across all loans for a customer.
     * Used for customer document dashboard widget.
     */
    @Aggregation(pipeline = {
            "{ $match: { 'customerId': ?0 } }",
            "{ $group: { _id: '$status', count: { $sum: 1 } } }",
            "{ $sort: { 'count': -1 } }"
    })
    Flux<Object> aggregateDocumentStatusSummary(String customerId);

    /**
     * Rolling 30-day document upload volume by day.
     * Used for operational monitoring dashboards.
     */
    @Aggregation(pipeline = {
            "{ $match: { 'uploadedAt': { $gte: ?0 } } }",
            "{ $group: { _id: { $dateToString: { format: '%Y-%m-%d', date: '$uploadedAt' } }, count: { $sum: 1 } } }",
            "{ $sort: { '_id': 1 } }"
    })
    Flux<Object> aggregateDailyUploadVolume(Instant fromDate);
}
