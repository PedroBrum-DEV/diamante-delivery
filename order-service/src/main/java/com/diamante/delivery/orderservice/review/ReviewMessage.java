package com.diamante.delivery.orderservice.review;

/** JSON sent to RabbitMQ: {"dishId":1,"dishName":"House Burger","rating":5,"comment":"Great"} */
public record ReviewMessage(Long dishId, String dishName, int rating, String comment) {
}
