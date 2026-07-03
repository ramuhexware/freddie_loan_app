package com.freddieapp.appian.client;

import com.freddieapp.appian.dto.ClientDtos.UnderwritingResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "underwriting-service", path = "/api/v1/underwriting")
public interface UnderwritingServiceClient {

    @GetMapping("/loan/{loanId}")
    UnderwritingResponse getLatestAssessment(@PathVariable("loanId") String loanId);

    @PostMapping("/override/{assessmentId}")
    UnderwritingResponse overrideDecision(
            @PathVariable("assessmentId") String assessmentId,
            @RequestParam("decision") String decision,
            @RequestParam("reason") String reason,
            @RequestParam("underwriter") String underwriter);
}
