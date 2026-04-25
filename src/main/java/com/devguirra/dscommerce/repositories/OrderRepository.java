package com.devguirra.dscommerce.repositories;
import org.springframework.data.jpa.repository.JpaRepository;
import com.devguirra.dscommerce.entities.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

   
}
