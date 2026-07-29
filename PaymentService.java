package com.airline.payment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * All the business-logic classes for Use Case 3 (Payment System) live here:
 * the gateway simulation, the fraud screening, and the core PaymentService
 * that ties them together (validate -> authorize -> fraud check -> confirm),
 * plus split payment and refund handling. Only PaymentService is public.
 */
public class PaymentService {

    private static final Pattern CARD_NUMBER_PATTERN = Pattern.compile("^\\d{13,19}$");
    private static final Pattern EXPIRY_PATTERN = Pattern.compile("^(0[1-9]|1[0-2])/\\d{2}$");
    private static final Pattern CVV_PATTERN = Pattern.compile("^\\d{3,4}$");

    private final PaymentGateway gateway;
    private final FraudDetectionService fraudDetectionService;
    private final List<String> auditLog = new ArrayList<>();

    public PaymentService(PaymentGateway gateway, FraudDetectionService fraudDetectionService) {
        this.gateway = gateway;
        this.fraudDetectionService = fraudDetectionService;
    }

    /**
     * Normal Flow: validates details, authorizes via gateway, runs fraud
     * check, and finalizes the payment. Throws PaymentException for
     * Exception 1.0 (invalid payment details).
     */
    public Payment processPayment(String pnr, BigDecimal amount, PaymentMethod method,
                                   String cardNumber, String expiry, String cvv,
                                   String cardHolder) throws PaymentException {

        validateCardDetails(method, cardNumber, expiry, cvv, cardHolder);

        Payment payment = new Payment(pnr, amount, method, cardNumber);
        logAudit("Payment initiated: " + payment.getTransactionRef() + " for PNR " + pnr);

        PaymentGateway.GatewayResponse response = gateway.authorize(payment);

        if (response.getStatus() == PaymentStatus.PENDING) {
            // Exception 4.0: Gateway timeout - do not double-charge, mark pending
            payment.setStatus(PaymentStatus.PENDING);
            logAudit("Gateway timeout for " + payment.getTransactionRef() + ". Marked PENDING.");
            return payment;
        }

        if (response.getStatus() == PaymentStatus.DECLINED) {
            // Exception 2.0: Payment declined
            payment.setStatus(PaymentStatus.DECLINED);
            payment.setDeclineReason(response.getMessage());
            logAudit("Payment declined for " + payment.getTransactionRef() + ": " + response.getMessage());
            return payment;
        }

        // Authorized - now run fraud screening before finalizing
        if (fraudDetectionService.isHighRisk(payment)) {
            // Exception 3.0: Fraud check failure - hold for manual review, do not issue ticket
            payment.setStatus(PaymentStatus.FRAUD_REVIEW);
            logAudit("Payment " + payment.getTransactionRef() + " flagged for manual fraud review.");
            return payment;
        }

        // Success path
        payment.setStatus(PaymentStatus.APPROVED);
        logAudit("Payment " + payment.getTransactionRef() + " approved. Ticket issuance triggered.");
        return payment;
    }

    /**
     * Alternative Flow 2.0: Split payment across two or more methods.
     * Each split is processed as its own authorized sub-transaction; all
     * must be approved for the reservation to move to 'Paid'.
     */
    public List<Payment> processSplitPayment(String pnr, List<BigDecimal> amounts,
                                              List<PaymentMethod> methods,
                                              List<String> cardNumbers,
                                              List<String> expiries,
                                              List<String> cvvs,
                                              List<String> cardHolders) throws PaymentException {

        if (amounts.size() != methods.size()) {
            throw new PaymentException("Split payment amounts and methods count mismatch.");
        }

        List<Payment> results = new ArrayList<>();
        for (int i = 0; i < amounts.size(); i++) {
            Payment p = processPayment(
                    pnr, amounts.get(i), methods.get(i),
                    cardNumbers.get(i), expiries.get(i), cvvs.get(i), cardHolders.get(i));
            results.add(p);
        }
        return results;
    }

    /**
     * Alternative Flow 3.0: Refund processing for a cancelled or downgraded
     * booking. Refund amount is expected to already be calculated per fare
     * rules by the caller.
     */
    public Payment processRefund(Payment originalPayment, BigDecimal refundAmount) throws PaymentException {
        if (originalPayment.getStatus() != PaymentStatus.APPROVED) {
            throw new PaymentException("Cannot refund a payment that was not successfully approved.");
        }
        if (refundAmount.compareTo(originalPayment.getAmount()) > 0) {
            throw new PaymentException("Refund amount cannot exceed the original payment amount.");
        }

        Payment refund = new Payment(originalPayment.getPnr(), refundAmount,
                originalPayment.getMethod(), originalPayment.getCardNumberMasked());
        refund.setStatus(PaymentStatus.REFUNDED);
        logAudit("Refund issued for original transaction " + originalPayment.getTransactionRef()
                + ", amount " + refundAmount);
        return refund;
    }

    /**
     * Exception 1.0: Invalid payment details validation.
     */
    private void validateCardDetails(PaymentMethod method, String cardNumber, String expiry,
                                      String cvv, String cardHolder) throws PaymentException {

        if (method == PaymentMethod.CREDIT_CARD || method == PaymentMethod.DEBIT_CARD) {
            String digitsOnly = cardNumber == null ? "" : cardNumber.replaceAll("\\s+", "");

            if (!CARD_NUMBER_PATTERN.matcher(digitsOnly).matches()) {
                throw new PaymentException("Invalid card number.");
            }
            if (expiry == null || !EXPIRY_PATTERN.matcher(expiry).matches()) {
                throw new PaymentException("Invalid expiry date.");
            }
            if (cvv == null || !CVV_PATTERN.matcher(cvv).matches()) {
                throw new PaymentException("Invalid CVV.");
            }
            if (cardHolder == null || cardHolder.trim().isEmpty()) {
                throw new PaymentException("Cardholder name is required.");
            }
        }
    }

    private void logAudit(String entry) {
        auditLog.add(entry);
    }

    public List<String> getAuditLog() {
        return auditLog;
    }
}

/**
 * Simulates routing a transaction to the payment gateway and bank/card
 * issuer for authorization (Normal Flow steps 4-5).
 * Replace this simulation with a real gateway SDK/API integration
 * (e.g. Stripe, Braintree, a bank's own API) in production.
 */
class PaymentGateway {

    private final Random random = new Random();

    /**
     * Attempts to authorize a transaction.
     * Returns one of: APPROVED, DECLINED, PENDING (gateway timeout - Exception 4.0)
     */
    public GatewayResponse authorize(Payment payment) {
        // Simulate a gateway timeout roughly 5% of the time (Exception 4.0)
        if (random.nextInt(100) < 5) {
            return new GatewayResponse(PaymentStatus.PENDING, "Gateway timeout - status pending verification");
        }

        // Simulate a decline roughly 10% of the time (Exception 2.0)
        if (random.nextInt(100) < 10) {
            return new GatewayResponse(PaymentStatus.DECLINED, "Insufficient funds");
        }

        return new GatewayResponse(PaymentStatus.APPROVED, "Transaction authorized");
    }

    public static class GatewayResponse {
        private final PaymentStatus status;
        private final String message;

        public GatewayResponse(PaymentStatus status, String message) {
            this.status = status;
            this.message = message;
        }

        public PaymentStatus getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }
    }
}

/**
 * Screens an authorized transaction for fraud risk before it is finalized
 * (Normal Flow step 6, Exception 3.0: Fraud check failure).
 */
class FraudDetectionService {

    private static final BigDecimal HIGH_RISK_THRESHOLD = new BigDecimal("2000.00");
    private final Random random = new Random();

    /**
     * Returns true if the transaction is flagged as high-risk and must be
     * routed for manual review instead of being finalized.
     */
    public boolean isHighRisk(Payment payment) {
        // Simple simulated rule: large amounts are more likely to be flagged
        if (payment.getAmount().compareTo(HIGH_RISK_THRESHOLD) > 0) {
            return random.nextInt(100) < 30; // 30% chance for large transactions
        }
        return random.nextInt(100) < 2; // 2% baseline chance
    }
}
