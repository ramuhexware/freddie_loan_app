package com.freddieapp.documentservice.controller;

import com.freddieapp.documentservice.entity.LoanDocument;
import com.freddieapp.documentservice.entity.LoanDocument.DocumentStatus;
import com.freddieapp.documentservice.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Document Management", description = "Reactive APIs for loan documents storage and indexing in GridFS & MongoDB")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload document reactive to GridFS")
    public Mono<LoanDocument> uploadDocument(
            @RequestPart("loanId") String loanId,
            @RequestPart("customerId") String customerId,
            @RequestPart("documentType") String documentType,
            @RequestPart("file") Mono<FilePart> filePartMono) {
        log.info("REST upload request: loanId={}, type={}", loanId, documentType);
        return documentService.uploadDocument(loanId, customerId, documentType, filePartMono);
    }

    @GetMapping("/{documentId}")
    @Operation(summary = "Get document metadata by ID")
    public Mono<ResponseEntity<LoanDocument>> getDocumentMetadata(@PathVariable String documentId) {
        return documentService.getDocumentById(documentId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/loan/{loanId}")
    @Operation(summary = "Get all documents for a specific loan")
    public Flux<LoanDocument> getDocumentsByLoan(@PathVariable String loanId) {
        log.info("REST fetch docs for loanId={}", loanId);
        return documentService.getDocumentsByLoan(loanId);
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get paginated customer documents by status")
    public Flux<LoanDocument> getCustomerDocuments(
            @PathVariable String customerId,
            @RequestParam(defaultValue = "UPLOADED") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("REST fetch docs for customerId={}, status={}", customerId, status);
        return documentService.getDocumentsByCustomerAndStatus(
                customerId,
                DocumentStatus.valueOf(status),
                PageRequest.of(page, size));
    }

    @GetMapping("/summary/loan/{loanId}")
    @Operation(summary = "Get document count/size summary aggregated by type")
    public Flux<Object> getDocumentSummaryByType(@PathVariable String loanId) {
        return documentService.getDocumentSummaryByType(loanId);
    }

    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete document and its binary from GridFS")
    public Mono<Void> deleteDocument(@PathVariable String documentId) {
        log.info("REST request to delete documentId={}", documentId);
        return documentService.deleteDocument(documentId);
    }
}
