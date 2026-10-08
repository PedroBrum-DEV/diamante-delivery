package com.diamante.delivery.reviewservice;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReviewFlushJob {

    private static final Logger log = LoggerFactory.getLogger(ReviewFlushJob.class);

    private final ReviewBuffer buffer;
    private final ReviewFlushService flushService;

    public ReviewFlushJob(ReviewBuffer buffer, ReviewFlushService flushService) {
        this.buffer = buffer;
        this.flushService = flushService;
    }

    /** Every 5 seconds: write the accumulated totals to H2 and clear the buffer. */
    @Scheduled(fixedDelayString = "${reviews.flush-interval-ms:5000}")
    public void flush() {
        Map<Long, ReviewBuffer.Accumulator> snapshot = buffer.drain();
        if (snapshot.isEmpty()) {
            return;
        }
        try {
            flushService.persist(snapshot);
            long reviews = snapshot.values().stream().mapToLong(ReviewBuffer.Accumulator::count).sum();
            log.info("Flushed {} reviews of {} dishes to the database", reviews, snapshot.size());
        } catch (RuntimeException e) {
            // Do not lose data: put the snapshot back and try again on the next run.
            buffer.restore(snapshot);
            log.error("Flush failed, {} dishes kept in the buffer for the next run", snapshot.size(), e);
        }
    }
}
