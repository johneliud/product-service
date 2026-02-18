package io.github.johneliud.product_service.repositories;

import io.github.johneliud.product_service.models.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    List<Product> findByUserId(String userId);
    Page<Product> findByUserId(String userId, Pageable pageable);
    Page<Product> findByUserIdAndNameContainingIgnoreCase(String userId, String name, Pageable pageable);
    Page<Product> findByUserIdAndPriceBetween(String userId, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
    Page<Product> findByUserIdAndNameContainingIgnoreCaseAndPriceBetween(String userId, String name, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
    
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Page<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
    Page<Product> findByNameContainingIgnoreCaseAndPriceBetween(String name, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
}
