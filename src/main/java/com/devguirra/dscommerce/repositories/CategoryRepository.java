package com.devguirra.dscommerce.repositories;
import org.springframework.data.jpa.repository.JpaRepository;
import com.devguirra.dscommerce.entities.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    

}
