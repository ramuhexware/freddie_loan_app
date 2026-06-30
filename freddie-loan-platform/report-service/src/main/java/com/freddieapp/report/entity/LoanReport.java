package com.freddieapp.report.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "loan_reports")
public class LoanReport {

    @Id
    @Column(name = "report_id")
    private String reportId;

    @Column(name = "report_type")
    private String reportType;

    @Column(name = "generated_at")
    private OffsetDateTime generatedAt;

    @Column(name = "total_loans_processed")
    private int totalLoansProcessed;

    @Column(name = "approved_loans_count")
    private int approvedLoansCount;

    @Column(name = "rejected_loans_count")
    private int rejectedLoansCount;

    @Column(name = "average_dti")
    private double averageDti;

    @Column(name = "average_ltv")
    private double averageLtv;

    @Column(name = "risk_distribution_json", length = 2048)
    private String riskDistributionJson;
}
