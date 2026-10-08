package com.diamante.delivery.orderservice.error;

public class DishNotFoundException extends RuntimeException {

    public DishNotFoundException(Long id) {
        super("Dish not found: " + id);
    }
}
