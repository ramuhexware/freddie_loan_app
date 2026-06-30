package com.freddieapp.customerservice.repository;

import com.freddieapp.customerservice.entity.Customer;
import com.freddieapp.customerservice.entity.Customer.CustomerStatus;
import com.freddieapp.customerservice.entity.Customer.KycStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID>,
        JpaSpecificationExecutor<Customer> {

    // ─── Native SELECT Queries (PostgreSQL) ──────────────────────────────────

    /**
     * Looks up an active customer by email using an exact match on the indexed
     * email column. Uses the partial index: WHERE customer_status = 'ACTIVE'.
     */
    @Query(value = """
            SELECT * FROM freddie_customer.customers
            WHERE email = :email
              AND customer_status = :#{#status.name()}
            LIMIT 1
            """,
            nativeQuery = true)
    Optional<Customer> findByEmailAndCustomerStatus(
            @Param("email") String email,
            @Param("status") CustomerStatus status);

    /**
     * Efficient existence check using a covering index on email.
     * Avoids full row fetch.
     */
    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM freddie_customer.customers
                WHERE email = :email
            )
            """,
            nativeQuery = true)
    boolean existsByEmail(@Param("email") String email);

    /**
     * Paginated fetch of customers by KYC status.
     * Uses ORDER BY created_at DESC so newest records come first.
     *
     * Note: Spring Data handles LIMIT/OFFSET via Pageable automatically
     * when nativeQuery = true and a countQuery is provided.
     */
    @Query(value = """
            SELECT * FROM freddie_customer.customers
            WHERE kyc_status = :#{#kycStatus.name()}
            ORDER BY created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM freddie_customer.customers
            WHERE kyc_status = :#{#kycStatus.name()}
            """,
            nativeQuery = true)
    Page<Customer> findByKycStatus(@Param("kycStatus") KycStatus kycStatus, Pageable pageable);

    /**
     * Full-text search across first_name, last_name, and email columns.
     * Uses PostgreSQL ILIKE for case-insensitive partial matching.
     * All filter parameters are optional (NULL bypasses that condition).
     */
    @Query(value = """
            SELECT * FROM freddie_customer.customers
            WHERE (:name    IS NULL OR CONCAT(first_name, ' ', last_name) ILIKE CONCAT('%', CAST(:name AS TEXT), '%'))
              AND (:email   IS NULL OR email       ILIKE CONCAT('%', CAST(:email AS TEXT), '%'))
              AND (:kycStatus IS NULL OR kyc_status = CAST(:kycStatus AS TEXT))
            ORDER BY created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM freddie_customer.customers
            WHERE (:name    IS NULL OR CONCAT(first_name, ' ', last_name) ILIKE CONCAT('%', CAST(:name AS TEXT), '%'))
              AND (:email   IS NULL OR email       ILIKE CONCAT('%', CAST(:email AS TEXT), '%'))
              AND (:kycStatus IS NULL OR kyc_status = CAST(:kycStatus AS TEXT))
            """,
            nativeQuery = true)
    Page<Customer> searchCustomers(
            @Param("name") String name,
            @Param("email") String email,
            @Param("kycStatus") String kycStatus,
            Pageable pageable);

    /**
     * Aggregate count grouped by KYC status — used for dashboard reporting.
     * Returns a native projection of [kyc_status, count].
     */
    @Query(value = """
            SELECT kyc_status, COUNT(*) AS total
            FROM freddie_customer.customers
            GROUP BY kyc_status
            ORDER BY total DESC
            """,
            nativeQuery = true)
    List<Object[]> countGroupedByKycStatus();

    /**
     * Single count for a specific KYC status value.
     */
    @Query(value = """
            SELECT COUNT(*) FROM freddie_customer.customers
            WHERE kyc_status = :#{#status.name()}
            """,
            nativeQuery = true)
    long countByKycStatus(@Param("status") KycStatus status);

    /**
     * Retrieves all customers whose KYC has expired (expiry_date < now)
     * and are still marked VERIFIED. Scheduled job uses this for batch re-verification.
     */
    @Query(value = """
            SELECT c.* FROM freddie_customer.customers c
            INNER JOIN freddie_customer.kyc_records k ON k.customer_id = c.id
            WHERE c.kyc_status = 'VERIFIED'
              AND k.expiry_date < CURRENT_DATE
              AND k.kyc_status  = 'PASSED'
            ORDER BY k.expiry_date ASC
            """,
            nativeQuery = true)
    List<Customer> findCustomersWithExpiredKyc();

    /**
     * Returns the most recent active customers (created in the last N days).
     * Used for new customer onboarding reports.
     */
    @Query(value = """
            SELECT * FROM freddie_customer.customers
            WHERE customer_status = 'ACTIVE'
              AND created_at >= NOW() - INTERVAL '1 day' * :days
            ORDER BY created_at DESC
            """,
            nativeQuery = true)
    List<Customer> findRecentlyCreatedCustomers(@Param("days") int days);

    // ─── Native UPDATE / DML Queries ─────────────────────────────────────────

    /**
     * Bulk status update — marks a customer as INACTIVE.
     * @Modifying + @Transactional ensures the UPDATE is flushed and the
     * first-level cache is cleared after execution.
     */
    @Modifying
    @Transactional
    @Query(value = """
            UPDATE freddie_customer.customers
               SET customer_status = 'INACTIVE',
                   updated_at      = NOW()
             WHERE id = :customerId
            """,
            nativeQuery = true)
    int deactivateCustomer(@Param("customerId") UUID customerId);

    /**
     * Updates only the KYC status and updated_at timestamp atomically.
     * Used by the KYC verification callback after external provider response.
     */
    @Modifying
    @Transactional
    @Query(value = """
            UPDATE freddie_customer.customers
               SET kyc_status  = :#{#kycStatus.name()},
                   updated_at  = NOW()
             WHERE id = :customerId
            """,
            nativeQuery = true)
    int updateKycStatus(
            @Param("customerId") UUID customerId,
            @Param("kycStatus") KycStatus kycStatus);

    /**
     * Soft-bulk deactivation for customers inactive since a given date.
     * Used by a scheduled compliance sweep job.
     */
    @Modifying
    @Transactional
    @Query(value = """
            UPDATE freddie_customer.customers
               SET customer_status = 'INACTIVE',
                   updated_at      = NOW()
             WHERE customer_status = 'ACTIVE'
               AND updated_at < :cutoffDate
            """,
            nativeQuery = true)
    int bulkDeactivateInactiveCustomers(@Param("cutoffDate") OffsetDateTime cutoffDate);
}
