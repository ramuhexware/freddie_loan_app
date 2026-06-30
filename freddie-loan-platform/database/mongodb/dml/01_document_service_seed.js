// =============================================================================
// Freddie Mac-Style Home Loan Platform
// Database   : MongoDB 7.0
// Database   : freddie_documents  (document-service)
// Collection : documents
// Script     : DML — Seed documents for 3 loan applications
// Run with   : mongosh freddie_documents 01_document_service_seed.js
// =============================================================================

use("freddie_documents");

const NOW       = new Date();
const NEXT_YEAR = new Date(NOW.getFullYear() + 1, NOW.getMonth(), NOW.getDate());

// =============================================================================
// SEED: documents — representative set for one approved mortgage
// =============================================================================
const docs = [
  // ── Loan 1 (james.harrison) ─────────────────────────────────────────────
  {
    documentId:   "DOC-0001-W2-2023",
    loanId:       "LOAN-00000000-0000-0001",
    customerId:   "00000000-0000-0000-0000-000000000001",
    documentType: "W2",
    fileName:     "james_harrison_w2_2023.pdf",
    mimeType:     "application/pdf",
    sizeBytes:    Long("204800"),
    checksum:     "sha256:a1b2c3d4e5f6789012345678901234567890abcdef1234567890abcdef123456",
    gridFsFileId: "gridfs-objectid-001",
    status:       "VERIFIED",
    verifiedBy:   "doc-verifier-service",
    verifiedAt:   new Date(NOW - 7 * 86400000),
    expiryDate:   NEXT_YEAR,
    metadata: {
      taxYear:    2023,
      employer:   "Tech Corp Inc.",
      grossWages: 120000.00
    },
    uploadedBy: "james.harrison@example.com",
    tags:       ["tax", "income", "2023"],
    uploadedAt: new Date(NOW - 14 * 86400000),
    updatedAt:  new Date(NOW - 7  * 86400000),
    version:    1
  },
  {
    documentId:   "DOC-0001-PAY-202401",
    loanId:       "LOAN-00000000-0000-0001",
    customerId:   "00000000-0000-0000-0000-000000000001",
    documentType: "PAY_STUB",
    fileName:     "james_harrison_paystub_jan2024.pdf",
    mimeType:     "application/pdf",
    sizeBytes:    Long("98304"),
    checksum:     "sha256:b2c3d4e5f6789012345678901234567890abcdef1234567890abcdef1234567a",
    gridFsFileId: "gridfs-objectid-002",
    status:       "VERIFIED",
    verifiedBy:   "doc-verifier-service",
    verifiedAt:   new Date(NOW - 7 * 86400000),
    expiryDate:   NEXT_YEAR,
    metadata: {
      payPeriod:  "2024-01",
      netPay:     7500.00,
      employer:   "Tech Corp Inc."
    },
    uploadedBy: "james.harrison@example.com",
    tags:       ["income", "payroll", "2024-01"],
    uploadedAt: new Date(NOW - 14 * 86400000),
    updatedAt:  new Date(NOW - 7  * 86400000),
    version:    1
  },
  {
    documentId:   "DOC-0001-APPR-001",
    loanId:       "LOAN-00000000-0000-0001",
    customerId:   "00000000-0000-0000-0000-000000000001",
    documentType: "APPRAISAL",
    fileName:     "123_maple_street_appraisal.pdf",
    mimeType:     "application/pdf",
    sizeBytes:    Long("512000"),
    checksum:     "sha256:c3d4e5f6789012345678901234567890abcdef1234567890abcdef1234567ab2",
    gridFsFileId: "gridfs-objectid-003",
    status:       "VERIFIED",
    verifiedBy:   "doc-verifier-service",
    verifiedAt:   new Date(NOW - 5 * 86400000),
    expiryDate:   new Date(NOW.getFullYear(), NOW.getMonth() + 6, NOW.getDate()),
    metadata: {
      appraisedValue: 550000.00,
      appraisalDate:  "2024-05-20",
      appraiser:      "Austin Appraisal Group LLC",
      propertyType:   "Single Family Residence"
    },
    uploadedBy: "appraisal-service",
    tags:       ["property", "appraisal", "austin-tx"],
    uploadedAt: new Date(NOW - 10 * 86400000),
    updatedAt:  new Date(NOW - 5  * 86400000),
    version:    1
  },
  // ── Loan 3 (michael.chen) — HELOC, just uploaded ────────────────────────
  {
    documentId:   "DOC-0003-TAX-2023",
    loanId:       "LOAN-00000000-0000-0003",
    customerId:   "00000000-0000-0000-0000-000000000003",
    documentType: "TAX_RETURN",
    fileName:     "michael_chen_1040_2023.pdf",
    mimeType:     "application/pdf",
    sizeBytes:    Long("307200"),
    checksum:     "sha256:d4e5f6789012345678901234567890abcdef1234567890abcdef1234567abc3",
    gridFsFileId: null,
    status:       "PROCESSING",
    verifiedBy:   null,
    verifiedAt:   null,
    expiryDate:   NEXT_YEAR,
    metadata: {
      taxYear:       2023,
      filingStatus:  "Married Filing Jointly",
      adjustedGrossIncome: 220000.00
    },
    uploadedBy: "michael.chen@example.com",
    tags:       ["tax", "federal", "2023"],
    uploadedAt: NOW,
    updatedAt:  NOW,
    version:    0
  },
  {
    documentId:   "DOC-0003-ID-001",
    loanId:       "LOAN-00000000-0000-0003",
    customerId:   "00000000-0000-0000-0000-000000000003",
    documentType: "ID_PROOF",
    fileName:     "michael_chen_drivers_license.pdf",
    mimeType:     "application/pdf",
    sizeBytes:    Long("153600"),
    checksum:     "sha256:e5f6789012345678901234567890abcdef1234567890abcdef1234567abc4d5",
    gridFsFileId: null,
    status:       "UPLOADED",
    verifiedBy:   null,
    verifiedAt:   null,
    expiryDate:   new Date("2029-11-30"),
    metadata: {
      idType:     "Drivers License",
      issueState: "WA",
      idNumber:   "WA-MASKED-0001"
    },
    uploadedBy: "michael.chen@example.com",
    tags:       ["identity", "government-id"],
    uploadedAt: NOW,
    updatedAt:  NOW,
    version:    0
  }
];

const result = db.documents.insertMany(docs);
print(`✅  Inserted ${result.insertedIds ? Object.keys(result.insertedIds).length : 0} seed documents.`);
print("📋  Documents collection is ready.");
