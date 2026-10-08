package com.diamante.delivery.paymentservice;

/** {"status": "APPROVED", "instance": 8081} */
public record PaymentResponse(String status, int instance) {
}
