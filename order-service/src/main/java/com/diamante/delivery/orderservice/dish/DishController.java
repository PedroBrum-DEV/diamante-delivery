package com.diamante.delivery.orderservice.dish;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.diamante.delivery.orderservice.error.DishNotFoundException;

@RestController
@RequestMapping("/dishes")
public class DishController {

    private final DishRepository dishRepository;

    public DishController(DishRepository dishRepository) {
        this.dishRepository = dishRepository;
    }

    @GetMapping
    public List<Dish> findAll() {
        return dishRepository.findAll();
    }

    @GetMapping("/{id}")
    public Dish findById(@PathVariable Long id) {
        return dishRepository.findById(id).orElseThrow(() -> new DishNotFoundException(id));
    }
}
