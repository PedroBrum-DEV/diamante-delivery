package com.diamante.delivery.orderservice.dish;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DishRepository extends JpaRepository<Dish, Long> {

    /**
     * SELECT ... FOR UPDATE. Concurrent transactions wait here until the one holding
     * the row lock commits or rolls back, which prevents the lost-update race on stock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Dish d where d.id = :id")
    Optional<Dish> findByIdForUpdate(@Param("id") Long id);
}
