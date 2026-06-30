package com.freddieapp.appraisal.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "APPRAISALS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appraisal {

    @Id
    @Column(name = "APPRAISAL_ID", length = 36)
    private String appraisalId;

    @Column(name = "LOAN_ID", nullable = false, length = 36)
    private String loanId;

    @Column(name = "PROPERTY_ADDRESS", nullable = false, length = 500)
    private String propertyAddress;

    @Column(name = "APPRAISER_NAME", length = 100)
    private String appraiserName;

    @Column(name = "APPRAISED_VALUE", precision = 18, scale = 2)
    private BigDecimal appraisedValue;

    @Column(name = "APPRAISAL_DATE")
    private LocalDate appraisalDate;

    @Column(name = "STATUS", nullable = false, length = 30)
    private String status; // SCHEDULED, COMPLETED, CERTIFIED
}
