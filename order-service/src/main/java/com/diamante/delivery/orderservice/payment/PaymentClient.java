package com.diamante.delivery.orderservice.payment;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * Calls the payment-service by its Eureka name. The load-balanced RestTemplate resolves
 * PAYMENT-SERVICE to one of the registered instances (8081 / 8082), alternating between them.
 * <p>
 * This is a separate bean on purpose: @Retryable works through a Spring proxy, so the call
 * must come from another bean (a self-invocation would bypass the proxy and never retry).
 * <p>
 * With maxRetries = 4 the method runs at most 5 times. Delays grow exponentially
 * (200ms, 400ms, 800ms, 1600ms), each with up to 100ms of random jitter, capped at 2s.
 */
@Component
public class PaymentClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentClient.class);

    private static final String PAYMENT_URL = "http://PAYMENT-SERVICE/payments";

    private final RestTemplate restTemplate;

    public PaymentClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Retryable(
            includes = {HttpServerErrorException.class, ResourceAccessException.class, IllegalStateException.class},
            maxRetries = 4,
            delay = 200,
            multiplier = 2,
            jitter = 100,
            maxDelay = 2000)
    public PaymentResponse charge(BigDecimal amount) {
        log.info("Calling PAYMENT-SERVICE for amount {}", amount);
        try {
            ResponseEntity<PaymentResponse> response =
                    restTemplate.postForEntity(PAYMENT_URL, new PaymentRequest(amount), PaymentResponse.class);
            PaymentResponse body = response.getBody();
            if (body == null) {
                throw new IllegalStateException("Empty response from payment-service");
            }
            log.info("Payment {} by payment-service instance {}", body.status(), body.instance());
            return body;
        } catch (HttpServerErrorException e) {
            // The 500 body carries the instance that failed, which makes the alternation visible in the logs.
            log.warn("Payment attempt failed ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }
}
