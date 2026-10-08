package com.diamante.delivery.reviewservice.ranking;

/** {"dishId": 1, "dishName": "House Burger", "average": 4.6, "count": 128} */
public record RankingItem(Long dishId, String dishName, double average, long count) {
}
