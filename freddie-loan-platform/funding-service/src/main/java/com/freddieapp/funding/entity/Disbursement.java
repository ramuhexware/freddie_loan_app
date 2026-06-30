package com.freddieapp.funding.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "DISBURSEMENTS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Disbursement {

    @Id
    @Column(name = "DISBURSEMENT_ID", length = 36)
    private String disbursementId;

    @Column(name = "LOAN_ID", nullable = false, length = 36)
    private String loanId;

    @Column(name = "BORROWER_ID", nullable = false, length = 36)
    private String borrowerId;

    @Column(name = "DISBURSEMENT_AMOUNT", nullable = false, precision = 18, scale = 2)
    private BigDecimal disbursementAmount;

    @Column(name = "BANK_NAME", nullable = false, length = 200)
    private String bankName;

    @Column(name = "ACCOUNT_NUMBER", nullable = false, length = 50)
    private String accountNumber;

    @Column(name = "ROUTING_NUMBER", nullable = false, length = 50)
    private String routingNumber;

    @Column(name = "DISBURSEMENT_DATE")
    private LocalDate disbursementDate;

    @Column(name = "STATUS", nullable = false, length = 30)
    private String status; // PENDING, IN_TRANSIT, COMPLETED, FAILED
}
