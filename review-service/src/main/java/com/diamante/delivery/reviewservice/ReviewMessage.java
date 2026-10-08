package com.diamante.delivery.reviewservice;

/** Message published by the order-service: {"dishId":1,"dishName":"House Burger","rating":5,"comment":"Great"} */
public record ReviewMessage(Long dishId, String dishName, int rating, String comment) {
}
