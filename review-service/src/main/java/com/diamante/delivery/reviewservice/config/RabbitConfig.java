package com.diamante.delivery.reviewservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Same topology as the order-service (declaring is idempotent), so whichever service
 * starts first creates the exchange, the durable queue and the binding.
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "delivery.exchange";
    public static final String QUEUE = "reviews.queue";
    public static final String ROUTING_KEY = "reviews.new";

    @Bean
    public TopicExchange deliveryExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue reviewsQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    public Binding reviewsBinding(Queue reviewsQueue, TopicExchange deliveryExchange) {
        return BindingBuilder.bind(reviewsQueue).to(deliveryExchange).with(ROUTING_KEY);
    }

    /**
     * JSON converter. The listener method parameter type (ReviewMessage) is used to
     * deserialize, so the producer's class does not need to exist in this service.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter("*");
    }
}
