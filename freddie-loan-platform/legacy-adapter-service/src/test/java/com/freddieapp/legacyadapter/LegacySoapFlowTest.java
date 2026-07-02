package com.freddieapp.legacyadapter;

import com.freddieapp.legacyadapter.client.LegacySoapClient;
import com.freddieapp.legacyadapter.client.LegacySoapClient.LoanEligibilityResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {
    "server.port=9999",
    "legacy.soap.osb.endpoint.loan-eligibility=http://localhost:9999/ws/LegacySoapService",
    "legacy.soap.osb.endpoint.customer-verification=http://localhost:9999/ws/CustomerVerificationService",
    "legacy.soap.wss.password=secret-pass",
    "eureka.client.enabled=false"
})
public class LegacySoapFlowTest {

    @Autowired
    private LegacySoapClient legacySoapClient;

    @Test
    public void testAutoDeclineCreditScoreBelow600() {
        LoanEligibilityResult result = legacySoapClient.checkLoanEligibility(
                "L-101",
                "C-101",
                "John Doe",
                BigDecimal.valueOf(250000),
                BigDecimal.valueOf(300000),
                550, // Credit score < 600
                BigDecimal.valueOf(100000),
                BigDecimal.valueOf(1500)
        );

        assertFalse(result.isEligible());
        assertTrue(result.getReason().contains("FICO credit score below minimum"));
        assertEquals("Auto-Declined by Pre-Qual Gate", result.getBureauReference());
    }

    @Test
    public void testOrchestratedEligibilitySuccessHighCredit() {
        LoanEligibilityResult result = legacySoapClient.checkLoanEligibility(
                "L-102",
                "C-102",
                "Alice Smith",
                BigDecimal.valueOf(350000),
                BigDecimal.valueOf(450000),
                750, // High credit score (>= 740 -> interest rate 5.75)
                BigDecimal.valueOf(120000),
                BigDecimal.valueOf(2000)
        );

        assertTrue(result.isEligible());
        assertTrue(result.getReason().contains("underwriting criteria"));
        assertEquals("Orchestrated Successfully via Legacy SOAP", result.getBureauReference());
    }

    @Test
    public void testEligibilityDeclineExceedsLoanAmountLimit() {
        LoanEligibilityResult result = legacySoapClient.checkLoanEligibility(
                "L-103",
                "C-103",
                "Bob Jones",
                BigDecimal.valueOf(600000), // Exceeds $500,000 legacy limit
                BigDecimal.valueOf(800000),
                700,
                BigDecimal.valueOf(150000),
                BigDecimal.valueOf(2500)
        );

        assertFalse(result.isEligible());
        assertTrue(result.getReason().contains("exceeds legacy maximum limit"));
        assertEquals("Orchestrated Successfully via Legacy SOAP", result.getBureauReference());
    }

    @Test
    public void testEligibilityDeclineHighDti() {
        LoanEligibilityResult result = legacySoapClient.checkLoanEligibility(
                "L-104",
                "C-104",
                "Charlie Brown",
                BigDecimal.valueOf(250000),
                BigDecimal.valueOf(300000),
                700,
                BigDecimal.valueOf(50000), // Monthly income ~ 4166
                BigDecimal.valueOf(2000)   // Monthly debt ~ 2000 (DTI = 48%)
        );

        assertFalse(result.isEligible());
        assertTrue(result.getReason().contains("exceeds maximum limit"));
        assertEquals("Orchestrated Successfully via Legacy SOAP", result.getBureauReference());
    }
}
