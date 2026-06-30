package com.freddieapp.legacyportal.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(name = "AdminDashboardServlet", urlPatterns = "/admin/dashboard")
public class AdminDashboardServlet extends HttpServlet {

    @Autowired
    private RestTemplate restTemplate;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        List<Map<String, Object>> loanApplications = new ArrayList<>();
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCustomers", 5);
        stats.put("activeLoans", 3);
        stats.put("underwrittenCount", 4);
        stats.put("systemHealth", "UP");

        try {
            Map<String, Object> app1 = new HashMap<>();
            app1.put("loanId", "LOAN-98A7B2C1");
            app1.put("customerId", "00000000-0000-0000-0000-000000000001");
            app1.put("borrowerName", "James Harrison");
            app1.put("loanType", "PURCHASE");
            app1.put("amount", 450000.00);
            app1.put("status", "APPROVED");

            Map<String, Object> app2 = new HashMap<>();
            app2.put("loanId", "LOAN-34F5E6D7");
            app2.put("customerId", "00000000-0000-0000-0000-000000000002");
            app2.put("borrowerName", "Sofia Martinez");
            app2.put("loanType", "REFINANCE");
            app2.put("amount", 280000.00);
            app2.put("status", "UNDER_REVIEW");

            Map<String, Object> app3 = new HashMap<>();
            app3.put("loanId", "LOAN-12C3D4E5");
            app3.put("customerId", "00000000-0000-0000-0000-000000000003");
            app3.put("borrowerName", "Michael Chen");
            app3.put("loanType", "HELOC");
            app3.put("amount", 100000.00);
            app3.put("status", "SUBMITTED");

            loanApplications.add(app1);
            loanApplications.add(app2);
            loanApplications.add(app3);
        } catch (Exception ex) {
            // Keep default mock data
        }

        // --- Interactive LLPA Pricing & Amortization Calculator Logic ---
        String loanAmtStr = request.getParameter("calcLoanAmount");
        String propValStr = request.getParameter("calcPropertyValue");
        String ficoStr = request.getParameter("calcFicoScore");

        if (loanAmtStr != null && propValStr != null && ficoStr != null) {
            try {
                double loanAmount = Double.parseDouble(loanAmtStr);
                double propertyValue = Double.parseDouble(propValStr);
                int creditScore = Integer.parseInt(ficoStr);

                double ltv = (loanAmount / propertyValue) * 100.0;

                // LLPA Fee Matrix
                double llpa = 0.0;
                if (creditScore >= 740) {
                    if (ltv <= 60.0) llpa = 0.0;
                    else if (ltv <= 80.0) llpa = 0.25;
                    else llpa = 0.50;
                } else if (creditScore >= 680) {
                    if (ltv <= 60.0) llpa = 0.50;
                    else if (ltv <= 80.0) llpa = 0.75;
                    else llpa = 1.25;
                } else if (creditScore >= 620) {
                    if (ltv <= 60.0) llpa = 1.00;
                    else if (ltv <= 80.0) llpa = 1.75;
                    else llpa = 2.25;
                } else {
                    if (ltv <= 60.0) llpa = 1.50;
                    else if (ltv <= 80.0) llpa = 2.50;
                    else llpa = 3.25;
                }

                // PMI Surcharge if LTV > 80%
                double pmiMonthly = 0.0;
                if (ltv > 80.0) {
                    double pmiAnnualRate = 0.005;
                    if (creditScore < 680) pmiAnnualRate = 0.011;
                    else if (creditScore < 740) pmiAnnualRate = 0.0075;
                    pmiMonthly = (loanAmount * pmiAnnualRate) / 12.0;
                }

                double baseRate = 6.50;
                double adjustedRate = baseRate + llpa;

                // 30-Year Amortization Schedule (first 36 months preview)
                List<Map<String, Object>> schedule = new ArrayList<>();
                double monthlyRate = adjustedRate / 100.0 / 12.0;
                int totalMonths = 360;
                double monthlyBasePayment = 0.0;
                
                if (monthlyRate > 0) {
                    monthlyBasePayment = loanAmount * (monthlyRate * Math.pow(1 + monthlyRate, totalMonths)) / (Math.pow(1 + monthlyRate, totalMonths) - 1);
                } else {
                    monthlyBasePayment = loanAmount / totalMonths;
                }

                double remainingBalance = loanAmount;
                for (int m = 1; m <= totalMonths; m++) {
                    double interestPaid = remainingBalance * monthlyRate;
                    double principalPaid = monthlyBasePayment - interestPaid;
                    if (remainingBalance < principalPaid) {
                        principalPaid = remainingBalance;
                        interestPaid = 0.0;
                    }

                    double currentLtv = (remainingBalance / propertyValue) * 100.0;
                    double currentPmi = (currentLtv > 80.0) ? pmiMonthly : 0.0;
                    remainingBalance -= principalPaid;
                    if (remainingBalance < 0) remainingBalance = 0;

                    if (m <= 36 || remainingBalance <= 0) {
                        Map<String, Object> payment = new HashMap<>();
                        payment.put("monthNumber", m);
                        payment.put("principalPaid", principalPaid);
                        payment.put("interestPaid", interestPaid);
                        payment.put("pmiPaid", currentPmi);
                        payment.put("totalPayment", principalPaid + interestPaid + currentPmi);
                        payment.put("remainingPrincipal", remainingBalance);
                        schedule.add(payment);
                    }
                    if (remainingBalance <= 0) break;
                }

                request.setAttribute("calcLtv", ltv);
                request.setAttribute("calcLlpa", llpa);
                request.setAttribute("calcPmi", pmiMonthly);
                request.setAttribute("calcAdjustedRate", adjustedRate);
                request.setAttribute("calcSchedule", schedule);
                request.setAttribute("showCalculatorResults", true);

            } catch (Exception ex) {
                request.setAttribute("calcError", "Invalid numerical inputs. Please check your data.");
            }
        }

        request.setAttribute("stats", stats);
        request.setAttribute("applications", loanApplications);

        request.getRequestDispatcher("/WEB-INF/jsp/dashboard.jsp").forward(request, response);
    }
}
