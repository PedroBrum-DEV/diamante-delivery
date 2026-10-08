package com.diamante.delivery.orderservice.review;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.diamante.delivery.orderservice.config.RabbitConfig;

@Component
public class ReviewPublisher {

    private final RabbitTemplate rabbitTemplate;

    public ReviewPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(ReviewMessage message) {
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, message);
    }
}
