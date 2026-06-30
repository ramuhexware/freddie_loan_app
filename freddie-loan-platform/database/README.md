# Database Scripts

This folder contains all DDL (schema creation) and DML (seed data) SQL scripts for every database used by the Freddie Mac-Style Home Loan Platform.

## Folder Structure

```
database/
├── postgresql/          # PostgreSQL 16 — customer-service, card-service
│   ├── ddl/
│   │   ├── 01_customer_service_schema.sql   # customers + kyc_records tables
│   │   └── 02_card_service_schema.sql       # CARDS table
│   └── dml/
│       ├── 01_customer_service_seed.sql     # 5 sample customers + KYC records
│       └── 02_card_service_seed.sql         # 5 sample cards
│
├── oracle/              # Oracle 21c / 19c — loan-origination-service
│   ├── ddl/
│   │   └── 01_loan_origination_schema.sql   # LOAN_APPLICATIONS + LOAN_STATUS_HISTORY
│   └── dml/
│       └── 01_loan_origination_seed.sql     # 5 loan apps + status history trail
│
├── db2/                 # IBM DB2 LUW 11.5 — underwriting-service
│   ├── ddl/
│   │   └── 01_underwriting_service_schema.sql   # UNDERWRITING_ASSESSMENTS table
│   └── dml/
│       └── 01_underwriting_service_seed.sql     # 4 risk assessments (APPROVED/REFERRED/DECLINED)
│
└── mongodb/             # MongoDB 7.0 — document-service
    ├── ddl/
    │   └── 01_document_service_schema.js    # Collection, JSON schema validation, indexes
    └── dml/
        └── 01_document_service_seed.js      # 5 sample loan documents (W2, pay stubs, appraisals)
```

## Database → Service Mapping

| Database | Version | Service | Schema / DB Name |
|---|---|---|---|
| PostgreSQL | 16 | `customer-service` | `freddie_customer` |
| PostgreSQL | 16 | `card-service` | `freddie_cards` |
| Oracle | 21c / 19c | `loan-origination-service` | `FREDDIE_LOANS` |
| IBM DB2 | LUW 11.5 | `underwriting-service` | `FREDDIE_UW` |
| MongoDB | 7.0 | `document-service` | `freddie_documents` |

## How to Run

### PostgreSQL
```bash
# Create databases first (run as superuser)
psql -U postgres -c "CREATE DATABASE freddie_customer;"
psql -U postgres -c "CREATE DATABASE freddie_cards;"

# Run DDL
psql -U freddie_admin -d freddie_customer -f postgresql/ddl/01_customer_service_schema.sql
psql -U freddie_admin -d freddie_cards    -f postgresql/ddl/02_card_service_schema.sql

# Run DML (seed)
psql -U freddie_admin -d freddie_customer -f postgresql/dml/01_customer_service_seed.sql
psql -U freddie_admin -d freddie_cards    -f postgresql/dml/02_card_service_seed.sql
```

### Oracle
```bash
# Connect as SYSDBA first to create user/schema, then:
sqlplus FREDDIE_LOANS/password@localhost:1521/FREEPDB1 @oracle/ddl/01_loan_origination_schema.sql
sqlplus FREDDIE_LOANS/password@localhost:1521/FREEPDB1 @oracle/dml/01_loan_origination_seed.sql
```

### IBM DB2
```bash
db2 connect to FREDDIEDB
db2 -tvf db2/ddl/01_underwriting_service_schema.sql
db2 -tvf db2/dml/01_underwriting_service_seed.sql
```

### MongoDB
```bash
mongosh "mongodb://localhost:27017" freddie_documents mongodb/ddl/01_document_service_schema.js
mongosh "mongodb://localhost:27017" freddie_documents mongodb/dml/01_document_service_seed.js
```

## Notes

- **DDL scripts are idempotent** — they use `CREATE TABLE IF NOT EXISTS` / `createCollection` so they are safe to re-run.
- **DML seed data** uses sample/placeholder UUIDs. In a real environment, replace `CUSTOMER_ID` foreign-key values with actual UUIDs from the PostgreSQL `customers` table.
- **MongoDB scripts** use `mongosh` JavaScript syntax (not the legacy `mongo` shell).
- **Oracle scripts** require the `FREDDIE_LOANS` schema user to already exist. See the commented `CREATE USER` block in the DDL file.
