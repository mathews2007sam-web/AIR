package com.airline.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * All data-model types for the Payment System use case (Use Case 3) live in
 * this one file: the enums, the exception, the Payment record itself, and
 * the Receipt generated on success. Only Payment is public; the rest are
 * package-private since they're only ever used alongside it.
 */
public class Payment {

    private final String transactionRef;
    private final String pnr;
    private final BigDecimal amount;
    private final PaymentMethod method;
    private final String cardNumberMasked;
    private PaymentStatus status;
    private final LocalDateTime createdAt;
    private String declineReason;

    public Payment(String pnr, BigDecimal amount, PaymentMethod method, String cardNumber) {
        this.transactionRef = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.pnr = pnr;
        this.amount = amount;
        this.method = method;
        this.cardNumberMasked = maskCardNumber(cardNumber);
        this.status = PaymentStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "N/A";
        }
        String digitsOnly = cardNumber.replaceAll("\\s+", "");
        return "**** **** **** " + digitsOnly.substring(digitsOnly.length() - 4);
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public String getPnr() {
        return pnr;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public String getCardNumberMasked() {
        return cardNumberMasked;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getDeclineReason() {
        return declineReason;
    }

    public void setDeclineReason(String declineReason) {
        this.declineReason = declineReason;
    }

    @Override
    public String toString() {
        return "Payment{" +
                "transactionRef='" + transactionRef + '\'' +
                ", pnr='" + pnr + '\'' +
                ", amount=" + amount +
                ", method=" + method +
                ", card='" + cardNumberMasked + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}

/**
 * How the Passenger is paying: card, wallet, net banking, or travel credit.
 */
enum PaymentMethod {
    CREDIT_CARD,
    DEBIT_CARD,
    DIGITAL_WALLET,
    NET_BANKING,
    TRAVEL_CREDIT
}

/**
 * Outcome of a payment transaction, matching the use case's Normal Flow,
 * Alternative Flows, and Exceptions.
 */
enum PaymentStatus {
    APPROVED,       // Normal flow: payment authorized and confirmed
    DECLINED,       // Exception 2.0: Payment declined by bank/card issuer
    FRAUD_REVIEW,   // Exception 3.0: Flagged as high-risk, held for manual review
    PENDING,        // Exception 4.0: Gateway timeout, awaiting status verification
    INVALID_DETAILS,// Exception 1.0: Invalid card number, expiry, CVV, or billing details
    REFUNDED        // Alternative Flow 3.0: Refund processed
}

/**
 * Thrown for validation or processing failures during a payment transaction
 * (e.g. Exception 1.0 Invalid payment details).
 */
class PaymentException extends Exception {
    public PaymentException(String message) {
        super(message);
    }
}

/**
 * Generated on successful payment. The Passenger shall be able to view and
 * download this receipt (Special Requirements).
 */
class Receipt {

    private final String receiptId;
    private final Payment payment;
    private final LocalDateTime issuedAt;

    public Receipt(Payment payment) {
        this.receiptId = "RCPT-" + payment.getTransactionRef();
        this.payment = payment;
        this.issuedAt = LocalDateTime.now();
    }

    public String getReceiptId() {
        return receiptId;
    }

    public Payment getPayment() {
        return payment;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public String toPrintableText() {
        return "----- PAYMENT RECEIPT -----\n" +
                "Receipt ID: " + receiptId + "\n" +
                "PNR: " + payment.getPnr() + "\n" +
                "Transaction Ref: " + payment.getTransactionRef() + "\n" +
                "Amount: $" + payment.getAmount() + "\n" +
                "Method: " + payment.getMethod() + "\n" +
                "Card: " + payment.getCardNumberMasked() + "\n" +
                "Status: " + payment.getStatus() + "\n" +
                "Issued At: " + issuedAt + "\n" +
                "----------------------------";
    }
}
