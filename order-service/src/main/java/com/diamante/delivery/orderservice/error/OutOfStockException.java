package com.diamante.delivery.orderservice.error;

public class OutOfStockException extends RuntimeException {

    public OutOfStockException() {
        super("Dish out of stock");
    }
}
