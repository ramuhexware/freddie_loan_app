package com.freddieapp.documentservice.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Document(collection = "documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanDocument {

    @Id
    private String id;

    @Field("documentId")
    private String documentId;

    @Indexed
    @Field("loanId")
    private String loanId;

    @Indexed
    @Field("customerId")
    private String customerId;

    @Field("documentType")
    private String documentType;      // W2, PAY_STUB, TAX_RETURN, APPRAISAL, ID_PROOF

    @Field("fileName")
    private String fileName;

    @Field("mimeType")
    private String mimeType;

    @Field("sizeBytes")
    private Long sizeBytes;

    @Field("checksum")
    private String checksum;          // SHA-256 hash for integrity verification

    @Field("gridFsFileId")
    private String gridFsFileId;      // MongoDB GridFS ObjectId reference

    @Field("status")
    private DocumentStatus status;

    @Field("verifiedBy")
    private String verifiedBy;

    @Field("verifiedAt")
    private Instant verifiedAt;

    @Field("expiryDate")
    private LocalDate expiryDate;

    @Field("metadata")
    private Map<String, Object> metadata;  // Flexible schema for doc-type-specific data

    @Field("uploadedBy")
    private String uploadedBy;

    @Field("tags")
    private List<String> tags;

    @CreatedDate
    @Field("uploadedAt")
    private Instant uploadedAt;

    @LastModifiedDate
    @Field("updatedAt")
    private Instant updatedAt;

    @Field("version")
    private Integer version;

    public enum DocumentStatus { UPLOADED, PROCESSING, VERIFIED, REJECTED, EXPIRED }
}
