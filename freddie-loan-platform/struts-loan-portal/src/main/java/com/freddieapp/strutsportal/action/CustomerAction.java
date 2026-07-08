package com.freddieapp.strutsportal.action;

import com.freddieapp.strutsportal.client.CustomerServiceClient;
import com.freddieapp.strutsportal.dao.CustomerDao;
import com.freddieapp.strutsportal.model.Customer;
import com.opensymphony.xwork2.ActionSupport;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Struts 2 Action for Customer screens.
 *
 * Methods:
 *  - list()         — load all customers from DB2
 *  - detail()       — load single customer from DB2 + REST for full profile
 *  - search()       — DB2 LIKE search on name/email/city
 *  - statsSummary() — DB2 COUNT by status + customers with loan count JOIN
 */
@Slf4j
@Getter
@Setter
public class CustomerAction extends ActionSupport {

    @Autowired private CustomerDao           customerDao;
    @Autowired private CustomerServiceClient customerServiceClient;

    // ---- Form parameters ----
    private String customerId;
    private String searchTerm;

    // ---- View data ----
    private List<Customer>           customers             = new ArrayList<>();
    private Customer                 customer;
    private List<Map<String, Object>> customerStatusCounts = new ArrayList<>();
    private List<Map<String, Object>> customersWithLoans  = new ArrayList<>();
    private List<Map<String, Object>> highRiskCustomers   = new ArrayList<>();
    private Map<String, Object>       serviceCustomer;

    // ================================================================== //
    //  list() — DB2 SELECT all customers                                  //
    // ================================================================== //
    public String list() {
        log.info("[CustomerAction.list] Loading all customers from DB2");
        // DB2 CALL
        customers = customerDao.findAllCustomers();
        log.info("[CustomerAction.list] Loaded {} customers", customers.size());
        return SUCCESS;
    }

    // ================================================================== //
    //  detail() — DB2 customer by ID + REST for full profile              //
    // ================================================================== //
    public String detail() {
        log.info("[CustomerAction.detail] customerId={}", customerId);

        // DB2 CALL — primary fetch
        customer = customerDao.findCustomerById(customerId).orElse(null);

        if (customer == null) {
            addActionError("Customer not found: " + customerId);
            return "notFound";
        }

        // REST CALL → customer-service for authoritative full profile
        serviceCustomer = customerServiceClient.getCustomerById(customerId);
        log.info("[CustomerAction.detail] Loaded customer {} from DB2 + REST", customerId);
        return SUCCESS;
    }

    // ================================================================== //
    //  search() — DB2 LIKE search                                         //
    // ================================================================== //
    public String search() {
        log.info("[CustomerAction.search] searchTerm='{}'", searchTerm);

        if (StringUtils.isBlank(searchTerm)) {
            addActionError("Please enter a search term");
            return INPUT;
        }

        // DB2 CALL — LIKE search
        customers = customerDao.searchCustomers(searchTerm);

        // Supplement with REST search
        // (REST results take precedence for fresh data)
        List<Map<String, Object>> restResults = customerServiceClient.searchCustomers(searchTerm);
        log.info("[CustomerAction.search] DB2 returned {} | REST returned {} results",
                customers.size(), restResults.size());

        return SUCCESS;
    }

    // ================================================================== //
    //  statsSummary() — DB2 COUNT by status + customers with loans JOIN   //
    // ================================================================== //
    public String statsSummary() {
        log.info("[CustomerAction.statsSummary] Loading customer statistics from DB2");

        // DB2 CALL — status counts
        customerStatusCounts = customerDao.countCustomersByStatus();

        // DB2 CALL — customer loan counts (JOIN)
        customersWithLoans = customerDao.findCustomersWithLoanCount();

        // DB2 CALL — high-risk customers (credit < 620)
        highRiskCustomers = customerDao.findHighRiskCustomers(620)
                .stream()
                .map(c -> Map.<String, Object>of(
                        "customerId",   c.getCustomerId(),
                        "name",         c.getFullName(),
                        "creditScore",  c.getCreditScore(),
                        "status",       c.getCustomerStatus()
                ))
                .toList();

        log.info("[CustomerAction.statsSummary] Stats loaded — {} status groups, {} with loans, {} high-risk",
                customerStatusCounts.size(), customersWithLoans.size(), highRiskCustomers.size());
        return SUCCESS;
    }
}
