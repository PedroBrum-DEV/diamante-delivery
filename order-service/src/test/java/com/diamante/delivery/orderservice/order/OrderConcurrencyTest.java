package com.diamante.delivery.orderservice.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.diamante.delivery.orderservice.dish.DishRepository;
import com.diamante.delivery.orderservice.error.OutOfStockException;
import com.diamante.delivery.orderservice.error.PaymentUnavailableException;
import com.diamante.delivery.orderservice.payment.PaymentClient;
import com.diamante.delivery.orderservice.payment.PaymentResponse;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
class OrderConcurrencyTest {

    private static final long PROMO_DISH_ID = 1L; // House Burger, stock 10

    @Autowired
    OrderService orderService;

    @Autowired
    DishRepository dishRepository;

    @Autowired
    CustomerOrderRepository orderRepository;

    @MockitoBean
    PaymentClient paymentClient;

    @Test
    void fiftyConcurrentOrdersConfirmExactlyTen() throws Exception {
        when(paymentClient.charge(any(BigDecimal.class))).thenReturn(new PaymentResponse("APPROVED", 8081));
        assertThat(dishRepository.findById(PROMO_DISH_ID).orElseThrow().getStock()).isEqualTo(10);
        long ordersBefore = orderRepository.count();

        int requests = 50;
        ExecutorService pool = Executors.newFixedThreadPool(requests);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger confirmed = new AtomicInteger();
        AtomicInteger outOfStock = new AtomicInteger();

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < requests; i++) {
            futures.add(pool.submit(() -> {
                startGate.await();
                try {
                    orderService.createOrder(new CreateOrderRequest(PROMO_DISH_ID, 1));
                    confirmed.incrementAndGet();
                } catch (OutOfStockException e) {
                    outOfStock.incrementAndGet();
                }
                return null;
            }));
        }
        startGate.countDown();
        for (Future<?> future : futures) {
            future.get();
        }
        pool.shutdown();

        assertThat(confirmed.get()).isEqualTo(10);
        assertThat(outOfStock.get()).isEqualTo(40);
        assertThat(dishRepository.findById(PROMO_DISH_ID).orElseThrow().getStock()).isZero();
        assertThat(orderRepository.count() - ordersBefore).isEqualTo(10);
    }

    @Test
    void failedPaymentRollsBackAndKeepsStockIntact() {
        long dishId = 2L; // Margherita Pizza
        int stockBefore = dishRepository.findById(dishId).orElseThrow().getStock();
        long ordersBefore = orderRepository.count();
        when(paymentClient.charge(any(BigDecimal.class))).thenThrow(new IllegalStateException("payment down"));

        assertThatThrownBy(() -> orderService.createOrder(new CreateOrderRequest(dishId, 3)))
                .isInstanceOf(PaymentUnavailableException.class);

        assertThat(dishRepository.findById(dishId).orElseThrow().getStock()).isEqualTo(stockBefore);
        assertThat(orderRepository.count()).isEqualTo(ordersBefore);
    }
}
