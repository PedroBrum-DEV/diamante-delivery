package com.diamante.delivery.orderservice.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.diamante.delivery.orderservice.dish.Dish;
import com.diamante.delivery.orderservice.dish.DishRepository;
import com.diamante.delivery.orderservice.error.DishNotFoundException;
import com.diamante.delivery.orderservice.error.OrderNotFoundException;
import com.diamante.delivery.orderservice.error.OutOfStockException;
import com.diamante.delivery.orderservice.error.PaymentUnavailableException;
import com.diamante.delivery.orderservice.payment.PaymentClient;
import com.diamante.delivery.orderservice.payment.PaymentResponse;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final DishRepository dishRepository;
    private final CustomerOrderRepository orderRepository;
    private final PaymentClient paymentClient;

    public OrderService(DishRepository dishRepository,
                        CustomerOrderRepository orderRepository,
                        PaymentClient paymentClient) {
        this.dishRepository = dishRepository;
        this.orderRepository = orderRepository;
        this.paymentClient = paymentClient;
    }

    /**
     * Reserves stock, charges the payment and confirms the order in ONE transaction.
     * <p>
     * The dish row is locked with PESSIMISTIC_WRITE, so concurrent orders for the same
     * dish are serialized: the stock can never go below zero. If the payment fails after
     * every retry, a RuntimeException leaves this method, the transaction is rolled back
     * and the stock decrement is discarded (stock intact, no order saved).
     */
    @Transactional
    public CustomerOrder createOrder(CreateOrderRequest request) {
        int quantity = request.quantity();

        Dish dish = dishRepository.findByIdForUpdate(request.dishId())
                .orElseThrow(() -> new DishNotFoundException(request.dishId()));

        if (dish.getStock() < quantity) {
            throw new OutOfStockException();
        }
        dish.decreaseStock(quantity);

        BigDecimal totalPrice = dish.getPrice().multiply(BigDecimal.valueOf(quantity));

        PaymentResponse payment;
        try {
            payment = paymentClient.charge(totalPrice);
        } catch (RuntimeException e) {
            log.error("Payment failed after all retries, order rolled back (dishId={}, quantity={}): {}",
                    dish.getId(), quantity, e.toString());
            throw new PaymentUnavailableException(e);
        }

        CustomerOrder order = orderRepository.save(new CustomerOrder(
                dish.getId(),
                quantity,
                totalPrice,
                OrderStatus.CONFIRMED,
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)));

        log.info("Order {} CONFIRMED: dishId={}, quantity={}, total={}, paid via instance {}",
                order.getId(), dish.getId(), quantity, totalPrice, payment.instance());
        return order;
    }

    @Transactional(readOnly = true)
    public CustomerOrder findById(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }
}
