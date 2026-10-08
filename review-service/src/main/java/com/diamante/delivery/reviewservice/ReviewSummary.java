package com.diamante.delivery.reviewservice;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** One row per dish with the running totals needed to compute the average. */
@Entity
public class ReviewSummary {

    @Id
    private Long dishId;

    @Column(nullable = false)
    private String dishName;

    @Column(nullable = false)
    private long ratingSum;

    @Column(nullable = false)
    private long reviewCount;

    protected ReviewSummary() {
        // required by JPA
    }

    public ReviewSummary(Long dishId, String dishName) {
        this.dishId = dishId;
        this.dishName = dishName;
    }

    public void add(String dishName, long ratingSum, long count) {
        this.dishName = dishName;
        this.ratingSum += ratingSum;
        this.reviewCount += count;
    }

    public double average() {
        return reviewCount == 0 ? 0.0 : (double) ratingSum / reviewCount;
    }

    public Long getDishId() {
        return dishId;
    }

    public String getDishName() {
        return dishName;
    }

    public long getRatingSum() {
        return ratingSum;
    }

    public long getReviewCount() {
        return reviewCount;
    }
}
