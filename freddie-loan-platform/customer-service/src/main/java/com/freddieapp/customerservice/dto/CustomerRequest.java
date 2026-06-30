package com.freddieapp.customerservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 100)
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Must be a valid phone number")
    private String phone;

    @NotBlank(message = "SSN is required")
    private String ssn; // Raw SSN — encrypted before persistence

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    @Size(max = 3)
    private String nationality;

    @Size(max = 255)
    private String addressLine1;

    @Size(max = 255)
    private String addressLine2;

    @Size(max = 100)
    private String city;

    @Size(max = 50)
    private String state;

    @Pattern(regexp = "^\\d{5}(-\\d{4})?$", message = "Must be a valid US ZIP code")
    private String zipCode;
}
