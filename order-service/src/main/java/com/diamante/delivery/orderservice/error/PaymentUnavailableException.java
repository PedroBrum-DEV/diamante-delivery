package com.diamante.delivery.orderservice.error;

public class PaymentUnavailableException extends RuntimeException {

    public PaymentUnavailableException(Throwable cause) {
        super("Payment service unavailable, order not confirmed", cause);
    }
}
