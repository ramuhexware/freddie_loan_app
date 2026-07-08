# Struts 2 Loan Portal — Walkthrough

## What Was Built

A new Maven module `struts-loan-portal` was added to the `freddie-loan-platform` project.
It is a classic **Struts 2 + Spring + DB2** web application packaged as a WAR.

---

## Files Created

### Configuration
| File | Purpose |
|------|---------|
| `struts-loan-portal/pom.xml` | WAR module — Struts 2.6, Spring JDBC, HikariCP, DB2 JDBC, HttpClient 5 |
| `WEB-INF/web.xml` | Jakarta EE 6 descriptor — Spring ContextLoader + Struts 2 filter |
| `resources/struts.xml` | All namespaces, action mappings, audit interceptor stack |
| `resources/db2.properties` | DB2 JDBC URL + HikariCP pool settings |
| `resources/applicationContext.xml` | Spring context — DataSource, JdbcTemplate, TxManager, ServiceConfig |
| `resources/db2-schema.sql` | DB2 DDL for all 4 tables + seed data |

### Java Source
| Class | Role |
|-------|------|
| `ServiceConfig` | POJO holding microservice base URLs |
| `LoanApplication` | Model → DB2 LOAN_APPLICATIONS |
| `Customer` | Model → DB2 CUSTOMERS |
| `UnderwritingDecision` | Model → DB2 UNDERWRITING_DECISIONS |
| `AuditLog` | Model → DB2 AUDIT_LOG |
| `ReportSummary` | DTO for aggregation query results |
| `LoanDao` | **12 DB2 calls** — SELECT/INSERT/UPDATE/GROUP BY/JOIN |
| `CustomerDao` | **6 DB2 calls** — SELECT/LIKE/COUNT/JOIN |
| `UnderwritingDao` | **5 DB2 calls** — SELECT/INSERT/JOIN |
| `AuditDao` | **3 DB2 calls** — INSERT + SELECT (REQUIRES_NEW tx) |
| `ReportDao` | **5 DB2 calls** — Aggregations, subqueries, GROUP BY month |
| `LoanServiceClient` | HTTP REST → loan-origination-service |
| `CustomerServiceClient` | HTTP REST → customer-service |
| `AuditInterceptor` | Wraps every Struts action → writes AUDIT_LOG to DB2 |
| `DashboardAction` | KPI aggregation from DB2 |
| `LoanAction` | Loan lifecycle — list/detail/apply/save/approve/reject |
| `CustomerAction` | Customer list/search/detail/stats |
| `UnderwritingAction` | Pending reviews/save decision |
| `ReportAction` | Summary/monthly stats/audit log |
| `LoginAction` | Session-based auth |

### JSP Views
| File | Screen |
|------|--------|
| `login.jsp` | Login page (gradient card) |
| `common/header.jsp` | Sticky nav bar (included by all pages) |
| `dashboard.jsp` | KPI cards + recent loans + breakdown charts |
| `loan/list.jsp` | Paginated loan table with filter bar |
| `loan/detail.jsp` | Full loan info + UW history + inline approve/reject |
| `loan/apply.jsp` | New loan application form |
| `underwriting/review.jsp` | UW decision form with loan summary |
| `customer/list.jsp` | Customer table with search + credit score color |
| `report/loanReport.jsp` | DB2 aggregation tables + approval rate bars |
| `report/auditLog.jsp` | Complete audit trail table |

---

## Total DB2 Calls Inventory

| DAO | DB2 Calls | Operations |
|-----|-----------|-----------|
| LoanDao | 12 | SELECT (6 variants), INSERT, UPDATE (2), COUNT GROUP BY, SUM GROUP BY, FETCH FIRST |
| CustomerDao | 6 | SELECT, LIKE search, COUNT GROUP BY, JOIN with loan count, High-risk filter |
| UnderwritingDao | 5 | SELECT by loan, SELECT pending JOIN, INSERT, FETCH FIRST 1, SELECT by risk |
| AuditDao | 3 | INSERT (REQUIRES_NEW), SELECT recent, SELECT by action |
| ReportDao | 5 | GROUP BY type, GROUP BY status, YEAR/MONTH aggregate, TOP-N, Subquery approval rate |
| **TOTAL** | **31** | |

---

## Integration with Microservices

```
Struts Action             → Microservice REST API
─────────────────────────────────────────────────
LoanAction.save()         → POST loan-origination-service:8081/api/v1/loans
LoanAction.underwrite()   → POST loan-origination-service:8081/api/v1/loans/{id}/submit-for-underwriting
CustomerAction.detail()   → GET  customer-service:8082/api/v1/customers/{id}
CustomerAction.search()   → GET  customer-service:8082/api/v1/customers/search
```

All other operations use **DB2 directly** (read-heavy queries, reports, audit).

---

## Deployment Instructions

### Step 1 — Set up DB2
```sql
-- Run the DDL script:
db2 -tf struts-loan-portal/src/main/resources/db2-schema.sql
```

### Step 2 — Install DB2 JDBC JAR into Maven
```bash
mvn install:install-file \
  -Dfile=/path/to/db2jcc4.jar \
  -DgroupId=com.ibm.db2 \
  -DartifactId=jcc \
  -Dversion=11.5.9.0 \
  -Dpackaging=jar
```

### Step 3 — Configure DB2 credentials
Edit `struts-loan-portal/src/main/resources/db2.properties`:
```properties
db2.url=jdbc:db2://YOUR_HOST:50000/YOUR_DB
db2.username=YOUR_USER
db2.password=YOUR_PASS
```

### Step 4 — Build
```bash
mvn clean package -pl struts-loan-portal -am
```

### Step 5 — Deploy
```bash
# Deploy WAR to Tomcat 10+
cp struts-loan-portal/target/struts-loan-portal.war $TOMCAT_HOME/webapps/

# OR run embedded Tomcat
mvn tomcat7:run -pl struts-loan-portal
```

### Step 6 — Access
```
http://localhost:8090/struts-loan-portal/
```
Login: `admin / freddie123` or `officer / freddie123`

---

## Key Design Decisions

- **AuditInterceptor** uses `PROPAGATION_REQUIRES_NEW` — audit always commits even if the business transaction fails
- **Writes go via REST** to existing microservices to respect business logic (Kafka events, status history)
- **Reads go direct to DB2** for speed (no REST overhead on list/search/report queries)
- **HikariCP** pool with 20 max connections, health-check via `SELECT 1 FROM SYSIBM.SYSDUMMY1`
- **Jakarta EE** namespace (`jakarta.*`) throughout — compatible with Tomcat 10+ and WAS Liberty
