package com.freddieapp.loanorigination.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "LOAN_STATUS_HISTORY", schema = "FREDDIE_LOANS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanStatusHistory {

    @Id
    @Column(name = "HISTORY_ID", length = 36, updatable = false, nullable = false)
    private String historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LOAN_ID", nullable = false)
    private LoanApplication loanApplication;

    @Column(name = "FROM_STATUS", length = 30)
    private String fromStatus;

    @Column(name = "TO_STATUS", nullable = false, length = 30)
    private String toStatus;

    @CreationTimestamp
    @Column(name = "CHANGED_AT", updatable = false)
    private OffsetDateTime changedAt;

    @Column(name = "CHANGED_BY", length = 100)
    private String changedBy;

    @Column(name = "NOTES", length = 2000)
    private String notes;
}
