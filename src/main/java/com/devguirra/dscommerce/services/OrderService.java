package com.devguirra.dscommerce.services;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.devguirra.dscommerce.dto.OrderDTO;
import com.devguirra.dscommerce.entities.Order;
import com.devguirra.dscommerce.repositories.OrderRepository;
import org.springframework.transaction.annotation.Transactional;
import com.devguirra.dscommerce.services.exceptions.ResourceNotFoundException;

@Service
public class OrderService {

    @Autowired
    private OrderRepository repository;

    @Transactional(readOnly = true)
    public OrderDTO findById(Long id){
        Order order = repository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Recurso não encontrado"));
        return new OrderDTO(order);
    }
    
}
