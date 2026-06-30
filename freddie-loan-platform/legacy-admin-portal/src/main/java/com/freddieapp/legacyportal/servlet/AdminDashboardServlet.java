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

        request.setAttribute("stats", stats);
        request.setAttribute("applications", loanApplications);

        request.getRequestDispatcher("/WEB-INF/jsp/dashboard.jsp").forward(request, response);
    }
}
