package com.freddieapp.strutsportal.dao;

import com.freddieapp.strutsportal.model.Customer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Data Access Object for Customers.
 * Executes DB2 queries against FREDDIE_LOANS.CUSTOMERS.
 *
 * DB2 Calls:
 *  1. findAllCustomers()           — SELECT all active customers
 *  2. findCustomerById()           — SELECT by PK
 *  3. searchCustomers()            — LIKE search on name/email
 *  4. countCustomersByStatus()     — COUNT GROUP BY STATUS
 *  5. findCustomersWithLoans()     — SELECT with loan count JOIN
 *  6. findHighRiskCustomers()      — SELECT WHERE credit_score < threshold
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class CustomerDao {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<Customer> CUSTOMER_ROW_MAPPER = (rs, rowNum) -> {
        Customer c = new Customer();
        c.setCustomerId(rs.getString("CUSTOMER_ID"));
        c.setFirstName(rs.getString("FIRST_NAME"));
        c.setLastName(rs.getString("LAST_NAME"));
        c.setEmail(rs.getString("EMAIL"));
        c.setPhoneNumber(rs.getString("PHONE_NUMBER"));
        c.setSsn(rs.getString("SSN"));
        c.setCustomerStatus(rs.getString("CUSTOMER_STATUS"));
        c.setAddressLine1(rs.getString("ADDRESS_LINE1"));
        c.setAddressLine2(rs.getString("ADDRESS_LINE2"));
        c.setCity(rs.getString("CITY"));
        c.setState(rs.getString("STATE"));
        c.setZipCode(rs.getString("ZIP_CODE"));
        c.setCreditScore(rs.getInt("CREDIT_SCORE") == 0 ? null : rs.getInt("CREDIT_SCORE"));
        Timestamp dob = rs.getTimestamp("DATE_OF_BIRTH");
        if (dob != null) c.setDateOfBirth(dob.toLocalDateTime());
        Timestamp createdAt = rs.getTimestamp("CREATED_AT");
        if (createdAt != null) c.setCreatedAt(createdAt.toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("UPDATED_AT");
        if (updatedAt != null) c.setUpdatedAt(updatedAt.toLocalDateTime());
        return c;
    };

    // ================================================================== //
    //  DB2 CALL #1 — Find all customers ordered by name                  //
    // ================================================================== //
    public List<Customer> findAllCustomers() {
        log.debug("[DB2-CALL-1] SELECT all customers");
        String sql = """
                SELECT CUSTOMER_ID, FIRST_NAME, LAST_NAME, EMAIL, PHONE_NUMBER,
                       SSN, DATE_OF_BIRTH, CUSTOMER_STATUS, ADDRESS_LINE1, ADDRESS_LINE2,
                       CITY, STATE, ZIP_CODE, CREDIT_SCORE, CREATED_AT, UPDATED_AT
                FROM   FREDDIE_LOANS.CUSTOMERS
                ORDER BY LAST_NAME, FIRST_NAME
                """;
        return jdbcTemplate.query(sql, CUSTOMER_ROW_MAPPER);
    }

    // ================================================================== //
    //  DB2 CALL #2 — Find customer by primary key                        //
    // ================================================================== //
    public Optional<Customer> findCustomerById(String customerId) {
        log.debug("[DB2-CALL-2] SELECT customer by CUSTOMER_ID={}", customerId);
        String sql = """
                SELECT CUSTOMER_ID, FIRST_NAME, LAST_NAME, EMAIL, PHONE_NUMBER,
                       SSN, DATE_OF_BIRTH, CUSTOMER_STATUS, ADDRESS_LINE1, ADDRESS_LINE2,
                       CITY, STATE, ZIP_CODE, CREDIT_SCORE, CREATED_AT, UPDATED_AT
                FROM   FREDDIE_LOANS.CUSTOMERS
                WHERE  CUSTOMER_ID = ?
                """;
        List<Customer> results = jdbcTemplate.query(sql, CUSTOMER_ROW_MAPPER, customerId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    // ================================================================== //
    //  DB2 CALL #3 — LIKE search on name, email, city                    //
    // ================================================================== //
    public List<Customer> searchCustomers(String searchTerm) {
        log.debug("[DB2-CALL-3] LIKE search for '{}'", searchTerm);
        String likeTerm = "%" + searchTerm.toUpperCase() + "%";
        String sql = """
                SELECT CUSTOMER_ID, FIRST_NAME, LAST_NAME, EMAIL, PHONE_NUMBER,
                       SSN, DATE_OF_BIRTH, CUSTOMER_STATUS, ADDRESS_LINE1, ADDRESS_LINE2,
                       CITY, STATE, ZIP_CODE, CREDIT_SCORE, CREATED_AT, UPDATED_AT
                FROM   FREDDIE_LOANS.CUSTOMERS
                WHERE  UPPER(FIRST_NAME)  LIKE ?
                    OR UPPER(LAST_NAME)   LIKE ?
                    OR UPPER(EMAIL)       LIKE ?
                    OR UPPER(CITY)        LIKE ?
                ORDER BY LAST_NAME, FIRST_NAME
                """;
        return jdbcTemplate.query(sql, CUSTOMER_ROW_MAPPER,
                likeTerm, likeTerm, likeTerm, likeTerm);
    }

    // ================================================================== //
    //  DB2 CALL #4 — COUNT customers grouped by status                   //
    // ================================================================== //
    public List<Map<String, Object>> countCustomersByStatus() {
        log.debug("[DB2-CALL-4] COUNT customers GROUP BY CUSTOMER_STATUS");
        String sql = """
                SELECT CUSTOMER_STATUS, COUNT(*) AS CUSTOMER_COUNT
                FROM   FREDDIE_LOANS.CUSTOMERS
                GROUP BY CUSTOMER_STATUS
                ORDER BY CUSTOMER_COUNT DESC
                """;
        return jdbcTemplate.queryForList(sql);
    }

    // ================================================================== //
    //  DB2 CALL #5 — Customers with their total loan count (JOIN)        //
    // ================================================================== //
    public List<Map<String, Object>> findCustomersWithLoanCount() {
        log.debug("[DB2-CALL-5] SELECT customers with loan COUNT JOIN");
        String sql = """
                SELECT c.CUSTOMER_ID,
                       TRIM(c.FIRST_NAME) || ' ' || TRIM(c.LAST_NAME) AS CUSTOMER_NAME,
                       c.EMAIL, c.CUSTOMER_STATUS, c.CREDIT_SCORE,
                       COUNT(la.LOAN_ID) AS LOAN_COUNT,
                       SUM(la.LOAN_AMOUNT) AS TOTAL_LOAN_AMOUNT
                FROM   FREDDIE_LOANS.CUSTOMERS c
                LEFT JOIN FREDDIE_LOANS.LOAN_APPLICATIONS la ON c.CUSTOMER_ID = la.CUSTOMER_ID
                GROUP BY c.CUSTOMER_ID, c.FIRST_NAME, c.LAST_NAME, c.EMAIL,
                         c.CUSTOMER_STATUS, c.CREDIT_SCORE
                ORDER BY LOAN_COUNT DESC
                """;
        return jdbcTemplate.queryForList(sql);
    }

    // ================================================================== //
    //  DB2 CALL #6 — High-risk customers (low credit score)              //
    // ================================================================== //
    public List<Customer> findHighRiskCustomers(int creditScoreThreshold) {
        log.debug("[DB2-CALL-6] SELECT high-risk customers with credit_score < {}", creditScoreThreshold);
        String sql = """
                SELECT CUSTOMER_ID, FIRST_NAME, LAST_NAME, EMAIL, PHONE_NUMBER,
                       SSN, DATE_OF_BIRTH, CUSTOMER_STATUS, ADDRESS_LINE1, ADDRESS_LINE2,
                       CITY, STATE, ZIP_CODE, CREDIT_SCORE, CREATED_AT, UPDATED_AT
                FROM   FREDDIE_LOANS.CUSTOMERS
                WHERE  CREDIT_SCORE < ?
                  AND  CUSTOMER_STATUS = 'ACTIVE'
                ORDER BY CREDIT_SCORE ASC
                """;
        return jdbcTemplate.query(sql, CUSTOMER_ROW_MAPPER, creditScoreThreshold);
    }
}
