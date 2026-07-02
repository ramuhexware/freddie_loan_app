# Freddie Mac-Style Home Loan & Customer Management Platform

This repository contains a production-grade, distributed Home Loan Application and Customer Management system designed around Freddie Mac system specifications. It employs a domain-driven, cloud-ready microservices architecture supporting mortgage loan lifecycle phases from intake and risk decisioning to legacy systems adapter processing.

---

## 🎯 Architecture Blueprint

The platform uses a hybrid communication topology combining synchronous REST endpoints (via Netflix Eureka & Feign) and asynchronous event-driven triggers (via Apache Kafka & ActiveMQ). 

```
                 [ Angular 18 Portal Client ]
                                 │
                            (HTTPS/REST)
                                 ▼
                     ┌───────────────────────┐
                     │      API Gateway      │◄─── [ Redis Rate Limiter ]
                     └───────────┬───────────┘
                                 │
         ┌───────────────────────┼───────────────────────┐
         ▼                       ▼                       ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│Customer Service │     │Loan Origination │     │Card Service     │
│  (PostgreSQL)   │     │  (Oracle DB)    │     │  (PostgreSQL)   │
└────────┬────────┘     └────────┬────────┘     └────────┬────────┘
         │                       │                       │
         │ (WebClient Sync)      │ (FeignClient verify)  │ (REST Endpoints)
         └───────────────────────┼───────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                       Internal Services                         │
├───────────────────────┬───────────────────────┬─────────────────┤
│ Underwriting (DB2)    │ Document (MongoDB)    │ Messaging       │
│ Notification (Kafka)  │ Legacy Adapter (SOAP) │ OSB / SOA Suite │
└───────────────────────┴───────────────────────┴─────────────────┘
```

---

## ⚙️ Technology Stack

*   **Runtime**: Java 17 (standard modules) and Java 21 (required for `underwriting-service` to leverage records/switch patterns/ Loom virtual threads).
*   **Frameworks**: Spring Boot 3.3.0, Spring Cloud 2023.0.2.
*   **Build Tools**: Maven 3.8+ (for parent aggregator and 14 modules) & Gradle 8.x+ (specifically for `notification-service`).
*   **Frontend**: Angular 18 (TypeScript, HTML5, Vanilla CSS3).
*   **Databases**: PostgreSQL (Customer & Card), Oracle XE (Loan Origination), IBM DB2 (Underwriting), MongoDB & GridFS (Documents).
*   **Messaging**: Apache Kafka (Real-time lifecycle events), Apache ActiveMQ (JMS Queue Audits).
*   **Integration/Orchestration**: Direct SOAP mock endpoints in Java (BPEL and OSB composite XML/BPEL files have been completely removed, with business rules and DTI/credit score checks implemented directly in `legacy-adapter-service`).
*   **Edge Layer**: Spring Cloud Gateway, Redis, Resilience4j.

---

## 🧱 Project Structure & Mixed Build Hierarchy

The project is structured as a mixed-build architecture:
1. **Maven Modules (14 modules)**: Grouped under the parent container `freddie-loan-platform`.
2. **Gradle Module (`notification-service`)**: Independently built and managed using Gradle.

```
Freddie_Style_Application/                       # Workspace root
└── freddie-loan-platform/                       # Consolidated project folder
    ├── pom.xml                                  # Aggregator Parent POM (com.freddieapp:freddie-loan-platform)
    ├── README.md                                # This documentation file
    ├── architecture_design.md                   # System Architecture Details
    │
    # ─── MICROSERVICE INFRASTRUCTURE & GATEWAYS ────────────────────────────────
    ├── eureka-server/                           # Netflix Eureka Service Discovery (Port: 8761)
    ├── api-gateway/                             # Spring Cloud API Gateway (Port: 8080)
    │
    # ─── CORE BUSINESS SERVICES ────────────────────────────────────────────────
    ├── customer-service/                        # Customer profiles & KYC (PostgreSQL, Port: 8081)
    ├── loan-origination-service/                # Mortgage application intake (Oracle DB, Port: 8082)
    ├── underwriting-service/                    # Rules engine & risk assessment (IBM DB2, Java 21, Port: 8083)
    ├── document-service/                        # Non-blocking loan document storage (MongoDB, Port: 8084)
    ├── card-service/                            # Credit/debit card management (PostgreSQL, Port: 8085)
    │
    # ─── MESSAGING, INTEGRATION & ADAPTER SERVICES ────────────────────────────
    ├── notification-service/                    # Kafka consumer for life-cycle alerts (Gradle build, Port: 8086)
    ├── messaging-service/                       # ActiveMQ JMS audit trail dispatch (Port: 8087)
    ├── legacy-adapter-service/                  # SOAP legacy client bridging service (Port: 8088)
    ├── legacy-admin-portal/                     # Servlet & JSP legacy portal webapp (Port: 8089)
    ├── appraisal-service/                       # Property Appraisal & Valuation service (H2 DB, Port: 8090)
    ├── funding-service/                         # Loan Disbursement & Settlement service (H2 DB, Port: 8091)
    │
    # ─── FRONTEND & DEPLOYMENT UTILITIES ───────────────────────────────────────
    ├── frontend/                                # Angular 18 Portal UI Client (Port: 4200)
    ├── deployment/                              # Docker Compose files & db initialization scripts
    └── database/                                # DDL / DML SQL and script files for all databases
```

### Parent-Child Relationships
*   **Root Aggregator POM (`pom.xml`)**: Serves as the central parent POM (`com.freddieapp:freddie-loan-platform`). It inherits from `spring-boot-starter-parent` (version `3.3.0`) and controls common dependency versions (e.g. Spring Cloud `2023.0.2`, Lombok, resilience4j) for all Maven children.
*   **Notification Service Gradle Configuration**: Built with `notification-service/build.gradle` and `notification-service/settings.gradle` targeting Java 17 and Spring Boot 3.3.0.

---

## 🔧 Configuration Management & Profiles

The platform utilizes a modern, split-profile `.properties` configuration strategy for all Spring Boot services. It strictly avoids `.yml` files to align with properties-driven environment management.

Each microservice contains:
1. **`application.properties`**: Contains all common, environment-agnostic system values.
2. **`application-local.properties`** & **`application.local.properties`**: Contains local, developer-specific connection strings and credentials (e.g. databases, local Kafka bootstrap servers, ActiveMQ brokers).

### Running with active local profile:
When launching any Maven service locally, make sure to activate the `local` profile to merge these properties:
```bash
# Run with local profile activated
mvn spring-boot:run -pl :customer-service -Dspring.profiles.active=local
```

---

## 🚀 Local Build & Execution Guide

Follow these steps to compile the projects and run them locally.

### Prerequisites
*   **Java Runtime**: JDK 17 (standard modules) and JDK 21 (required for `underwriting-service`). Ensure your `JAVA_HOME` or active shell supports compilation using JDK 21.
*   **Build Tools**: Maven 3.8+ (Gradle is built internally in Docker; no local Gradle installation required)
*   **UI Tooling**: Node.js 18+ & npm
*   **Containerization**: Docker Desktop & Docker Compose

---

### Step 1: Build the Backend Modules
1. **Build Maven-managed modules**:
   Run the clean and package command on the root aggregator POM to build the 14 Java modules:
   ```bash
   # Navigate to the consolidated project folder
   cd freddie-loan-platform

   # Compile and package all Maven modules
   mvn clean package -DskipTests
   ```
   This builds fat-jars under their respective `target/` directories (e.g., `eureka-server/target/eureka-server-1.0.0-SNAPSHOT.jar`).

2. **Gradle-managed module (`notification-service`)**:
   - You **do not** need to build the Gradle project locally.
   - The `notification-service` is containerized using a multi-stage Dockerfile that compiles and packages the code internally inside the container during Step 2.


---

### Step 2: Spin Up Databases & Messaging Infrastructure
Use Docker Compose to launch all the datastores (PostgreSQL, MongoDB, Kafka, Zookeeper, ActiveMQ) and application services:
```bash
# Navigate to the deployment directory
cd deployment

# Start the stack (databases and application services)
docker compose up --build -d
```
*   **Eureka Discovery Dashboard**: Verify registration of all microservices by opening `http://localhost:8761` in your browser.
*   **Edge API Gateway**: Gateway route mapping begins at `http://localhost:8080`.

---

### Step 3: Run the Angular Frontend Portal
1. Navigate to the frontend directory:
   ```bash
   cd ../frontend
   ```
2. Install npm dependencies:
   ```bash
   npm install
   ```
3. Run the development server with local gateway proxying:
   ```bash
   npm run start
   ```
   *(This starts the Angular UI on `http://localhost:4200` and proxies all outgoing `/api/*` REST calls to the gateway running at `http://localhost:8080`)*.
4. Access the browser app at `http://localhost:4200`.

---

### Step 4: Run the JSP & Servlet Legacy Admin Portal (Optional)
The new Servlet and JSP-based admin dashboard can be run independently:
1. Navigate back to the root folder:
   ```bash
   cd ../
   ```
2. Run the legacy admin module using the Spring Boot Maven plugin:
   ```bash
   mvn spring-boot:run -pl :legacy-admin-portal
   ```
3. Open your browser and navigate to:
   👉 **`http://localhost:8089/admin/dashboard`**

---

### Step 5: Shutting Down the Stack
To tear down the container environments and clean volumes:
```bash
cd deployment
docker compose down -v
```

---

## 🧪 End-to-End Testing Guide (Role-Based Workspace Flow)

The platform is secured by a role-restricted entry point. Access to user spaces and tabs is governed by your active session.

### 👤 1. Authenticate at the Gateway
1.  Open the portal in your browser (`http://localhost:4200`).
2.  You will be presented with the **Freddie Mac Mortgage Gateway** login page.
3.  Select a user persona tab at the top of the card:
    *   **Borrower**: Pre-fills `borrower@freddiemac.com`
    *   **Underwriter**: Pre-fills `underwriter_09@freddiemac.com`
    *   **Loan Officer**: Pre-fills `lo_officer_01@freddiemac.com`
    *   **Ops Manager**: Pre-fills `ops_manager@freddiemac.com`
    *   **Compliance**: Pre-fills `compliance_officer@freddiemac.com`
4.  Click **Authenticate & Access Gateway**.

---

### 👤 2. Borrower (Customer) Testing Flow
1.  **Authenticate**: Log in as **Borrower** from the gateway.
2.  **Access Intake Form**: The sidebar menu displays the **Origination Intake** tab.
3.  **Fill Application Details**:
    *   *Borrower Name*: e.g., `Jane Doe`
    *   *Credit Score*: `720` (FICO check)
    *   *Requested Loan*: `240000`
    *   *Appraisal Value*: `300000` (Calculates to `80.00%` LTV)
    *   *Annual Income*: `120000`
    *   *Monthly Debt*: `2000` (Calculates to `20.00%` DTI)
4.  **Submit Application**: Click **Submit Application to Underwriting**. 
5.  **Track Status**: The application is securely registered, and you are redirected to the **My Application Status** tab. You will see a live **stepper tracker** (Intake Submitted ➔ Risk Assessment ➔ Decision Output) and a list of W-2 verification files uploaded to MongoDB GridFS.
6.  **Upload Documents**: Navigate to the **Documents (GridFS)** tab to upload W-2/payslip files.
7.  **Logout**: To test another role, click the **Logout button** (`[->`) at the bottom of the sidebar profile card to return to the gateway.

---

### 🛡️ 3. Admin (Underwriter & Loan Officer) Testing Flow
1.  **Authenticate**: Log in as **Underwriter** or **Loan Officer** from the gateway.
2.  **Verify Dashboard Restriction**: Notice that the global **Dashboard** containing portfolio analytics and intake queues is now visible (this was hidden from the Borrower).
3.  **Evaluate Automated Rules**:
    *   Click the **Underwriting Rules** tab.
    *   Select the application created by the Borrower and click **Evaluate**. The rules engine assesses the parameters and transitions the loan status.
4.  **Audit Manual Override**:
    *   For applications requiring review, use the **Manual Underwriter Review** panel.
    *   Set the decision to `APPROVED` or `REJECTED`, type a compliance remark (e.g., *"Strong cash buffers compensate for debt"*), and click **Apply Underwriting Override**.
5.  **Logout**: Click the **Logout button** to end your session.

---

### 🔎 4. Compliance & Auditing Flow
1.  **Authenticate**: Log in as **Compliance** or **Ops Manager** from the gateway.
2.  **Review JMS logs**: Click **Compliance & Audits** in the sidebar.
3.  **Audit Trace Console**: Monitor the live ActiveMQ audit traces, Kafka lifecycle messages, and HMDA statistics (LTV/DTI averages).
4.  **Logout**: Click the **Logout button** to return to the gateway.
