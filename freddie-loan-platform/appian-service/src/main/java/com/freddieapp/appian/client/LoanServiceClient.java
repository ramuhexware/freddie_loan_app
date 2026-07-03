package com.freddieapp.appian.client;

import com.freddieapp.appian.dto.ClientDtos.CustomPageResponse;
import com.freddieapp.appian.dto.ClientDtos.LoanResponse;
import com.freddieapp.appian.dto.ClientDtos.LoanApplicationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "loan-origination-service", path = "/api/v1/loans")
public interface LoanServiceClient {

    @GetMapping
    CustomPageResponse<LoanResponse> getAllLoans(
            @RequestParam("page") int page,
            @RequestParam("size") int size);

    @GetMapping("/{loanId}")
    LoanResponse getLoanById(@PathVariable("loanId") String loanId);

    @PostMapping
    LoanResponse submitLoanApplication(@RequestBody LoanApplicationRequest request);

    @PostMapping("/{loanId}/submit-for-underwriting")
    LoanResponse submitForUnderwriting(@PathVariable("loanId") String loanId);
}
