# Appian Integration Workflow Verification Walkthrough

We have successfully resolved the routing, database migration, and Kafka listener issues that were blocking the integration. The workflow is now fully operational.

---

## 🛠️ Resolved Issues Summary

### 1. API Gateway Routing (404 Not Found)
* **Problem**: Calling `/api/v1/appian/integration-logs` returned a `404 Not Found`.
* **Cause**: Although the static routing logic was added to the `api-gateway` configuration, the gateway's JAR was not repackaged, and the Docker container was running an older build.
* **Resolution**: Rebuilt `api-gateway` with `mvn clean package` and recreated the Docker container, successfully registering the `/api/v1/appian/**` routing prefixes.

### 2. Customer Service Crash (Unsupported Database: PostgreSQL 16.14)
* **Problem**: Submitting a loan returned a gateway timeout because `customer-service` was down.
* **Cause**: Flyway 10+ modularized database drivers. `customer-service` did not declare the `flyway-database-postgresql` dependency, causing it to fail to identify PostgreSQL 16 or 15 and crash on boot.
* **Resolution**: 
  * Added `flyway-database-postgresql` to the dependencies in [customer-service/pom.xml](file:///c:/ramu/Project_Assignment/RapidX/FreddeMac_Project_RapidX/Freddie_Style_Application/freddie-loan-platform/customer-service/pom.xml#L103-L108).
  * Downgraded the postgres image in [docker-compose.yml](file:///c:/ramu/Project_Assignment/RapidX/FreddeMac_Project_RapidX/Freddie_Style_Application/freddie-loan-platform/deployment/docker-compose.yml#L91-L95) to `postgres:15-alpine` to align with the framework dependencies.
  * Rebuilt and restarted the stack, resulting in the successful startup and Eureka registration of `CUSTOMER-SERVICE`.

### 3. Appian Kafka Consumer Failure (No Acknowledgment Available)
* **Problem**: The Kafka event consumer (`LoanEventListener`) in `appian-service` crashed with `IllegalStateException` when trying to commit offsets.
* **Cause**: The listener signature expected a manual `Acknowledgment` object, but the container's default ack-mode was not set to manual.
* **Resolution**: Added `spring.kafka.listener.ack-mode=manual` to [appian-service/src/main/resources/application.properties](file:///c:/ramu/Project_Assignment/RapidX/FreddeMac_Project_RapidX/Freddie_Style_Application/freddie-loan-platform/appian-service/src/main/resources/application.properties#L19-L26) and rebuilt the service.

---

## 🚀 Verification Steps

Follow these steps to run a full test of the Appian integration workflow:

### Step 1: Create a Valid Customer
Submit a POST request to register a customer. The system will return a valid customer UUID (e.g. `26c6da04-7374-49ba-95d6-f0f2cb39a74b`):

```bash
curl.exe -i -X POST http://localhost:8080/api/v1/customers \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "phone": "+15550101",
    "ssn": "123-456-7890",
    "dateOfBirth": "1990-01-01",
    "nationality": "USA",
    "addressLine1": "123 Main St",
    "city": "Austin",
    "state": "TX",
    "zipCode": "78701"
  }'
```

### Step 2: Submit a Loan via Appian Intake Endpoint
Use the generated customer ID to submit a loan application. The Appian service will call the loan-origination microservice:

```bash
curl.exe -i -X POST http://localhost:8080/api/v1/appian/loans \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "YOUR_CUSTOMER_UUID_HERE",
    "loanType": "PURCHASE",
    "loanAmount": 430000,
    "propertyValue": 560000,
    "propertyAddress": "988 Maple Ave",
    "loanTermMonths": 360
  }'
```
*Response*: Returns `201 Created` with a new `loanId` and `loanStatus: "SUBMITTED"`.

### Step 3: Check Appian Audit Logs
Query the Appian Integration Logs to verify the inbound ingestion and outbound status updates (processed via Kafka events) are logged:

```bash
curl.exe -i http://localhost:8080/api/v1/appian/integration-logs
```

*Expected JSON Contents*:
1. `INBOUND_LOAN_SUBMIT`: Logs the request payload from Appian and the `201 Created` response.
2. `OUTBOUND_STATUS_UPDATE`: Logs the outbound status synchronization event processed by the Kafka consumer.
