package com.diamante.delivery.reviewservice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * In-memory accumulator: the backpressure mechanism. Thousands of reviews per second
 * only update counters in memory; the database receives ONE write per dish every flush.
 */
@Component
public class ReviewBuffer {

    /** Sum of ratings and number of reviews of one dish. Immutable, merged atomically per key. */
    public record Accumulator(String dishName, long ratingSum, long count) {

        Accumulator plus(Accumulator other) {
            return new Accumulator(other.dishName, ratingSum + other.ratingSum, count + other.count);
        }
    }

    private final ConcurrentHashMap<Long, Accumulator> buffer = new ConcurrentHashMap<>();

    public void add(ReviewMessage message) {
        buffer.merge(message.dishId(),
                new Accumulator(message.dishName(), message.rating(), 1),
                Accumulator::plus);
    }

    /**
     * Removes and returns everything accumulated so far. Each key is removed atomically,
     * so a review arriving while draining either lands in the returned snapshot or in a
     * fresh entry for the next flush. Nothing is lost or counted twice.
     */
    public Map<Long, Accumulator> drain() {
        Map<Long, Accumulator> snapshot = new HashMap<>();
        for (Long dishId : List.copyOf(buffer.keySet())) {
            Accumulator accumulator = buffer.remove(dishId);
            if (accumulator != null) {
                snapshot.put(dishId, accumulator);
            }
        }
        return snapshot;
    }

    /** Puts a drained snapshot back (used when persisting it failed). */
    public void restore(Map<Long, Accumulator> snapshot) {
        snapshot.forEach((dishId, accumulator) -> buffer.merge(dishId, accumulator, Accumulator::plus));
    }

    public int size() {
        return buffer.size();
    }
}
