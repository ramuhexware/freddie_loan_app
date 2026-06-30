package com.freddieapp.report.client;

import com.freddieapp.report.dto.ClientDtos.CustomPageResponse;
import com.freddieapp.report.dto.ClientDtos.UnderwritingResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "underwriting-service", path = "/api/v1/underwriting")
public interface UnderwritingServiceClient {

    @GetMapping
    CustomPageResponse<UnderwritingResponse> getAllAssessments(
            @RequestParam("page") int page,
            @RequestParam("size") int size);
}
