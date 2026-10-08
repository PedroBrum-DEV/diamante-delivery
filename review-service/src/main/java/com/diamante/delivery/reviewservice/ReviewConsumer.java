package com.diamante.delivery.reviewservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.diamante.delivery.reviewservice.config.RabbitConfig;

@Component
public class ReviewConsumer {

    private static final Logger log = LoggerFactory.getLogger(ReviewConsumer.class);

    private final ReviewBuffer buffer;

    public ReviewConsumer(ReviewBuffer buffer) {
        this.buffer = buffer;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void onReview(ReviewMessage message) {
        buffer.add(message);
        log.debug("Review buffered: dishId={} rating={}", message.dishId(), message.rating());
    }
}
