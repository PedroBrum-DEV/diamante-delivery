package com.diamante.delivery.paymentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Registered in Eureka as PAYMENT-SERVICE. Run two instances (ports 8081 and 8082)
 * so the order-service can load balance between them.
 */
@SpringBootApplication
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
