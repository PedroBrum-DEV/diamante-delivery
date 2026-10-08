package com.diamante.delivery.paymentservice;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaymentRequest(@NotNull @Positive BigDecimal amount) {
}
