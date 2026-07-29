package com.airline.payment;

import java.math.BigDecimal;

/**
 * Demo entry point showing the Payment System use case Normal Flow.
 * Run with: java com.airline.payment.Main
 */
public class Main {
    public static void main(String[] args) {
        PaymentService paymentService = new PaymentService(new PaymentGateway(), new FraudDetectionService());

        try {
            Payment payment = paymentService.processPayment(
                    "AR12345",
                    new BigDecimal("248.50"),
                    PaymentMethod.CREDIT_CARD,
                    "4111111111111111",
                    "09/28",
                    "123",
                    "Bill Gates"
            );

            System.out.println(payment);

            if (payment.getStatus() == PaymentStatus.APPROVED) {
                Receipt receipt = new Receipt(payment);
                System.out.println(receipt.toPrintableText());
            } else if (payment.getStatus() == PaymentStatus.DECLINED) {
                System.out.println("Payment declined: " + payment.getDeclineReason());
            } else if (payment.getStatus() == PaymentStatus.FRAUD_REVIEW) {
                System.out.println("Payment held for manual fraud review. Ticket not yet issued.");
            } else if (payment.getStatus() == PaymentStatus.PENDING) {
                System.out.println("Payment gateway timed out. Status pending verification.");
            }

            System.out.println("\nAudit log:");
            paymentService.getAuditLog().forEach(System.out::println);

        } catch (PaymentException e) {
            System.out.println("Payment failed validation: " + e.getMessage());
        }
    }
}
