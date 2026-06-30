package com.freddieapp.report.client;

import com.freddieapp.report.dto.ClientDtos.CustomPageResponse;
import com.freddieapp.report.dto.ClientDtos.LoanResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "loan-origination-service", path = "/api/v1/loans")
public interface LoanServiceClient {

    @GetMapping
    CustomPageResponse<LoanResponse> getAllLoans(
            @RequestParam("page") int page,
            @RequestParam("size") int size);
}
