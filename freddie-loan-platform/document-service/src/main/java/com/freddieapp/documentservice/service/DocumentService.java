package com.freddieapp.documentservice.service;

import com.freddieapp.documentservice.entity.LoanDocument;
import com.freddieapp.documentservice.entity.LoanDocument.DocumentStatus;
import com.freddieapp.documentservice.repository.LoanDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final LoanDocumentRepository documentRepository;
    private final ReactiveGridFsTemplate gridFsTemplate;

    public Mono<LoanDocument> uploadDocument(String loanId, String customerId, String documentType, Mono<FilePart> filePartMono) {
        return filePartMono.flatMap(filePart -> {
            String documentId = UUID.randomUUID().toString();
            String fileName = filePart.filename();
            log.info("Uploading document reactive to GridFS: filename={}, type={}, loanId={}", fileName, documentType, loanId);

            // Store file binary in GridFS reactive
            return gridFsTemplate.store(filePart.content(), fileName)
                    .flatMap(objectId -> {
                        log.info("Binary stored in GridFS under objectId={}", objectId.toHexString());
                        LoanDocument doc = LoanDocument.builder()
                                .documentId(documentId)
                                .loanId(loanId)
                                .customerId(customerId)
                                .documentType(documentType)
                                .fileName(fileName)
                                .gridFsFileId(objectId.toHexString())
                                .status(DocumentStatus.UPLOADED)
                                .uploadedBy("SYSTEM_USER")
                                .build();
                        return documentRepository.save(doc);
                    });
        });
    }

    public Mono<LoanDocument> getDocumentById(String documentId) {
        return documentRepository.findByDocumentId(documentId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Document not found: " + documentId)));
    }

    public Flux<LoanDocument> getDocumentsByLoan(String loanId) {
        return documentRepository.findByLoanId(loanId);
    }

    public Flux<LoanDocument> getDocumentsByCustomerAndStatus(String customerId, DocumentStatus status, Pageable pageable) {
        return documentRepository.findByCustomerIdAndStatus(customerId, status, pageable);
    }

    public Flux<Object> getDocumentSummaryByType(String loanId) {
        return documentRepository.aggregateDocumentSummaryByType(loanId);
    }

    public Mono<Void> deleteDocument(String documentId) {
        return documentRepository.findByDocumentId(documentId)
                .flatMap(doc -> {
                    log.info("Deleting document from db and GridFS: documentId={}", documentId);
                    // Delete metadata entry and GridFS binary
                    // In GridFS reactive we can delete by query
                    org.springframework.data.mongodb.core.query.Query query =
                            new org.springframework.data.mongodb.core.query.Query(
                                    org.springframework.data.mongodb.core.query.Criteria.where("_id").is(doc.getGridFsFileId())
                            );
                    return gridFsTemplate.delete(query)
                            .then(documentRepository.delete(doc));
                });
    }
}
