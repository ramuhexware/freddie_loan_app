package com.freddieapp.loanorigination.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDto {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String customerStatus;
    private String kycStatus;
}
