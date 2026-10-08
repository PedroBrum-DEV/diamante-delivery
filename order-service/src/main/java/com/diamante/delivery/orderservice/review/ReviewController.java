package com.diamante.delivery.orderservice.review;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.diamante.delivery.orderservice.dish.Dish;
import com.diamante.delivery.orderservice.dish.DishRepository;
import com.diamante.delivery.orderservice.error.DishNotFoundException;

/**
 * Producer side of the review flow. Nothing is written to the database here:
 * the review is published to RabbitMQ and the request is answered with 202 Accepted.
 */
@RestController
@RequestMapping("/reviews")
public class ReviewController {

    private static final Logger log = LoggerFactory.getLogger(ReviewController.class);

    private final DishRepository dishRepository;
    private final ReviewPublisher publisher;

    public ReviewController(DishRepository dishRepository, ReviewPublisher publisher) {
        this.dishRepository = dishRepository;
        this.publisher = publisher;
    }

    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody ReviewRequest request) {
        Dish dish = dishRepository.findById(request.dishId())
                .orElseThrow(() -> new DishNotFoundException(request.dishId()));

        publisher.publish(new ReviewMessage(dish.getId(), dish.getName(), request.rating(), request.comment()));
        log.debug("Review published for dishId={} rating={}", dish.getId(), request.rating());

        return ResponseEntity.accepted().build();
    }
}
