package com.freddieapp.loanorigination.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "LOAN_APPLICATIONS", schema = "FREDDIE_LOANS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanApplication {

    @Id
    @Column(name = "LOAN_ID", length = 36, updatable = false, nullable = false)
    private String loanId;

    @Column(name = "CUSTOMER_ID", nullable = false, length = 36)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "LOAN_TYPE", nullable = false, length = 50)
    private LoanType loanType;

    @Column(name = "LOAN_AMOUNT", nullable = false, precision = 18, scale = 2)
    private BigDecimal loanAmount;

    @Column(name = "PROPERTY_VALUE", precision = 18, scale = 2)
    private BigDecimal propertyValue;

    @Column(name = "PROPERTY_ADDRESS", length = 500)
    private String propertyAddress;

    @Column(name = "INTEREST_RATE", precision = 6, scale = 4)
    private BigDecimal interestRate;

    @Column(name = "LOAN_TERM_MONTHS")
    private Integer loanTermMonths;

    @Enumerated(EnumType.STRING)
    @Column(name = "LOAN_STATUS", length = 30)
    private LoanStatus loanStatus = LoanStatus.PENDING;

    @CreationTimestamp
    @Column(name = "APPLICATION_DATE", updatable = false)
    private OffsetDateTime applicationDate;

    @Column(name = "DECISION_DATE")
    private OffsetDateTime decisionDate;

    @Column(name = "DISBURSEMENT_DATE")
    private OffsetDateTime disbursementDate;

    @Column(name = "APPROVED_AMOUNT", precision = 18, scale = 2)
    private BigDecimal approvedAmount;

    @Column(name = "REJECTION_REASON", length = 1000)
    private String rejectionReason;

    @Column(name = "CREATED_BY", length = 100)
    private String createdBy;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT")
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "VERSION")
    private Long version;

    @OneToMany(mappedBy = "loanApplication", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<LoanStatusHistory> statusHistory = new ArrayList<>();

    public enum LoanType   { PURCHASE, REFINANCE, HELOC, HOME_EQUITY }
    public enum LoanStatus { PENDING, SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED, DISBURSED, CLOSED }
}
