package com.devguirra.dscommerce.services;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.devguirra.dscommerce.dto.OrderDTO;
import com.devguirra.dscommerce.dto.OrderItemDTO;
import com.devguirra.dscommerce.entities.Order;
import com.devguirra.dscommerce.entities.OrderItem;
import com.devguirra.dscommerce.entities.OrderStatus;
import com.devguirra.dscommerce.entities.Product;
import com.devguirra.dscommerce.entities.User;
import com.devguirra.dscommerce.repositories.OrderItemRepository;
import com.devguirra.dscommerce.repositories.OrderRepository;
import com.devguirra.dscommerce.repositories.ProductRepository;

import org.springframework.transaction.annotation.Transactional;
import com.devguirra.dscommerce.services.exceptions.ResourceNotFoundException;

@Service
public class OrderService {

    @Autowired
    private OrderRepository repository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private AuthService authService;

    @Transactional(readOnly = true)
    public OrderDTO findById(Long id){
        Order order = repository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Recurso não encontrado"));
        authService.validateSelfOrAdmin(order.getClient().getId());
        return new OrderDTO(order);
    }

    @Transactional
    public OrderDTO insert(OrderDTO dto) {
       
        Order order = new Order();
        order.setMoment(Instant.now());
        order.setStatus(OrderStatus.WAITING_PAYMENT);

        User user = userService.authenticated();
        order.setClient(user);

        for(OrderItemDTO itemDto : dto.getItems()) {

            Product product = productRepository.getReferenceById(itemDto.getProductId());
            OrderItem item = new OrderItem(order, product, itemDto.getQuantity(), product.getPrice());
            order.getItems().add(item);
        }

        repository.save(order);
        orderItemRepository.saveAll(order.getItems());

        return new OrderDTO(order);

    }
    
}
