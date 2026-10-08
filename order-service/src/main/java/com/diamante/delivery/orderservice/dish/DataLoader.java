package com.diamante.delivery.orderservice.dish;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds the menu. The first dish (id 1) is the flash promotion and has only 10 units.
 */
@Component
public class DataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private final DishRepository dishRepository;

    public DataLoader(DishRepository dishRepository) {
        this.dishRepository = dishRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (dishRepository.count() > 0) {
            return;
        }
        dishRepository.saveAll(List.of(
                new Dish("House Burger",
                        "Brioche bun, beef patty, cheddar and caramelized onions (flash promotion)",
                        new BigDecimal("39.90"), 10),
                new Dish("Margherita Pizza",
                        "Tomato sauce, mozzarella and fresh basil (vegetarian)",
                        new BigDecimal("44.90"), 50),
                new Dish("Caesar Salad",
                        "Romaine lettuce, grilled chicken, parmesan and croutons",
                        new BigDecimal("32.50"), 40),
                new Dish("Veggie Bowl",
                        "Quinoa, roasted vegetables, chickpeas and tahini (vegan)",
                        new BigDecimal("36.00"), 30),
                new Dish("Chocolate Brownie",
                        "Warm brownie with vanilla ice cream (vegetarian)",
                        new BigDecimal("18.90"), 60),
                new Dish("Mint Lemonade",
                        "Fresh squeezed lemon with mint (vegan)",
                        new BigDecimal("9.90"), 100)));
        log.info("Menu loaded with {} dishes", dishRepository.count());
    }
}
