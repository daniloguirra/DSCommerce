package com.devguirra.dscommerce.repositories;
import org.springframework.data.jpa.repository.JpaRepository;
import com.devguirra.dscommerce.entities.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, OrderItemRepository> {

   
}
