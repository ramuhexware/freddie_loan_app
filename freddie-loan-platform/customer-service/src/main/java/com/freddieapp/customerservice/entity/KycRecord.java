package com.freddieapp.customerservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "kyc_records", schema = "freddie_customer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "kyc_provider", length = 100)
    private String kycProvider;

    @Column(name = "kyc_reference", length = 255)
    private String kycReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", length = 20)
    private KycVerificationStatus kycStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 20)
    private RiskLevel riskLevel;

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    public enum KycVerificationStatus { SUBMITTED, IN_REVIEW, PASSED, FAILED, EXPIRED }
    public enum RiskLevel              { LOW, MEDIUM, HIGH, PROHIBITED }
}
