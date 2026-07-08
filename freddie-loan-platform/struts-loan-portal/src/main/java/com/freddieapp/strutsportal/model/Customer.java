package com.freddieapp.strutsportal.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Struts-layer model for a Customer.
 * Maps to FREDDIE_LOANS.CUSTOMERS in DB2.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    private String        customerId;
    private String        firstName;
    private String        lastName;
    private String        email;
    private String        phoneNumber;
    private String        ssn;               // masked on display
    private LocalDateTime dateOfBirth;
    private String        customerStatus;    // ACTIVE | INACTIVE | SUSPENDED
    private String        addressLine1;
    private String        addressLine2;
    private String        city;
    private String        state;
    private String        zipCode;
    private Integer       creditScore;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Derived: full name for display */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /** Derived: masked SSN e.g. ***-**-1234 */
    public String getMaskedSsn() {
        if (ssn == null || ssn.length() < 4) return "***-**-****";
        return "***-**-" + ssn.substring(ssn.length() - 4);
    }
}
