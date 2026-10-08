package com.diamante.delivery.orderservice.payment;

import java.math.BigDecimal;

public record PaymentRequest(BigDecimal amount) {
}
