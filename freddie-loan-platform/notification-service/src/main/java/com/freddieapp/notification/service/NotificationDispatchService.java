package com.freddieapp.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatchService {

    private final JavaMailSender mailSender;

    public void dispatch(String eventType, String customerId, String loanId, Map<String, Object> event) {
        switch (eventType) {
            case "LOAN_APPLICATION_CREATED" -> sendLoanCreatedNotification(customerId, loanId);
            case "LOAN_APPROVED"            -> sendLoanApprovedNotification(customerId, loanId);
            case "LOAN_REJECTED"            -> sendLoanRejectedNotification(customerId, loanId,
                                                     (String) event.getOrDefault("reason", ""));
            case "LOAN_DISBURSED"           -> sendLoanDisbursedNotification(customerId, loanId);
            default -> log.warn("No notification handler for eventType={}", eventType);
        }
    }

    private void sendLoanCreatedNotification(String customerId, String loanId) {
        log.info("Sending LOAN_CREATED email to customerId={}", customerId);
        // TODO: resolve customer email from customer-service
        sendEmail("customer@example.com",
                "Loan Application Received",
                "Your loan application " + loanId + " has been received and is under review.");
    }

    private void sendLoanApprovedNotification(String customerId, String loanId) {
        log.info("Sending LOAN_APPROVED email to customerId={}", customerId);
        sendEmail("customer@example.com",
                "Congratulations! Your Loan is Approved",
                "Loan ID: " + loanId + " has been approved. Our team will contact you shortly.");
    }

    private void sendLoanRejectedNotification(String customerId, String loanId, String reason) {
        log.info("Sending LOAN_REJECTED email to customerId={}", customerId);
        sendEmail("customer@example.com",
                "Loan Application Update",
                "Loan ID: " + loanId + " could not be approved. Reason: " + reason);
    }

    private void sendLoanDisbursedNotification(String customerId, String loanId) {
        log.info("Sending LOAN_DISBURSED email to customerId={}", customerId);
        sendEmail("customer@example.com",
                "Loan Disbursed",
                "Your loan " + loanId + " has been disbursed. Please check your account.");
    }

    private void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        message.setFrom("noreply@freddieapp.com");
        mailSender.send(message);
        log.info("Email sent to={} subject={}", to, subject);
    }
}
