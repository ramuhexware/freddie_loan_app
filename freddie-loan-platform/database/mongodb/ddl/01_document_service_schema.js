// =============================================================================
// Freddie Mac-Style Home Loan Platform
// Database   : MongoDB 7.0
// Database   : freddie_documents  (document-service)
// Collection : documents
// Script     : DDL — Collection creation, schema validation, indexes
// Run with   : mongosh freddie_documents 01_document_service_schema.js
// =============================================================================

use("freddie_documents");

// =============================================================================
// 1. Create collection with JSON Schema validation
//    Mapped to: LoanDocument.java  (@Document(collection = "documents"))
// =============================================================================
db.createCollection("documents", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["documentId", "loanId", "customerId", "documentType",
                 "fileName", "mimeType", "status", "uploadedBy"],
      additionalProperties: true,
      properties: {
        documentId: {
          bsonType: "string",
          description: "Application-level document UUID — required"
        },
        loanId: {
          bsonType: "string",
          description: "Reference to LOAN_APPLICATIONS.LOAN_ID — required"
        },
        customerId: {
          bsonType: "string",
          description: "Reference to customers.id — required"
        },
        documentType: {
          bsonType: "string",
          enum: ["W2", "PAY_STUB", "TAX_RETURN", "APPRAISAL",
                 "ID_PROOF", "BANK_STATEMENT", "CREDIT_REPORT", "PROPERTY_DEED"],
          description: "Supported document types — required"
        },
        fileName: {
          bsonType: "string",
          description: "Original file name — required"
        },
        mimeType: {
          bsonType: "string",
          description: "MIME type e.g. application/pdf — required"
        },
        sizeBytes: {
          bsonType: ["long", "int", "null"],
          minimum: 0,
          description: "File size in bytes"
        },
        checksum: {
          bsonType: ["string", "null"],
          description: "SHA-256 integrity hash"
        },
        gridFsFileId: {
          bsonType: ["string", "null"],
          description: "MongoDB GridFS ObjectId for binary storage"
        },
        status: {
          bsonType: "string",
          enum: ["UPLOADED", "PROCESSING", "VERIFIED", "REJECTED", "EXPIRED"],
          description: "Document lifecycle status — required"
        },
        verifiedBy: {
          bsonType: ["string", "null"]
        },
        verifiedAt: {
          bsonType: ["date", "null"]
        },
        expiryDate: {
          bsonType: ["date", "null"]
        },
        metadata: {
          bsonType: ["object", "null"],
          description: "Flexible document-type-specific key-value pairs"
        },
        uploadedBy: {
          bsonType: "string",
          description: "User or service that uploaded the document — required"
        },
        tags: {
          bsonType: ["array", "null"],
          items: { bsonType: "string" }
        },
        uploadedAt: {
          bsonType: ["date", "null"]
        },
        updatedAt: {
          bsonType: ["date", "null"]
        },
        version: {
          bsonType: ["int", "null"]
        }
      }
    }
  },
  validationLevel: "moderate",    // existing docs are not re-validated on update
  validationAction: "error"
});

print("✅  Collection 'documents' created with schema validation.");

// =============================================================================
// 2. Indexes
// =============================================================================

// Primary lookup: loanId (most common query pattern)
db.documents.createIndex(
  { loanId: 1 },
  { name: "idx_documents_loan_id", background: true }
);

// Customer-level document listing
db.documents.createIndex(
  { customerId: 1 },
  { name: "idx_documents_customer_id", background: true }
);

// Compound: loan + type (used by document-service to check completeness)
db.documents.createIndex(
  { loanId: 1, documentType: 1 },
  { name: "idx_documents_loan_type", background: true }
);

// Status filtering
db.documents.createIndex(
  { status: 1 },
  { name: "idx_documents_status", background: true }
);

// Unique documentId (application-level unique key)
db.documents.createIndex(
  { documentId: 1 },
  { name: "idx_documents_document_id", unique: true, background: true }
);

// GridFS reference (fast lookup from binary store)
db.documents.createIndex(
  { gridFsFileId: 1 },
  { name: "idx_documents_gridfs_id", sparse: true, background: true }
);

// TTL index: auto-expire EXPIRED documents after 365 days
db.documents.createIndex(
  { expiryDate: 1 },
  { name: "idx_documents_expiry_ttl", expireAfterSeconds: 0, sparse: true }
);

print("✅  All indexes created on 'documents' collection.");
print("📋  Collection 'documents' schema and indexes are ready.");
