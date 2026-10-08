package com.diamante.delivery.paymentservice;

import java.util.Map;
import java.util.Random;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    /** Probability of a simulated failure (about 50% of the calls). */
    private static final double FAILURE_RATE = 0.5;

    private final Random random = new Random();
    private final int port;

    public PaymentController(@Value("${server.port}") int port) {
        this.port = port;
    }

    @PostMapping
    public ResponseEntity<?> pay(@Valid @RequestBody PaymentRequest request) {
        if (random.nextDouble() < FAILURE_RATE) {
            log.warn("[payment-service:{}] simulated FAILURE for amount {}", port, request.amount());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Simulated payment failure", "instance", port));
        }
        log.info("[payment-service:{}] APPROVED amount {}", port, request.amount());
        return ResponseEntity.ok(new PaymentResponse("APPROVED", port));
    }
}
