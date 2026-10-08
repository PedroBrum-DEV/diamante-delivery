package com.diamante.delivery.orderservice.payment;

/** {"status": "APPROVED", "instance": 8081} */
public record PaymentResponse(String status, int instance) {
}
