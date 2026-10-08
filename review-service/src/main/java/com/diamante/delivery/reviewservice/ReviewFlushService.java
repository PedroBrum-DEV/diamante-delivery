package com.diamante.delivery.reviewservice;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewFlushService {

    private final ReviewSummaryRepository repository;

    public ReviewFlushService(ReviewSummaryRepository repository) {
        this.repository = repository;
    }

    /** Merges the accumulated totals into the database, one row per dish, in one transaction. */
    @Transactional
    public void persist(Map<Long, ReviewBuffer.Accumulator> snapshot) {
        snapshot.forEach((dishId, accumulator) -> {
            ReviewSummary summary = repository.findById(dishId)
                    .orElseGet(() -> new ReviewSummary(dishId, accumulator.dishName()));
            summary.add(accumulator.dishName(), accumulator.ratingSum(), accumulator.count());
            repository.save(summary);
        });
    }
}
